package monoton.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ColorSetting;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.utils.font.Fonts;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.ProjectionUtils;
import monoton.utils.render.RenderUtilka;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.joml.Vector2d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;

import static monoton.module.impl.render.Hud.drawItemStack;
import static monoton.utils.render.ColorUtils.rgba;

@Annotation(name = "Predictions", type = TypeList.Render, desc = "Показывает траекторию падения снарядов")
public class Predictions extends Module {

    public MultiBoxSetting projectiles = new MultiBoxSetting("Предсказывать для",
            new BooleanOption("Эндер Жемчуг", true),
            new BooleanOption("Стрела", true),
            new BooleanOption("Предметы", true),
            new BooleanOption("Трезубец", true));

    public final ColorSetting color = new ColorSetting("Цвет", ColorUtils.rgba(245, 66, 72, 255));

    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.0");

    public Predictions() {
        addSettings(projectiles, color);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender render) {
            if (render.isRender3D()) {
                renderTrajectories3D(render);
            }
            if (render.isRender2D()) {
                renderTrajectories2D(render.matrixStack);
            }
        }
        return false;
    }

    private void renderTrajectories2D(MatrixStack e) {
        int itemCount = 0;
        for (Entity entity : mc.world.getAllEntities()) {
            // Предметы в воде, лаве или на земле — пропускаем
            if (entity instanceof ItemEntity && (entity.isInWater() || entity.isInLava() || entity.isOnGround())) {
                continue;
            }

            if (entity instanceof ItemEntity && projectiles.get("Предметы")) {
                itemCount++;
                if (itemCount > 33) {
                    return;
                }
            }

            if (((entity instanceof EnderPearlEntity && projectiles.get("Эндер Жемчуг")) ||
                    (entity instanceof ArrowEntity && projectiles.get("Стрела")) ||
                    (entity instanceof ItemEntity && projectiles.get("Предметы")) ||
                    (entity instanceof TridentEntity && projectiles.get("Трезубец"))) &&
                    (entity.prevPosY != entity.getPosY() || entity.prevPosX != entity.getPosX() || entity.prevPosZ != entity.getPosZ())) {
                Vector3d entityPosition = entity.getPositionVec();
                Vector3d entityMotion = entity.getMotion();
                Vector3d lastPosition = entityPosition;
                Vector2d finalProjectedPosition = null;
                int steps = 0;

                for (int i = 0; i <= 300; i++) {
                    steps++;
                    lastPosition = entityPosition;
                    entityPosition = entityPosition.add(entityMotion);
                    {
                        Vector3d motionUpdated = entityMotion;
                        if ((entity.isInWater() || mc.world.getBlockState(new BlockPos(entityPosition)).getBlock() == Blocks.WATER) &&
                                !(entity instanceof TridentEntity)) {
                            float scale = entity instanceof EnderPearlEntity ? 0.8f :
                                    entity instanceof ItemEntity ? 0.8f : 0.6f;
                            motionUpdated = motionUpdated.scale(scale);
                        } else {
                            motionUpdated = motionUpdated.scale(0.99f);
                        }
                        if (!entity.hasNoGravity()) {
                            motionUpdated.y -= entity instanceof EnderPearlEntity ? 0.03 :
                                    entity instanceof ItemEntity ? 0.04 : 0.05;
                        }
                        entityMotion = motionUpdated;
                    }

                    final RayTraceContext rayTraceContext = new RayTraceContext(lastPosition, entityPosition,
                            RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
                    final BlockRayTraceResult blockHitResult = mc.world.rayTraceBlocks(rayTraceContext);
                    if (blockHitResult.getType() == RayTraceResult.Type.BLOCK || entityPosition.y <= 0) {
                        finalProjectedPosition = ProjectionUtils.project(lastPosition.x, lastPosition.y, lastPosition.z);
                        break;
                    }
                }

                if (finalProjectedPosition == null) {
                    finalProjectedPosition = ProjectionUtils.project(lastPosition.x, lastPosition.y, lastPosition.z);
                }

                if (finalProjectedPosition == null || finalProjectedPosition.x == Float.MAX_VALUE || finalProjectedPosition.y == Float.MAX_VALUE) {
                    continue;
                }

                float timeInSeconds = steps * 0.05f;
                float x = (float) finalProjectedPosition.x;
                float y = (float) finalProjectedPosition.y + 3;

                String timeText = TIME_FORMAT.format(timeInSeconds) + " сек";
                float timeWidth = Fonts.intl[11].getWidth(timeText);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                if (entity instanceof ItemEntity) {
                    RenderUtilka.Render2D.drawRoundedRect(x - 12.5f, y - 2, timeWidth + 5f, 8.5f, 0, rgba(0, 0, 0, 128));
                    Fonts.intl[11].drawString(e, timeText, x - (timeWidth / 2), y + 1.5f, -1);

                    ITextComponent displayName = ((ItemEntity) entity).getItem().getDisplayName();
                    String itemName = ((ItemEntity) entity).getItem().getDisplayName().getString();
                    float itemNameWidth = Fonts.intl[11].getWidth(itemName);
                    RenderUtilka.Render2D.drawRoundedRect(x - (itemNameWidth / 2) - 2.5f, y - 2 + 8f, itemNameWidth + 5f, 8.5f, 0, rgba(0, 0, 0, 128));
                    Fonts.intl[11].drawText(e, displayName, x - (itemNameWidth / 2), y + 1.5f + 8f);
                } else {
                    RenderUtilka.Render2D.drawRoundedRect(x - 17f, y - 2, timeWidth + 12.5f, 8.5f, 0, rgba(0, 0, 0, 128));
                    drawItemStack(getProjectileItemStack(entity), x - 16f, y - 1.5f, 0.45f, null, true);
                    Fonts.intl[11].drawString(e, timeText, x - (timeWidth / 2) + 3.5f, y + 1.5f, -1);
                }
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
        }
    }

    private ItemStack getProjectileItemStack(Entity entity) {
        if (entity instanceof EnderPearlEntity) {
            return new ItemStack(Items.ENDER_PEARL);
        } else if (entity instanceof ArrowEntity) {
            return new ItemStack(Items.ARROW);
        } else if (entity instanceof ItemEntity) {
            return ((ItemEntity) entity).getItem();
        } else if (entity instanceof TridentEntity) {
            return new ItemStack(Items.TRIDENT);
        }
        return ItemStack.EMPTY;
    }

    private void renderTrajectories3D(EventRender e) {
        MatrixStack matrix = new MatrixStack();
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrix.getLast().getMatrix());
        RenderSystem.translated(-mc.getRenderManager().renderPosX(), -mc.getRenderManager().renderPosY(), -mc.getRenderManager().renderPosZ());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(1.5F);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        BUFFER.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        int itemCount = 0;
        for (Entity entity : mc.world.getAllEntities()) {
            // Предметы в воде, лаве или на земле — пропускаем
            if (entity instanceof ItemEntity && (entity.isInWater() || entity.isInLava() || entity.isOnGround())) {
                continue;
            }

            if (entity instanceof ItemEntity && projectiles.get("Предметы")) {
                itemCount++;
                if (itemCount > 33) {
                    TESSELLATOR.draw();
                    RenderSystem.enableDepthTest();
                    RenderSystem.enableTexture();
                    RenderSystem.disableBlend();
                    GL11.glDisable(GL11.GL_LINE_SMOOTH);
                    RenderSystem.translated(mc.getRenderManager().renderPosX(), mc.getRenderManager().renderPosY(), mc.getRenderManager().renderPosZ());
                    RenderSystem.popMatrix();
                    return;
                }
            }

            if (((entity instanceof EnderPearlEntity && projectiles.get("Эндер Жемчуг")) ||
                    (entity instanceof ArrowEntity && projectiles.get("Стрела")) ||
                    (entity instanceof ItemEntity && projectiles.get("Предметы")) ||
                    (entity instanceof TridentEntity && projectiles.get("Трезубец"))) &&
                    (entity.prevPosY != entity.getPosY() || entity.prevPosX != entity.getPosX() || entity.prevPosZ != entity.getPosZ())) {
                renderLine(entity);
            }
        }
        TESSELLATOR.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.translated(mc.getRenderManager().renderPosX(), mc.getRenderManager().renderPosY(), mc.getRenderManager().renderPosZ());
        RenderSystem.popMatrix();
    }

    private void renderLine(Entity entity) {
        Vector3d entityPosition = entity.getPositionVec().add(0, 0, 0);
        Vector3d entityMotion = entity.getMotion();
        Vector3d lastPosition;
        final int fadeSteps = 4;

        for (int i = 0; i <= 300; i++) {
            lastPosition = entityPosition;
            entityPosition = entityPosition.add(entityMotion);
            {
                Vector3d motionUpdated = entityMotion;
                if ((entity.isInWater() || mc.world.getBlockState(new BlockPos(lastPosition)).getBlock() == Blocks.WATER) &&
                        !(entity instanceof TridentEntity)) {
                    float scale = entity instanceof EnderPearlEntity ? 0.8f :
                            entity instanceof ItemEntity ? 0.8f : 0.6f;
                    motionUpdated = motionUpdated.scale(scale);
                } else {
                    motionUpdated = motionUpdated.scale(0.99f);
                }
                if (!entity.hasNoGravity()) {
                    motionUpdated.y -= entity instanceof EnderPearlEntity ? 0.03 :
                            entity instanceof ItemEntity ? 0.04 : 0.05;
                }
                entityMotion = motionUpdated;
            }

            final RayTraceContext rayTraceContext = new RayTraceContext(lastPosition, entityPosition,
                    RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
            final BlockRayTraceResult blockHitResult = mc.world.rayTraceBlocks(rayTraceContext);
            if (blockHitResult.getType() == RayTraceResult.Type.BLOCK || entityPosition.y <= 0) {
                break;
            }

            Color baseColor = Manager.FUNCTION_MANAGER.predictions.color.getColor();
            Color darkerColor = baseColor.darker();
            int color = ColorUtils.gradient(3, i * 8, baseColor, darkerColor, 0.5f);

            int alpha = 200;
            if (i < fadeSteps) {
                alpha = (int) ((i / (float) fadeSteps) * 255);
            }

            int red = (color >> 16) & 0xFF;
            int green = (color >> 8) & 0xFF;
            int blue = color & 0xFF;
            int newColor = (alpha << 24) | (red << 16) | (green << 8) | blue;

            BUFFER.pos(lastPosition.x, lastPosition.y, lastPosition.z).color(newColor).endVertex();
            BUFFER.pos(entityPosition.x, entityPosition.y, entityPosition.z).color(newColor).endVertex();
        }
    }
}