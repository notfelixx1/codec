package monoton.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.control.Manager;
import monoton.module.settings.imp.ModeSetting;
import monoton.ui.clickgui.Panel;
import monoton.utils.other.SoundUtils;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.font.Fonts;
import org.joml.Vector4i;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.Random;

public class ModeObject extends Object {
    public ModeSetting set;
    public ModuleObject object;

    public ModeObject(ModuleObject object, ModeSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        y -= 0.5f;
        x -= 3;
        width += 7;
        int textColor = Panel.getColorByName("textColor");
        int iconnoColor = Panel.getColorByName("iconnoColor");

        super.draw(stack, mouseX, mouseY);

        float offsetY = 0;
        height += 7;

        Fonts.intl[12].drawString(stack, set.getName(), x + 14, y + height / 2f - 5f, object.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));

        Fonts.intl[12].drawString(stack, "Мод " + (set.getIndex() + 1), x + 88 - Fonts.intl[12].getWidth("Мод " + (set.getIndex() + 1)), y + height / 2f - 5f, object.module.state ? ColorUtils.setAlpha(textColor, 184) : ColorUtils.setAlpha(textColor, 61));
        height += 10 * set.modes.length;

        int i = 0;
        for (String mode : set.modes) {
            if (set.getIndex() == i) {
                Fonts.intl[11].drawString(stack, mode, x + 14, y + 14f + offsetY, object.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));
                RenderUtilka.Render2D.drawRoundedRect(x + 81, y + 11f + offsetY, 7f, 7f, 3F, object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 15));
                RenderUtilka.Render2D.drawRoundOutline(x + 81, y + 11f + offsetY, 7f, 7f, 3f, -0.6f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(object.module.state ? ColorUtils.setAlpha(iconnoColor, 61) : ColorUtils.setAlpha(iconnoColor, 61), ColorUtils.setAlpha(iconnoColor, 61), ColorUtils.setAlpha(iconnoColor, 61), ColorUtils.setAlpha(iconnoColor, 61)));

                RenderUtilka.Render2D.drawRoundedRect(x + 83f, y + 13f + offsetY, 3f, 3f, 1F, object.module.state ? ColorUtils.setAlpha(iconnoColor, 245) : ColorUtils.setAlpha(iconnoColor, 122));
            } else {
                Fonts.intl[11].drawString(stack, mode, x + 14, y + 14f + offsetY, object.module.state ? ColorUtils.setAlpha(textColor, 184) : ColorUtils.setAlpha(textColor, 61));
                RenderUtilka.Render2D.drawRoundOutline(x + 81, y + 11f + offsetY, 7f, 7f, 3f, -0.6f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 15), object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 15), object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 15), object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 15)));
            }
            offsetY += 10;
            i++;
        }
        height -= 13;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (object.module.expanded) {
            y -= 0.5f;
            x -= 3;
            width += 7;
            float offsetY = 0;
            height += 7;
            int i = 0;
            for (String mode : set.modes) {
                if (RenderUtilka.isInRegion(mouseX, mouseY, x + 15, y + 12f + offsetY, 73, Fonts.intl[11].getFontHeight() / 2f + 3)) {
                    set.setIndex(i);
                    int volume = new Random().nextInt(13) + 59;
                    if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                        SoundUtils.playSound("select.wav", volume);
                    }
                }
                offsetY += 10;
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