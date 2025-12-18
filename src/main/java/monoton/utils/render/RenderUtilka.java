package monoton.utils.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import monoton.control.Manager;
import monoton.utils.IMinecraft;
import monoton.utils.math.Interpolator;
import monoton.utils.render.shader.ColorUtil;
import monoton.utils.render.shader.KawaseBlur;
import monoton.utils.render.shader.ShaderUtil;
import net.minecraft.client.MainWindow;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector4f;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Objects;

import static com.mojang.blaze3d.platform.GlStateManager.*;
import static com.mojang.blaze3d.systems.RenderSystem.enableBlend;
import static monoton.ui.clickgui.Panel.selectedColor;
import static monoton.utils.render.ColorUtils.quads;
import static monoton.utils.render.RenderUtilka.IntColor.*;
import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX;
import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX_COLOR;
import static org.lwjgl.opengl.GL11.*;


public class RenderUtilka implements IMinecraft {
    private static final ShaderUtils ROUNDED_GRADIENT = ShaderUtils.create("roundedGradient");
    private static ResourceLocation currentBatchTexture = null;
    private static int batchQuadCount = 0;
    private static boolean image3DBatchActive = false;

    public static void drawImage3DQuadInternal(ResourceLocation texture, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int color0, int color1, int color2, int color3, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {

        boolean needNewBatch = !image3DBatchActive || !texture.equals(currentBatchTexture) || batchQuadCount >= 8192;

        if (needNewBatch) {
            if (image3DBatchActive) {
                if (BUFFER.isDrawing()) {
                    TESSELLATOR.draw();
                }
            }

            currentBatchTexture = texture;
            image3DBatchActive = true;
            batchQuadCount = 0;

            setupRendering();
            mc.getTextureManager().bindTexture(texture);
            BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        }

        addVerticesToBuffer(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color0, color1, color2, color3, u0, v0, u1, v1, u2, v2, u3, v3);

        batchQuadCount++;
    }

    private static void setupRendering() {
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(7425);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);
    }

    private static void addVerticesToBuffer(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int color0, int color1, int color2, int color3, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        int[][] colors = {decomposeColor(color0), decomposeColor(color1), decomposeColor(color2), decomposeColor(color3)};

        BUFFER.pos(x0, y0, z0).tex(u0, v0).color(colors[0][0], colors[0][1], colors[0][2], colors[0][3]).endVertex();
        BUFFER.pos(x1, y1, z1).tex(u1, v1).color(colors[1][0], colors[1][1], colors[1][2], colors[1][3]).endVertex();
        BUFFER.pos(x2, y2, z2).tex(u2, v2).color(colors[2][0], colors[2][1], colors[2][2], colors[2][3]).endVertex();
        BUFFER.pos(x3, y3, z3).tex(u3, v3).color(colors[3][0], colors[3][1], colors[3][2], colors[3][3]).endVertex();
    }

    private static int[] decomposeColor(int color) {
        return new int[]{(color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, color >>> 24};
    }

    private static boolean image3DBatchBoost = false;

    public static void beginImage3DBatch(ResourceLocation image, boolean boost) {
        if (image3DBatchActive) {
            endImage3DBatch();
        }
        image3DBatchActive = true;
        image3DBatchBoost = boost;

        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);

        mc.getTextureManager().bindTexture(image);
        BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    }

    public static void addImage3DQuad(
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            int color
    ) {

        int boostalpha = image3DBatchBoost ? 90 : 0;
        int red = Math.min(255, ((color >> 16) & 0xFF) + boostalpha);
        int green = Math.min(255, ((color >> 8) & 0xFF) + boostalpha);
        int blue = Math.min(255, (color & 0xFF) + boostalpha);
        int alpha = color >>> 24;

        BUFFER.pos(x0, y0, z0).tex(0f, 0f).color(red, green, blue, alpha).endVertex();
        BUFFER.pos(x1, y1, z1).tex(1f, 0f).color(red, green, blue, alpha).endVertex();
        BUFFER.pos(x2, y2, z2).tex(1f, 1f).color(red, green, blue, alpha).endVertex();
        BUFFER.pos(x3, y3, z3).tex(0f, 1f).color(red, green, blue, alpha).endVertex();
    }

    public static void endImage3DBatch() {
        if (!image3DBatchActive) return;
        TESSELLATOR.draw();
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        image3DBatchActive = false;
    }

    public static void drawImage3DQuad(ResourceLocation texture, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int color) {
        drawImage3DQuadInternal(texture, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color, color, color, color, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f);
    }

    public static void flushImage3DBatch() {
        if (!image3DBatchActive) return;

        if (BUFFER.isDrawing()) {
            TESSELLATOR.draw();
        }
        cleanupRendering();
        image3DBatchActive = false;
        currentBatchTexture = null;
        batchQuadCount = 0;
    }

