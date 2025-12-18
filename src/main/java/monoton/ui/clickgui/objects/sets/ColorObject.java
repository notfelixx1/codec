package monoton.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.module.settings.imp.ColorSetting;
import monoton.ui.clickgui.Panel;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.font.Fonts;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import org.joml.Vector4i;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.awt.Color;

public class ColorObject extends Object {
    public ModuleObject object;
    public ColorSetting setting;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;
    private boolean colorSelectorDragging;
    private boolean hueSelectorDragging;
    private boolean alphaSelectorDragging;

    public ColorObject(ModuleObject object, ColorSetting setting) {
        height = 11;
        this.object = object;
        this.setting = setting;
        float[] hsb = RGBtoHSB(setting.get());
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = (setting.get() >> 24 & 0xFF) / 255F;
    }

    protected boolean extended;
    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!setting.visible()) {
            height -= 19F;
        }
        if (!setting.visible()) return;
        y += 4f;
        x -= 3.5f;
        width += 7.5f;
        height = (extended ? 82 : 3F);

        int textColor = monoton.ui.clickgui.Panel.getColorByName("textColor");
        int iconnoColor = Panel.getColorByName("iconnoColor");

        Fonts.intl[11].drawString(stack, setting.getName(), x + 14.5f, y + 1f, object.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));
        RenderUtilka.Render2D.drawRoundOutline(x + width - 21F, y - 1.5F, 7f, 7f, 3f, -0.6f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(object.module.state ? ColorUtils.setAlpha(iconnoColor, 61): ColorUtils.setAlpha(iconnoColor, 31), object.module.state ? ColorUtils.setAlpha(iconnoColor, 61): ColorUtils.setAlpha(iconnoColor, 31), object.module.state ? ColorUtils.setAlpha(iconnoColor, 61): ColorUtils.setAlpha(iconnoColor, 31), object.module.state ? ColorUtils.setAlpha(iconnoColor, 61): ColorUtils.setAlpha(iconnoColor, 31)));

        RenderUtilka.Render2D.drawRoundedRect(x + width - 19f, y + 0.5f , 3f, 3f, 1F, object.module.state ? ColorUtils.setAlpha(setting.get(), 245) : ColorUtils.setAlpha(setting.get(), 122));

        if (extended) {
            RenderUtilka.Render2D.drawRoundedRect(x + 10, y + 8, 61f, 78f, 4F, new Color(14, 13, 20, 214).getRGB());
            if (colorSelectorDragging && RenderUtilka.isInRegion(mouseX, mouseY, x + 13.5f, y + 12, 54, 54)) {
                float xDifference = mouseX - (x + 13.5f);
                saturation = Math.max(0, Math.min(1, xDifference / 54));
                float yDifference = mouseY - (y + 12);
                brightness = Math.max(0, Math.min(1, 1 - yDifference / 54));
                setting.setValue(ColorUtils.applyOpacity(new Color(Color.HSBtoRGB(hue, saturation, brightness)), alpha).getRGB());
            }

            RenderUtilka.Render2D.drawGradientRoundedRect(stack, x + 13.5f, y + 12, 54, 54, 3, -1, Color.BLACK.getRGB(), Color.HSBtoRGB(hue, 1, 1), Color.BLACK.getRGB());

            float selectorX = x + 13.5f + saturation * 54;
            float selectorY = y + 12 + (1 - brightness) * 54;

            selectorX = Math.max(x + 13.5f, Math.min(x + 13.5f + 54 - 4, selectorX));
            selectorY = Math.max(y + 12, Math.min(y + 12 + 54 - 4, selectorY));

            RenderUtilka.Render2D.drawCircle2(selectorX, selectorY, 2.25f, ColorUtils.rgba(255, 255, 255, 255));

            // Hue slider
            if (hueSelectorDragging && RenderUtilka.isInRegion(mouseX, mouseY, x + 13.5f, y + 70, 54, 4.5f)) {
                float xDifference = mouseX - (x + 13.5f);
                hue = Math.max(0, Math.min(1, xDifference / 54));
                setting.setValue(ColorUtils.applyOpacity(new Color(Color.HSBtoRGB(hue, saturation, brightness)), alpha).getRGB());
            }

            float times = 5;
            float size = 54f / times; // Aligned with color selector width
            float sliderX = x + 13.5f; // Aligned with color selector x
            float sliderY = y + 70; // Positioned below color selector

            float hueSelectorX = hue * 54;
            hueSelectorX = Math.max(0, Math.min(54 - 4.5f, hueSelectorX)); // Clamp selector position

            for (int i = 0; i < times; i++) {
                int color1 = Color.HSBtoRGB(0.2F * i, 1, 1);
                int color2 = Color.HSBtoRGB(0.2F * (i + 1), 1, 1);

                if (i == 0) {
                    RenderUtilka.Render2D.drawCustomGradientRoundedRect(stack, sliderX, sliderY, size + 0.4f, 4.5f, 4, 4, 0, 0, color1, color1, color2, color2);
                } else if (i == times - 1) {
                    RenderUtilka.Render2D.drawCustomGradientRoundedRect(stack, sliderX, sliderY, size, 4.5f, 0, 0, 4, 4, color1, color1, color2, color2);
                } else {
                    RenderUtilka.Render2D.drawGradientRectCustom(stack, sliderX + 0.001f, sliderY, size + 0.4f, 4.5f, color1, color1, color2, color2);
                }

                sliderX += size;
            }

            RenderUtilka.Render2D.drawRoundOutline(x + 13.5f + hueSelectorX, sliderY, 4.5f, 4.5f, 2f, 0.4f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(new Color(255, 255, 255, 255).getRGB(), new Color(255, 255, 255, 255).getRGB(), new Color(255, 255, 255, 255).getRGB(), new Color(255, 255, 255, 255).getRGB()));

            int color = Color.HSBtoRGB(hue, saturation, brightness);

            // Alpha slider
            if (alphaSelectorDragging && RenderUtilka.isInRegion(mouseX, mouseY, x + 13.5f, y + 77, 54, 4.5f)) {
                float xDifference = mouseX - (x + 13.5f);
                alpha = Math.max(0, Math.min(1, xDifference / 54));
                setting.setValue(ColorUtils.applyOpacity(new Color(color), alpha).getRGB());
            }

            RenderUtilka.Render2D.drawGradientRoundedRect(stack, x + 13.5f, y + 77, 54, 4.5f, 2, -1, -1, color, color);

            float alphaSelectorX = alpha * 54;
            alphaSelectorX = Math.max(0, Math.min(54 - 4.5f, alphaSelectorX)); // Clamp selector position

            RenderUtilka.Render2D.drawRoundOutline(x + 13.5f + alphaSelectorX, y + 77, 4.5f, 4.5f, 2f, 0.4f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(new Color(255, 255, 255, 255).getRGB(), new Color(255, 255, 255, 255).getRGB(), new Color(255, 255, 255, 255).getRGB(), new Color(255, 255, 255, 255).getRGB()));
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!setting.visible()) return;

        if (RenderUtilka.isInRegion(mouseX, mouseY, x, y, width, 11) && mouseButton == 1) {
            extended = !extended;
        }

        if (!colorSelectorDragging && RenderUtilka.isInRegion(mouseX, mouseY, x + 13.5f, y + 12, 69, 60) && mouseButton == 0) {
            colorSelectorDragging = true;
        }

        if (!hueSelectorDragging && RenderUtilka.isInRegion(mouseX, mouseY, x + 13.5f, y + 70, 54, 4) && mouseButton == 0) {
            hueSelectorDragging = true;
        }

        if (!alphaSelectorDragging && RenderUtilka.isInRegion(mouseX, mouseY, x + 13.5f, y + 77, 54, 4) && mouseButton == 0) {
            alphaSelectorDragging = true;
        }
    }

    private float[] RGBtoHSB(int color) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        return Color.RGBtoHSB(red, green, blue, null);
    }

    @Override
    public void drawComponent(MatrixStack matrixStack, int mouseX, int mouseY) {
        // Пустая реализация, если нужно, добавьте логику
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        colorSelectorDragging = false;
        hueSelectorDragging = false;
        alphaSelectorDragging = false;
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        // Пустая реализация
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        // Пустая реализация
    }
}