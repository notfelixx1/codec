package monoton.ui.alt;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.utils.IMinecraft;
import monoton.utils.anim.Animation;
import monoton.utils.anim.impl.DecelerateAnimation;
import monoton.utils.font.Fonts;
import monoton.utils.render.*;
import monoton.utils.render.animation.AnimationMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.RandomStringUtils;
import org.joml.Vector4i;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import static monoton.ui.clickgui.Panel.getColorByName;
import static monoton.utils.IMinecraft.mc;

public class AltManager extends Screen {
    private final Animation targetInfoAnimation;
    public ArrayList<Account> accounts = new ArrayList<>();
    private boolean animationStarted = false;
    private String altName = "";
    private boolean typing;
    public float scroll;
    public float scrollAn;
    public boolean hoveredFirst;
    public boolean hoveredSecond;
    public float hoveredFirstAn;
    public float hoveredSecondAn;
    private boolean showWarning = false;
    private boolean isDraggingScrollbar = false; // Flag to track scrollbar dragging
    private float dragStartY; // Y position where drag started
    private float dragStartScroll; // Scroll value when drag started

    public AltManager() {
        super(new StringTextComponent(""));
        this.targetInfoAnimation = new DecelerateAnimation(300, 1.0);
    }

    @Override
    protected void init() {
        super.init();
        MainMenuScreen.animationAlpha = 0.0f;
        animationStarted = true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false; // Stop dragging when mouse is released
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clipboardText = Minecraft.getInstance().keyboardListener.getClipboardString();
            if (clipboardText != null) {
                String filteredText = clipboardText.replaceAll("[^a-zA-Z0-9_]", "");
                altName += filteredText;
                if (!filteredText.isEmpty()) {
                    showWarning = false;
                }
            }
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!altName.isEmpty())
                altName = altName.substring(0, altName.length() - 1);
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (!altName.isEmpty()) {
                accounts.add(new Account(altName));
                altName = "";
                typing = false;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if ((codePoint >= 'a' && codePoint <= 'z') || (codePoint >= 'A' && codePoint <= 'Z') || (codePoint >= '0' && codePoint <= '9') || codePoint == '_') {
            if (altName.length() < 16) {
                altName += Character.toString(codePoint);
                showWarning = false;
            }
        } else if (codePoint >= '\u0400' && codePoint <= '\u04FF') {
            showWarning = true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Vec2i fixed = ScaleMath.getMouse((int) mouseX, (int) mouseY);
        mouseX = fixed.getX();
        mouseY = fixed.getY();

        float totalRows = (float) Math.ceil((accounts.size() + 2) / 2.0);
        float visibleRows = 9;
        float scrollbarX = (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 79.5f - 85 + 3 + 85 + 60 + 250 + 5;
        float scrollbarY = IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 162.5f + 26 + 24;
        float scrollbarWidth = 1;
        float scrollbarHeight = 251;
        float thumbHeight = Math.max(20, scrollbarHeight * (visibleRows / totalRows));
        float maxScroll = totalRows > visibleRows ? -(totalRows - visibleRows) : 0;
        float scrollRatio = maxScroll != 0 ? scrollAn / maxScroll : 0;
        float thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollRatio;

        if (RenderUtilka.isInRegion(mouseX, mouseY, scrollbarX, thumbY, scrollbarWidth, thumbHeight) && button == 0) {
            isDraggingScrollbar = true;
            dragStartY = (float) mouseY;
            dragStartScroll = scroll;
            return true;
        }

        if (RenderUtilka.isInRegion(mouseX, mouseY, (int) ((int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f + 92), IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21, 58, 18)) {
            AltConfig.updateFile();
            accounts.add(new Account(RandomStringUtils.randomAlphabetic(9)));
            altName = "";
            typing = false;
        }

        if (RenderUtilka.isInRegion(mouseX, mouseY, (int) (((int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f)), IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21, 90, 18)) {
            AltConfig.updateFile();
            if (!altName.isEmpty()) {
                accounts.add(new Account(altName));
                altName = "";
                typing = false;
            }
        }

        if (RenderUtilka.isInRegion(mouseX, mouseY, (int) ((int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183 + (156 / 2) - (57 / 2)), IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 46, 57, 18)) {
            IMinecraft.mc.displayGuiScreen(new MainMenuScreen());
        }

        if (RenderUtilka.isInRegion(mouseX, mouseY, (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11,
                150, 18)) {
            typing = !typing;
        }

        float altX = (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 79.5f - 85 + 3;
        float panWidth = 29;
        Iterator<Account> iterator = accounts.iterator();
        int index = 0;
        boolean isInScissorRegion = RenderUtilka.isInRegion(mouseX, mouseY,
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 79.5f - 85 + 3 + 104 + 60,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 162.5f + 26 + 27,
                250, 246);
        while (iterator.hasNext()) {
            Account account = iterator.next();
            float columnOffset = (index % 2 == 0) ? 0 : 118;
            float rowIndex = (float) Math.floor(index / 2.0);
            float acX = altX + 104 + 60 + columnOffset;
            float acY = IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 159.5f + 26 + (rowIndex * (panWidth + 2f)) + 23.5f + scrollAn * (panWidth + 2f);
            if (RenderUtilka.isInRegion(mouseX, mouseY, acX, acY + 1.5f - 1, 159 - 6 - 3 - 38 + 3, 26 + 2)) {
                if (button == 0 && isInScissorRegion) {
                    IMinecraft.mc.session = new Session(account.accountName, "", "", "mojang");
                } else if (button != 0) {
                    iterator.remove();
                    AltConfig.updateFile();
                }
            }
            index++;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar && button == 0) {
            float totalRows = (float) Math.ceil((accounts.size() + 2) / 2.0);
            float visibleRows = 9;
            float scrollbarHeight = 251;
            float thumbHeight = Math.max(20, scrollbarHeight * (visibleRows / totalRows));
            float maxScroll = totalRows > visibleRows ? -(totalRows - visibleRows) : 0;

            float mouseDeltaY = (float) mouseY - dragStartY;
            float scrollRange = scrollbarHeight - thumbHeight;
            float scrollPerPixel = maxScroll / scrollRange;
            scroll = dragStartScroll + (mouseDeltaY * scrollPerPixel);

            scroll = MathHelper.clamp(scroll, maxScroll, 0);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
    }

    @Override
    public void tick() {
        super.tick();
        if (animationStarted && MainMenuScreen.animationAlpha < 1.0f) {
            MainMenuScreen.animationAlpha = lerp(MainMenuScreen.animationAlpha, 1.0f, 0.11f);
            if (MainMenuScreen.animationAlpha > 0.99f) {
                MainMenuScreen.animationAlpha = 1.0f;
                animationStarted = false;
            }
        }
        scrollAn = lerp(scrollAn, scroll, 0.1f);
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        return mouseX >= buttonX && mouseY >= buttonY && mouseX < buttonX + buttonWidth && mouseY < buttonY + buttonHeight;
    }

    private float lerp(float start, float end, float speed) {
        return start + (end - start) * speed;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        float totalRows = (float) Math.ceil((accounts.size() + 2) / 2.0);
        float visibleRows = 9;
        float maxScroll = totalRows > visibleRows ? -(totalRows - visibleRows) : 0;
        scroll += delta * 0.5f;
        scroll = MathHelper.clamp(scroll, maxScroll, 0);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        scrollAn = lerp(scrollAn, scroll, 0.1f);
        hoveredFirst = RenderUtilka.isInRegion(mouseX, mouseY, (int) ((int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 140f) + 69, IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 4, 19, 19);
        hoveredSecond = RenderUtilka.isInRegion(mouseX, mouseY, (int) ((int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 140f), IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 4, 19, 19);
        hoveredFirstAn = AnimationMath.lerp(hoveredFirstAn, hoveredFirst ? 1 : 0, 10);
        hoveredSecondAn = AnimationMath.lerp(hoveredSecondAn, hoveredSecond ? 1 : 0, 10);

        IMinecraft.mc.gameRenderer.setupOverlayRendering(2);

        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");
        int iconnoColor = getColorByName("iconnoColor");
        int scrollColor = getColorByName("scrollColor");

        int alpha = (int) (MainMenuScreen.animationAlpha * 255);
        int scaledAlpha115 = (int) (115 * MainMenuScreen.animationAlpha);
        int scaledAlpha25 = (int) (25 * MainMenuScreen.animationAlpha);
        int scaledAlpha255 = (int) (255 * MainMenuScreen.animationAlpha);
        int scaledAlpha192 = (int) (192 * MainMenuScreen.animationAlpha);
        int scaledAlpha140 = (int) (140 * MainMenuScreen.animationAlpha);
        int scaledAlpha185 = (int) (185 * MainMenuScreen.animationAlpha);
        int scaledAlpha63 = (int) (63 * MainMenuScreen.animationAlpha);
        int windowWidth = mc.getMainWindow().getScaledWidth();
        int windowHeight = mc.getMainWindow().getScaledHeight();
        int renderWidth = (int) (windowWidth * 1.05f);
        int renderHeight = (int) (windowHeight * 1.05f);

        float normMouseX = (mouseX / (float) windowWidth) * 2 - 1;
        float normMouseY = (mouseY / (float) windowHeight) * 2 - 1;

        float maxOffsetX = (renderWidth - windowWidth) / 2.0f;
        float maxOffsetY = (renderHeight - windowHeight) / 2.0f;

        float offsetX = MathHelper.clamp(normMouseX * 4, -maxOffsetX, maxOffsetX);
        float offsetY = MathHelper.clamp(normMouseY * 4, -maxOffsetY, maxOffsetY);
        RenderUtilka.Render2D.drawImage(new ResourceLocation("monoton/images/background.png"), offsetX - (renderWidth - windowWidth) / 2.0f, offsetY - (renderHeight - windowHeight) / 2.0f, renderWidth, renderHeight, -1);

        if (showWarning) {
            int blinkingAlpha = (int) (150 + (80 * (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 400.0))));
            Fonts.intl[12].drawString(matrixStack, "Переключите на англ раскладку",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 102.5f - (Fonts.intl[12].getWidth("Переключите на англ раскладку") / 2),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 60,
                    ColorUtils.rgba(255, 77, 79, (int)(blinkingAlpha * MainMenuScreen.animationAlpha)));
        }

        GaussianBlur.startBlur();
        RenderUtilka.Render2D.drawRoundedRect(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 2,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 105f - 10,
                246, 255, 4F,
                ColorUtils.rgba(255, 255, 255, 5));
        GaussianBlur.endBlur(6, 3);

        RenderUtilka.Render2D.drawRoundedRect(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 2,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 105f - 10,
                246, 255, 4F,
                ColorUtils.setAlpha(iconnoColor, 5));

        RenderUtilka.Render2D.drawRoundOutline(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 2,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 105f - 10,
                246, 255, 4F, -0.9f,
                ColorUtils.rgba(25, 26, 33, 0),
                new Vector4i(
                        ColorUtils.setAlpha(iconnoColor, 10),
                        ColorUtils.setAlpha(iconnoColor, 10),
                        ColorUtils.setAlpha(iconnoColor, 10),
                        ColorUtils.setAlpha(iconnoColor, 10)
                ));

        GaussianBlur.startBlur();
        RenderUtilka.Render2D.drawRoundedRect(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 45f,
                156, 76, 4F,
                ColorUtils.setAlpha(iconnoColor, 5));
        GaussianBlur.endBlur(6, 3);

        RenderUtilka.Render2D.drawRoundedRect(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 45f,
                156, 76, 4F,
                ColorUtils.setAlpha(iconnoColor, 5));

        RenderUtilka.Render2D.drawRoundOutline(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 45f,
                156, 76, 4F, -0.9f,
                ColorUtils.rgba(25, 26, 33, 0),
                new Vector4i(
                        ColorUtils.setAlpha(iconnoColor, 10),
                        ColorUtils.setAlpha(iconnoColor, 10),
                        ColorUtils.setAlpha(iconnoColor, 10),
                        ColorUtils.setAlpha(iconnoColor, 10)
                ));

        Fonts.icon[32].drawString(matrixStack, "a",
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 185 + 75,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 31f,
                ColorUtils.setAlpha(iconColor, 245));

        RenderUtilka.Render2D.drawRoundedRect(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11,
                150, 18, 3F,
                ColorUtils.setAlpha(iconnoColor, 5));

        RenderUtilka.Render2D.drawRoundOutline(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11,
                150, 18, 3F, -0.9f,
                ColorUtils.rgba(25, 26, 33, 0),
                new Vector4i(
                        ColorUtils.setAlpha(iconnoColor, 10),
                        ColorUtils.setAlpha(iconnoColor, 10),
                        ColorUtils.setAlpha(iconnoColor, 10),
                        ColorUtils.setAlpha(iconnoColor, 10)
                ));

        Fonts.icon[15].drawString(matrixStack, "e",
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 175f,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 3,
                ColorUtils.setAlpha(iconColor, 61));

        Fonts.intl[14].drawString(matrixStack,
                (altName.isEmpty() && !typing ? "Username" : altName) + (typing ? (System.currentTimeMillis() % 1000 > 500 ? "|" : "") : ""),
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 165f,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 3.5f,
                ColorUtils.setAlpha(textColor, 122));


        if (isMouseOverButton(mouseX, mouseY, (int) ((int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f), IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21, 90, 18)) {
            RenderUtilka.Render2D.drawRoundedRect((IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f, IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21, 90, 18, 3F, ColorUtils.setAlpha(iconnoColor, 15));
            RenderUtilka.Render2D.drawRoundOutline(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f,
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21,
                    90, 18, 3F, -0.9f,
                    ColorUtils.rgba(25, 26, 33, 0),
                    new Vector4i(
                            ColorUtils.setAlpha(iconnoColor, 30),
                            ColorUtils.setAlpha(iconnoColor, 30),
                            ColorUtils.setAlpha(iconnoColor, 30),
                            ColorUtils.setAlpha(iconnoColor, 30)
                    ));
            Fonts.intl[14].drawString(matrixStack, "Create",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 165f + Fonts.intl[14].getWidth("Create"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 18f,
                    ColorUtils.setAlpha(textColor, 245));
            Fonts.icon[15].drawString(matrixStack, "E",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 155f + Fonts.intl[14].getWidth("E"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 18f,
                    ColorUtils.setAlpha(textColor, 184));
        } else {
            RenderUtilka.Render2D.drawRoundedRect(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f,
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21,
                    90, 18, 3F,
                    ColorUtils.setAlpha(iconnoColor, 5));
            RenderUtilka.Render2D.drawRoundOutline(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f,
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21,
                    90, 18, 3F, -0.9f,
                    ColorUtils.rgba(25, 26, 33, 0),
                    new Vector4i(
                            ColorUtils.setAlpha(iconnoColor, 10),
                            ColorUtils.setAlpha(iconnoColor, 10),
                            ColorUtils.setAlpha(iconnoColor, 10),
                            ColorUtils.setAlpha(iconnoColor, 10)
                    ));

            Fonts.intl[14].drawString(matrixStack, "Create",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 165f + Fonts.intl[14].getWidth("Create"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 18f,
                    ColorUtils.setAlpha(textColor, 122));
            Fonts.icon[15].drawString(matrixStack, "E",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 155f + Fonts.intl[14].getWidth("E"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 18f,
                    ColorUtils.setAlpha(textColor, 61));
        }

        if (isMouseOverButton(mouseX, mouseY, (int) ((int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f + 92), IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21, 58, 18)) {
            RenderUtilka.Render2D.drawRoundedRect(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f + 92,
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21,
                    58, 18, 3F,
                    ColorUtils.setAlpha(iconnoColor, 15));
            RenderUtilka.Render2D.drawRoundOutline(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f + 92,
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21,
                    58, 18, 3F, -0.9f,
                    ColorUtils.rgba(25, 26, 33, 0),
                    new Vector4i(
                            ColorUtils.setAlpha(iconnoColor, 30),
                            ColorUtils.setAlpha(iconnoColor, 30),
                            ColorUtils.setAlpha(iconnoColor, 30),
                            ColorUtils.setAlpha(iconnoColor, 30)
                    ));
            Fonts.intl[14].drawString(matrixStack, "Random",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 94f + Fonts.intl[14].getWidth("Create"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 18f,
                    ColorUtils.setAlpha(textColor, 245));
            Fonts.icon[15].drawString(matrixStack, "E",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 84f + Fonts.intl[14].getWidth("E"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 18f,
                    ColorUtils.setAlpha(textColor, 184));
        } else {
            RenderUtilka.Render2D.drawRoundedRect(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f + 92,
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21,
                    58, 18, 3F,
                    ColorUtils.setAlpha(iconnoColor, 5));
            RenderUtilka.Render2D.drawRoundOutline(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 180f + 92,
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 21,
                    58, 18, 3F, -0.9f,
                    ColorUtils.rgba(25, 26, 33, 0),
                    new Vector4i(
                            ColorUtils.setAlpha(iconnoColor, 10),
                            ColorUtils.setAlpha(iconnoColor, 10),
                            ColorUtils.setAlpha(iconnoColor, 10),
                            ColorUtils.setAlpha(iconnoColor, 10)
                    ));
            Fonts.intl[14].drawString(matrixStack, "Random",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 94f + Fonts.intl[14].getWidth("Create"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 18f,
                    ColorUtils.setAlpha(textColor, 122));
            Fonts.icon[15].drawString(matrixStack, "E",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 84f + Fonts.intl[14].getWidth("E"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 18f,
                    ColorUtils.setAlpha(textColor, 61));
        }
        GaussianBlur.startBlur();
        RenderUtilka.Render2D.drawRoundedRect(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183 + (156 / 2) - (57 / 2),
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 46,
                57, 18, 3F,
                ColorUtils.setAlpha(iconnoColor, 5));
        GaussianBlur.endBlur(6, 3);
        if (isMouseOverButton(mouseX, mouseY, (int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183 + (156 / 2) - (57 / 2), IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 46, 57, 18)) {
            RenderUtilka.Render2D.drawRoundedRect(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183 + (156 / 2) - (57 / 2),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 46,
                    57, 18, 3F,
                    new Color(218, 56, 56, 15).getRGB());
            RenderUtilka.Render2D.drawRoundOutline(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183 + (156 / 2) - (57 / 2),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 46,
                    57, 18, 3F, -0.9f,
                    ColorUtils.rgba(25, 26, 33, 0),
                    new Vector4i(
                            new Color(218, 56, 56, 30).getRGB(),
                            new Color(218, 56, 56, 30).getRGB(),
                            new Color(218, 56, 56, 30).getRGB(),
                            new Color(218, 56, 56, 30).getRGB()
                    ));
            Fonts.intl[14].drawString(matrixStack, "Back",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 125f + Fonts.intl[14].getWidth("Back"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 43,
                    new Color(218, 56, 56, 122).getRGB());
            Fonts.icon[15].drawString(matrixStack, "E",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 122f + Fonts.intl[14].getWidth("E"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 43,
                    new Color(218, 56, 56, 61).getRGB());
        } else {
            RenderUtilka.Render2D.drawRoundedRect(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183 + (156 / 2) - (57 / 2),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 46,
                    57, 18, 3F,
                    ColorUtils.setAlpha(iconnoColor, 5));
            RenderUtilka.Render2D.drawRoundOutline(
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 183 + (156 / 2) - (57 / 2),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 11 + 46,
                    57, 18, 3F, -0.9f,
                    ColorUtils.rgba(25, 26, 33, 0),
                    new Vector4i(
                            ColorUtils.setAlpha(iconnoColor, 10),
                            ColorUtils.setAlpha(iconnoColor, 10),
                            ColorUtils.setAlpha(iconnoColor, 10),
                            ColorUtils.setAlpha(iconnoColor, 10)
                    ));
            Fonts.intl[14].drawString(matrixStack, "Back",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 125f + Fonts.intl[14].getWidth("Back"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 43,
                    ColorUtils.setAlpha(textColor, 245));
            Fonts.icon[15].drawString(matrixStack, "E",
                    (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 122f + Fonts.intl[14].getWidth("E"),
                    IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 43,
                    ColorUtils.setAlpha(textColor, 184));
        }

        float altX = (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 79.5f - 85 + 3;
        float altY = 298 / 2f;
        float size = 0;
        SmartScissor.push();
        SmartScissor.setFromComponentCoordinates(
                (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 79.5f - 85 + 3 + 104 + 60,
                IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 162.5f + 26 + 27,
                250, 246);

        int index = 0;
        for (Account account : accounts) {
            float panWidth = 29;
            float columnOffset = (index % 2 == 0) ? 0 : 118;
            float rowIndex = (float) Math.floor(index / 2.0);
            float acX = altX + 104 + 60 + columnOffset;
            float acY = IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 159.5f + 26 + (rowIndex * (panWidth + 2f)) + 23.5f + scrollAn * (panWidth + 2f);
            GaussianBlur.startBlur();
            RenderUtilka.Render2D.drawRoundedRect(
                    acX + 1.5f, acY + 1.5f, 159 - 6 - 3 - 38, 26, 5f,
                    ColorUtils.rgba(20, 20, 20, scaledAlpha140));
            GaussianBlur.endBlur(6, 3);

            if (!account.accountName.equalsIgnoreCase(IMinecraft.mc.session.getUsername())) {
                RenderUtilka.Render2D.drawRoundedRect(acX, acY + 1.5f - 1, 159 - 6 - 3 - 38 + 3, 26 + 2, 3f, ColorUtils.setAlpha(iconnoColor, 10));

                RenderUtilka.Render2D.drawRoundOutline(
                        acX, acY + 1.5f - 1, 159 - 6 - 3 - 38 + 3, 26 + 2, 3f, -0.9f,
                        ColorUtils.rgba(25, 26, 33, 0),
                        new Vector4i(
                                ColorUtils.setAlpha(iconnoColor, 10),
                                ColorUtils.setAlpha(iconnoColor, 10),
                                ColorUtils.setAlpha(iconnoColor, 10),
                                ColorUtils.setAlpha(iconnoColor, 10)
                        ));

                Fonts.intl[13].drawCenteredString(matrixStack, account.accountName,
                        acX + 60 + (Fonts.intl[13].getWidth(account.accountName) / 2f) - 34,
                        acY + 9.5f,
                        ColorUtils.setAlpha(textColor, 122));

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                String formattedDate = dateFormat.format(new Date(account.dateAdded));

                Fonts.intl[12].drawCenteredString(
                        matrixStack,
                        formattedDate,
                        acX + 60 + (Fonts.intl[12].getWidth(formattedDate) / 2f) - 34,
                        acY + 17.5f,
                        ColorUtils.setAlpha(textColor, 61)
                );
                String languageLogoPath = "monoton/images/head.png";
                mc.getTextureManager().bindTexture(new ResourceLocation(languageLogoPath));
                RenderUtilka.Render2D.drawTexture(acX + 6F, acY + 6f, 16f, 16f, 2f, 0.5f);
            }

            if (account.accountName.equalsIgnoreCase(IMinecraft.mc.session.getUsername())) {
                RenderUtilka.Render2D.drawRoundedRect(acX, acY + 1.5f - 1, 159 - 6 - 3 - 38 + 3, 26 + 2, 3f, ColorUtils.setAlpha(iconnoColor, 15));

                RenderUtilka.Render2D.drawRoundOutline(
                        acX, acY + 1.5f - 1, 159 - 6 - 3 - 38 + 3, 26 + 2, 3f, -0.9f,
                        ColorUtils.rgba(25, 26, 33, 0),
                        new Vector4i(
                                ColorUtils.setAlpha(iconnoColor, 31),
                                ColorUtils.setAlpha(iconnoColor, 31),
                                ColorUtils.setAlpha(iconnoColor, 31),
                                ColorUtils.setAlpha(iconnoColor, 31)
                        ));

                Fonts.intl[13].drawCenteredString(matrixStack, account.accountName,
                        acX + 60 + (Fonts.intl[13].getWidth(account.accountName) / 2f) - 34,
                        acY + 9.5f,
                        ColorUtils.setAlpha(textColor, 245));

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                String formattedDate = dateFormat.format(new Date(account.dateAdded));

                Fonts.intl[12].drawCenteredString(
                        matrixStack,
                        formattedDate,
                        acX + 60 + (Fonts.intl[12].getWidth(formattedDate) / 2f) - 34,
                        acY + 17.5f,
                        ColorUtils.setAlpha(textColor, 184)
                );
                String languageLogoPath = "monoton/images/head.png";
                mc.getTextureManager().bindTexture(new ResourceLocation(languageLogoPath));
                RenderUtilka.Render2D.drawTexture(acX + 6F, acY + 6f, 16f, 16f, 2f, 1);
            }

            index++;
            size = (float) Math.ceil((index + 2) / 2.0);
        }

        float totalRows = (float) Math.ceil((accounts.size() + 2) / 2.0);
        float visibleRows = 9;
        float maxScroll = totalRows > visibleRows ? -(totalRows - visibleRows) : 0;
        scroll = MathHelper.clamp(scroll, maxScroll, 0);
        scrollAn = MathHelper.clamp(scrollAn, maxScroll, 0);

        SmartScissor.unset();
        SmartScissor.pop();

        float scrollbarX = (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 79.5f - 85 + 3 + 85 + 60 + 250 + 5;
        float scrollbarY = IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 162.5f + 26 + 24;
        float scrollbarWidth = 1;
        float scrollbarHeight = 251;
        float thumbHeight = Math.max(20, scrollbarHeight * (visibleRows / totalRows));
        float scrollRatio = maxScroll != 0 ? scrollAn / maxScroll : 0;
        float thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollRatio;
        StencilUtils.initStencilToWrite();
        RenderUtilka.Render2D.drawRoundedRect(
                scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, 4f,
                ColorUtils.setAlpha(scrollColor, 15));
        StencilUtils.readStencilBuffer(1);

        RenderUtilka.Render2D.drawRoundedRect(
                scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, 4f,
                ColorUtils.setAlpha(scrollColor, 15));

        RenderUtilka.Render2D.drawRoundedRect(
                scrollbarX, thumbY, scrollbarWidth, thumbHeight, 4f,
                ColorUtils.setAlpha(scrollColor, isDraggingScrollbar ? 30 : 20));
        StencilUtils.uninitStencilBuffer();

        IMinecraft.mc.gameRenderer.setupOverlayRendering();
    }
}