    private static void cleanupRendering() {
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        RenderSystem.depthMask(true);
    }


    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float width, float height, int color) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.enableTexture();
        mc.getTextureManager().bindTexture(resourceLocation);

        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        quads(x, y, width, height, 7, color);

        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    public static void drawStack(ItemStack itemStack, float x, float y, float size) {
        if (itemStack.isEmpty()) return;
        RenderSystem.pushMatrix();
        RenderSystem.translatef(x, y, 0);
        RenderSystem.scalef(size, size, size);

        mc.getItemRenderer().renderItemAndEffectIntoGUI(itemStack, 0, 0);
        mc.getItemRenderer().renderItemOverlays(mc.fontRenderer, itemStack, 0, 0);

        RenderSystem.popMatrix();
    }




    public static int reAlphaInt(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 16777215);
    }

    public static void color(int rgb) {
        GL11.glColor3f(getRed(rgb) / 255f, getGreen(rgb) / 255f, getBlue(rgb) / 255f);
    }

    public static boolean isInRegion(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static boolean isInRegion(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static boolean isInRegion(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static Vector3d interpolate(Entity entity, float partialTicks) {
        double posX = Interpolator.lerp(entity.lastTickPosX, entity.getPosX(), partialTicks);
        double posY = Interpolator.lerp(entity.lastTickPosY, entity.getPosY(), partialTicks);
        double posZ = Interpolator.lerp(entity.lastTickPosZ, entity.getPosZ(), partialTicks);
        return new Vector3d(posX, posY, posZ);
    }

    public static float interpolateRotation(float prevRenderYawOffset, float renderYawOffset, float partialTicks) {
        float delta = renderYawOffset - prevRenderYawOffset;
        while (delta < -180.0F) {
            delta += 360.0F;
        }
        while (delta >= 180.0F) {
            delta -= 360.0F;
        }
        return prevRenderYawOffset + partialTicks * delta;
    }

    public static class IntColor {
        public static float[] rgb(final int color) {
            return new float[]{
                    (color >> 16 & 0xFF) / 255f,
                    (color >> 8 & 0xFF) / 255f,
                    (color & 0xFF) / 255f,
                    (color >> 24 & 0xFF) / 255f
            };
        }

        public static int getRed(final int hex) {
            return hex >> 16 & 255;
        }

        public static int getGreen(final int hex) {
            return hex >> 8 & 255;
        }

        public static int getBlue(final int hex) {
            return hex & 255;
        }

        public static int getAlpha(final int hex) {
            return hex >> 24 & 255;
        }
    }


    public static void drawImage2D(ResourceLocation image, float x, float y, float width, float height, int color) {
        mc.getTextureManager().bindTexture(image);
        int filter;
        if (width > 128 || height > 128) {
            filter = GL11.GL_NEAREST;
        } else {
            filter = GL11.GL_LINEAR;
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;
        RenderSystem.color4f(r, g, b, a);
        quads(x, y, width, height, 7, color);
    }

    public static class Render2D {
        private static final HashMap<Integer, Integer> shadowCache = new HashMap<>();
        private static HashMap<Integer, Integer> shadowCache2 = new HashMap<>();

        public static int downloadImage(String url) {
            int texId = -1;
            int identifier = Objects.hash(url);
            if (shadowCache2.containsKey(identifier)) {
                texId = shadowCache2.get(identifier);
            } else {
                URL stringURL;
                try {
                    stringURL = new URL(url);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                BufferedImage img;
                try {
                    img = ImageIO.read(stringURL);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    texId = loadTexture(img);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                shadowCache2.put(identifier, texId);
            }
            return texId;
        }


        public static void drawCircle(float x, float y, float start, float end, float radius, float width, boolean filled, int color) {
            float i;
            float endOffset;
            if (start > end) {
                endOffset = end;
                end = start;
                start = endOffset;
            }

            GlStateManager.enableBlend();
            GL11.glDisable(GL_TEXTURE_2D);
            RenderSystem.blendFuncSeparate(770, 771, 1, 0);

            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(width);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (i = end; i >= start; i--) {
                ColorUtils.setColor(color);
                float cos = (float) (MathHelper.cos((float) (i * Math.PI / 180)) * radius);
                float sin = (float) (MathHelper.sin((float) (i * Math.PI / 180)) * radius);
                GL11.glVertex2f(x + cos, y + sin);
            }
            GL11.glEnd();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);

            if (filled) {
                GL11.glBegin(GL11.GL_TRIANGLE_FAN);
                for (i = end; i >= start; i--) {
                    ColorUtils.setColor(color);
                    float cos = (float) MathHelper.cos((float) (i * Math.PI / 180)) * radius;
                    float sin = (float) MathHelper.sin((float) (i * Math.PI / 180)) * radius;
                    GL11.glVertex2f(x + cos, y + sin);
                }
                GL11.glEnd();
            }

            GL11.glEnable(GL_TEXTURE_2D);
            GlStateManager.disableBlend();
        }

        public static void drawRoundedTexture(ResourceLocation texture, float x, float y, float width, float height, float radius, int color) {
            if (texture == null) return;
            mc.getTextureManager().bindTexture(texture);

            ShaderUtil.rounded_texture.attach();
            ShaderUtil.rounded_texture.setUniformf("size", width, height);
            ShaderUtil.rounded_texture.setUniformf("radius", radius);
            ShaderUtil.rounded_texture.setUniformf("color", ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, (color >>> 24) / 255f);

            drawQuads(x, y, width, height);

            ShaderUtil.rounded_texture.detach();
        }

        public static int loadTexture(BufferedImage image) throws Exception {
            int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length * 4);

            for (int pixel : pixels) {
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
            buffer.flip();

            int textureID = GlStateManager.genTexture();
            RenderSystem.bindTexture(textureID);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
            GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, buffer);
            RenderSystem.bindTexture(0);
            return textureID;
        }


        public static void drawMinecraftRectangle(MatrixStack matrixStack, float x, float y, float width, float height, int color) {
            matrixStack.push();
            matrixStack.translate(x, y, 0);
            matrixStack.scale(width, height, 1);
            AbstractGui.fill(matrixStack, 0, 0, 1, 1, color);
            matrixStack.pop();
        }

        public static void drawMinecraftGradientRectangle(MatrixStack matrixStack, float x, float y, float width, float height, int color0, int color1) {
            matrixStack.push();
            matrixStack.translate(x, y, 0);
            matrixStack.scale(width, height, 1);
            AbstractGui.fillGradient(matrixStack, 0, 0, 1, 1, color0, color1);
            matrixStack.pop();
        }

        public static void drawBlurredRoundedRectangle(float x, float y, float width, float height, float radius, int color, float alpha) {
            drawBlurredRoundedRectangle(x, y, width, height, new Vector4f(radius, radius, radius, radius), color, alpha);
        }

        public static void drawBlurredRoundedRectangle(float x, float y, float width, float height, Vector4f radius, int color, float alpha) {
            RenderSystem.bindTexture(KawaseBlur.blur.BLURRED.framebufferTexture);
            ShaderUtil.blurred_round_rectangle.attach();

            ShaderUtil.blurred_round_rectangle.setUniformf("resolution", FRAMEBUFFER.framebufferTextureWidth, FRAMEBUFFER.framebufferTextureHeight);
            ShaderUtil.blurred_round_rectangle.setUniformf("start", x, y);
            ShaderUtil.blurred_round_rectangle.setUniformf("size", width, height);
            ShaderUtil.blurred_round_rectangle.setUniform("round", radius.getX(), radius.getY(), radius.getZ(), radius.getW());
            ShaderUtil.blurred_round_rectangle.setUniform("alpha", alpha);
            ShaderUtil.blurred_round_rectangle.setUniform("color", ColorUtil.getColor(color));

            beginRectBatch(false, true);
            drawQuads(x - 0.5f, y - 0.5f, width + 1, height + 1);
            endRectBatch();

            ShaderUtil.blurred_round_rectangle.detach();
        }

        private static boolean rectBatchActive = false;
        private static boolean rectBatchWithColor = false;
        private static boolean rectBatchTextured = false;

        public static void drawQuads(double x, double y, double width, double height) {
            boolean batching = rectBatchActive && !rectBatchWithColor;
            if (!batching) {
                BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            }

            BUFFER.pos(x, y, 0).tex(0, 0).endVertex();
            BUFFER.pos(x, y + height, 0).tex(0, 1).endVertex();
            BUFFER.pos(x + width, y + height, 0).tex(1, 1).endVertex();
            BUFFER.pos(x + width, y, 0).tex(1, 0).endVertex();

            if (!batching) {
                TESSELLATOR.draw();
            }
        }

        public static void beginRectBatch(boolean withColor, boolean textured) {
            if (rectBatchActive) {
                endRectBatch();
            }
            rectBatchActive = true;
            rectBatchWithColor = withColor;
            rectBatchTextured = textured;

            RenderSystem.pushMatrix();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.shadeModel(GL11.GL_SMOOTH);
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);
            if (rectBatchTextured) {
                RenderSystem.enableTexture();
            } else {
                RenderSystem.disableTexture();
            }

            BUFFER.begin(GL11.GL_QUADS, withColor ? DefaultVertexFormats.POSITION_TEX_COLOR : DefaultVertexFormats.POSITION_TEX);
        }

        public static void endRectBatch() {
            if (!rectBatchActive) return;
            TESSELLATOR.draw();
            if (!rectBatchTextured) {
                RenderSystem.enableTexture();
            }
            RenderSystem.shadeModel(GL11.GL_FLAT);
            RenderSystem.disableBlend();
            RenderSystem.popMatrix();
            rectBatchActive = false;
        }


        public static void drawFace(float d, float y, float u, float v, float uWidth, float vHeight, float width, float height, float tileWidth, float tileHeight, AbstractClientPlayerEntity target) {
            try {
                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                ResourceLocation skin = target.getLocationSkin();
                mc.getTextureManager().bindTexture(skin);
                float hurtPercent = getHurtPercent(target);
                GL11.glColor4f(1, 1 - hurtPercent, 1 - hurtPercent, 1);
                AbstractGui.drawScaledCustomSizeModalRect(d, y, u, v, uWidth, vHeight, width, height, tileWidth, tileHeight);
                GL11.glColor4f(1, 1, 1, 1);
                GL11.glPopMatrix();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void drawFace(float x, float y, float width, float height) {
            try {
                AbstractGui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, width, height, 64, 64);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static float getRenderHurtTime(LivingEntity hurt) {
            return hurt.hurtTime - (hurt.hurtTime != 0 ? mc.timer.renderPartialTicks : 0);
        }

        public static float getHurtPercent(LivingEntity hurt) {
            return getRenderHurtTime(hurt) / 10f;
        }

        public static void drawRect(float x, float y, float width, float height, int color) {
            drawMcRect(x, y, x + width, y + height, color);
        }

        public static void drawRoundCircle(float x, float y, float radius, int color) {
            drawRoundCircle(x - (radius / 2), y - (radius / 2), (radius), 255.0f, 255.0f, 255.0f);
        }

        public static void drawRoundCircle(float x, float y, float radius, float red, float green, float blue) {
            glColor3f(red, green, blue);
            glLineWidth(1.0f);
            glBegin(GL_LINE_LOOP);
            for (int i = 0; i < 360; i++) {
                double angle = org.joml.Math.toRadians(i);
                float x1 = (float) (x + org.joml.Math.cos(angle) * radius);
                float y1 = (float) (y + org.joml.Math.sin(angle) * radius);
                glVertex2f(x1, y1);
            }
            glEnd();
        }

        public static void drawMcRect(double left, double top, double right, double bottom, int color) {
            if (left < right) {
                double i = left;
                left = right;
                right = i;
            }

            if (top < bottom) {
                double j = top;
                top = bottom;
                bottom = j;
            }

            float f3 = (float) (color >> 24 & 255) / 255.0F;
            float f = (float) (color >> 16 & 255) / 255.0F;
            float f1 = (float) (color >> 8 & 255) / 255.0F;
            float f2 = (float) (color & 255) / 255.0F;
            BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            RenderSystem.defaultBlendFunc();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
            bufferbuilder.pos(left, bottom, 0.0F).color(f, f1, f2, f3).endVertex();
            bufferbuilder.pos(right, bottom, 0.0F).color(f, f1, f2, f3).endVertex();
            bufferbuilder.pos(right, top, 0.0F).color(f, f1, f2, f3).endVertex();
            bufferbuilder.pos(left, top, 0.0F).color(f, f1, f2, f3).endVertex();
            bufferbuilder.finishDrawing();
            WorldVertexBufferUploader.draw(bufferbuilder);
            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
        }

        public static void drawMCHorizontal(double x, double y, double width, double height, int start, int end) {
            float f = (float) (start >> 24 & 255) / 255.0F;
            float f1 = (float) (start >> 16 & 255) / 255.0F;
            float f2 = (float) (start >> 8 & 255) / 255.0F;
            float f3 = (float) (start & 255) / 255.0F;
            float f4 = (float) (end >> 24 & 255) / 255.0F;
            float f5 = (float) (end >> 16 & 255) / 255.0F;
            float f6 = (float) (end >> 8 & 255) / 255.0F;
            float f7 = (float) (end & 255) / 255.0F;

            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.shadeModel(7425);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);

            bufferbuilder.pos(x, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, height, 0f).color(f5, f6, f7, f4).endVertex();
            bufferbuilder.pos(width, y, 0f).color(f5, f6, f7, f4).endVertex();
            bufferbuilder.pos(x, y, 0f).color(f1, f2, f3, f).endVertex();

            tessellator.draw();
            RenderSystem.shadeModel(7424);
            RenderSystem.disableBlend();
            RenderSystem.enableAlphaTest();
            RenderSystem.enableTexture();
        }

        public static void drawMCHorizontalBuilding(double x, double y, double width, double height, int start, int end) {
            float f = (float) (start >> 24 & 255) / 255.0F;
            float f1 = (float) (start >> 16 & 255) / 255.0F;
            float f2 = (float) (start >> 8 & 255) / 255.0F;
            float f3 = (float) (start & 255) / 255.0F;
            float f4 = (float) (end >> 24 & 255) / 255.0F;
            float f5 = (float) (end >> 16 & 255) / 255.0F;
            float f6 = (float) (end >> 8 & 255) / 255.0F;
            float f7 = (float) (end & 255) / 255.0F;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            bufferbuilder.pos(x, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, height, 0f).color(f5, f6, f7, f4).endVertex();
            bufferbuilder.pos(width, y, 0f).color(f5, f6, f7, f4).endVertex();
            bufferbuilder.pos(x, y, 0f).color(f1, f2, f3, f).endVertex();
        }

        public static void drawVertical(float x, float y, float width, float height, int start, int end) {
            width += x;
            height += y;

            float f = (float) (start >> 24 & 255) / 255.0F;
            float f1 = (float) (start >> 16 & 255) / 255.0F;
            float f2 = (float) (start >> 8 & 255) / 255.0F;
            float f3 = (float) (start & 255) / 255.0F;
            float f4 = (float) (end >> 24 & 255) / 255.0F;
            float f5 = (float) (end >> 16 & 255) / 255.0F;
            float f6 = (float) (end >> 8 & 255) / 255.0F;
            float f7 = (float) (end & 255) / 255.0F;

            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.shadeModel(7425);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);

            bufferbuilder.pos(x, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, y, 0f).color(f5, f6, f7, f4).endVertex();
            bufferbuilder.pos(x, y, 0f).color(f5, f6, f7, f4).endVertex();

            tessellator.draw();
            RenderSystem.shadeModel(7424);
            RenderSystem.disableBlend();
            RenderSystem.enableAlphaTest();
            RenderSystem.enableTexture();
        }

        public static void drawMCVertical(double x, double y, double width, double height, int start, int end) {
            float f = (float) (start >> 24 & 255) / 255.0F;
            float f1 = (float) (start >> 16 & 255) / 255.0F;
            float f2 = (float) (start >> 8 & 255) / 255.0F;
            float f3 = (float) (start & 255) / 255.0F;
            float f4 = (float) (end >> 24 & 255) / 255.0F;
            float f5 = (float) (end >> 16 & 255) / 255.0F;
            float f6 = (float) (end >> 8 & 255) / 255.0F;
            float f7 = (float) (end & 255) / 255.0F;

            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.shadeModel(7425);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);

            bufferbuilder.pos(x, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, y, 0f).color(f5, f6, f7, f4).endVertex();
            bufferbuilder.pos(x, y, 0f).color(f5, f6, f7, f4).endVertex();

            tessellator.draw();
            RenderSystem.shadeModel(7424);
            RenderSystem.disableBlend();
            RenderSystem.enableAlphaTest();
            RenderSystem.enableTexture();
        }

        public static void drawMCVerticalBuilding(double x, double y, double width, double height, int start, int end) {
            float f = (float) (start >> 24 & 255) / 255.0F;
            float f1 = (float) (start >> 16 & 255) / 255.0F;
            float f2 = (float) (start >> 8 & 255) / 255.0F;
            float f3 = (float) (start & 255) / 255.0F;
            float f4 = (float) (end >> 24 & 255) / 255.0F;
            float f5 = (float) (end >> 16 & 255) / 255.0F;
            float f6 = (float) (end >> 8 & 255) / 255.0F;
            float f7 = (float) (end & 255) / 255.0F;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            bufferbuilder.pos(x, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, y, 0f).color(f5, f6, f7, f4).endVertex();
            bufferbuilder.pos(x, y, 0f).color(f5, f6, f7, f4).endVertex();
        }

        public static void drawVerticalBuilding(double x, double y, double width, double height, int start, int end) {
            width += x;
            height += y;

            float f = (float) (start >> 24 & 255) / 255.0F;
            float f1 = (float) (start >> 16 & 255) / 255.0F;
            float f2 = (float) (start >> 8 & 255) / 255.0F;
            float f3 = (float) (start & 255) / 255.0F;
            float f4 = (float) (end >> 24 & 255) / 255.0F;
            float f5 = (float) (end >> 16 & 255) / 255.0F;
            float f6 = (float) (end >> 8 & 255) / 255.0F;
            float f7 = (float) (end & 255) / 255.0F;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            bufferbuilder.pos(x, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, height, 0f).color(f1, f2, f3, f).endVertex();
            bufferbuilder.pos(width, y, 0f).color(f5, f6, f7, f4).endVertex();
            bufferbuilder.pos(x, y, 0f).color(f5, f6, f7, f4).endVertex();
        }

        public static void drawTexture(float x, float y, float width, float height, float radius, float alpha) {
            pushMatrix();
            enableBlend();
            blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            ShaderUtils.TEXTURE_ROUND_SHADER.attach();

            ShaderUtils.TEXTURE_ROUND_SHADER.setUniform("rectSize", (float) (width * 2), (float) (height * 2));
            ShaderUtils.TEXTURE_ROUND_SHADER.setUniform("radius", radius * 2);
            ShaderUtils.TEXTURE_ROUND_SHADER.setUniform("alpha", alpha);

            quadsBegin(x, y, width, height, 7);

            ShaderUtils.TEXTURE_ROUND_SHADER.detach();
            popMatrix();
        }

        public static void quadsBegin(float x, float y, float width, float height, int glQuads) {
            BUFFER.begin(glQuads, POSITION_TEX);
            {
                BUFFER.pos(x, y, 0).tex(0, 0).endVertex();
                BUFFER.pos(x, y + height, 0).tex(0, 1).endVertex();
                BUFFER.pos(x + width, y + height, 0).tex(1, 1).endVertex();
                BUFFER.pos(x + width, y, 0).tex(1, 0).endVertex();
            }
            TESSELLATOR.draw();
        }

        public static void quadsBeginC(float x, float y, float width, float height, int glQuads, Vector4i color) {
            BUFFER.begin(glQuads, POSITION_TEX_COLOR);
            {
                BUFFER.pos(x, y, 0).tex(0, 0).color(color.get(0)).endVertex();
                BUFFER.pos(x, y + height, 0).tex(0, 1).color(color.get(1)).endVertex();
                BUFFER.pos(x + width, y + height, 0).tex(1, 1).color(color.get(2)).endVertex();
                BUFFER.pos(x + width, y, 0).tex(1, 0).color(color.get(3)).endVertex();
            }
            TESSELLATOR.draw();
        }

        public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
            pushMatrix();
            enableBlend();
            ShaderUtils.ROUND_SHADER.attach();

            ShaderUtils.setupRoundedRectUniforms(x, y, width, height, radius, ShaderUtils.ROUND_SHADER);

            ShaderUtils.ROUND_SHADER.setUniform("blur", 0);
            ShaderUtils.ROUND_SHADER.setUniform("color", getRed(color) / 255f,
                    getGreen(color) / 255f,
                    getBlue(color) / 255f,
                    getAlpha(color) / 255f);

            ShaderUtils.ROUND_SHADER.drawQuads(x, y, width, height);

            ShaderUtils.ROUND_SHADER.detach();
            disableBlend();
            popMatrix();
        }

        public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color1, int color2) {
            pushMatrix();
            enableBlend();
            ShaderUtils.ROUND_SHADER.attach();

            ShaderUtils.setupRoundedRectUniforms(x, y, width, height, radius, ShaderUtils.ROUND_SHADER);

            ShaderUtils.ROUND_SHADER.setUniform("color1", getRed(color1) / 255f,
                    getGreen(color1) / 255f,
                    getBlue(color1) / 255f,
                    getAlpha(color1) / 255f);

            ShaderUtils.ROUND_SHADER.setUniform("color2", getRed(color2) / 255f,
                    getGreen(color2) / 255f,
                    getBlue(color2) / 255f,
                    getAlpha(color2) / 255f);

            ShaderUtils.ROUND_SHADER.setUniform("blur", 0);

            ShaderUtils.ROUND_SHADER.drawQuads(x, y, width, height);

            ShaderUtils.ROUND_SHADER.detach();
            disableBlend();
            popMatrix();
        }

        public static void drawRoundOutline(float x, float y, float width, float height, float radius, float outlineThickness, int color, Vector4i outlineColor) {
            GlStateManager.color4f(1, 1, 1, 1);
            GlStateManager.enableBlend();
            RenderSystem.alphaFunc(GL_GREATER, 0.01f);
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            ShaderUtils.ROUND_SHADER_OUTLINE.attach();

            ShaderUtils.setupRoundedRectUniforms(x, y, width, height, radius, ShaderUtils.ROUND_SHADER_OUTLINE);

            float[] clr = RenderUtilka.IntColor.rgb(color);
            ShaderUtils.ROUND_SHADER_OUTLINE.setUniform("color", clr[0], clr[1], clr[2], clr[3]);
            ShaderUtils.ROUND_SHADER_OUTLINE.setUniform("outlineThickness", outlineThickness);

            for (int i = 0; i < 4; i++) {
                float[] col = RenderUtilka.IntColor.rgb(outlineColor.get(i));
                ShaderUtils.ROUND_SHADER_OUTLINE.setUniformf("outlineColor" + (i + 1), col[0], col[1], col[2], col[3]);
            }

            float expansion = 1 + outlineThickness / 2;
            float expandedWidth = width + 2 * expansion;
            float expandedHeight = height + 2 * expansion;

            ShaderUtils.ROUND_SHADER_OUTLINE.drawQuads(x - expansion, y - expansion, expandedWidth, expandedHeight);

            ShaderUtils.ROUND_SHADER_OUTLINE.detach();
            GlStateManager.disableBlend();
        }

        public static void drawRoundedCorner(float x, float y, float width, float height, float radius, int color) {
            pushMatrix();
            enableBlend();
            ShaderUtils.CORNER_ROUND_SHADER.attach();

            ShaderUtils.CORNER_ROUND_SHADER.setUniform("size", (float) (width * 2), (float) (height * 2));
            ShaderUtils.CORNER_ROUND_SHADER.setUniform("round", radius * 2, radius * 2, radius * 2, radius * 2);

            ShaderUtils.CORNER_ROUND_SHADER.setUniform("smoothness", 0.f, 1.5f);
            ShaderUtils.CORNER_ROUND_SHADER.setUniform("color",
                    getRed(color) / 255f,
                    getGreen(color) / 255f,
                    getBlue(color) / 255f,
                    IntColor.getAlpha(color) / 255f);

            ShaderUtils.CORNER_ROUND_SHADER.drawQuads(x, y, width, height);

            ShaderUtils.CORNER_ROUND_SHADER.detach();
            disableBlend();
            popMatrix();
        }

        public static void drawRoundedCorner(float x, float y, float width, float height, Vector4f vector4f, int color) {
            pushMatrix();
            enableBlend();
            RenderSystem.alphaFunc(GL_GREATER, 0.01f);
            ShaderUtils.CORNER_ROUND_SHADER.attach();

            ShaderUtils.CORNER_ROUND_SHADER.setUniform("size", (float) (width * 2), (float) (height * 2));
            ShaderUtils.CORNER_ROUND_SHADER.setUniform("round", vector4f.x * 2, vector4f.y * 2, vector4f.z * 2, vector4f.w * 2);

            ShaderUtils.CORNER_ROUND_SHADER.setUniform("smoothness", 0.f, 1.5f);
            ShaderUtils.CORNER_ROUND_SHADER.setUniform("color",
                    getRed(color) / 255f,
                    getGreen(color) / 255f,
                    getBlue(color) / 255f,
                    IntColor.getAlpha(color) / 255f);

            ShaderUtils.CORNER_ROUND_SHADER.drawQuads(x, y, width, height);

            ShaderUtils.CORNER_ROUND_SHADER.detach();
            disableBlend();
            popMatrix();
        }

        public static void drawImageAlpha(ResourceLocation resourceLocation, float x, float y, float width, float height, Vector4i color) {
            RenderSystem.pushMatrix();
            RenderSystem.disableLighting();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.shadeModel(7425);
            RenderSystem.disableCull();
            RenderSystem.disableAlphaTest();
            RenderSystem.blendFuncSeparate(770, 1, 0, 1);
            mc.getTextureManager().bindTexture(resourceLocation);
            BUFFER.begin(7, POSITION_TEX_COLOR);
            {
                BUFFER.pos(x, y, 0).tex(0, 1 - 0.01f).lightmap(0, 240).color(color.x).endVertex();
                BUFFER.pos(x, y + height, 0).tex(1, 1 - 0.01f).lightmap(0, 240).color(color.y).endVertex();
                BUFFER.pos(x + width, y + height, 0).tex(1, 0).lightmap(0, 240).color(color.z).endVertex();
                BUFFER.pos(x + width, y, 0).tex(0, 0).lightmap(0, 240).color(color.w).endVertex();
            }
            TESSELLATOR.draw();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableAlphaTest();
            RenderSystem.depthMask(true);
            RenderSystem.popMatrix();
        }

        public static void drawImageAlph(ResourceLocation resourceLocation, float x, float y, float width, float height, Vector4i color) {
            GlStateManager.pushMatrix();
            GlStateManager.translatef((float) x, (float) y, 0.0F);
            GlStateManager.rotatef(-90, 0.0F, 0.0F, 1.0F);
            GlStateManager.translatef((float) -x, (float) -y, 0.0F);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 1);

            RenderSystem.pushMatrix();
            RenderSystem.disableLighting();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.shadeModel(7425);
            RenderSystem.disableCull();
            RenderSystem.disableAlphaTest();
            RenderSystem.blendFuncSeparate(770, 1, 0, 1);
            mc.getTextureManager().bindTexture(resourceLocation);
            BUFFER.begin(7, POSITION_TEX_COLOR);
            {
                BUFFER.pos(x, y, 0).tex(0, 1 - 0.01f).lightmap(0, 240).color(color.x).endVertex();
                BUFFER.pos(x, y + height, 0).tex(1, 1 - 0.01f).lightmap(0, 240).color(color.y).endVertex();
                BUFFER.pos(x + width, y + height, 0).tex(1, 0).lightmap(0, 240).color(color.z).endVertex();
                BUFFER.pos(x + width, y, 0).tex(0, 0).lightmap(0, 240).color(color.w).endVertex();
            }
            TESSELLATOR.draw();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableAlphaTest();
            RenderSystem.depthMask(true);
            RenderSystem.popMatrix();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }

        public static void drawImage(ResourceLocation resourceLocation, float x, float y, float width, float height, int color) {
            RenderSystem.pushMatrix();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            RenderSystem.enableTexture();
            mc.getTextureManager().bindTexture(resourceLocation);

            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            RenderSystem.shadeModel(GL11.GL_SMOOTH);

            quads(x, y, width, height, 7, color);

            RenderSystem.shadeModel(GL11.GL_FLAT);
            RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            RenderSystem.popMatrix();
        }

        public static void drawImage(ResourceLocation resourceLocation, float x, float y, float width, float height, Vector4i color) {
            RenderSystem.pushMatrix();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.shadeModel(7425);
            mc.getTextureManager().bindTexture(resourceLocation);
            quadsBeginC(x, y, width, height, 7, color);
            RenderSystem.shadeModel(7424);
            RenderSystem.color4f(1, 1, 1, 1);
            RenderSystem.popMatrix();
        }

        public static void setColor(int color) {
            setColor(color, (float) (color >> 24 & 255) / 255.0F);
        }

        public static void setColor(int color, float alpha) {
            float r = (float) (color >> 16 & 255) / 255.0F;
            float g = (float) (color >> 8 & 255) / 255.0F;
            float b = (float) (color & 255) / 255.0F;
            RenderSystem.color4f(r, g, b, alpha);
        }

        public static void drawRoundFace(float x, float y, float width, float height, float radius, float alpha, AbstractClientPlayerEntity target) {
            try {
                final ResourceLocation skin = target.getLocationSkin();
                IMinecraft.mc.getTextureManager().bindTexture(skin);
                RenderSystem.pushMatrix();
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(770, 771);
                ShaderUtil.rounded_head_texture.attach();
                ShaderUtil.rounded_head_texture.setUniform("location", x * 2.0f, IMinecraft.mw.getHeight() - height * 2.0f - y * 2.0f);
                ShaderUtil.rounded_head_texture.setUniform("size", width * 2.0f, height * 2.0f);
                ShaderUtil.rounded_head_texture.setUniform("texture", 0);
                ShaderUtil.rounded_head_texture.setUniform("radius", radius * 2.0f);
                ShaderUtil.rounded_head_texture.setUniform("alpha", alpha);
                ShaderUtil.rounded_head_texture.setUniform("u", 0.125f);
                ShaderUtil.rounded_head_texture.setUniform("v", 0.125f);
                ShaderUtil.rounded_head_texture.setUniform("w", 0.125f);
                ShaderUtil.rounded_head_texture.setUniform("h", 0.125f);
                quadsBegin(x, y, width, height, 7);
                ShaderUtil.rounded_head_texture.detach();
                RenderSystem.disableBlend();
                RenderSystem.popMatrix();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public static void drawRectOutlineBuildingGradient(double x, double y, double width, double height, double size, int colors) {
            drawMCHorizontalBuilding(x + size, y, width - size, y + size, colors, colors);
            drawMCVerticalBuilding(x, y, x + size, height, colors, colors);

            drawMCVerticalBuilding(width - size, y, width, height, colors, colors);
            drawMCHorizontalBuilding(x + size, height - size, width - size, height, colors, colors);
        }

        private static final ShaderUtils CUSTOM_ROUNDED_GRADIENT = ShaderUtils.create("customRoundedGradient");

        public static void drawCustomGradientRoundedRect(MatrixStack stack, float x, float y, float width, float height, float radius1, float radius2, float radius3, float radius4, int bottomLeft, int topLeft, int bottomRight, int topRight) {
            MainWindow mw = mc.getMainWindow();
            CUSTOM_ROUNDED_GRADIENT.attach();
            CUSTOM_ROUNDED_GRADIENT.setUniformf("location", x * 2, (mw.getScaledHeight() - (height * 2)) - (y * 2));
            CUSTOM_ROUNDED_GRADIENT.setUniformf("rectSize", width * 2, height * 2);
            CUSTOM_ROUNDED_GRADIENT.setUniformf("radius", radius1, radius2, radius3, radius4);
            CUSTOM_ROUNDED_GRADIENT.setUniformf("color1", ColorUtils.rgba(bottomLeft));
            CUSTOM_ROUNDED_GRADIENT.setUniformf("color2", ColorUtils.rgba(topLeft));
            CUSTOM_ROUNDED_GRADIENT.setUniformf("color3", ColorUtils.rgba(bottomRight));
            CUSTOM_ROUNDED_GRADIENT.setUniformf("color4", ColorUtils.rgba(topRight));
            processDraw(() -> quadsBegin(x, y, width, height, 7));
            CUSTOM_ROUNDED_GRADIENT.detach();
        }

        public static void drawGradientRoundedRect(MatrixStack stack, float x, float y, float width, float height, float radius, int bottomLeft, int topLeft, int bottomRight, int topRight) {
            ROUNDED_GRADIENT.attach();
            ShaderUtils.setupRoundedRectUniforms(x, y, width, height, radius, ROUNDED_GRADIENT);
            ROUNDED_GRADIENT.setUniformf("color1", ColorUtils.rgba(bottomLeft));
            ROUNDED_GRADIENT.setUniformf("color2", ColorUtils.rgba(topLeft));
            ROUNDED_GRADIENT.setUniformf("color3", ColorUtils.rgba(bottomRight));
            ROUNDED_GRADIENT.setUniformf("color4", ColorUtils.rgba(topRight));
            processDraw(() -> quadsBegin(x, y, width, height, 7));
            ROUNDED_GRADIENT.detach();
        }

        public static void drawGradientRectCustom(MatrixStack stack, float x, float y, float width, float height, int color1, int color2, int color3, int color4) {
            float[] color1f = ColorUtils.rgba(color1);
            float[] color2f = ColorUtils.rgba(color2);
            float[] color3f = ColorUtils.rgba(color3);
            float[] color4f = ColorUtils.rgba(color4);
            Matrix4f matrix4f = stack.getLast().getMatrix();

            processDraw(() -> {
                GlStateManager.shadeModel(GL_SMOOTH);
                BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                BUFFER.pos(matrix4f, x, y + height, 0).color(color2f[0], color2f[1], color2f[2], color2f[3]).endVertex();
                BUFFER.pos(matrix4f, x + width, y + height, 0).color(color4f[0], color4f[1], color4f[2], color4f[3]).endVertex();
                BUFFER.pos(matrix4f, x + width, y, 0).color(color3f[0], color3f[1], color3f[2], color3f[3]).endVertex();
                BUFFER.pos(matrix4f, x, y, 0).color(color1f[0], color1f[1], color1f[2], color1f[3]).endVertex();
                TESSELLATOR.draw();
                GlStateManager.shadeModel(GL_FLAT);
            });
        }

        public static void drawCircle2(float x, float y, float radius, int color) {
            drawRoundedCorner(x, y, radius * 2, radius * 2, radius + 1, color);
        }
    }

    public static void processDraw(Runnable runnable) {
        GlStateManager.clearCurrentColor();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture();
        GlStateManager.disableAlphaTest();
        runnable.run();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        GlStateManager.clearCurrentColor();
    }

    public static class Render3D {

        public static void drawBlockBox(BlockPos blockPos, int color) {
            drawBox(new AxisAlignedBB(blockPos).offset(-mc.getRenderManager().info.getProjectedView().x, -mc.getRenderManager().info.getProjectedView().y, -mc.getRenderManager().info.getProjectedView().z), color);
        }

        public static void drawBlockBoxSkull(BlockPos blockPos, int color) {
            drawBoxSkull(new AxisAlignedBB(blockPos).offset(-mc.getRenderManager().info.getProjectedView().x, -mc.getRenderManager().info.getProjectedView().y, -mc.getRenderManager().info.getProjectedView().z), color);
        }

        public static void drawBoxVectorElytra(AxisAlignedBB bb, int color) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(1);

            float[] rgb = IntColor.rgb(color);
            GlStateManager.color4f(rgb[0], rgb[1], rgb[2], rgb[3]);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder vertexbuffer = tessellator.getBuffer();

            double minX = bb.minX;
            double minY = bb.minY;
            double minZ = bb.minZ;
            double maxX = minX + 0.5;
            double maxY = minY + 0.5;
            double maxZ = minZ + 0.5;

            vertexbuffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
            vertexbuffer.pos(minX, minY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(maxX, minY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(maxX, minY, maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(minX, minY, maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(minX, minY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            tessellator.draw();

            vertexbuffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
            vertexbuffer.pos(minX, maxY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(maxX, maxY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(maxX, maxY, maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(minX, maxY, maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(minX, maxY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            tessellator.draw();

            vertexbuffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
            vertexbuffer.pos(minX, minY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(minX, maxY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(maxX, minY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(maxX, maxY, minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(maxX, minY, maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(maxX, maxY, maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(minX, minY, maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(minX, maxY, maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            tessellator.draw();

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL_LINE_SMOOTH);
            GL11.glPopMatrix();
        }

        public static void renderFilledBox(BufferBuilder bufferBuilder, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
            float red = ((color >> 16) & 0xFF) / 255.0f;
            float green = ((color >> 8) & 0xFF) / 255.0f;
            float blue = (color & 0xFF) / 255.0f;
            float alpha = ((color >> 24) & 0xFF) / 255.0f;

            bufferBuilder.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        }

        public static void renderBlockOutline(MatrixStack matrixStack, VoxelShape shape, BlockPos blockPos) {
            matrixStack.push();
            RenderSystem.pushMatrix();
            RenderSystem.multMatrix(matrixStack.getLast().getMatrix());

            double renderX = mc.getRenderManager().renderPosX();
            double renderY = mc.getRenderManager().renderPosY();
            double renderZ = mc.getRenderManager().renderPosZ();
            RenderSystem.translated(-renderX, -renderY, -renderZ);

            RenderSystem.disableTexture();
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();

            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            RenderSystem.lineWidth(1);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();

            int outlineColorValue = selectedColor;
            int fillColorValue = ColorUtils.multAlpha(outlineColorValue, 0.5f);

            for (AxisAlignedBB aabb : shape.toBoundingBoxList()) {
                double minX = blockPos.getX() + aabb.minX;
                double minY = blockPos.getY() + aabb.minY;
                double minZ = blockPos.getZ() + aabb.minZ;
                double maxX = blockPos.getX() + aabb.maxX;
                double maxY = blockPos.getY() + aabb.maxY;
                double maxZ = blockPos.getZ() + aabb.maxZ;

                // Render filled box using RenderUtilka
                AxisAlignedBB bb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
                RenderUtilka.Render3D.drawBox(bb, fillColorValue);

                // Render wireframe box using provided method
                bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                renderWireframeBox(bufferBuilder, minX, minY, minZ, maxX, maxY, maxZ, outlineColorValue);
                tessellator.draw();
            }

            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();

            RenderSystem.translated(renderX, renderY, renderZ);
            RenderSystem.popMatrix();
            matrixStack.pop();
        }

        public static void renderWireframeBox(BufferBuilder bufferBuilder, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
            float red = ((color >> 16) & 0xFF) / 255.0f;
            float green = ((color >> 8) & 0xFF) / 255.0f;
            float blue = (color & 0xFF) / 255.0f;
            float alpha = ((color >> 24) & 0xFF) / 255.0f;

            bufferBuilder.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();

            bufferBuilder.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            bufferBuilder.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        }

        public static void drawBox(AxisAlignedBB bb, int color) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL_DEPTH_TEST);
            GL11.glEnable(GL_LINE_SMOOTH);
            GL11.glLineWidth(1);
            float[] rgb = IntColor.rgb(color);
            GlStateManager.color4f(rgb[0], rgb[1], rgb[2], rgb[3]);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder vertexbuffer = tessellator.getBuffer();
            vertexbuffer.begin(3, DefaultVertexFormats.POSITION);
            vertexbuffer.pos(bb.minX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.maxX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.maxX, bb.minY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.minX, bb.minY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.minX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            tessellator.draw();
            vertexbuffer.begin(3, DefaultVertexFormats.POSITION);
            vertexbuffer.pos(bb.minX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.maxX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.minX, bb.maxY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.minX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            tessellator.draw();
            vertexbuffer.begin(1, DefaultVertexFormats.POSITION);
            vertexbuffer.pos(bb.minX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.minX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.maxX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.maxX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.maxX, bb.minY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.minX, bb.minY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(bb.minX, bb.maxY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            tessellator.draw();
            GlStateManager.color4f(rgb[0], rgb[1], rgb[2], rgb[3]);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL_DEPTH_TEST);
            GL11.glDisable(GL_LINE_SMOOTH);
            GL11.glPopMatrix();

        }

        public static void drawBoxSkull(AxisAlignedBB bb, int color) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(1);
            float[] rgb = IntColor.rgb(color);
            GlStateManager.color4f(rgb[0], rgb[1], rgb[2], rgb[3]);

            double skullWidth = 0.5;
            double skullHeight = 0.5;
            double skullDepth = 0.5;

            double centerX = (bb.minX + bb.maxX) / 2.0;
            double centerY = (bb.minY + bb.maxY) / 2.0 - 0.25;
            double centerZ = (bb.minZ + bb.maxZ) / 2.0;

            AxisAlignedBB skullBB = new AxisAlignedBB(
                    centerX - skullWidth / 2, centerY - skullHeight / 2, centerZ - skullDepth / 2,
                    centerX + skullWidth / 2, centerY + skullHeight / 2, centerZ + skullDepth / 2
            );

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder vertexbuffer = tessellator.getBuffer();

            vertexbuffer.begin(3, DefaultVertexFormats.POSITION);
            vertexbuffer.pos(skullBB.minX, skullBB.minY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.maxX, skullBB.minY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.maxX, skullBB.minY, skullBB.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.minX, skullBB.minY, skullBB.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.minX, skullBB.minY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            tessellator.draw();

            vertexbuffer.begin(3, DefaultVertexFormats.POSITION);
            vertexbuffer.pos(skullBB.minX, skullBB.maxY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.maxX, skullBB.maxY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.maxX, skullBB.maxY, skullBB.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.minX, skullBB.maxY, skullBB.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.minX, skullBB.maxY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            tessellator.draw();

            vertexbuffer.begin(1, DefaultVertexFormats.POSITION);
            vertexbuffer.pos(skullBB.minX, skullBB.minY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.minX, skullBB.maxY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.maxX, skullBB.minY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.maxX, skullBB.maxY, skullBB.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.maxX, skullBB.minY, skullBB.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.maxX, skullBB.maxY, skullBB.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.minX, skullBB.minY, skullBB.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            vertexbuffer.pos(skullBB.minX, skullBB.maxY, skullBB.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
            tessellator.draw();

            GlStateManager.color4f(rgb[0], rgb[1], rgb[2], rgb[3]);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL_LINE_SMOOTH);
            GL11.glPopMatrix();
        }
    }

    public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(Manager.FUNCTION_MANAGER.skeletonEsp.size.getValue().floatValue());
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
        bufferBuilder.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawLine2(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(Manager.FUNCTION_MANAGER.skeletonEsp.size.getValue().floatValue() + 0.1f);
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
        bufferBuilder.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
        bufferBuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferBuilder);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
