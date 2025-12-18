    package monoton.control.handler.impl;

    import com.mojang.blaze3d.matrix.MatrixStack;
    import com.mojang.blaze3d.platform.GlStateManager;
    import com.mojang.blaze3d.systems.RenderSystem;
    import monoton.control.Manager;
    import monoton.control.events.client.Event;
    import monoton.control.events.render.EventRender;
    import monoton.module.impl.combat.Aura;
    import monoton.module.impl.combat.ProjectileHelper;
    import monoton.module.impl.render.TargetEsp;
    import monoton.utils.IMinecraft;
    import monoton.utils.math.MathUtil;
    import monoton.utils.render.shader.AnimationUtil;
    import monoton.utils.render.shader.ColorUtil;
    import monoton.utils.render.shader.Easings;
    import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
    import net.minecraft.entity.Entity;
    import net.minecraft.entity.LivingEntity;
    import net.minecraft.util.ResourceLocation;
    import net.minecraft.util.math.MathHelper;
    import net.minecraft.util.math.vector.Matrix4f;
    import net.minecraft.util.math.vector.Vector3d;
    import net.minecraft.util.math.vector.Vector3f;
    import org.lwjgl.opengl.GL11;

    import static monoton.ui.clickgui.Panel.selectedColor;

    public class TargetESPHandler implements IMinecraft {
        private final AnimationUtil ghostAlphaAnimation = new AnimationUtil(0f, 10f, Easings.CUBIC_IN_OUT);
        private final AnimationUtil ghostScaleAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);

        private final AnimationUtil sizeAnimation = new AnimationUtil(1f, 6f, Easings.LINEAR);
        private final AnimationUtil alphaAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);

        private final AnimationUtil hurtColorAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);

        private final AnimationUtil ghostHurtColorAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);
        private final AnimationUtil circleHurtColorAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);

        private Entity lastGhostEntity;
        private Entity lastTargetEntity;

        private Vector3d lastPosition;
        private final int redFactor = 200;

        private float ghostAnimationTime;
        private long lastGhostUpdateTimestamp;

        private float lastHalfHeight;
        private boolean lastHadTarget;
        private double rotationPhase;
        private long lastRotationUpdateMs;

        public boolean onEvent(Event event) {
            if (event instanceof EventRender render && render.isRender3D()) {
                onRender3D(render);
            }
            return false;
        }

        private void onRender3D(EventRender event) {
            TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
            if (targetEsp == null || !targetEsp.state) return;

            String mode = TargetEsp.targetesp.get();
            switch (mode) {
                case "Ромб" -> handleRhombus(event);
                case "Призраки" -> handleGhost(event);
                case "Кольцо" -> handleCircle(event);
            }
        }

        // Новый метод для получения текущего таргета (Aura или ProjectileHelper)
        private LivingEntity getCurrentTarget() {
            LivingEntity auraTarget = Aura.getTarget();
            if (auraTarget != null && auraTarget.isAlive()) {
                return auraTarget;
            }

            ProjectileHelper projectileHelper = Manager.FUNCTION_MANAGER.projectileHelper;
            if (projectileHelper != null && projectileHelper.state && projectileHelper.target != null && projectileHelper.target.isAlive()) {
                return projectileHelper.target;
            }

            return null;
        }

        private void handleRhombus(EventRender event) {
            TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
            if (targetEsp == null || !targetEsp.state || !targetEsp.targetesp.is("Ромб")) return;
            Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();

            Entity target = getCurrentTarget();
            boolean hasTarget = target != null;

            alphaAnimation.update(hasTarget ? 1f : 0f);

            if (!hasTarget && alphaAnimation.getValue() <= 0.01f) {
                lastHadTarget = false;
                lastTargetEntity = null;
                return;
            }

            Vector3d entityPos = lastPosition;
            float halfHeight = lastHalfHeight;
            Entity currentEntity = hasTarget ? target : lastTargetEntity;

            if (currentEntity != null && currentEntity.isAlive()) {
                entityPos = MathUtil.interpolate(currentEntity, event.getPartialTicks());
                halfHeight = currentEntity.getHeight() / 2f;
            } else if (lastPosition == null || !lastHadTarget) return;

            if (hasTarget) {
                lastPosition = entityPos;
                lastHalfHeight = halfHeight;
                lastHadTarget = true;
                lastTargetEntity = target;
            }

            MatrixStack matrixStack = new MatrixStack();
            matrixStack.translate(entityPos.x - cameraPosition.x, entityPos.y + halfHeight - cameraPosition.y, entityPos.z - cameraPosition.z);

            float hurtFactor = 0f;
            if (targetEsp.hitred.get()) {
                hurtFactor = hasTarget && target instanceof LivingEntity living && living.maxHurtTime > 0
                        ? MathHelper.clamp((float) living.hurtTime / living.maxHurtTime, 0f, 1f) : 0f;
            }
            hurtColorAnimation.update(hurtFactor);

            float rawSize = targetEsp.size.getValue().floatValue() / 65;
            float displayedSize = MathUtil.lerp(1f, rawSize, alphaAnimation.getValue());
            float halfSize = displayedSize / 2f;

            double deltaTime = lastRotationUpdateMs == 0 ? 0 : (System.currentTimeMillis() - lastRotationUpdateMs) / 1000f;
            lastRotationUpdateMs = System.currentTimeMillis();
            rotationPhase += 2 * (hasTarget ? 1 : 1.5) * deltaTime;

            matrixStack.push();
            matrixStack.rotate(mc.getRenderManager().info.getRotation().copy());
            matrixStack.rotate(Vector3f.ZP.rotationDegrees((float) (Math.sin(rotationPhase) * 180)));
            Matrix4f rotationMatrix = matrixStack.getLast().getMatrix();
            matrixStack.pop();

            RenderSystem.pushMatrix();
            RenderSystem.enableTexture();
            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.blendFuncSeparate(770, 1, 0, 1);
            RenderSystem.shadeModel(7425);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);

            int baseColor = selectedColor;
            int color = ColorUtil.boostColor(ColorUtil.getColor(
                    MathHelper.clamp((int) (ColorUtil.red(baseColor) * (1f - hurtColorAnimation.getValue()) + redFactor * hurtColorAnimation.getValue()), 0, 255),
                    MathHelper.clamp((int) (ColorUtil.green(baseColor) * (1f - hurtColorAnimation.getValue())), 0, 255),
                    MathHelper.clamp((int) (ColorUtil.blue(baseColor) * (1f - hurtColorAnimation.getValue())), 0, 255),
                    (int) (100 * MathHelper.clamp(alphaAnimation.getValue(), 0f, 1f))), 45);

            int r = ColorUtil.red(color), g = ColorUtil.green(color), b = ColorUtil.blue(color), a = ColorUtil.alpha(color);

            mc.getTextureManager().bindTexture(new ResourceLocation("monoton/images/target.png"));

            BUFFER.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            BUFFER.pos(rotationMatrix, -halfSize, -halfSize + displayedSize, 0).tex(0, 1).color(r, g, b, a).endVertex();
            BUFFER.pos(rotationMatrix, halfSize, -halfSize + displayedSize, 0).tex(1, 1).color(r, g, b, a).endVertex();
            BUFFER.pos(rotationMatrix, halfSize, -halfSize, 0).tex(1, 0).color(r, g, b, a).endVertex();
            BUFFER.pos(rotationMatrix, -halfSize, -halfSize, 0).tex(0, 0).color(r, g, b, a).endVertex();
            TESSELLATOR.draw();

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.shadeModel(7424);
            RenderSystem.disableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableAlphaTest();
            RenderSystem.enableCull();
            RenderSystem.popMatrix();
        }

        private void handleGhost(EventRender event) {
            TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
            if (targetEsp == null || !targetEsp.state || !TargetEsp.targetesp.is("Призраки")) return;

            LivingEntity target = getCurrentTarget();
            boolean alive = target != null && target.isAlive();

            ghostAlphaAnimation.update(alive ? 1f : 0f);
            ghostScaleAnimation.update(alive ? 1f : 0f);

            if (ghostAlphaAnimation.getValue() <= 0.01f && ghostScaleAnimation.getValue() <= 0.01f) {
                lastGhostEntity = null;
                ghostAnimationTime = 0;
                lastGhostUpdateTimestamp = 0;
                return;
            }

            if (alive) {
                if (lastGhostEntity == null) lastGhostUpdateTimestamp = System.currentTimeMillis();
                lastGhostEntity = target;
            }
            if (lastGhostEntity == null) return;

            float speedMul       = targetEsp.speed.getValue().floatValue() / 40f;
            float spriteSize     = targetEsp.sizee.getValue().floatValue() * 0.01f;
            float radiusMod      = targetEsp.distancee.getValue().floatValue();
            float alphaStep      = targetEsp.alpha.getValue().floatValue();
            int totalSprites     = (int) (targetEsp.distance.getValue().floatValue() * 1.1f);

            ghostAnimationTime += (4f * (System.currentTimeMillis() - lastGhostUpdateTimestamp) / 600) * speedMul;
            lastGhostUpdateTimestamp = System.currentTimeMillis();

            Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();
            double x = lastGhostEntity.lastTickPosX + (lastGhostEntity.getPosX() - lastGhostEntity.lastTickPosX) * event.getPartialTicks() - cameraPosition.x;
            double y = lastGhostEntity.lastTickPosY + (lastGhostEntity.getPosY() - lastGhostEntity.lastTickPosY) * event.getPartialTicks() - cameraPosition.y;
            double z = lastGhostEntity.lastTickPosZ + (lastGhostEntity.getPosZ() - lastGhostEntity.lastTickPosZ) * event.getPartialTicks() - cameraPosition.z;

            float rawHurt = 0f;
            if (targetEsp.hitred.get() && alive && target.maxHurtTime > 0) {
                rawHurt = MathHelper.clamp((float) target.hurtTime / target.maxHurtTime, 0f, 1f);
            }
            ghostHurtColorAnimation.update(rawHurt);

            RenderSystem.pushMatrix();
            RenderSystem.enableTexture();
            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.blendFuncSeparate(770, 1, 0, 1);
            RenderSystem.shadeModel(7425);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);

            mc.getTextureManager().bindTexture(new ResourceLocation("monoton/images/glow.png"));

            BUFFER.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

            int baseColor = selectedColor;
            int color = ColorUtil.getColor(
                    MathHelper.clamp((int) (ColorUtil.red(baseColor) * (1f - ghostHurtColorAnimation.getValue()) + redFactor * ghostHurtColorAnimation.getValue()), 0, 255),
                    MathHelper.clamp((int) (ColorUtil.green(baseColor) * (1f - ghostHurtColorAnimation.getValue())), 0, 255),
                    MathHelper.clamp((int) (ColorUtil.blue(baseColor) * (1f - ghostHurtColorAnimation.getValue())), 0, 255),
                    ColorUtil.alpha(baseColor)
            );

            for (int ringLayer = 0; ringLayer < 9; ringLayer += 3) {
                for (int spriteIndex = 0; spriteIndex < totalSprites; ++spriteIndex) {
                    MatrixStack stack = new MatrixStack();
                    stack.translate(
                            x + (radiusMod * 0.8f * MathHelper.sin(ghostAnimationTime + spriteIndex * 0.1f + (int) Math.pow(ringLayer, 2.0))),
                            y + 0.5f + (radiusMod * 0.3f * MathHelper.sin(ghostAnimationTime + spriteIndex * 0.2f)) + (0.2f * ringLayer),
                            z + (radiusMod * 0.8f * MathHelper.cos(ghostAnimationTime + spriteIndex * 0.1f - (int) Math.pow(ringLayer, 2.0)))
                    );

                    float spriteScale = ghostScaleAnimation.getValue() * (spriteSize + spriteIndex / 2000.0f);
                    stack.scale(spriteScale, spriteScale, spriteScale);
                    stack.rotate(mc.getRenderManager().info.getRotation().copy());

                    float distAlpha = Math.max(0f, 1f - (spriteIndex / (float) totalSprites * alphaStep / 20f));
                    int finalAlpha = (int) (ghostAlphaAnimation.getValue() * 255 * distAlpha);
                    int spriteColor = ColorUtil.applyOpacity(color, finalAlpha);

                    int r = ColorUtil.red(spriteColor), g = ColorUtil.green(spriteColor),
                            b = ColorUtil.blue(spriteColor), a = ColorUtil.alpha(spriteColor);

                    BUFFER.pos(stack.getLast().getMatrix(), -25, 25, 0.0f).tex(0.0f, 1.0f).color(r, g, b, a).endVertex();
                    BUFFER.pos(stack.getLast().getMatrix(), 25, 25, 0.0f).tex(1.0f, 1.0f).color(r, g, b, a).endVertex();
                    BUFFER.pos(stack.getLast().getMatrix(), 25, -25, 0.0f).tex(1.0f, 0.0f).color(r, g, b, a).endVertex();
                    BUFFER.pos(stack.getLast().getMatrix(), -25, -25, 0.0f).tex(0.0f, 0.0f).color(r, g, b, a).endVertex();
                }
            }

            TESSELLATOR.draw();

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.shadeModel(7424);
            RenderSystem.disableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableAlphaTest();
            RenderSystem.enableCull();
            RenderSystem.popMatrix();
        }

        private void handleCircle(EventRender event) {
            TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
            if (!targetEsp.state || !TargetEsp.targetesp.is("Кольцо")) return;

            LivingEntity target = getCurrentTarget();
            if (target == null) return;

            circleHurtColorAnimation.update(0f);

            float radius = target.getWidth() * 0.8F;
            Vector3d targetPosition = MathUtil.interpolate(target, event.getPartialTicks());

            double duration = 2000;
            double elapsedMillis = (System.currentTimeMillis() % duration);
            double progress = elapsedMillis / (duration / 2);

            progress = elapsedMillis > duration / 2 ? progress - 1 : 1 - progress;
            progress = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2;

            Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();

            RenderSystem.pushMatrix();
            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.shadeModel(7425);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.lineWidth(2f);
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

            BUFFER.begin(8, DefaultVertexFormats.POSITION_COLOR);
            for (int angleDegree = 0; angleDegree <= 360; ++angleDegree) {
                int gradientColor = ColorUtil.gradient(10, angleDegree * 5, selectedColor, ColorUtil.darken(selectedColor, 0.5f));

                int ringColor = gradientColor;

                double angleRadians = Math.toRadians(angleDegree);
                double cos = Math.cos(angleRadians);
                double sin = Math.sin(angleRadians);
                double heightOffset = (target.getHeight() / 2) * (progress > 0.5 ? 1 - progress : progress) * (elapsedMillis > duration / 2 ? -1 : 1);

                BUFFER.pos((float) (targetPosition.x + cos * radius - cameraPosition.x),
                        (float) (targetPosition.y + target.getHeight() * progress - cameraPosition.y),
                        (float) (targetPosition.z + sin * radius - cameraPosition.z)).color(ringColor).endVertex();
                BUFFER.pos((float) (targetPosition.x + cos * radius - cameraPosition.x),
                        (float) (targetPosition.y + target.getHeight() * progress + heightOffset - cameraPosition.y),
                        (float) (targetPosition.z + sin * radius - cameraPosition.z)).color(ColorUtil.applyOpacity(ringColor, 0)).endVertex();
            }
            TESSELLATOR.draw();

            BUFFER.begin(2, DefaultVertexFormats.POSITION_COLOR);
            for (int angleDegree = 0; angleDegree <= 360; ++angleDegree) {
                int gradientColor = ColorUtil.gradient(10, angleDegree * 5, selectedColor, ColorUtil.darken(selectedColor, 0.5f));
                int ringColor = gradientColor;

                double angleRadians = Math.toRadians(angleDegree);
                BUFFER.pos((float) (targetPosition.x + Math.cos(angleRadians) * radius - cameraPosition.x),
                        (float) (targetPosition.y + target.getHeight() * progress - cameraPosition.y),
                        (float) (targetPosition.z + Math.sin(angleRadians) * radius - cameraPosition.z)).color(ringColor).endVertex();
            }
            TESSELLATOR.draw();

            RenderSystem.enableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.enableTexture();
            RenderSystem.enableAlphaTest();
            RenderSystem.shadeModel(7424);
            RenderSystem.depthMask(true);
            RenderSystem.popMatrix();
        }
    }