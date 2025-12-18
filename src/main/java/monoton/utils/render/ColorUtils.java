package monoton.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import monoton.utils.math.MathUtil;
import monoton.utils.other.MathUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import monoton.utils.IMinecraft;
import org.joml.Vector4i;

import java.awt.*;

import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX_COLOR;

public class ColorUtils implements IMinecraft {
    public static final int green = ColorUtils.rgba(36, 218, 118, 255);
    public static final int yellow = ColorUtils.rgba(255, 196, 67, 255);
    public static final int orange = ColorUtils.rgba(255, 134, 0, 255);
    public static final int red = ColorUtils.rgba(239, 72, 54, 255);

    public static int replAlpha(int color, int alpha) {
        return getColor(getRed(color), getGreen(color), getBlue(color), alpha);
    }

    public static int multDark(int color, float brPC) {
        return getColor(Math.round(getRed(color) * brPC), Math.round(getGreen(color) * brPC), Math.round(getBlue(color) * brPC), getAlpha(color));
    }

    public static void setAlphaColor(final int color, final float alpha) {
        final float red = (float) (color >> 16 & 255) / 255.0F;
        final float green = (float) (color >> 8 & 255) / 255.0F;
        final float blue = (float) (color & 255) / 255.0F;
        RenderSystem.color4f(red, green, blue, alpha);
    }
    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    public static int getOverallColorFrom(int color1, int color2, float percentTo2) {
        final int finalRed = (int) MathUtil.lerp(color1 >> 16 & 0xFF, color2 >> 16 & 0xFF, percentTo2),
                finalGreen = (int) MathUtil.lerp(color1 >> 8 & 0xFF, color2 >> 8 & 0xFF, percentTo2),
                finalBlue = (int) MathUtil.lerp(color1 & 0xFF, color2 & 0xFF, percentTo2),
                finalAlpha = (int) MathUtil.lerp(color1 >> 24 & 0xFF, color2 >> 24 & 0xFF, percentTo2);
        return getColor(finalRed, finalGreen, finalBlue, finalAlpha);
    }

    public static int toColor(String hexColor) {
        int argb = Integer.parseInt(hexColor.substring(1), 16);
        return setAlpha(argb, 255);
    }
    public static void setColor(int color) {
        setAlphaColor(color, (float) (color >> 24 & 255) / 255.0F);
    }

    public static int astolfo(int speed, int offset, float saturation, float brightness, float alpha) {
        float hue = (float) calculateHueDegrees(speed, offset);
        hue = (float) ((double) hue % 360.0);
        float hueNormalized;
        return reAlphaInt(
                Color.HSBtoRGB((double) ((hueNormalized = hue % 360.0F) / 360.0F) < 0.5 ? -(hueNormalized / 360.0F) : hueNormalized / 360.0F, saturation, brightness),
                Math.max(0, Math.min(255, (int) (alpha * 255.0F)))
        );
    }

    private static int calculateHueDegrees(int divisor, int offset) {
        long currentTime = System.currentTimeMillis();
        long calculatedValue = (currentTime / divisor + offset) % 360L;
        return (int) calculatedValue;
    }


