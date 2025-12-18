package monoton.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.control.Manager;
import monoton.module.settings.imp.ButtonSetting;
import monoton.ui.clickgui.Panel;
import monoton.utils.other.SoundUtils;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.font.Fonts;
import org.joml.Vector4i;

public class ButtonObject extends Object {

    public ButtonSetting set;
    public ModuleObject object;

    public ButtonObject(ModuleObject object, ButtonSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        y -= 1f;
        height -= 1;
        x -= 4;
        width += 12f;
        super.draw(stack, mouseX, mouseY);
        int textColor = monoton.ui.clickgui.Panel.getColorByName("textColor");
        int iconnoColor = Panel.getColorByName("iconnoColor");
        float wwidth = 74;

        boolean isHovered = RenderUtilka.isInRegion(mouseX, mouseY, x + 15, y, wwidth, 13);

        RenderUtilka.Render2D.drawRoundedRect(x + 15, y, wwidth, 13, 2.5f, isHovered ? object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 10) : object.module.state ? ColorUtils.setAlpha(iconnoColor, 15) : ColorUtils.setAlpha(iconnoColor, 5));
        RenderUtilka.Render2D.drawRoundOutline(x + 15, y, wwidth, 13, 2.5f, -0.6f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(isHovered ? object.module.state ? ColorUtils.setAlpha(iconnoColor, 61) : ColorUtils.setAlpha(iconnoColor, 31) : object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 15), isHovered ? object.module.state ? ColorUtils.setAlpha(iconnoColor, 61) : ColorUtils.setAlpha(iconnoColor, 31) : object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 15), isHovered ? object.module.state ? ColorUtils.setAlpha(iconnoColor, 61) : ColorUtils.setAlpha(iconnoColor, 31) : object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 15), isHovered ? object.module.state ? ColorUtils.setAlpha(iconnoColor, 61) : ColorUtils.setAlpha(iconnoColor, 31) : object.module.state ? ColorUtils.setAlpha(iconnoColor, 31) : ColorUtils.setAlpha(iconnoColor, 15)));

        Fonts.intl[13].drawCenteredString(stack, set.getCurrentToggleText(), x + 52, y + 5.2, isHovered ? object.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122) : object.module.state ? ColorUtils.setAlpha(textColor, 122) : ColorUtils.setAlpha(textColor, 61));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (object.module.expanded) {
            height += 25;
            if (isHovered(mouseX, mouseY)) {
                float wwidth = 74;
                if (RenderUtilka.isInRegion(mouseX, mouseY, x + 15, y - 1.5f, wwidth, 13)) {
                    if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                        SoundUtils.playSound("moduleclose.wav", 60.0F);
                    }
                    set.toggle();
                    set.getRun().run();
                }
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