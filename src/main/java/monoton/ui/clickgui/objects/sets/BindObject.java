package monoton.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.module.settings.imp.BindSetting;
import monoton.ui.clickgui.Panel;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.other.OtherUtil;
import monoton.utils.font.Fonts;

public class BindObject extends Object {

    public BindSetting set;
    public ModuleObject object;
    public boolean bind;
    private static final int MOUSE_BUTTON_3 = 3;
    private static final int MOUSE_BUTTON_4 = 4;
    private static final int MOUSE_BUTTON_RIGHT = 1;
    private static final int MOUSE_BUTTON_MIDDLE = 2;
    public boolean isBinding;

    public BindObject(ModuleObject object, BindSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack matrixStack, int mouseX, int mouseY) {
        if (!setting.visible()) return;

        y += 0.5f;
        height -= 5;
        x -= 4;
        width += 10;
        int textColor = Panel.getColorByName("textColor");
        int warnColor = Panel.getColorByName("warnColor");

        String bindString = bind ? "..." : (set.getKey() == 0 ? "n/a" : OtherUtil.getKey(set.getKey()));

        if (bindString == null) {
            bindString = "";
        }

        bindString = bindString.replace("MOUSE", "M")
                .replace("LEFT", "L")
                .replace("RIGHT", "R")
                .replace("CONTROL", "C")
                .replace("SHIFT", "S")
                .replace("_", "");

        String shortBindString = bindString.substring(0, Math.min(bindString.length(), 4));
        shortBindString = shortBindString.equals("n/a") ? shortBindString : shortBindString.toUpperCase();

        boolean isDuplicate = isDuplicateBind(shortBindString);

        float widthtext = Fonts.intl[11].getWidth(shortBindString);

        Fonts.intl[11].drawString(matrixStack, set.getName(), x + 14.5f, y + 5,
                object.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));

        Fonts.intl[11].drawCenteredString(matrixStack, shortBindString, x + width - 25 - (widthtext / 2) + 11 - 1.8f, y + 5.2f,
                object.module.state ? (isDuplicate ? ColorUtils.setAlpha(warnColor, 184) : ColorUtils.setAlpha(textColor, 122))
                        : (isDuplicate ? ColorUtils.setAlpha(warnColor, 61) : ColorUtils.setAlpha(textColor, 31)));
    }

    private boolean isDuplicateBind(String currentBind) {
        if (currentBind.equals("n/a")) {
            return false;
        }

        for (Object obj : object.object) {
            if (obj instanceof BindObject && obj != this && obj.setting.visible()) {
                BindObject otherBindObject = (BindObject) obj;
                String otherBindString = otherBindObject.set.getKey() == 0 ? "n/a" : OtherUtil.getKey(otherBindObject.set.getKey());
                if (otherBindString != null) {
                    otherBindString = otherBindString.replace("MOUSE", "M")
                            .replace("LEFT", "L")
                            .replace("RIGHT", "R")
                            .replace("CONTROL", "C")
                            .replace("SHIFT", "S")
                            .replace("_", "");
                    String otherShortBindString = otherBindString.substring(0, Math.min(otherBindString.length(), 4));
                    otherShortBindString = otherShortBindString.equals("n/a") ? otherShortBindString : otherShortBindString.toUpperCase();
                    if (currentBind.equals(otherShortBindString) && !otherShortBindString.equals("n/a")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!setting.visible()) return;
        if (object.module.expanded) {
            if (RenderUtilka.isInRegion(mouseX, mouseY, x + 12, y + 1, width - 22, 16)) {
                height += 12;
                y -= 7;
                if (isHovered(mouseX, mouseY) && mouseButton == 0) {
                    bind = true;
                    isBinding = true; // Устанавливаем флаг активности
                }
            }
            if (bind) {
                if (mouseButton == MOUSE_BUTTON_RIGHT || mouseButton == MOUSE_BUTTON_MIDDLE ||
                        mouseButton == MOUSE_BUTTON_3 || mouseButton == MOUSE_BUTTON_4) {
                    set.setKey(-100 + mouseButton);
                    bind = false;
                    isBinding = false; // Сбрасываем флаг
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
        if (!setting.visible()) return;
        if (bind) {
            if (keyCode == 261 || keyCode == 259) {
                set.setKey(0);
                bind = false;
                isBinding = false; // Сбрасываем флаг
                return;
            }

            set.setKey(keyCode);
            bind = false;
            isBinding = false; // Сбрасываем флаг
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return RenderUtilka.isInRegion(mouseX, mouseY, x + 12, y + 1, width - 22, 16);
    }
}