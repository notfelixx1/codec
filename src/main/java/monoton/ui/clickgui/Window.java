    package monoton.ui.clickgui;

    import com.mojang.blaze3d.matrix.MatrixStack;
    import monoton.utils.render.*;
    import net.minecraft.client.gui.screen.Screen;
    import net.minecraft.util.math.vector.Vector2f;
    import net.minecraft.util.math.vector.Vector4f;
    import net.minecraft.util.text.ITextComponent;
    import monoton.module.TypeList;
    import monoton.module.api.Module;
    import monoton.ui.clickgui.objects.ModuleObject;
    import monoton.control.Manager;
    import monoton.utils.other.SoundUtils;
    import monoton.utils.anim.Animation;
    import monoton.utils.anim.Direction;
    import monoton.utils.anim.impl.EaseBackIn;
    import monoton.utils.font.Fonts;
    import monoton.utils.font.styled.StyledFont;
    import monoton.utils.render.animation.AnimationMath;
    import org.joml.Vector4i;
    import org.lwjgl.opengl.GL11;
    import ru.kotopushka.compiler.sdk.annotations.Compile;

    import java.awt.*;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.Map;

    import static monoton.utils.IMinecraft.mc;
    import static monoton.utils.IMinecraft.sr;

    public class Window extends Screen {
        private Vector2f position = new Vector2f(0, 0);
        public static Vector2f size = new Vector2f(500, 400);
        public static int light = new Color(129, 134, 153).getRGB();
        private TypeList currentCategory;
        public static ArrayList<ModuleObject> objects = new ArrayList<>();
        ArrayList<Panel> panels = new ArrayList<>();
        public static float scrolling;
        public static float scrollingOut;
        public static boolean searching;
        public static String searchText = "";
        public static boolean openAnimation = false;
        public Animation animation = new EaseBackIn(400, 1, 1.5f);

        public Window(ITextComponent titleIn) {
            super(titleIn);
            scrolling = 0;
            for (Module module : Manager.FUNCTION_MANAGER.getFunctions()) {
                objects.add(new ModuleObject(module));
            }
            size = new Vector2f(450, 350);
            position = new Vector2f(mc.getMainWindow().scaledWidth() / 2f, mc.getMainWindow().scaledHeight() / 2f);
            float offset = 0;
            float width = 120;
            for (TypeList typeList : TypeList.values()) {
                panels.add(new Panel(typeList, (mc.getMainWindow().scaledWidth() / 2f) + offset, mc.getMainWindow().scaledHeight() / 2f, width, 300));
                offset += width + 3;
            }
        }

        @Override
        @Compile
        protected void init() {
            super.init();
            panels.clear();
            size = new Vector2f(450, 350);
            float offset = 0;
            float width = 120;
            float height = 300;
            position = new Vector2f(mc.getMainWindow().scaledWidth() / 2f - (TypeList.values().length * width) / 2f,
                    (mc.getMainWindow().scaledHeight() / 2f) - height / 2f);

            for (TypeList typeList : TypeList.values()) {
                Panel panel = new Panel(typeList, position.x + offset, position.y, width, height);
                panels.add(panel);
                offset += width - 29;
            }

            if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                SoundUtils.playSound("guiopen.wav", 62);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        public ArrayList<Panel> getPanels() {
            return panels;
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            super.render(matrixStack, mouseX, mouseY, partialTicks);
            MatrixStack ms = new MatrixStack();

            GL11.glPushMatrix();
            mc.gameRenderer.setupOverlayRendering(2);

            Vec2i fixed = ScaleMath.getMouse(mouseX, mouseY);
            int scaledMouseX = fixed.getX();
            int scaledMouseY = fixed.getY();

            if (openAnimation) {
                animation.setDirection(Direction.FORWARDS);
            } else {
                animation.setDirection(Direction.BACKWARDS);
            }

            for (Panel p : panels) {
                p.render(matrixStack, scaledMouseX, scaledMouseY);
            }

            Panel.search(matrixStack);
            if (Panel.isThemesActive()) {
                if (!panels.isEmpty()) {
                    Panel.theme(matrixStack, scaledMouseX, scaledMouseY, panels.get(0));
                }
            }
            int textColor = Panel.getColorByName("textColor");

            if (!searching && searchText.isEmpty()) {
                Fonts.intl[14].drawCenteredString(ms, "Search", sr.scaledWidth() / 2 - 52f, sr.scaledHeight() / 2f + 118 + 20, ColorUtils.setAlpha(textColor, 122));
            } else {
                Fonts.intl[14].drawString(ms, searchText + (searching ? (System.currentTimeMillis() % 1000 > 500 ? "|" : "") : ""), sr.scaledWidth() / 2 - 64f, sr.scaledHeight() / 2f + 118 + 20, ColorUtils.setAlpha(textColor, 245));
            }
            for (Panel p : panels) {
                p.renderBind(matrixStack);
            }
            scrollingOut = AnimationMath.fast(scrollingOut, scrolling, 15);

            StencilUtils.initStencilToWrite();
            RenderUtilka.Render2D.drawRoundedCorner(position.x, position.y, size.x, size.y, new Vector4f(8.5f, 8.5f, 8.5f, 8.5f), -1);
            StencilUtils.readStencilBuffer(0);
            StencilUtils.uninitStencilBuffer();

            if (animation.getOutput() < 0.1f && !openAnimation) {
                openAnimation = true;
            }
            GL11.glPopMatrix();
        }

        private static String replaceCyrillicWithLatin(String input) {
            Map<Character, Character> cyrillicToLatinMap = new HashMap<>();
            String cyrillic = "йцукенгшщзхъфывапролджэячсмитьбюё";
            String latin = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";

            for (int i = 0; i < cyrillic.length(); i++) {
                cyrillicToLatinMap.put(cyrillic.charAt(i), latin.charAt(i));
                cyrillicToLatinMap.put(Character.toUpperCase(cyrillic.charAt(i)), Character.toUpperCase(latin.charAt(i)));
            }

            StringBuilder result = new StringBuilder();
            for (char ch : input.toCharArray()) {
                result.append(cyrillicToLatinMap.getOrDefault(ch, ch));
            }
            return result.toString();
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            // Проверяем, активен ли какой-либо бинд в данный момент
            if (isAnyBindActive()) {
                return false; // Блокируем ввод, если активен бинд
            }

            if (searching && searchText.length() < 11) {
                if (codePoint == ' ') {
                    return super.charTyped(codePoint, modifiers);
                }

                char convertedChar = replaceCyrillicWithLatin(String.valueOf(codePoint)).charAt(0);
                searchText += convertedChar;
            }
            return super.charTyped(codePoint, modifiers);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            Vec2i fixed = ScaleMath.getMouse((int)mouseX, (int)mouseY);
            for (Panel p : panels) {
                p.onThemeDrag(fixed.getX(), fixed.getY());
            }
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            for (Panel p : panels) {
                p.onKey(keyCode, scanCode, modifiers);
            }

            if (isAnyBindActive()) {
                if (keyCode == 256) {
                    mc.displayGuiScreen(null);
                    openAnimation = false;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }

            if (keyCode == 256) {
                mc.displayGuiScreen(null);
                openAnimation = false;
            }

            if (keyCode == 259 && searching && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
            }

            if (keyCode == 257) {
                searchText = "";
                searching = false;
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        private boolean isAnyBindActive() {
            for (ModuleObject moduleObject : objects) {
                for (Object obj : moduleObject.object) {
                    if (obj instanceof monoton.ui.clickgui.objects.sets.BindObject) {
                        monoton.ui.clickgui.objects.sets.BindObject bindObject = (monoton.ui.clickgui.objects.sets.BindObject) obj;
                        if (bindObject.bind) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            for (Panel p : panels) {
                p.onRelease(mouseX, mouseY, button);
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void onClose() {
            super.onClose();
            searching = false;
            searchText = "";
            openAnimation = false;

            for (Panel.ColorEntry entry : Panel.getColorEntries()) {
                entry.isPickerOpen = false;
            }
            Manager.CONFIG_MANAGER.saveConfiguration("default");

            for (Panel p : panels) {
                p.saveScrollPosition();
            }

            for (ModuleObject m : objects) {
                m.exit();
            }

            if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                SoundUtils.playSound("guiclose.wav", 62);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            Vec2i fixed = ScaleMath.getMouse((int) mouseX, (int) mouseY);
            mouseX = fixed.getX();
            mouseY = fixed.getY();

            for (TypeList typeList : TypeList.values()) {
                if (RenderUtilka.isInRegion(mouseX, mouseY, sr.scaledWidth() / 2 - 55 + 10, sr.scaledHeight() / 2f + 111 + 20, 91, 17.5f)) {
                    currentCategory = typeList;
                    searching = false;
                }
            }

            for (ModuleObject m : objects) {
                if (searching || !searchText.isEmpty()) {
                    if (!searchText.isEmpty()) {
                        if (!m.module.name.toLowerCase().contains(searchText.toLowerCase())) continue;
                    }
                    m.mouseClicked((int) mouseX, (int) mouseY, button);
                } else {
                    if (m.module.category == currentCategory) {
                        m.mouseClicked((int) mouseX, (int) mouseY, button);
                    }
                }
            }

            if (RenderUtilka.isInRegion(mouseX, mouseY, sr.scaledWidth() / 2 - 68, sr.scaledHeight() / 2f + 111 + 20, 103, 17.5f)) {
                searching = !searching;
            }

            for (Panel p : panels) {
                p.onClick(mouseX, mouseY, button);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            Vec2i fixed = ScaleMath.getMouse((int)mouseX, (int)mouseY);
            for (Panel p : panels) {
                p.onScroll(fixed.getX(), fixed.getY(), delta);
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
    }