    public static int rgba(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static float[] rgba(final int color) {
        return new float[]{
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f
        };
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int rgba(double r, double g, double b, double a) {
        return rgba((int) r, (int) g, (int) b, (int) a);
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

    public static int getColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= alpha << 24;
        color |= red << 16;
        color |= green << 8;
        return color |= blue;
    }

    public static int getColor(int bright) {
        return getColor(bright, bright, bright, 255);
    }

    public static float[] getColor2(int color) {
        return new float[]{red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f};
    }


    public static int gradient(int speed, int index, int... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int) (angle / 360f * colors.length);
        if (colorIndex == colors.length) {
            colorIndex--;
        }
        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return interpolateColor(color1, color2, angle / 360f * colors.length - colorIndex);
    }

    public static int gradient(int speed, int index, Color color1, Color color2, float progress) {
        float fraction = ((System.currentTimeMillis() / (float) speed + index) % 1.0f);
        fraction = fraction * progress;

        int red = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * fraction);
        int green = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int blue = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * fraction);
        int alpha = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);

        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));
        alpha = Math.max(0, Math.min(255, alpha));

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        int red1 = getRed(color1);
        int green1 = getGreen(color1);
        int blue1 = getBlue(color1);
        int alpha1 = getAlpha(color1);

        int red2 = getRed(color2);
        int green2 = getGreen(color2);
        int blue2 = getBlue(color2);
        int alpha2 = getAlpha(color2);

        int interpolatedRed = interpolateInt(red1, red2, amount);
        int interpolatedGreen = interpolateInt(green1, green2, amount);
        int interpolatedBlue = interpolateInt(blue1, blue2, amount);
        int interpolatedAlpha = interpolateInt(alpha1, alpha2, amount);

        return (interpolatedAlpha << 24) | (interpolatedRed << 16) | (interpolatedGreen << 8) | interpolatedBlue;
    }


    public static int interpolate(int start, int end, float value) {
        double percent = MathHelper.clamp(value, 0f, 1f);
        return getColor(MathUtils.interpolate(red(start), red(end), percent), MathUtils.interpolate(green(start), green(end), percent), MathUtils.interpolate(blue(start), blue(end), percent), MathUtils.interpolate(alpha(start), alpha(end), percent));
    }

    public static Double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return interpolate(oldValue, newValue, (float) interpolationValue);
    }
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r, g, b;

        if (saturation == 0) {
            int value = (int) (brightness * 255.0f + 0.5f);
            return 0xff000000 | (value << 16) | (value << 8) | value;
        }

        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1.0f - saturation);
        float q = brightness * (1.0f - saturation * f);
        float t = brightness * (1.0f - (saturation * (1.0f - f)));

        switch ((int) h) {
            case 0:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (t * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 1:
                r = (int) (q * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 2:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (t * 255.0f + 0.5f);
                break;
            case 3:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (q * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 4:
                r = (int) (t * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 5:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (q * 255.0f + 0.5f);
                break;
            default:
                throw new IllegalArgumentException("Invalid hue value");
        }

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public static int setAlpha(int color, int alpha) {
        Color c = new Color(color, true);
        int existingAlpha = c.getAlpha();
        int finalAlpha = (existingAlpha != 255) ? Math.max(3, (existingAlpha * alpha / 255)) : Math.max(3, alpha);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), finalAlpha).getRGB();
    }

    public static int setAlpha(int color, int alpha, int dopAlpha) {
        Color c = new Color(color, true);
        int finalAlpha = 200;
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), finalAlpha).getRGB();
    }

    public static void drawImageAlpha(ResourceLocation resourceLocation, float x, float y, float width, float height,
                                      int color) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(resourceLocation);
        quads(x, y, width, height, 7, color);
        RenderSystem.shadeModel(7424);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.popMatrix();
    }

    public static void quads(float x, float y, float width, float height, int glQuads, int color) {
        BUFFER.begin(glQuads, POSITION_TEX_COLOR);
        {
            BUFFER.pos(x, y, 0).tex(0, 0).color(color).endVertex();
            BUFFER.pos(x, y + height, 0).tex(0, 1).color(color).endVertex();
            BUFFER.pos(x + width, y + height, 0).tex(1, 1).color(color).endVertex();
            BUFFER.pos(x + width, y, 0).tex(1, 0).color(color).endVertex();
        }
        TESSELLATOR.draw();
    }

    public static int reAlphaInt(final int color, final int alpha) {
        int existingAlpha = (color >> 24) & 0xFF;
        int clampedAlpha = MathHelper.clamp(alpha, 0, 255);
        int finalAlpha = (existingAlpha != 255) ? Math.max(3, (existingAlpha * clampedAlpha / 255)) : clampedAlpha;
        return (finalAlpha << 24) | (color & 0xFFFFFF);
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

    public static float getHue(int selectedColor) {
        int r = (selectedColor >> 16) & 0xFF;
        int g = (selectedColor >> 8) & 0xFF;
        int b = selectedColor & 0xFF;

        float red = r / 255.0f;
        float green = g / 255.0f;
        float blue = b / 255.0f;

        float max = Math.max(Math.max(red, green), blue);
        float min = Math.min(Math.min(red, green), blue);
        float delta = max - min;

        float hue;
        if (delta == 0) {
            hue = 0;
        } else if (max == red) {
            hue = ((green - blue) / delta) % 6;
        } else if (max == green) {
            hue = ((blue - red) / delta) + 2;
        } else {
            hue = ((red - green) / delta) + 4;
        }

        hue = hue * 60 / 360;
        if (hue < 0) {
            hue += 1;
        }

        return hue;
    }

    public static int intRGBAtoRGBA(int i, int buttonAlpha) {
        int red = (i >> 16) & 0xFF;
        int green = (i >> 8) & 0xFF;
        int blue = i & 0xFF;
        return (buttonAlpha & 0xFF) << 24 | (red << 16) | (green << 8) | blue;
    }
    public static int alpha(int c) {
        return (c >> 24) & 0xFF;
    }
    public static int red(int c) {
        return (c >> 16) & 0xFF;
    }

    public static int green(int c) {
        return (c >> 8) & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }
    public static int multAlpha(int color, float percent01) {
        return getColor(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
    }

    private static final int selectedColor = new Color(0, 120, 255).getRGB(); // ARGB: #FF0078FF

    public static int gradientModule(int i) {
        Color baseColor = new Color(selectedColor);
        int red = baseColor.getRed();
        int green = baseColor.getGreen();
        int blue = baseColor.getBlue();
        int alpha = baseColor.getAlpha();

        float lightenFactor = 1.3f; // 30% brighter
        int lightRed = Math.min(255, (int) (red * lightenFactor));
        int lightGreen = Math.min(255, (int) (green * lightenFactor));
        int lightBlue = Math.min(255, (int) (blue * lightenFactor));
        Color lightColor = new Color(lightRed, lightGreen, lightBlue, alpha);

        float t = (float) (Math.sin(Math.toRadians(i % 360)) + 1) / 2; // Maps i to [0, 1]

        int interpolatedRed = (int) (red + (lightRed - red) * t);
        int interpolatedGreen = (int) (green + (lightGreen - green) * t);
        int interpolatedBlue = (int) (blue + (lightBlue - blue) * t);

        return (alpha << 24) | (interpolatedRed << 16) | (interpolatedGreen << 8) | interpolatedBlue;
    }

    public static int setAlpha(int color, float alpha) {
        int alphaInt = (int) (Math.min(1.0f, Math.max(0.0f, alpha)) * 255);
        return (color & 0x00FFFFFF) | (alphaInt << 24);
    }


    public static int hex(String hex) {
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);

        if (s.length() == 3 || s.length() == 4) {
            char r = s.charAt(0);
            char g = s.charAt(1);
            char b = s.charAt(2);
            char a = s.length() == 4 ? s.charAt(3) : 'F';
            s = ("" + r + r + g + g + b + b + a + a);
        }

        if (s.length() == 6) {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return ColorUtils.getColor(r, g, b, 255);
        }
        if (s.length() == 8) {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            int a = Integer.parseInt(s.substring(6, 8), 16);
            return ColorUtils.getColor(r, g, b, a);
        }
        throw new IllegalArgumentException("Unsupported hex format: " + hex);
    }


    public static int overCol(int color1, int color2, float factor) {
        // Extract RGB and alpha components from color1
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        // Extract RGB and alpha components from color2
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        // Interpolate components based on factor (hurtPC)
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        int a = (int) (a1 + (a2 - a1) * factor);

        // Clamp values to ensure they stay in the valid range (0-255)
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));
        a = Math.min(255, Math.max(0, a));

        // Combine components into ARGB int
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}