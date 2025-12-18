package monoton.ui.clickgui;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import monoton.control.Manager;
import monoton.module.TypeList;
import monoton.module.api.Module;
import monoton.ui.clickgui.objects.ModuleObject;
import monoton.ui.clickgui.objects.Object;
import monoton.utils.font.Fonts;
import monoton.utils.math.GLUtils;
import monoton.utils.other.SoundUtils;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.GaussianBlur;
import monoton.utils.render.RenderUtilka;
import monoton.utils.render.ScaleMath;
import monoton.utils.render.SmartScissor;
import monoton.utils.render.StencilUtils;
import monoton.utils.render.Vec2i;
import monoton.utils.render.RenderUtilka.Render2D;
import monoton.utils.render.animation.AnimationMath;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import org.joml.Vector4i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import static java.awt.Color.RGBtoHSB;
import static monoton.utils.IMinecraft.mc;
import static monoton.utils.IMinecraft.sr;

public class Panel {
    private float savedScrolling;
    TypeList typeList;
    float x;
    float y;
    float width;
    float height;
    float scrolling;
    float scrollingOut;
    float animationProgress;
    ArrayList<ModuleObject> moduleObjects = new ArrayList<>();
    private boolean isOpen;
    private static final float SCROLL_ANIMATION_SPEED = 17.0f;
    public static int selectedColor;
    private ModuleObject hoveredModule = null;
    private ModuleObject lastHoveredModule = null;
    private float themeX = 2.0F;
    private float themeY = (float)(mc.getMainWindow().scaledHeight()) - 2;
    private static float savedHue;
    private static float savedSaturation;
    private static float savedBrightness;
    private static float savedAlpha;
    private static float savedThemeX = 2.0F;
    private static float savedThemeY = (float)(mc.getMainWindow().scaledHeight()) - 2;
    private boolean isThemeDragging = false;
    private float themeDragOffsetX;
    private float themeDragOffsetY;
    private static final Map<TypeList, Float> scrollPositions = new HashMap<>();
    private float totalContentHeight = 0.0f;
    private static boolean isEnglish = false;
    private static boolean isThemesActive = true;
    private static final float COLOR_PICKER_ANIMATION_SPEED = 20.0f;
    public static boolean isThemesActive() {
        return isThemesActive;
    }

    public static void setThemesActive(boolean active) {
        isThemesActive = active;
    }

    public static class ColorEntry {
        public int color;
        String displayName;
        public String useName;
        int position;
        public float hue;
        public float saturation;
        public float brightness;
        public float alpha;
        public boolean isPickerOpen;
        float pickerAnimation;
        boolean colorSelectorDragging;
        boolean hueSelectorDragging;
        boolean alphaSelectorDragging;

        ColorEntry(int color, String displayName, String useName, int position) {
            this.color = color;
            this.displayName = displayName;
            this.useName = useName;
            this.position = position;
            float[] hsb = RGBtoHSB(color);
            this.hue = hsb[0];
            this.saturation = hsb[1];
            this.brightness = hsb[2];
            this.alpha = ((color >> 24) & 0xFF) / 255.0f;
            this.isPickerOpen = false;
            this.pickerAnimation = 0.0f;
            this.colorSelectorDragging = false;
            this.hueSelectorDragging = false;
            this.alphaSelectorDragging = false;
        }
    }

    private static final ArrayList<ColorEntry> colorEntries = new ArrayList<>();

