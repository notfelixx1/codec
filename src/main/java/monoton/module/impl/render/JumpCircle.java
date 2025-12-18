package monoton.module.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventJump;
import monoton.control.events.render.EventRender;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.TypeList;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.render.RenderUtilka;
import monoton.utils.render.animation.AnimationMath;

import java.util.concurrent.CopyOnWriteArrayList;

import static monoton.ui.clickgui.Panel.selectedColor;
import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.*;
import static org.lwjgl.opengl.GL11.*;

@Annotation(name = "JumpCircle", type = TypeList.Render, desc = "Круг в месте прыжка")
public class JumpCircle extends Module {
    private final CopyOnWriteArrayList<Circle> circles = new CopyOnWriteArrayList<>();
    private final SliderSetting speed = new SliderSetting("Скорость", 1, 1, 5, 0.1f);
    private final SliderSetting fadeSpeed = new SliderSetting("Скорость исчезновения", 1, 1f, 5, 0.5f);

    public JumpCircle() {
        addSettings(speed, fadeSpeed);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventJump) {
            addCircle();
        } else if (event instanceof EventRender render && render.isRender3D()) {
            updateCircles();
            renderCircles();
        }
        return false;
    }

    private void addCircle() {
        if (circles.size() >= 15) {
            circles.remove(0);
        }
        circles.add(new Circle((float) mc.player.getPosX(), (float) mc.player.getPosY(), (float) mc.player.getPosZ()));
    }

    private void updateCircles() {
        circles.forEach(circle -> {
            circle.factor = AnimationMath.fast(circle.factor, 1, speed.getValue().floatValue());
            circle.alpha = AnimationMath.fast(circle.alpha, 0, fadeSpeed.getValue().floatValue());
        });
        circles.removeIf(circle -> circle.alpha <= 0.005f);
    }

    private void renderCircles() {
        setupRenderSettings();
        circles.forEach(circle -> drawJumpCircle(circle, circle.factor, circle.alpha, 0));
        restoreRenderSettings();
    }

    private void setupRenderSettings() {
        RenderSystem.pushMatrix();
        RenderSystem.disableLighting();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
    }

    private void restoreRenderSettings() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.depthMask(true);
        RenderSystem.popMatrix();
    }

    private void drawJumpCircle(Circle circle, float radius, float alpha, float shadowSize) {
        double camX = mc.getRenderManager().info.getProjectedView().getX();
        double camY = mc.getRenderManager().info.getProjectedView().getY();
        double camZ = mc.getRenderManager().info.getProjectedView().getZ();

        double x = circle.spawnX - camX;
        double y = circle.spawnY + 0.1 - camY;
        double z = circle.spawnZ - camZ;

        GlStateManager.translated(x, y, z);
        GlStateManager.rotatef(circle.factor * 70, 0, -1, 0);

        ResourceLocation texture = new ResourceLocation("monoton/images/jump.png");
        mc.getTextureManager().bindTexture(texture);

        BUFFER.begin(GL_QUAD_STRIP, POSITION_COLOR_TEX);
        for (int i = 0; i <= 360; i += 1) {
            float[] colors = RenderUtilka.IntColor.rgb(selectedColor);
            double sin = MathHelper.sin(Math.toRadians(i + 0.1F)) * radius;
            double cos = MathHelper.cos(Math.toRadians(i + 0.1F)) * radius;
            BUFFER.pos(0, 0, 0).color(colors[0], colors[1], colors[2], MathHelper.clamp(circle.alpha, 0, 1)).tex(0.5f, 0.5f).endVertex();
            BUFFER.pos(sin, 0, cos).color(colors[0], colors[1], colors[2], MathHelper.clamp(circle.alpha, 0, 1)).tex((float) ((sin / (2 * radius)) + 0.5f), (float) ((cos / (2 * radius)) + 0.5f)).endVertex();
        }
        TESSELLATOR.draw();

        GlStateManager.rotatef(-circle.factor * 70, 0, -1, 0);
        GlStateManager.translated(-x, -y, -z);
    }

    private static class Circle {
        public final float spawnX;
        public final float spawnY;
        public final float spawnZ;
        public float factor = 0;
        public float alpha = 5;

        public Circle(float spawnX, float spawnY, float spawnZ) {
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.spawnZ = spawnZ;
        }
    }
}