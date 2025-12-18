package monoton.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.module.settings.imp.InfoSetting;
import monoton.ui.clickgui.Panel;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.font.Fonts;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;

public class InfoObject extends Object {

    public InfoSetting set;
    public ModuleObject object;

    public InfoObject(ModuleObject object, InfoSetting set) {
        this.object = object;
        this.set = set;
        setting = set;

    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        y -= 3f;
        height -= 10;
        x -= 4;
        width += 12f;
        super.draw(stack, mouseX, mouseY);
        int infoColor = Panel.getColorByName("infoColor");

        Fonts.intl[14].drawCenteredString(stack, set.getName(), x + 52, y + 4, object.module.state ? ColorUtils.setAlpha(infoColor, 122) : ColorUtils.setAlpha(infoColor, 61));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
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