    static {
        selectedColor = Color.WHITE.getRGB();
        float[] hsb = RGBtoHSB(selectedColor);
        savedHue = hsb[0];
        savedSaturation = hsb[1];
        savedBrightness = hsb[2];
        savedAlpha = 1.0f;

        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(Color.HSBtoRGB(savedHue, savedSaturation, savedBrightness)), savedAlpha).getRGB(),
                "Основной",
                "primaryColor",
                0
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(24, 25, 32, 245), 1.0f).getRGB(),
                "Фон",
                "fonColor",
                1
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(16, 17, 21, 245), 1.0f).getRGB(),
                "Верхний фон",
                "fonduoColor",
                2
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(246, 246, 247, 255), 1.0f).getRGB(),
                "Текста",
                "textColor",
                3
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(199, 200, 204, 255), 1.0f).getRGB(),
                "Иконки",
                "iconColor",
                4
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(255, 255, 255, 150), 1.0f).getRGB(),
                "Прочее",
                "iconnoColor",
                5
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(250, 250, 250, 255), 1.0f).getRGB(),
                "Заголовки",
                "infoColor",
                6
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(214, 41, 49, 255), 1.0f).getRGB(),
                "Предупреждение",
                "warnColor",
                7
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(255, 255, 255, 150), 1.0f).getRGB(),
                "Скролл бар",
                "scrollColor",
                8
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(160, 125, 82, 255), 1.0f).getRGB(),
                "Золотая полоска",
                "goldColor",
                9
        ));
    }

    private static final Map<Integer, String> ENGLISH_KEY_NAMES = new HashMap<>();
    static {
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_A, "A");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_B, "B");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_C, "C");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_D, "D");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_E, "E");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F, "F");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_G, "G");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_H, "H");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_I, "I");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_J, "J");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_K, "K");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_L, "L");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_M, "M");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_N, "N");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_O, "O");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_P, "P");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_Q, "Q");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_R, "R");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_S, "S");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_T, "T");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_U, "U");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_V, "V");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_W, "W");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_X, "X");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_Y, "Y");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_Z, "Z");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_0, "0");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_1, "1");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_2, "2");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_3, "3");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_4, "4");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_5, "5");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_6, "6");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_7, "7");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_8, "8");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_9, "9");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_SPACE, "SPACE");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_ENTER, "ENTER");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_TAB, "TAB");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_BACKSPACE, "BACKSPACE");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_DELETE, "DELETE");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_ESCAPE, "ESCAPE");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_LEFT_CONTROL, "LCTRL");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCTRL");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_LEFT_SHIFT, "LSHIFT");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "RSHIFT");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_LEFT_ALT, "LALT");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT_ALT, "RALT");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_LEFT, "LEFT");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT, "RIGHT");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_UP, "UP");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_DOWN, "DOWN");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_CAPS_LOCK, "CAPSLOCK");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_NUM_LOCK, "NUMLOCK");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_SCROLL_LOCK, "SCROLLLOCK");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F1, "F1");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F2, "F2");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F3, "F3");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F4, "F4");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F5, "F5");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F6, "F6");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F7, "F7");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F8, "F8");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F9, "F9");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F10, "F10");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F11, "F11");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_F12, "F12");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_MINUS, "MINUS");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_EQUAL, "EQUAL");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_LEFT_BRACKET, "LBRACKET");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_RIGHT_BRACKET, "RBRACKET");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_SEMICOLON, "SEMICOLON");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_APOSTROPHE, "APOSTROPHE");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_BACKSLASH, "BACKSLASH");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_COMMA, "COMMA");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_PERIOD, "PERIOD");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_SLASH, "SLASH");
        ENGLISH_KEY_NAMES.put(GLFW.GLFW_KEY_GRAVE_ACCENT, "GRAVE");
    }

    private String getEnglishKeyName(int keyCode) {
        if (keyCode == 0) {
            return "n/a";
        }
        String keyName = ENGLISH_KEY_NAMES.get(keyCode);
        if (keyName == null) {
            String glfwName = GLFW.glfwGetKeyName(keyCode, 0);
            keyName = glfwName != null ? glfwName.toUpperCase() : "UNKNOWN";
        }
        return keyName;
    }

    public float getScrolling() {
        return this.scrolling;
    }

    public Panel(TypeList typeList, float x, float y, float width, float height) {
        this.themeX = savedThemeX;
        this.themeY = savedThemeY;
        this.savedScrolling = 0.0F;
        this.typeList = typeList;
        this.x = x + 71.0F;
        this.y = y + 36.0F;
        this.width = width - 25.0F;
        this.height = height - 121.0F;
        this.animationProgress = 0.0F;
        this.isOpen = false;
        this.scrolling = scrollPositions.getOrDefault(typeList, 0.0F);
        this.scrollingOut = this.scrolling;
        Comparator<Module> moduleComparator = (m1, m2) -> {
            String name1 = m1.name;
            String name2 = m2.name;
            int result = name1.compareToIgnoreCase(name2);
            if (result == 0) {
                result = name1.compareTo(name2);
            }
            return result;
        };
        this.moduleObjects = (ArrayList)Manager.FUNCTION_MANAGER.getFunctions().stream().filter((m) -> {
            return m.category == typeList;
        }).sorted(moduleComparator).map(ModuleObject::new).collect(Collectors.toCollection(ArrayList::new));
    }

    private void saveThemePosition() {
        savedThemeX = this.themeX;
        savedThemeY = this.themeY;
        Manager.CONFIG_MANAGER.saveConfiguration("default");
    }

    public void setScrolling(float scrolling) {
        this.scrolling = scrolling;
        this.scrollingOut = scrolling;
    }

    public void saveScrollPosition() {
        scrollPositions.put(this.typeList, this.scrolling);
    }

    public static int getColorByName(String useName) {
        for (ColorEntry entry : colorEntries) {
            if (entry.useName.equals(useName)) {
                return entry.color;
            }
        }
        return Color.WHITE.getRGB();
    }

    public static ArrayList<ColorEntry> getColorEntries()  {
        return colorEntries;
    }

    public static void setSelectedColor(int color, float hue, float saturation, float brightness, float alpha) {
        selectedColor = color;
        savedHue = hue;
        savedSaturation = saturation;
        savedBrightness = brightness;
        savedAlpha = alpha;
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY) {
        MatrixStack ms = new MatrixStack();
        scrollingOut = AnimationMath.fast(scrollingOut, scrolling, SCROLL_ANIMATION_SPEED);
        int infoColor = Panel.getColorByName("infoColor");
        int iconnoColor = getColorByName("iconnoColor");
        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);

        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x + 4.0f, y + 7.5f, width - 7.0f, 18.0f, new Vector4f(4.0f, 0.0f, 4.0f, 0.0f), fonduoColor, 1);
        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x + 4.0f, y + 25.0f, width - 7.0f, height + 18.5f, new Vector4f(0.0f, 4.0f, 0.0f, 4.0f), fonColor, 1);

        for (int i = 0; i < 3; i++) {
            RenderUtilka.Render2D.drawCircle2(x + 77.5f + 1f + i * 3.5f, y + 11.5f + 1, 1f, iconnoColor);
        }

        Fonts.intl[13].drawCenteredString(ms, typeList.name(), x + width / 2.0f + 5.0f, y + 15.0f, infoColor);
        Fonts.icon[14].drawCenteredString(ms, typeList.icon, x + width / 2.0f - Fonts.intl[14].getWidth(typeList.name()) / 2.0f - 1.5f, y + 15.6f, iconColor);

        float offset = -3.5f;
        float off = 16.0f;

        float originalWidth = width - 1.0f;
        float originalHeight = 15.0f;

        float totalHeight = 0;

        ModuleObject newHoveredModule = null;
        for (ModuleObject m : moduleObjects) {
            if (Window.searching) {
                String moduleName = m.module.name.toLowerCase();
                if (!moduleName.contains(Window.searchText.toLowerCase())) {
                    continue;
                }
            }
            StencilUtils.initStencilToWrite();
            RenderUtilka.Render2D.drawRoundedCorner((int) x + 4.0f, y + 25.0f, width - 7.0f, height + 18.5f, new Vector4f(0.0f, 6.5f, 0.0f, 6.5f), new Color(14, 13, 20, 184).getRGB());
            StencilUtils.readStencilBuffer(1);

            SmartScissor.push();
            SmartScissor.setFromComponentCoordinates((int) x + 4.0f, y + 25.0f, width - 7.0f, height + 18.5f);

            m.width = originalWidth;
            m.height = originalHeight;
            m.x = x + 1.0f;
            m.y = y + off + offset + scrollingOut + 12.5f;

            float moduleHeight = m.height;
            m.expand_anim = AnimationMath.fast(m.expand_anim, m.module.expanded ? 1.0f : 0.0f, 60.0f);
            GL11.glPushMatrix();

            if (m.module.expanded) {
                for (Object object1 : m.object) {
                    if (object1.setting != null && !object1.setting.visible()) continue;
                    moduleHeight += object1.height + 9.5f;
                }
            }

            Render2D.drawRoundedRect(m.x + 2.7f, m.y + 0.1f, m.width - 5.4f, 17.08f, 0.0f, new Color(255, 255, 255, m.module.state ? 15 : 5).getRGB());
            if (m.module.expanded) {
                Render2D.drawRoundedRect(m.x + 2.7f, m.y + 0.1f + 16.58f, m.width - 5.4f, moduleHeight - (0.1f + 14.48f), 0.0f, new Color(255, 255, 255, m.module.state ? 10 : 0).getRGB());
            }
            String name = m.module.name;

            if (Window.searching && !Window.searchText.isEmpty()) {
                String searchText = Window.searchText.toLowerCase();
                String lowerName = name.toLowerCase();
                int matchIndex = lowerName.indexOf(searchText);
                if (matchIndex != -1) {
                    String beforeMatch = name.substring(0, matchIndex);
                    String match = name.substring(matchIndex, matchIndex + searchText.length());
                    String afterMatch = name.substring(matchIndex + searchText.length());

                    float currentX = x + 10.5f;
                    if (!beforeMatch.isEmpty()) {
                        Fonts.intl[12].drawString(ms, beforeMatch, currentX, m.y + 7.6f, m.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));
                        currentX += Fonts.intl[12].getWidth(beforeMatch);
                    }
                    int lighterTextColor = new Color((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, 255).getRGB();
                    Fonts.intl[12].drawString(ms, match, currentX, m.y + 7.6f, ColorUtils.setAlpha(lighterTextColor, 255));
                    currentX += Fonts.intl[12].getWidth(match);
                    if (!afterMatch.isEmpty()) {
                        Fonts.intl[12].drawString(ms, afterMatch, currentX, m.y + 7.6f, m.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));
                    }
                } else {
                    Fonts.intl[12].drawString(ms, name, x + 10.5f, m.y + 7.6f, m.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));
                }
            } else {
                Fonts.intl[12].drawString(ms, name, x + 10.5f, m.y + 7.6f, m.module.state ? ColorUtils.setAlpha(textColor, 245) : ColorUtils.setAlpha(textColor, 122));
            }

            float size = 10.0f;

            if (!m.module.settingList.isEmpty()) {
                Fonts.iconnew[14].drawCenteredString(ms, "i", m.x + m.width - size - 3.0f, m.y + 7.5f, m.module.state ? ColorUtils.setAlpha(iconColor, 184) : ColorUtils.setAlpha(iconColor, 60));
            }

            float yd = 6.0f;
            for (Object object1 : m.object) {
                if (object1.setting != null && !object1.setting.visible()) continue;
                object1.x = this.x;
                object1.y = y + yd + off + offset + this.scrollingOut + 25.0f;
                object1.width = this.width;
                object1.height = 10.0f;
                if ((double) m.expand_anim > 0.6) {
                    object1.draw(ms, mouseX, mouseY);
                }
                off += (object1.height + 9.5f) * m.expand_anim;
            }

            GL11.glPopMatrix();
            off += offset + 20.0f;
            SmartScissor.pop();
            StencilUtils.uninitStencilBuffer();

            if (mouseX >= this.x && mouseX <= (this.x + this.width) &&
                    mouseY >= (y + 20) && mouseY <= (y + this.height + 43) &&
                    isMouseOverButton(mouseX, mouseY, (int) (m.x + 7.5f), (int) m.y, (int) (m.width - 20 + 5), (int) 18)) {
                newHoveredModule = m;
            }
            totalHeight += moduleHeight + 20.0f;
        }

        lastHoveredModule = hoveredModule;
        hoveredModule = newHoveredModule;

        if (hoveredModule != null) {
            String desc = hoveredModule.module.getDescription();
            if (desc != null && !desc.isEmpty()) {
                float descX = (mc.getMainWindow().getScaledWidth() / 2f) + 2;
                float descY = (mc.getMainWindow().getScaledHeight() / 2f) - 165 + 2;
                Fonts.intl[14].drawCenteredString(ms, desc, descX, descY, textColor);
            }
        }

        this.totalContentHeight = totalHeight;
        float max2 = off;
        this.scrolling = max2 < (this.height + 34.5f) ? 0.0f : MathHelper.clamp(this.scrolling, -(max2 - (this.height + 34.5f)), 0.0f);
    }

    public void renderBind(MatrixStack matrixStack) {
        int textColor = getColorByName("textColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        MatrixStack ms = new MatrixStack();
        for (ModuleObject m : moduleObjects) {
            if (m.isBinding) {
                String keyText = getEnglishKeyName(m.module.bind);
                GaussianBlur.startBlur();
                RenderUtilka.Render2D.drawRoundedCorner(0, 0, mc.getMainWindow().scaledWidth(), mc.getMainWindow().scaledHeight(), 0F, new Color(14, 13, 20, 255).getRGB());
                GaussianBlur.endBlur(8, 2);

                float textWidth = Fonts.intl[30].getWidth(keyText);
                float boxWidth = textWidth + 40;
                float boxX = mc.getMainWindow().scaledWidth() / 2f - textWidth / 2 - 20f;
                float boxY = mc.getMainWindow().scaledHeight() / 2f - 45;

                RenderUtilka.Render2D.drawRoundedCorner(boxX, boxY, boxWidth, 50, 7F, ColorUtils.setAlpha(fonColor, 80));
                RenderUtilka.Render2D.drawRoundOutline(boxX, boxY, boxWidth, 50, 5F, 0F, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(
                        ColorUtils.setAlpha(fonColor, 55),
                        ColorUtils.setAlpha(fonColor, 55),
                        ColorUtils.setAlpha(fonColor, 55),
                        ColorUtils.setAlpha(fonColor, 55)
                ));

                Fonts.intl[30].drawString(ms, keyText, mc.getMainWindow().scaledWidth() / 2f - textWidth / 2, mc.getMainWindow().scaledHeight() / 2f - 26, ColorUtils.setAlpha(textColor, 184));

                Fonts.intl[19].drawString(ms, "Press any key", mc.getMainWindow().scaledWidth() / 2f - (Fonts.intl[19].getWidth("Press any key") / 2), mc.getMainWindow().scaledHeight() / 2f + 16, ColorUtils.setAlpha(textColor, 81));
                Fonts.intl[15].drawString(ms, "ESC to cancel", mc.getMainWindow().scaledWidth() / 2f - (Fonts.intl[15].getWidth("ESC to cancel") / 2), mc.getMainWindow().scaledHeight() / 2f + 27, ColorUtils.setAlpha(textColor, 51));
            }
        }
    }

    public static void theme(MatrixStack matrixStack, int mouseX, int mouseY, Panel panel) {
        MatrixStack ms = new MatrixStack();
        int x = 14;
        int y = sr.scaledHeight() / 2 - 88;
        int width = 122;
        int height = 197;
        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 225);
        int iconnoColor = getColorByName("iconnoColor");

        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x, y - 19, width, 19.5f, new Vector4f(5, 0, 5, 0), fonduoColor, 1);
        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x, y, width, height, new Vector4f(0, 5, 0, 5), fonColor, 1);

        RenderUtilka.Render2D.drawRoundedCorner(x, y - 18.9f, width / 2, 19f,new Vector4f(6, 0, 0, 0), ColorUtils.rgba(255, 255, 255, isThemesActive ? 15 : 0));

        Fonts.intl[13].drawString(ms, "Themes", x + 25.5f, y - 11, ColorUtils.setAlpha(textColor, isThemesActive ? 245 : 122));

        Fonts.iconnew[13].drawString(ms, "g", x + 16.5f, y - 10.5f, ColorUtils.setAlpha(iconColor, isThemesActive ? 245 : 122));

        RenderUtilka.Render2D.drawRoundedCorner(x + width / 2, y - 19, width / 2, 19, new Vector4f(0, 0, 6, 0), ColorUtils.rgba(255, 255, 255, isThemesActive ? 0 : 15));

        Fonts.intl[13].drawString(ms, "Configs", x + 79, y - 11, ColorUtils.setAlpha(textColor, isThemesActive ? 122 : 245));

        Fonts.iconnew[13].drawString(ms, "h", x + 70, y - 10.5f, ColorUtils.setAlpha(iconColor, isThemesActive ? 122 : 245));

        for (int i = 0; i < 3; i++) {
            RenderUtilka.Render2D.drawCircle2(x + 107.5f + i * 3.5f, y - 15, 1f, ColorUtils.setAlpha(iconnoColor, 31));
        }

        if (isThemesActive) {
            float colorOffsetY = y;
            for (ColorEntry entry : colorEntries) {
                entry.pickerAnimation = AnimationMath.fast(entry.pickerAnimation, entry.isPickerOpen ? 1.0f : 0.0f, COLOR_PICKER_ANIMATION_SPEED);

                RenderUtilka.Render2D.drawRoundedCorner(x, colorOffsetY, width, 16, 0, ColorUtils.setAlpha(entry.color, 10));
                RenderUtilka.Render2D.drawCircle2(x + width - 11.5f, colorOffsetY + 5.5f, 2f, ColorUtils.setAlpha(entry.color, 122));
                RenderUtilka.Render2D.drawRoundOutline(x + width - 13, colorOffsetY + 4, 7, 7, 3f, -0.9f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(
                        ColorUtils.setAlpha(entry.color, 31),
                        ColorUtils.setAlpha(entry.color, 31),
                        ColorUtils.setAlpha(entry.color, 31),
                        ColorUtils.setAlpha(entry.color, 31)
                ));
                Fonts.intl[13].drawString(ms, entry.displayName, x + 6, colorOffsetY + 7, ColorUtils.setAlpha(entry.color, 200, 255));

                if (entry.pickerAnimation > 0.01f) {
                    float pickerX = x + width + 7;
                    float pickerY = colorOffsetY - 15.5f;
                    float pickerWidth = 54;
                    float pickerHeight = 54;

                    float animationScale = entry.pickerAnimation;
                    float animationAlpha = entry.pickerAnimation * 214;
                    float scaleCenterX = pickerX + pickerWidth / 2;
                    float scaleCenterY = pickerY + pickerHeight / 2;

                    GLUtils.scaleStart(scaleCenterX, scaleCenterY, animationScale);

                    if (entry.colorSelectorDragging && RenderUtilka.isInRegion(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
                        float xDifference = mouseX - pickerX;
                        entry.saturation = Math.max(0, Math.min(1, xDifference / pickerWidth));
                        float yDifference = mouseY - pickerY;
                        entry.brightness = Math.max(0, Math.min(1, 1 - yDifference / pickerHeight));
                        entry.color = ColorUtils.applyOpacity(new Color(Color.HSBtoRGB(entry.hue, entry.saturation, entry.brightness)), entry.alpha).getRGB();
                        if (entry.useName.equals("primaryColor")) {
                            selectedColor = entry.color;
                            savedHue = entry.hue;
                            savedSaturation = entry.saturation;
                            savedBrightness = entry.brightness;
                            savedAlpha = entry.alpha;
                        }
                        Manager.CONFIG_MANAGER.saveConfiguration("default");
                    }

                    GaussianBlur.startBlur();
                    RenderUtilka.Render2D.drawRoundedRect(x + width + 3.5f, colorOffsetY - 19.5f, 61f, 77f, 4F, ColorUtils.rgba(14, 13, 20, (int)animationAlpha));
                    GaussianBlur.endBlur(8, 2);
                    RenderUtilka.Render2D.drawRoundedRect(x + width + 3.5f, colorOffsetY - 19.5f, 61f, 77f, 4F, ColorUtils.rgba(14, 13, 20, (int)animationAlpha));

                    RenderUtilka.Render2D.drawGradientRoundedRect(matrixStack, pickerX, pickerY, pickerWidth, pickerHeight, 3, -1, Color.BLACK.getRGB(), Color.HSBtoRGB(entry.hue, 1, 1), Color.BLACK.getRGB());

                    float selectorX = pickerX + entry.saturation * pickerWidth;
                    float selectorY = pickerY + (1 - entry.brightness) * pickerHeight;

                    selectorX = Math.max(pickerX, Math.min(pickerX + pickerWidth - 4, selectorX));
                    selectorY = Math.max(pickerY, Math.min(pickerY + pickerHeight - 4, selectorY));

                    RenderUtilka.Render2D.drawCircle2(selectorX, selectorY, 2f, ColorUtils.rgba(255, 255, 255, (int)(animationAlpha * 255 / 214)));

                    float hueSliderX = x + width + 7;
                    float hueSliderY = colorOffsetY + 41.5f;
                    float hueSliderWidth = 54;
                    float hueSliderHeight = 4.5f;

                    if (entry.hueSelectorDragging && RenderUtilka.isInRegion(mouseX, mouseY, hueSliderX, hueSliderY, hueSliderWidth, hueSliderHeight)) {
                        float xDifference = mouseX - hueSliderX;
                        entry.hue = Math.max(0, Math.min(1, xDifference / hueSliderWidth));
                        entry.color = ColorUtils.applyOpacity(new Color(Color.HSBtoRGB(entry.hue, entry.saturation, entry.brightness)), entry.alpha).getRGB();
                        if (entry.useName.equals("primaryColor")) {
                            selectedColor = entry.color;
                            savedHue = entry.hue;
                            savedSaturation = entry.saturation;
                            savedBrightness = entry.brightness;
                            savedAlpha = entry.alpha;
                        }
                        Manager.CONFIG_MANAGER.saveConfiguration("default");
                    }

                    float times = 5;
                    float size = hueSliderWidth / times;
                    float sliderX = hueSliderX;

                    for (int i = 0; i < times; i++) {
                        int color1 = Color.HSBtoRGB(0.2F * i, 1, 1);
                        int color2 = Color.HSBtoRGB(0.2F * (i + 1), 1, 1);

                        if (i == 0) {
                            RenderUtilka.Render2D.drawCustomGradientRoundedRect(matrixStack, sliderX, hueSliderY, size + 0.4f, hueSliderHeight, 4, 4, 0, 0, color1, color1, color2, color2);
                        } else if (i == times - 1) {
                            RenderUtilka.Render2D.drawCustomGradientRoundedRect(matrixStack, sliderX, hueSliderY, size, hueSliderHeight, 0, 0, 4, 4, color1, color1, color2, color2);
                        } else {
                            RenderUtilka.Render2D.drawGradientRectCustom(matrixStack, sliderX + 0.001f, hueSliderY, size + 0.4f, hueSliderHeight, color1, color1, color2, color2);
                        }

                        sliderX += size;
                    }

                    float hueSelectorX = entry.hue * hueSliderWidth;
                    hueSelectorX = Math.max(0, Math.min(hueSliderWidth - 4.5f, hueSelectorX));

                    RenderUtilka.Render2D.drawRoundOutline(hueSliderX + hueSelectorX, hueSliderY, 4.5f, 4.5f, 2f, 0.4f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(
                            ColorUtils.rgba(255, 255, 255, (int)(animationAlpha * 255 / 214)),
                            ColorUtils.rgba(255, 255, 255, (int)(animationAlpha * 255 / 214)),
                            ColorUtils.rgba(255, 255, 255, (int)(animationAlpha * 255 / 214)),
                            ColorUtils.rgba(255, 255, 255, (int)(animationAlpha * 255 / 214))
                    ));

                    float alphaSliderY = colorOffsetY + 48.5f;
                    if (entry.alphaSelectorDragging && RenderUtilka.isInRegion(mouseX, mouseY, hueSliderX, alphaSliderY, hueSliderWidth, hueSliderHeight)) {
                        float xDifference = mouseX - hueSliderX;
                        entry.alpha = Math.max(0, Math.min(1, xDifference / hueSliderWidth));
                        entry.color = ColorUtils.applyOpacity(new Color(Color.HSBtoRGB(entry.hue, entry.saturation, entry.brightness)), entry.alpha).getRGB();
                        if (entry.useName.equals("primaryColor")) {
                            selectedColor = entry.color;
                            savedHue = entry.hue;
                            savedSaturation = entry.saturation;
                            savedBrightness = entry.brightness;
                            savedAlpha = entry.alpha;
                        }
                        Manager.CONFIG_MANAGER.saveConfiguration("default");
                    }

                    int color = Color.HSBtoRGB(entry.hue, entry.saturation, entry.brightness);
                    RenderUtilka.Render2D.drawGradientRoundedRect(matrixStack, hueSliderX, alphaSliderY, hueSliderWidth, hueSliderHeight, 2, -1, -1, ColorUtils.rgba(color >> 16 & 255, color >> 8 & 255, color & 255, (int)(animationAlpha * 255 / 214)), ColorUtils.rgba(color >> 16 & 255, color >> 8 & 255, color & 255, (int)(animationAlpha * 255 / 214)));

                    float alphaSelectorX = entry.alpha * hueSliderWidth;
                    alphaSelectorX = Math.max(0, Math.min(hueSliderWidth - 4.5f, alphaSelectorX));

                    RenderUtilka.Render2D.drawRoundOutline(hueSliderX + alphaSelectorX, alphaSliderY, 4.5f, 4.5f, 2f, 0.4f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(
                            ColorUtils.rgba(255, 255, 255, (int)(animationAlpha * 255 / 214)),
                            ColorUtils.rgba(255, 255, 255, (int)(animationAlpha * 255 / 214)),
                            ColorUtils.rgba(255, 255, 255, (int)(animationAlpha * 255 / 214)),
                            ColorUtils.rgba(255, 255, 255, (int)(animationAlpha * 255 / 214))
                    ));

                    GLUtils.scaleEnd();
                }

                colorOffsetY += 16;
            }
        }
    }

    private boolean isMouseOverThemeHeader(int mouseX, int mouseY) {
        float headerHeight = 16.0F;
        float width = 78.0F;
        return mouseX >= themeX && mouseX <= themeX + width &&
                mouseY >= themeY - 14 && mouseY <= themeY - 14 + headerHeight;
    }

    private static boolean isMouseOver(int mouseX, int mouseY, float x, float y, float width, float height) {
        return (float)mouseX >= x && (float)mouseX <= x + width && (float)mouseY >= y && (float)mouseY <= y + height;
    }

    public static void search(MatrixStack matrixStack) {
        MatrixStack ms = new MatrixStack();
        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);

        Render2D.drawBlurredRoundedRectangle((float)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10) - 19 + 105F, (float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20, 30F, 17.5F, 4.0F, ColorUtils.setAlpha(fonColor, 184), 1);
        Fonts.intl[14].drawCenteredString(ms, isEnglish ? "ENG" : "RUS", (float)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10) - 4 + 105F, (double)((float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20 + 7F), ColorUtils.setAlpha(textColor, 184));

        SmartScissor.push();
        SmartScissor.setFromComponentCoordinates((double)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10) - 10 - 14, (double)((float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20), 84.0, 17.5);
        Render2D.drawBlurredRoundedRectangle((float)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10) - 14 - 10, (float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20, 105F, 17.5F, 4.0F,ColorUtils.setAlpha(fonColor, 184), 1);
        SmartScissor.pop();
        StencilUtils.uninitStencilBuffer();
        SmartScissor.push();
        SmartScissor.setFromComponentCoordinates((double)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10 + 74) - 14, (double)((float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20), 71.0, 17.5);
        Render2D.drawBlurredRoundedRectangle((float)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10) - 14, (float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20, 95F, 17.5F, 4.0F, ColorUtils.setAlpha(fonduoColor, 214), 1);
        SmartScissor.pop();
        StencilUtils.uninitStencilBuffer();
        if (Window.searching) {
            SmartScissor.push();
            SmartScissor.setFromComponentCoordinates((double)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10) - 10 - 14, (double)((float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20), 84.0, 17.5);
            Render2D.drawBlurredRoundedRectangle((float)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10) - 14 - 10, (float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20, 105F, 17.5F, 4.0F, ColorUtils.rgba(255, 255, 255, 10), 1);
            SmartScissor.pop();
            StencilUtils.uninitStencilBuffer();
            SmartScissor.push();
            SmartScissor.setFromComponentCoordinates((double)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10 + 74) - 14, (double)((float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20), 71.0, 17.5);
            Render2D.drawBlurredRoundedRectangle((float)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10) - 14, (float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20, 95F, 17.5F, 4.0F, (new Color(255, 255, 255, 10)).getRGB(), 1);
            SmartScissor.pop();
            StencilUtils.uninitStencilBuffer();
        }
        Fonts.icon[15].drawCenteredString(ms, "g", (double)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10 + 82) - 12, (double)((float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20 + 7.5F), Window.searching ? ColorUtils.setAlpha(iconColor, 245) : ColorUtils.setAlpha(iconColor, 122));
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        return mouseX >= buttonX && mouseY >= buttonY && mouseX < buttonX + buttonWidth && mouseY < buttonY + buttonHeight;
    }

    public void onClick(double mouseX, double mouseY, int button) {
        Vec2i mo = ScaleMath.getMouse((int)mouseX, (int)mouseY);
        mouseX = mo.getX();
        mouseY = mo.getY();

        if (button == 0 && isMouseOver((int)mouseX, (int)mouseY, 14, sr.scaledHeight() / 2 - 107, 66, 19)) {
            isThemesActive = true;
            return;
        }

        if (button == 0 && isMouseOver((int)mouseX, (int)mouseY,
                (float)(mc.getMainWindow().scaledWidth() / 2 - 55 + 10) - 19 + 105F,
                (float) mc.getMainWindow().scaledHeight() / 2.0F + 111.0F + 20,
                30F, 17.5F)) {
            isEnglish = !isEnglish;
            return;
        }

        if (button == 0 && isMouseOverThemeHeader((int)mouseX, (int)mouseY)) {
            isThemeDragging = true;
            themeDragOffsetX = (float)mouseX - themeX;
            themeDragOffsetY = (float)mouseY - themeY;
            return;
        }

        if (isThemesActive) {
            boolean isClickInAnyPickerRegion = false;
            float colorOffsetY = sr.scaledHeight() / 2 - 88;
            for (ColorEntry entry : colorEntries) {
                float pickerX = 14 + 132 + 7;
                float pickerY = colorOffsetY - 15.5f;
                float pickerWidth = 54;
                float pickerHeight = 54;
                float hueSliderX = 14 + 132 + 7;
                float hueSliderY = colorOffsetY + 41.5f;
                float hueSliderWidth = 54;
                float hueSliderHeight = 4.5f;
                float alphaSliderY = colorOffsetY + 48.5f;

                boolean isClickInPicker = RenderUtilka.isInRegion((int) mouseX, (int) mouseY, pickerX, pickerY, pickerWidth, pickerHeight);
                boolean isClickInHueSlider = RenderUtilka.isInRegion((int) mouseX, (int) mouseY, hueSliderX, hueSliderY, hueSliderWidth, hueSliderHeight);
                boolean isClickInAlphaSlider = RenderUtilka.isInRegion((int) mouseX, (int) mouseY, hueSliderX, alphaSliderY, hueSliderWidth, hueSliderHeight);
                boolean isClickInColorEntry = isMouseOver((int) mouseX, (int) mouseY, 14, colorOffsetY, 122, 16);

                if (entry.isPickerOpen && (isClickInPicker || isClickInHueSlider || isClickInAlphaSlider || isClickInColorEntry)) {
                    isClickInAnyPickerRegion = true;
                }

                if ((button == 0 || button == 1) && isClickInColorEntry) {
                    boolean wasOpen = entry.isPickerOpen;
                    for (ColorEntry other : colorEntries) {
                        other.isPickerOpen = false;
                    }
                    entry.isPickerOpen = !wasOpen;
                    entry.pickerAnimation = entry.isPickerOpen ? 0.0f : 1.0f;
                    if (entry.isPickerOpen) {
                        entry.hue = RGBtoHSB(entry.color)[0];
                        entry.saturation = RGBtoHSB(entry.color)[1];
                        entry.brightness = RGBtoHSB(entry.color)[2];
                        entry.alpha = ((entry.color >> 24) & 0xFF) / 255.0f;
                    }
                    Manager.CONFIG_MANAGER.saveConfiguration("default");
                    return;
                }

                if (entry.isPickerOpen) {
                    if (button == 0 && !entry.colorSelectorDragging && isClickInPicker) {
                        entry.colorSelectorDragging = true;
                        return;
                    }

                    if (button == 0 && !entry.hueSelectorDragging && isClickInHueSlider) {
                        entry.hueSelectorDragging = true;
                        return;
                    }

                    if (button == 0 && !entry.alphaSelectorDragging && isClickInAlphaSlider) {
                        entry.alphaSelectorDragging = true;
                        return;
                    }
                }
                colorOffsetY += 16;
            }

            if ((button == 0 || button == 1) && !isClickInAnyPickerRegion) {
                for (ColorEntry entry : colorEntries) {
                    entry.isPickerOpen = false;
                }
                Manager.CONFIG_MANAGER.saveConfiguration("default");
            }
        }

        int zoneX = (int)this.x;
        int zoneY = (int)this.y + 24;
        int zoneWidth = (int)this.width;
        int zoneHeight = (int)this.height + 18;
        if (RenderUtilka.isInRegion(mouseX, mouseY, zoneX, zoneY, zoneWidth, zoneHeight)) {
            float offset = -4.0F;
            float off = 11.0F;
            for (ModuleObject m : moduleObjects) {
                if (Window.searching && !m.module.name.toLowerCase().contains(Window.searchText.toLowerCase())) {
                    continue;
                }
                m.mouseClicked((int)mouseX, (int)mouseY, button);
                if (RenderUtilka.isInRegion(mouseX, mouseY, m.x + 8.0F, m.y, m.width - 20.0F + 4.0F, m.height) && button == 1) {
                    m.module.expanded = !m.module.expanded;
                    if (m.module.expanded && !this.isOpen && !m.module.settingList.isEmpty()) {
                        if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                            SoundUtils.playSound("moduleopen.wav", 60.0F);
                        }
                        this.isOpen = true;
                    } else if (!m.module.expanded && this.isOpen && !m.module.settingList.isEmpty()) {
                        if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                            SoundUtils.playSound("moduleclose.wav", 60.0F);
                        }
                        this.isOpen = false;
                    }
                }
                if (m.module.expanded) {
                    float yd = 5.0F;
                    for (Object object1 : m.object) {
                        if (object1.setting != null && object1.setting.visible()) {
                            object1.y = this.y + yd + off + offset + this.scrollingOut + 25.0F;
                            off += object1.height + 5.0F;
                        }
                    }
                }
                off += offset + 20.0F;
            }
        }
    }

    private static float[] RGBtoHSB(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return Color.RGBtoHSB(red, green, blue, null);
    }

    public void onThemeDrag(double mouseX, double mouseY) {
        Vec2i mo = ScaleMath.getMouse((int)mouseX, (int)mouseY);
        mouseX = mo.getX();
        mouseY = mo.getY();

        if (isThemeDragging) {
            float newX = (float)mouseX - themeDragOffsetX;
            float newY = (float)mouseY - themeDragOffsetY;

            themeX = MathHelper.clamp(newX, 0, mc.getMainWindow().scaledWidth() - 78.0F);   // ← было -88
            themeY = MathHelper.clamp(newY, 14, mc.getMainWindow().scaledHeight() - 35.0F);

            saveThemePosition();
        }

        if (isThemesActive) {
                float colorOffsetY = sr.scaledHeight() / 2 - 88;
                for (ColorEntry entry : colorEntries) {
                    if (entry.isPickerOpen) {
                        float pickerX = 14 + 132 + 7;
                        float pickerY = colorOffsetY - 15.5f;
                        float pickerWidth = 54;
                        float pickerHeight = 54;
                        float hueSliderX = 14 + 132 + 7;
                        float hueSliderY = colorOffsetY + 41.5f;
                        float hueSliderWidth = 54;
                        float hueSliderHeight = 4.5f;
                        float alphaSliderY = colorOffsetY + 48.5f;

                        boolean colorUpdated = false;

                        if (entry.colorSelectorDragging && RenderUtilka.isInRegion((int) mouseX, (int) mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
                            entry.saturation = MathHelper.clamp(((float) mouseX - pickerX) / pickerWidth, 0.0f, 1.0f);
                            entry.brightness = MathHelper.clamp(1.0f - ((float) mouseY - pickerY) / pickerHeight, 0.0f, 1.0f);
                            colorUpdated = true;
                        }

                        if (entry.hueSelectorDragging && RenderUtilka.isInRegion((int) mouseX, (int) mouseY, hueSliderX, hueSliderY, hueSliderWidth, hueSliderHeight)) {
                            entry.hue = MathHelper.clamp(((float) mouseX - hueSliderX) / hueSliderWidth, 0.0f, 1.0f);
                            colorUpdated = true;
                        }

                        if (entry.alphaSelectorDragging && RenderUtilka.isInRegion((int) mouseX, (int) mouseY, hueSliderX, alphaSliderY, hueSliderWidth, hueSliderHeight)) {
                            entry.alpha = MathHelper.clamp(((float) mouseX - hueSliderX) / hueSliderWidth, 0.0f, 1.0f);
                            colorUpdated = true;
                        }

                        if (colorUpdated) {
                            entry.color = ColorUtils.applyOpacity(new Color(Color.HSBtoRGB(entry.hue, entry.saturation, entry.brightness)), entry.alpha).getRGB();
                            if (entry.useName.equals("primaryColor")) {
                                selectedColor = entry.color;
                                savedHue = entry.hue;
                                savedSaturation = entry.saturation;
                                savedBrightness = entry.brightness;
                                savedAlpha = entry.alpha;
                            }
                            Manager.CONFIG_MANAGER.saveConfiguration("default");
                        }
                    }
                    colorOffsetY += 16;

            }
        }
    }

    public void onScroll(double mouseX, double mouseY, double delta) {
        Vec2i m = ScaleMath.getMouse((int)mouseX, (int)mouseY);
        if (RenderUtilka.isInRegion((double)m.getX(), (double)m.getY(), this.x + 4.0f, this.y + 25.0f, this.width - 7.0f, this.height + 18.5f)) {
            float scrollDelta = (float)(delta * 25.0);
            float newScrolling = this.scrolling + scrollDelta;
            float maxScroll = this.totalContentHeight < (this.height + 34.5f) ? 0.0f : -(this.totalContentHeight - (this.height + 34.5f));
            this.scrolling = MathHelper.clamp(newScrolling, maxScroll, 0.0f);
        }
    }

    public void onRelease(double mouseX, double mouseY, int button) {
        Vec2i mo = ScaleMath.getMouse((int)mouseX, (int)mouseY);
        mouseX = mo.getX();
        mouseY = mo.getY();

        if (button == 0) {
            isThemeDragging = false;
            for (ColorEntry entry : colorEntries) {
                entry.colorSelectorDragging = false;
                entry.hueSelectorDragging = false;
                entry.alphaSelectorDragging = false;
            }
            saveThemePosition();
            Manager.CONFIG_MANAGER.saveConfiguration("default");
        }

        for (ModuleObject m : moduleObjects) {
            for (Object o : m.object) {
                o.mouseReleased((int)mouseX, (int)mouseY, button);
            }
        }
    }

    public void onKey(int keyCode, int scanCode, int modifiers) {
        for (ModuleObject m : this.moduleObjects) {
            m.keyTyped(keyCode, scanCode, modifiers);
        }
    }
}