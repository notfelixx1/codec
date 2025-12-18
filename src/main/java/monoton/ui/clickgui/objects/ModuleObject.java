package monoton.ui.clickgui.objects;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.module.settings.imp.*;
import monoton.ui.clickgui.objects.sets.*;
import monoton.module.api.Module;
import monoton.module.settings.Setting;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import monoton.utils.render.animation.AnimationMath;
import org.joml.Vector4i;
import org.lwjgl.glfw.GLFW;
import monoton.utils.font.Fonts;
import monoton.utils.other.OtherUtil;

import java.util.ArrayList;

import static monoton.ui.clickgui.Window.light;

public class ModuleObject extends Object {

    public Module module;
    public ArrayList<Object> object = new ArrayList<>();
    public float animation, animation_height;

    boolean bind; // Renamed to match BindObject
    public boolean isBinding; // Renamed from waitingForKey to match BindObject

    private static final int MOUSE_BUTTON_RIGHT = 1;
    private static final int MOUSE_BUTTON_MIDDLE = 2;
    private static final int MOUSE_BUTTON_3 = 3;
    private static final int MOUSE_BUTTON_4 = 4;

    public ModuleObject(Module module) {
        this.module = module;
        for (Setting setting : module.settingList) {
            if (setting instanceof BooleanOption option) {
                object.add(new BooleanObject(this, option));
            }
            if (setting instanceof ColorSetting option) {
                object.add(new ColorObject(this, option));
            }
            if (setting instanceof SliderSetting option) {
                object.add(new SliderObject(this, option));
            }
            if (setting instanceof ModeSetting option) {
                object.add(new ModeObject(this, option));
            }
            if (setting instanceof MultiBoxSetting option) {
                object.add(new MultiObject(this, option));
            }
            if (setting instanceof BindSetting option) {
                object.add(new BindObject(this, option));
            }
            if (setting instanceof ButtonSetting option) {
                object.add(new ButtonObject(this, option));
            }
            if (setting instanceof InfoSetting option) {
                object.add(new InfoObject(this, option));
            }
            if (setting instanceof TextSetting option) {
                object.add(new TextObject(this, option));
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (Object object1 : object) {
            object1.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (isHovered(mouseX, mouseY, 15)) {
            if (mouseButton == 0) {
                module.toggle();
            } else if (mouseButton == MOUSE_BUTTON_MIDDLE && !bind) {
                bind = true;
                isBinding = true;
            } else if (bind) {
                if (mouseButton == MOUSE_BUTTON_RIGHT || mouseButton == MOUSE_BUTTON_MIDDLE ||
                        mouseButton == MOUSE_BUTTON_3 || mouseButton == MOUSE_BUTTON_4) {
                    module.bind = -100 + mouseButton;
                    bind = false;
                    isBinding = false;
                }
            }
        }
    }

    @Override
    public void drawComponent(MatrixStack matrixStack, int mouseX, int mouseY) {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        for (Object object1 : object) {
            object1.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        if (bind) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                module.bind = 0;
                bind = false;
                isBinding = false;
                return;
            }
            if (keyCode >= GLFW.GLFW_KEY_SPACE && keyCode <= GLFW.GLFW_KEY_LAST) {
                module.bind = keyCode;
                bind = false;
                isBinding = false;
            }
        }

        for (Object obj : object) {
            if (obj instanceof BindObject bindObj) {
                if (bindObj.isBinding) {
                    if (keyCode >= GLFW.GLFW_MOUSE_BUTTON_1 && keyCode <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
                        bindObj.set.setKey(-100 + (keyCode - GLFW.GLFW_MOUSE_BUTTON_1));
                        bindObj.isBinding = false;
                        continue;
                    }
                    if (keyCode >= GLFW.GLFW_KEY_SPACE && keyCode <= GLFW.GLFW_KEY_LAST) {
                        bindObj.set.setKey(keyCode);
                        bindObj.isBinding = false;
                    }
                }
            }
            obj.keyTyped(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        object.forEach(component -> component.charTyped(codePoint, modifiers));
    }

    float hover_anim;
    boolean expand = false;
    public float expand_anim;

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        super.draw(stack, mouseX, mouseY);
        hover_anim = AnimationMath.fast(hover_anim, RenderUtilka.isInRegion(mouseX, mouseY, x, y, width, height) ? 1 : 0, 10);
        RenderUtilka.Render2D.drawRoundOutline(x, y, width, height, 1, 0f, ColorUtils.rgba(25, 26, 33, 100), new Vector4i(
                ColorUtils.gradient(10, 0, ColorUtils.rgba(155, 155, 155, 255 * hover_anim), ColorUtils.rgba(26, 26, 26, 0 * hover_anim)),
                ColorUtils.gradient(10, 90, ColorUtils.rgba(155, 155, 155, 255 * hover_anim), ColorUtils.rgba(25, 26, 33, 0 * hover_anim)),
                ColorUtils.gradient(10, 180, ColorUtils.rgba(155, 155, 155, 255 * hover_anim), ColorUtils.rgba(25, 26, 33, 0 * hover_anim)),
                ColorUtils.gradient(10, 270, ColorUtils.rgba(155, 155, 155, 255 * hover_anim), ColorUtils.rgba(25, 26, 33, 0 * hover_anim))
        ));
        animation = AnimationMath.fast(animation, module.state ? 1 : 0, 5);
        animation_height = AnimationMath.fast(animation_height, height, 5);

        String text = module.name;
        if (isBinding) text += "...";
        if (module.bind != 0) text += " [" + getKeyName(module.bind) + "]";
        if (module.state) {
            Fonts.msSemiBold[15].drawString(stack, text, x + 10, y + 10, ColorUtils.interpolateColor(light, -1, animation));
        } else {
            Fonts.msSemiBold[15].drawString(stack, text, x + 10, y + 10, ColorUtils.interpolateColor(light, -1, animation));
        }
        if (!module.settingList.isEmpty()) {
            RenderUtilka.Render2D.drawRect(x + 10, y + 22, width - 20, 0.5f, ColorUtils.rgba(32, 35, 57, 255));
        }

        drawObjects(stack, mouseX, mouseY);
    }

    public void drawObjects(MatrixStack stack, int mouseX, int mouseY) {
        float offset = -4;
        for (Object object : object) {
            if (object.setting.visible()) {
                object.x = x;
                object.y = y + 15 + offset;
                object.width = 160;
                object.height = 8;
                object.draw(stack, mouseX, mouseY);
                offset += object.height;
            }
        }
    }

    private String getKeyName(int keyCode) {
        String bindString = OtherUtil.getKey(keyCode);
        if (bindString == null) return "";
        return bindString.replace("MOUSE", "M")
                .replace("LEFT", "L")
                .replace("RIGHT", "R")
                .replace("CONTROL", "C")
                .replace("SHIFT", "S")
                .replace("_", "")
                .substring(0, Math.min(bindString.length(), 4))
                .toUpperCase();
    }
}