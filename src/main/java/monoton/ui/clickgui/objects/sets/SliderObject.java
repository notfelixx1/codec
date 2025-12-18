package monoton.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.module.settings.imp.SliderSetting;
import monoton.ui.clickgui.Panel;
import monoton.utils.math.MathUtil;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import monoton.utils.render.SmartScissor;
import monoton.utils.render.StencilUtils;
import monoton.utils.render.animation.AnimationMath;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.font.Fonts;
import org.joml.Vector2i;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.awt.*;

import static monoton.ui.clickgui.Panel.selectedColor;
import static org.joml.Math.lerp;

public class SliderObject extends Object {

    public ModuleObject object;
    public SliderSetting set;
    public boolean sliding;

    public float animatedVal;
    public float animatedThumbX;
    private float slider = 0;

    public SliderObject(ModuleObject object, SliderSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
        animatedThumbX = x + 10 + 3 + ((set.getValue().floatValue() - set.getMin()) / (set.getMax() - set.getMin())) * (width - 20); // Инициализация
    }
    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!setting.visible()) return;
        super.draw(stack, mouseX, mouseY);
        int firstColor = selectedColor;
        int secondColor = selectedColor;
        x -= 2f;
        y += 2f;
        width += 4.5f;
        height -= 2.5f;
        int textColor = monoton.ui.clickgui.Panel.getColorByName("textColor");
        int warnColor = Panel.getColorByName("warnColor");
        if (sliding) {
            float value = (float) ((mouseX - (x + 10 + 3)) / (width - 30) * (set.getMax() - set.getMin()) + set.getMin());
            value = (float) MathUtil.round(value, set.getIncrement());
            set.setValue(value);
        }

        int firstColor2 = selectedColor;

        float sliderWidth = ((set.getValue().floatValue() - set.getMin()) / (set.getMax() - set.getMin())) * (width - 27) + 3f;
        animatedVal = AnimationMath.fast(animatedVal, sliderWidth, 11);

        float targetThumbX = x + 10 + 3 + animatedVal;
        animatedThumbX = AnimationMath.fast(animatedThumbX, targetThumbX, 11);

        RenderUtilka.Render2D.drawRoundedRect(x + 10 + 3, y + height / 2f + 2.5f, width - 34 + 9, 2.5f, 0.5f, object.module.state ? ColorUtils.rgba(255, 255, 255, 15) : ColorUtils.rgba(255, 255, 255, 10));
        RenderUtilka.Render2D.drawRoundedRect(x + 10 + 3, y + height / 2f + 2.5f, animatedVal - 5 + 2, 2.5f, 0.5f, object.module.state ? ColorUtils.setAlpha(firstColor2, 200) : ColorUtils.setAlpha(firstColor2, 31));

        RenderUtilka.Render2D.drawRoundedRect(x + 10 + 3 + animatedVal - 5 + 1 - 0.5f, y + height / 2f + 2.5f - 0.5f, 3.5f, 3.5f, 1.25f, object.module.state ?  ColorUtils.rgba(225,226,226,255) :  ColorUtils.rgba(102,103,107,255));

        float slidert = Float.parseFloat(String.valueOf(set.getValue().floatValue()));
        slider = lerp(slider, slidert, 0.001f);
        if (!set.isSafe()) {
            Fonts.icon[11].drawString(stack, "j", x + width - 12f - Fonts.intl[11].getWidth(String.valueOf(set.getValue().floatValue())) - 6.5f, y + height / 2f - 3.7f, object.module.state ? ColorUtils.setAlpha(warnColor, 122) : ColorUtils.setAlpha(warnColor, 31));
        }
        SmartScissor.push();
        SmartScissor.setFromComponentCoordinates(x + 12.8f, y + height / 2f - 8, (!set.isSafe() ? (int) this.width - 45 : (int) this.width - 45 + 4), 14);
        Fonts.intl[11].drawString(stack, set.getName(), x + 12.8f, y + height / 2f - 4, object.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));
        SmartScissor.pop();
        StencilUtils.uninitStencilBuffer();
        Fonts.intl[11].drawString(stack, String.valueOf(slidert), x + width - 12f - Fonts.intl[11].getWidth(String.valueOf(set.getValue().floatValue())), y + height / 2f - 4, object.module.state ? ColorUtils.setAlpha(textColor, 184) : ColorUtils.setAlpha(textColor, 64));

    }
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!setting.visible()) return;
        if (object.module.expanded) {
            if (isHovered(mouseX, mouseY - 4)) {
                sliding = true;
            }
        }
    }

    @Override
    public void drawComponent(MatrixStack matrixStack, int mouseX, int mouseY) {

    }

    @Override
    public void exit() {
        super.exit();
        sliding = false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        sliding = false;
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {

    }

    @Override
    public void charTyped(char codePoint, int modifiers) {

    }
}
