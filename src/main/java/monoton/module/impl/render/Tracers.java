package monoton.module.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ColorSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.render.ColorUtils;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;

@Annotation(name = "Tracers", type = TypeList.Render)
public class Tracers extends Module {
    public static BooleanOption onlyArmor = new BooleanOption("Только в незеритовом сете", false);
    public SliderSetting width = new SliderSetting("Ширина", 1, 0.1f, 2, 0.1f);
    private final ColorSetting color = new ColorSetting("Цвет", ColorUtils.rgba(255, 255, 255, 255));
    private final ColorSetting colorfriend = new ColorSetting("Цвет друзей", ColorUtils.rgba(104, 255, 142, 255));

    public Tracers() {
        addSettings(onlyArmor, width, color, colorfriend);
    }

    private boolean hasFullNetheriteArmor(PlayerEntity player) {
        return player.inventory.armorInventory.get(0).getItem() == Items.NETHERITE_BOOTS &&
                player.inventory.armorInventory.get(1).getItem() == Items.NETHERITE_LEGGINGS &&
                player.inventory.armorInventory.get(2).getItem() == Items.NETHERITE_CHESTPLATE &&
                player.inventory.armorInventory.get(3).getItem() == Items.NETHERITE_HELMET;
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender eventRender) {
            if (eventRender.isRender3D()) {
                GL11.glPushMatrix();
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glLineWidth(width.getValue().floatValue());
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

                Vector3d vec = new Vector3d(0, 0, 150)
                        .rotatePitch((float) -(Math.toRadians(mc.player.rotationPitch)))
                        .rotateYaw((float) -Math.toRadians(mc.player.rotationYaw));

                BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

                for (PlayerEntity entity : mc.world.getPlayers()) {
                    if (entity instanceof RemoteClientPlayerEntity && entity.botEntity && mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON) {
                        boolean isFriend = Manager.FRIEND_MANAGER.isFriend(entity.getName().getString());
                        if (onlyArmor.getValue() && !isFriend && !hasFullNetheriteArmor(entity)) {
                            continue;
                        }

                        int tracersColor = isFriend ? colorfriend.get() : color.get();

                        double x = entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * mc.getRenderPartialTicks() - mc.getRenderManager().info.getProjectedView().getX();
                        double y = entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * mc.getRenderPartialTicks() - mc.getRenderManager().info.getProjectedView().getY();
                        double z = entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * mc.getRenderPartialTicks() - mc.getRenderManager().info.getProjectedView().getZ();

                        ColorUtils.setColor(tracersColor);

                        bufferBuilder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
                        bufferBuilder.pos(vec.x, vec.y, vec.z).endVertex();
                        bufferBuilder.pos(x, y, z).endVertex();
                        Tessellator.getInstance().draw();
                    }
                }
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
                GL11.glDisable(GL11.GL_LINE_SMOOTH);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(true);
                GL11.glDisable(GL11.GL_BLEND);
                GlStateManager.color4f(1, 1, 1, 1);
                GL11.glPopMatrix();
            }
        }
        return false;
    }
}