package monoton.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.module.settings.imp.TextSetting;
import monoton.ui.clickgui.Panel;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.font.Fonts;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.SharedConstants;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class TextObject extends Object {

    public TextSetting set;
    public ModuleObject object;
    public boolean isTyping;
    private boolean isCapsLockToggled;

    public TextObject(ModuleObject object, TextSetting set) {
        this.object = object;
        this.set = set;
        this.isCapsLockToggled = false;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        height -= 5;
        x += 5;
        y -= 1;
        int textColor = monoton.ui.clickgui.Panel.getColorByName("textColor");
        String text = set.get().isEmpty() ? isTyping ? "" : "String..." : set.get();

        float width = Fonts.intl[11].getWidth(text) + 4;

        RenderUtilka.Render2D.drawRoundedCorner(x + 79f - Fonts.intl[11].getWidth(text), y + 10, width - 3F, 0.5F, 0, isTyping ? object.module.state ?  ColorUtils.setAlpha(textColor, 122) :  ColorUtils.setAlpha(textColor, 61) : object.module.state ?  ColorUtils.setAlpha(textColor, 61) :  ColorUtils.setAlpha(textColor, 31));

        Fonts.intl[11].drawCenteredString(stack, text + (isTyping && System.currentTimeMillis() % 1000 > 500 ? "|" : ""), x + 77.5f - Fonts.intl[11].getWidth(text) + (width / 2), y + 6, isTyping ? object.module.state ?  ColorUtils.setAlpha(textColor, 183) :  ColorUtils.setAlpha(textColor, 91) : object.module.state ? ColorUtils.setAlpha(textColor, 122) :  ColorUtils.setAlpha(textColor, 61));
        Fonts.intl[11].drawString(stack, set.getName(), x + 5.5F, y + 6, object.module.state ?  ColorUtils.setAlpha(textColor, 245) :  ColorUtils.setAlpha(textColor, 122));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            isTyping = !isTyping;
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
        if (!isTyping) return;

        boolean isShiftPressed = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (keyCode == GLFW.GLFW_KEY_CAPS_LOCK) {
            isCapsLockToggled = !isCapsLockToggled;
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                set.text = "";
            } else if (!set.text.isEmpty()) {
                set.text = set.text.substring(0, set.text.length() - 1);
            }
        } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
            isTyping = false;
        } else if (keyCode == GLFW.GLFW_KEY_SPACE) {
            if (set.text.length() < 14) {
                set.text += " ";
            }
        } else if (Screen.isCopy(keyCode)) {
            Minecraft.getInstance().keyboardListener.setClipboardString(set.text);
        } else if (Screen.isPaste(keyCode)) {
            String clipboard = Minecraft.getInstance().keyboardListener.getClipboardString();
            if (clipboard != null && !clipboard.isEmpty()) {
                int remainingSpace = 14 - set.text.length();
                if (remainingSpace > 0) {
                    String validClipboard = clipboard.length() > remainingSpace ? clipboard.substring(0, remainingSpace) : clipboard;
                    StringBuilder filteredText = new StringBuilder();
                    for (char c : validClipboard.toCharArray()) {
                        if (SharedConstants.isAllowedCharacter(c)) {
                            if (Character.isLetter(c)) {
                                c = isCapsLockToggled ? Character.toUpperCase(c) : Character.toLowerCase(c);
                            } else if (isShiftPressed && !Character.isLetter(c)) {
                                String shiftedChar = GLFW.glfwGetKeyName(keyCode, scanCode);
                                if (shiftedChar != null && shiftedChar.length() == 1) {
                                    c = shiftedChar.charAt(0);
                                }
                            }
                            filteredText.append(c);
                        }
                    }
                    set.text += filteredText.toString();
                }
            }
        } else {
            String keyChar = GLFW.glfwGetKeyName(keyCode, scanCode);
            if (keyChar != null && keyChar.length() == 1 && set.text.length() < 14) {
                char c = keyChar.charAt(0);
                if (SharedConstants.isAllowedCharacter(c)) {
                    if (Character.isLetter(c)) {
                        c = isCapsLockToggled ? Character.toUpperCase(c) : Character.toLowerCase(c);
                    } else if (isShiftPressed && !Character.isLetter(c)) {
                        String shiftedChar = GLFW.glfwGetKeyName(keyCode, scanCode);
                        if (shiftedChar != null && shiftedChar.length() == 1) {
                            c = shiftedChar.charAt(0);
                        }
                    }
                    set.text += c;
                }
            }
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (!isTyping || set.text.length() >= 14) return;

        boolean isShiftPressed = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (SharedConstants.isAllowedCharacter(codePoint)) {
            char c = codePoint;
            if (Character.isLetter(c)) {
                c = isCapsLockToggled ? Character.toUpperCase(c) : Character.toLowerCase(c);
            } else if (isShiftPressed && !Character.isLetter(c)) {
                c = codePoint;
            }
            set.text += c;
        }
    }
}