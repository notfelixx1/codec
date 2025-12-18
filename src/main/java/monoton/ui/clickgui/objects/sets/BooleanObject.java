package monoton.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.control.Manager;
import monoton.module.settings.imp.BooleanOption;
import monoton.ui.clickgui.Panel;
import monoton.utils.other.SoundUtils;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import monoton.utils.render.SmartScissor;
import monoton.utils.render.StencilUtils;
import monoton.utils.render.animation.AnimationMath;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.font.Fonts;
import org.joml.Vector4i;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.awt.*;
import java.util.Random;

public class BooleanObject extends Object {

    public ModuleObject object;
    public BooleanOption set;
    public float enabledAnimation;

    public BooleanObject(ModuleObject object, BooleanOption set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        super.draw(stack, mouseX, mouseY);
        x -= 3.5f;
        y -= 3;
        height -= 5;
        width += 6;
        int textColor = monoton.ui.clickgui.Panel.getColorByName("textColor");
        int iconnoColor = Panel.getColorByName("iconnoColor");
        double max = !set.get() ? 0 : 8.5f;
        this.enabledAnimation = AnimationMath.fast(enabledAnimation, (float) max, 10);
        SmartScissor.push();
        SmartScissor.setFromComponentCoordinates(x + 14.1f, y, (int) this.width - 37, 14);
        Fonts.intl[11].drawString(stack, set.getName(), x + 14.5f, y + 7f, object.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));
        SmartScissor.pop();
        StencilUtils.uninitStencilBuffer();
        int color2 = ColorUtils.interpolateColor(ColorUtils.setAlpha(iconnoColor, object.module.state ? 31 : 15), ColorUtils.setAlpha(iconnoColor, object.module.state ? 61 : 31), enabledAnimation / 6.5f);
        int color1 = ColorUtils.interpolateColor(ColorUtils.setAlpha(iconnoColor, 0), ColorUtils.setAlpha(iconnoColor, object.module.state ? 31 : 15), enabledAnimation / 6.5f);
        RenderUtilka.Render2D.drawRoundedRect(x + 74 + 7f,y + 3.5f,8, 8, 2.5f,color1);
        RenderUtilka.Render2D.drawRoundOutline(x + 74 + 7f,y + 3.5f,8, 8, 2.5f, -0.6f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(color2, color2, color2, color2));
        int color3 = ColorUtils.interpolateColor(ColorUtils.rgba(22, 22, 22, 0), object.module.state ? ColorUtils.setAlpha(iconnoColor, 245) : ColorUtils.setAlpha(iconnoColor, 124), enabledAnimation / 6.5f);
        Fonts.icon[9].drawCenteredString(stack, "i", x + 77.8f + 7.5f,y + 8f, color3);
    }
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (object.module.expanded) {
            if (mouseButton == 0) {
                if (isHovered(mouseX, mouseY - 5)) {
                    set.toggle();
                    int volume = new Random().nextInt(13) + 59;
                    if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                        SoundUtils.playSound("select.wav", volume);
                    }
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
