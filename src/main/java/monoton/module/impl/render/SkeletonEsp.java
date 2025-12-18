package monoton.module.impl.render;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.Setting;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ColorSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;

import static monoton.module.TypeList.Render;

@Annotation(name = "SkeletonEsp", type = Render, desc = "Рендерит скелет игроков линиями")
public class SkeletonEsp extends Module {
    public ColorSetting color = new ColorSetting("Цвет", ColorUtils.rgba(255, 255, 255, 255));
    public ColorSetting friendColor = new ColorSetting("Цвет друзей", ColorUtils.rgba(0, 255, 0, 255));
    public final SliderSetting size = new SliderSetting("Толщина", 1.5f, 0.5f, 3f, 0.5f);
    public final BooleanOption penis = new BooleanOption("Хуй", true);
    public final SliderSetting dlina = new SliderSetting("Длина хуя в см", 5f, 2.5f, 40f, 2.5f).setVisible(() -> penis.get());

    public SkeletonEsp() {
        this.addSettings(new Setting[]{color, friendColor, size, dlina, penis});
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player.isAlive() && player.botEntity) {
                    if (player != mc.player || (!mc.gameSettings.getPointOfView().func_243192_a() && mc.player == player)) {
                        renderSkeleton(player, ((EventRender) event).getPartialTicks());
                    }
                }
            }

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glPopMatrix();
        }
        return false;
    }

    private void renderSkeleton(PlayerEntity player, float partialTicks) {
        Vector3d pos = RenderUtilka.interpolate(player, partialTicks);
        double x = pos.x - mc.getRenderManager().info.getProjectedView().x;
        double y = pos.y - mc.getRenderManager().info.getProjectedView().y;
        double z = pos.z - mc.getRenderManager().info.getProjectedView().z;

        float scale = player.isChild() ? 0.5f : 1.05f;
        double headY = y + 1.6 * scale;
        double torsoTopY = y + 1.2 * scale;
        double torsoBottomY = y + 0.6 * scale;
        double armOffset = 0.33f;
        double handY = y + 0.8 * scale;
        double legOffset = 0.15 * scale;
        double footY = y;

        if (player.isSwimming()) {
            headY = y + 1.5 * scale;
            torsoTopY = y + 1.2 * scale;
            torsoBottomY = y + 0.6 * scale;
            handY = y + 0.8 * scale;
            footY = y + 0.1 * scale;
            armOffset = 0.33f;
            legOffset = 0.25 * scale;
        } else if (player.isCrouching()) {
            headY = y + 1.25 * scale;
            torsoTopY = y + 0.95 * scale;
            torsoBottomY = y + 0.45 * scale;
            handY = y + 0.55 * scale;
            footY = y - 0.1 * scale;
        } else if (player.isElytraFlying()) {
            headY = y + 1.5 * scale;
            torsoTopY = y + 1.2 * scale;
            torsoBottomY = y + 0.6 * scale;
            handY = y + 0.8 * scale;
            footY = y + 0.1 * scale;
            armOffset = 0.33f;
        } else if (player.isForcedDown() && !player.isCrouching()) {
            headY = y + 1.5 * scale;
            torsoTopY = y + 1.2 * scale;
            torsoBottomY = y + 0.6 * scale;
            handY = y + 0.8 * scale;
            footY = y + 0.1 * scale;
            armOffset = 0.33f;
        }

        int lineColor = Manager.FRIEND_MANAGER.isFriend(player.getName().getString()) ? friendColor.get() : color.get();

        // Interpolate rotations
        float bodyYaw = RenderUtilka.interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
        float headYaw = RenderUtilka.interpolateRotation(player.prevRotationYawHead, player.rotationYawHead, partialTicks);
        float headPitch = RenderUtilka.interpolateRotation(player.prevRotationPitch, player.rotationPitch, partialTicks);
        float netHeadYaw = headYaw - bodyYaw;

        // Clamp head pitch to prevent excessive tilting
        headPitch = MathHelper.clamp(headPitch, -60.0f, 60.0f);

        // Limb swing calculations
        float limbSwing = player.limbSwing - player.limbSwingAmount * (1.0f - partialTicks);
        float limbSwingAmount = MathHelper.lerp(partialTicks, player.prevLimbSwingAmount, player.limbSwingAmount);
        if (player.isChild()) {
            limbSwing *= 3.0f;
            limbSwingAmount *= 0.8f;
        }
        if (limbSwingAmount > 1.0f) {
            limbSwingAmount = 1.0f;
        }

        // Arm and leg rotations
        float rightArmRotateX = MathHelper.cos(limbSwing * 0.6662f + (float)Math.PI) * 2.0f * limbSwingAmount * 0.5f;
        float leftArmRotateX = MathHelper.cos(limbSwing * 0.6662f) * 2.0f * limbSwingAmount * 0.5f;
        float rightLegRotateX = MathHelper.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        float leftLegRotateX = MathHelper.cos(limbSwing * 0.6662f + (float)Math.PI) * 1.4f * limbSwingAmount;

        // Attack animation (hand swing) for both arms
        float swingProgress = player.getSwingProgress(partialTicks);
        if (swingProgress > 0.0f) {
            float swingAngle = -MathHelper.sin(swingProgress * (float)Math.PI) * 1.5f;
            leftArmRotateX += swingAngle;
        }

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-bodyYaw, 0.0f, 1.0f, 0.0f);
        if (player.isSwimming()) {
            GL11.glRotatef(headPitch + 90.0f, 1.0f, 0.0f, 0.0f);
        } else if (player.isElytraFlying() ) {
            GL11.glRotatef(headPitch + 90.0f, 1.0f, 0.0f, 0.0f);
        } else if (player.isForcedDown() && !player.isCrouching()) {
            GL11.glRotatef(headPitch + 90.0f, 1.0f, 0.0f, 0.0f);
        } else if (player.isCrouching()) {
            GL11.glTranslatef(0.0f, -0.1f * scale, 0.0f);
            GL11.glRotatef(20.0f / 5.0f, 1.0f, 0.0f, 0.0f); // Reduced crouching tilt
        }

        // Head
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0f, (float)(torsoTopY - y), 0.0f);
        GL11.glRotatef(-netHeadYaw, 0.0f, 1.0f, 0.0f); // Inverted netHeadYaw
        GL11.glRotatef(headPitch * 0.5f, 1.0f, 0.0f, 0.0f); // Reduced pitch influence
        RenderUtilka.drawLine(0, 0, 0, 0, (float)(headY - torsoTopY), 0, lineColor);
        GL11.glPopMatrix();

        // Torso
        RenderUtilka.drawLine(0, (float)(torsoTopY - y), 0, 0, (float)(torsoBottomY - y), 0, lineColor);

        // Left Arm
        GL11.glPushMatrix();
        GL11.glTranslatef((float)-armOffset, (float)(torsoTopY - y), 0.0f);
        GL11.glRotatef(leftArmRotateX * (180f / (float)Math.PI), 1.0f, 0.0f, 0.0f);
        RenderUtilka.drawLine(0, 0, 0, 0, (float)(handY - torsoTopY), 0, lineColor);
        GL11.glPopMatrix();
        RenderUtilka.drawLine(0, (float)(torsoTopY - y), 0, (float)-armOffset, (float)(torsoTopY - y), 0, lineColor);

        // Right Arm
        GL11.glPushMatrix();
        GL11.glTranslatef((float)armOffset, (float)(torsoTopY - y), 0.0f);
        GL11.glRotatef(rightArmRotateX * (180f / (float)Math.PI), 1.0f, 0.0f, 0.0f);
        RenderUtilka.drawLine(0, 0, 0, 0, (float)(handY - torsoTopY), 0, lineColor);
        GL11.glPopMatrix();
        RenderUtilka.drawLine(0, (float)(torsoTopY - y), 0, (float)armOffset, (float)(torsoTopY - y), 0, lineColor);

        // Left Leg
        GL11.glPushMatrix();
        GL11.glTranslatef((float)-legOffset, (float)(torsoBottomY - y), 0.0f);
        GL11.glRotatef(leftLegRotateX * (180f / (float)Math.PI), 1.0f, 0.0f, 0.0f);
        RenderUtilka.drawLine(0, 0, 0, 0, (float)(footY - torsoBottomY), 0, lineColor);
        GL11.glPopMatrix();
        RenderUtilka.drawLine(0, (float)(torsoBottomY - y), 0, (float)-legOffset, (float)(torsoBottomY - y), 0, lineColor);

        // Right Leg
        GL11.glPushMatrix();
        GL11.glTranslatef((float)legOffset, (float)(torsoBottomY - y), 0.0f);
        GL11.glRotatef(rightLegRotateX * (180f / (float)Math.PI), 1.0f, 0.0f, 0.0f);
        RenderUtilka.drawLine(0, 0, 0, 0, (float)(footY - torsoBottomY), 0, lineColor);
        GL11.glPopMatrix();
        RenderUtilka.drawLine(0, (float)(torsoBottomY - y), 0, (float)legOffset, (float)(torsoBottomY - y), 0, lineColor);

        // Penis (optional)
        if (penis.get() && !player.isSwimming() && !player.isElytraFlying()) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0f, (float)(torsoBottomY - y), 0.0f);
            GL11.glRotatef(player.isCrouching() ? -10.0f : -20.0f, 1.0f, 0.0f, 0.0f);
            RenderUtilka.drawLine2(0, 0, 0, 0, 0, dlina.getValue().floatValue() / 20, lineColor);
            GL11.glPopMatrix();
        }

        GL11.glPopMatrix();
    }
}