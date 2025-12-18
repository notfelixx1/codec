package monoton.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.control.Manager;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.ui.clickgui.Panel;
import monoton.utils.other.SoundUtils;
import monoton.utils.font.styled.StyledFont;
import monoton.utils.render.*;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.font.Fonts;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.joml.Vector4i;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.Random;

public class MultiObject extends Object {

    public MultiBoxSetting set;
    public ModuleObject object;

    public MultiObject(ModuleObject object, MultiBoxSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        super.draw(stack, mouseX, mouseY);
        int offsetY = 0;
        y += 3;
        x -= 3;
        height += 4.5f;

        int textColor = Panel.getColorByName("textColor");
        int iconnoColor = Panel.getColorByName("iconnoColor");

        int totalOptions = set.options.size();
        int enabledOptions = 0;
        for (BooleanOption option : set.options) {
            if (option.get()) {
                enabledOptions++;
            }
        }

        String enabledPart = String.valueOf(enabledOptions);
        String separator = "/";
        String totalPart = String.valueOf(totalOptions);

        StyledFont fontmono = Fonts.intl[12];
        float enabledWidth = fontmono.getWidth(enabledPart);
        float separatorWidth = fontmono.getWidth(separator);
        float totalWidth = fontmono.getWidth(totalPart);
        float totalTitleWidth = enabledWidth + separatorWidth + totalWidth;

        float baseX = x + 88.5f - totalTitleWidth;
        float baseY = y + height / 2f - 7f;

        fontmono.drawString(stack, enabledPart, baseX, baseY, object.module.state ? ColorUtils.setAlpha(textColor, 184) : ColorUtils.setAlpha(textColor, 61));
        fontmono.drawString(stack, separator, baseX + enabledWidth, baseY, object.module.state ? ColorUtils.setAlpha(textColor, 122) : ColorUtils.setAlpha(textColor, 31));
        fontmono.drawString(stack, totalPart, baseX + enabledWidth + separatorWidth, baseY, object.module.state ? ColorUtils.setAlpha(textColor, 184) : ColorUtils.setAlpha(textColor, 61));

        Fonts.intl[12].drawString(stack, set.getName(), x + 14, y + height / 2f - 7f, object.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));

        height += 8;
        float maxWidth = 75f;
        float currentX = x + 16.5f;
        float currentY = y + 12f;
        float lineHeight = 11.2f;
        int i = 0;

        float tempX = x + 16.5f;
        int lineCount = 1;
        for (BooleanOption mode : set.options) {
            String modeName = mode.getName();
            float textWidth = Fonts.intl[11].getWidth(modeName);
            float paddedWidth = textWidth + 5.0f;

            if (tempX + paddedWidth > x + 16.5f + maxWidth) {
                tempX = x + 16.5f;
                lineCount++;
            }
            tempX += paddedWidth;
        }

        float rectHeight = lineHeight * lineCount + 6f;
        RenderUtilka.Render2D.drawRoundedRect(x + 13, y + 6f, 76f, rectHeight - 4f, 2.5f, object.module.state ? ColorUtils.setAlpha(iconnoColor, 10) : ColorUtils.setAlpha(iconnoColor, 5));

        for (BooleanOption mode : set.options) {
            String modeName = mode.getName();
            float textWidth = Fonts.intl[11].getWidth(modeName);
            float paddedWidth = textWidth + 5.0f;

            if (currentX + paddedWidth > x + 16.5f + maxWidth) {
                currentX = x + 16.5f;
                currentY += lineHeight;
                offsetY += lineHeight;
            }

            boolean isEnabled = set.get(i);
            if (isEnabled) {
                RenderUtilka.Render2D.drawRoundedRect(currentX - 2f, currentY - 4.5f, Fonts.intl[11].getWidth(modeName) + 4, 10, 1.5f, object.module.state ? ColorUtils.setAlpha(iconnoColor, 15) : ColorUtils.setAlpha(iconnoColor, 5));
                RenderUtilka.Render2D.drawRoundOutline(currentX - 2f, currentY - 4.5f, Fonts.intl[11].getWidth(modeName) + 4, 10, 1.5f, -0.9f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(object.module.state ? ColorUtils.setAlpha(iconnoColor, 61) : ColorUtils.setAlpha(iconnoColor, 31)));
            }
            Fonts.intl[11].drawString(stack, modeName, currentX, currentY, isEnabled ? object.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122) : object.module.state ? ColorUtils.setAlpha(textColor, 122) : ColorUtils.setAlpha(textColor, 61));

            currentX += paddedWidth;
            i++;
        }

        height += rectHeight - 21.5f;
        y -= 10;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (object.module.expanded) {
            float currentX = x + 16.5f;
            float currentY = y + 17f;
            float maxWidth = 75f;
            float lineHeight = 11.2f;
            int i = 0;
            for (BooleanOption mode : set.options) {
                String modeName = mode.getName();
                float textWidth = Fonts.intl[11].getWidth(modeName);
                float paddedWidth = textWidth + 5.0f;
                float textHeight = lineHeight - 1.2f;

                if (currentX + paddedWidth > x + 16.5f + maxWidth) {
                    currentX = x + 16.5f;
                    currentY += lineHeight;
                }

                if (RenderUtilka.isInRegion(mouseX, mouseY, currentX, currentY, textWidth, textHeight)) {
                    if (mouseButton == 0) {
                        set.set(i, !set.get(i));
                        int volume = new Random().nextInt(13) + 59;
                        if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                            SoundUtils.playSound("select.wav", volume);
                        }
                    }
                }
                currentX += paddedWidth;
                i++;
            }
        }
    }

    @Override
    public void drawComponent(MatrixStack matrixStack, int mouseX, int mouseY) {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
    }
}