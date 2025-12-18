package monoton.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import monoton.Monoton;
import monoton.control.Manager;
import monoton.control.drag.Dragging;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ButtonSetting;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.ui.clickgui.Panel;
import monoton.utils.IMinecraft;
import monoton.utils.anim.Animation;
import monoton.utils.anim.Direction;
import monoton.utils.anim.impl.DecelerateAnimation;
import monoton.utils.anim.impl.EaseBackIn;
import monoton.utils.font.Fonts;
import monoton.utils.font.ReplaceUtil;
import monoton.utils.font.styled.StyledFont;
import monoton.utils.math.GLUtils;
import monoton.utils.math.MathUtil;
import monoton.utils.misc.HudUtil;
import monoton.utils.misc.TimerUtil;
import monoton.utils.other.OtherUtil;
import monoton.utils.render.*;
import monoton.utils.render.animation.AnimationMath;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.DisplayEffectsScreen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.texture.PotionSpriteUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.CooldownTracker;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import org.lwjgl.opengl.GL11;
import ru.kotopushka.compiler.sdk.classes.Profile;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static monoton.module.impl.render.Hud.Status.*;
import static monoton.ui.clickgui.Panel.getColorByName;
import static monoton.ui.clickgui.Panel.selectedColor;
import static monoton.utils.math.MathUtil.lerp;

@Annotation(name = "Hud", type = TypeList.Render, desc = "Интерфейс чита")
public class Hud extends Module {
    private float smoothFPS, smoothPING, smoothX, smoothY, smoothZ, targetYOffset, widthDynamic, CounterEffects, perc;
    private double scale = 0.0D;
    private int itemCount = 0;
    private final Map<String, LocalDateTime> staffJoinTimes = new HashMap<>();

    public MultiBoxSetting elements = new MultiBoxSetting("Элементы",
            new BooleanOption("Ватермарка", true),
            new BooleanOption("Кейбинд", true),
            new BooleanOption("Тотемы", true),
            new BooleanOption("Фейры", false),
            new BooleanOption("Броня", true),
            new BooleanOption("Уведомления", true),
            new BooleanOption("Стафф лист", true),
            new BooleanOption("Список зелий", true),
            new BooleanOption("Таргет Худ", true),
            new BooleanOption("Информация", true),
            new BooleanOption("Таймер индикатор", false),
            new BooleanOption("Расписание евентов", true),
            new BooleanOption("Задержка предметов", false));

    public MultiBoxSetting elementswater = new MultiBoxSetting("Элементы лого",
            new BooleanOption("Лого", true),
            new BooleanOption("Кординаты", true),
            new BooleanOption("Версия", true)).setVisible(() -> elements.get("Ватермарка"));
    public Dragging Music = Monoton.createDrag(this, "Music", 220, 140);

    private final Animation musicAnimation = new EaseBackIn(300, 1, 1.5f);
    private final TimerUtil musicTimer = new TimerUtil();
    private final TimerUtil timerUtil = new TimerUtil();
    private boolean wasStaffChatOpen = false;

    public final BooleanOption optimizer = new BooleanOption("Оптимизировать", false).setVisible(() -> elements.get("Ватермарка"));
    public final BooleanOption particle = new BooleanOption("Партиклы с головы", true);

    public ButtonSetting theme = new ButtonSetting("Закрыть темы", "Открыть темы", () -> {
        Panel.setThemesActive(!Panel.isThemesActive());
        if (!Panel.isThemesActive()) {
            for (Panel.ColorEntry entry : Panel.getColorEntries()) {
                entry.isPickerOpen = false;
            }
            Manager.CONFIG_MANAGER.saveConfiguration("default");
        }
    });

    private final Animation targetInfoAnimation;
    private final Animation keyBindsAnimation = new EaseBackIn(300, 1, 1.5f);
    private final Animation chatAnimation = new EaseBackIn(200, 1, 1.5f);
    private final Animation staffListAnimation = new EaseBackIn(300, 1, 1.5f);
    private final Map<Module, Animation> moduleSlideAnimations = new HashMap<>();
    private final Map<Module, Animation> moduleFadeAnimations = new HashMap<>();
    private final Map<String, Animation> staffSlideAnimations = new HashMap<>();
    private final Map<String, Animation> staffFadeAnimations = new HashMap<>();
    public Dragging keyBinds = Monoton.createDrag(this, "KeyBinds", 120, 95);
    public Dragging staffList = Monoton.createDrag(this, "StaffList", 350, 50);
    private float heightDynamic = 0;
    private int activeModules = 0;
    private int activeStaff = 0;
    private float hDynam = 0;
    private boolean wasChatOpen = false;
    private List<StaffPlayer> staffPlayers = new ArrayList<>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern prefixMatches = Pattern.compile(".*(mod|der|adm|wne|мод|medi|хелп|помо|стаж|адм|владе|отри|таф|taf|yout|curat|курато|dev|раз|supp|сапп|yt|ютуб)(?<!D\\.HELPER).*", Pattern.CASE_INSENSITIVE);
    public Dragging FirworkCountDrag = Monoton.createDrag(this, "FirworkCount", 150, 169);
    public Dragging totemCountDrag = Monoton.createDrag(this, "TotemCount", 150, 150);
    public final Dragging HUDCooldown = Monoton.createDrag(this, "Cooldown", 7, 105);
    public Dragging TimerHUD = Monoton.createDrag(this, "TimerHUD", 160, 180);
    public Dragging events = Monoton.createDrag(this, "onEventsRender", 350, 55);
    private String countdownText = "";
    private String countdownText2 = "";
    private String countdownText3 = "";
    private float health = 0;
    private float health2 = 0;
    private float healthplus = 0;
    private float healthplus2 = 0;
    public final Dragging targetHUD = Monoton.createDrag(this, "TargetHUD", 380, 240);
    private final Animation targetHudAnimation = new EaseBackIn(200, 1, 1.5f);
    private final Animation hurtTintAnimation = new DecelerateAnimation(210, 1.0);
    private final List<HeadParticle> particles = new ArrayList<>();
    private LivingEntity target = null;
    private boolean particlesSpawnedThisHit = false;
    public CopyOnWriteArrayList<net.minecraft.util.text.TextComponent> components = new CopyOnWriteArrayList<>();

    public Hud() {
        addSettings(elements, elementswater, optimizer, particle, theme);
        this.targetInfoAnimation = new DecelerateAnimation(300, 1.0);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate && elements.get(6)) {
            staffPlayers.clear();
            for (ScorePlayerTeam team : mc.world.getScoreboard().getTeams().stream().sorted(Comparator.comparing(Team::getName)).toList()) {
                String name = team.getMembershipCollection().toString();
                name = name.substring(1, name.length() - 1);
                if (namePattern.matcher(name).matches()) {
                    String cleanedPrefix = ReplaceUtil.replaceCustomFonts(team.getPrefix().getString());
                    if (prefixMatches.matcher(cleanedPrefix.toLowerCase(Locale.ROOT)).matches() || Manager.STAFF_MANAGER.isStaff(name)) {
                        staffPlayers.add(new StaffPlayer(name, team.getPrefix()));
                        staffJoinTimes.putIfAbsent(name, LocalDateTime.now());
                    }
                }
            }
        }
        if (event instanceof EventRender eventRender && eventRender.isRender2D()) {
            handleRender(eventRender);
        }
        return false;
    }

    private void handleRender(EventRender renderEvent) {
        final MatrixStack stack = renderEvent.matrixStack;
        if (!this.mc.gameSettings.showDebugInfo) {
            if (elements.get(0)) renderWatermark(stack);
            if (elements.get(1)) renderKeyBinds(stack);
            if (elements.get(2)) renderTotem(stack, renderEvent);
            if (elements.get(3)) renderFirwork(stack, renderEvent);
            if (elements.get(4)) renderArmor(renderEvent);
            if (elements.get(6)) onStaffListRender(stack, renderEvent);
            if (elements.get(7)) renderPotion(stack, renderEvent);
            if (elements.get(10)) renderTimer(stack);
            if (elements.get(11)) renderEvents(stack);
            if (elements.get(12)) renderCooldown(stack, renderEvent);
        }
        if (elements.get(8)) renderTarget(stack);
        if (elements.get(9)) renderInfo(stack);
    }

    private void renderWatermark(MatrixStack stack) {
        StyledFont font = Fonts.intl[14];
        String name = "Monoton Client";
        String version = (elementswater.get(1) ? "v" : "Build - #") + "2.1";
        String coords = String.format("x" + "%d" + " y" + "%d" + " z" + "%d", (int) smoothX, (int) smoothY, (int) smoothZ);
        float widthname = Fonts.intl[14].getWidth(name) + 18;
        float widthversion = Fonts.intl[14].getWidth(version) + 7;
        float widthcoords = Fonts.intl[14].getWidth(coords) + 18;

        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);

        String languageLogoPath = optimizer.get() ? "monoton/images/logo.png" : "monoton/images/gif/" + (new GifUtils().getFrame(120, 32, true) + 1) + ".png";
        mc.getTextureManager().bindTexture(new ResourceLocation(languageLogoPath));

        if (elementswater.get(0)) {
            RenderUtilka.Render2D.drawTexture(7, 7, 32f, 32f, 5f, (float) this.targetInfoAnimation.getOutput());
        }

        RenderUtilka.Render2D.drawBlurredRoundedRectangle(elementswater.get(0) ? 42 : 42 - 33, 7, widthname, 15f, 4, fonColor, 1);
        Fonts.iconnew[15].drawCenteredString(stack, "a", elementswater.get(0) ? 50f : 50f - 33, 13.5f, iconColor);
        font.drawString(stack, name, elementswater.get(0) ? 56f : 56f - 33, 13f, textColor);

        if (elementswater.get(2)) {
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(elementswater.get(0) ? elementswater.get(1) ? 42 + widthname + 2 : 42 : (elementswater.get(1) ? 42 + widthname + 2 : 42) - 33, elementswater.get(1) ? 7 : 23.5f, widthversion, 15f, 4, fonColor, 1);
            font.drawString(stack, version, elementswater.get(0) ? elementswater.get(1) ? 45 + widthname + 2 : 45 : (elementswater.get(1) ? 45 + widthname + 2 : 45) - 33, elementswater.get(1) ? 13f : 30f, textColor);
        }
        if (elementswater.get(1)) {
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(elementswater.get(0) ? 42 : 42 - 33, 23.5f, widthcoords, 15f, 4, fonColor, 1);
            Fonts.icon[15].drawCenteredString(stack, "x", elementswater.get(0) ? 50f : 50f - 33, 30.5f, iconColor);
            font.drawString(stack, coords, elementswater.get(0) ? 56f : 56 - 33f, 30f, textColor);
        }
    }

    private void renderKeyBinds(MatrixStack stack) {
        float maxCombinedWidth = 0;
        float posX = keyBinds.getX();
        float posY = keyBinds.getY();
        this.keyBindsAnimation.setDuration(400);
        this.chatAnimation.setDuration(300);
        float moduleScale = (float) keyBindsAnimation.getOutput();
        float chatScale = (float) chatAnimation.getOutput();
        int headerHeight = 15;
        int padding = 5;
        int offset = 9;
        int iconColor = getColorByName("iconColor");
        int infoColor = getColorByName("infoColor");

        int tempActiveModules = 0;
        Map<Module, Boolean> currentModuleStates = new HashMap<>();
        for (Module f : Manager.FUNCTION_MANAGER.getFunctions()) {
            boolean isEnabled = f.bind != 0 && f.state;
            currentModuleStates.put(f, isEnabled);
            boolean isActive = isEnabled || (moduleSlideAnimations.containsKey(f) && !moduleSlideAnimations.get(f).isDone());
            if (isActive) {
                String functionName = f.name;
                float nameWidth = Fonts.intl[16].getWidth(functionName);
                String bindString = OtherUtil.getKey(f.bind);
                if (bindString == null) {
                    bindString = "NONE";
                }
                bindString = bindString.replace("MOUSE", "M")
                        .replace("LEFT", "L")
                        .replace("RIGHT", "R")
                        .replace("CONTROL", "CTRL")
                        .replace("_", "");
                String shortBindString = bindString.substring(0, Math.min(bindString.length(), 4));
                float bindWidth = Fonts.intl[12].getWidth(shortBindString.toUpperCase());
                float combinedWidth = nameWidth + bindWidth + padding;
                maxCombinedWidth = Math.max(maxCombinedWidth, combinedWidth);
                tempActiveModules++;

                if (isEnabled && !moduleSlideAnimations.containsKey(f)) {
                    Animation slideAnim = new DecelerateAnimation(250, 1, 1.0f);
                    Animation fadeAnim = new DecelerateAnimation(250, 1, 1.0f);
                    slideAnim.setDirection(Direction.FORWARDS);
                    fadeAnim.setDirection(Direction.FORWARDS);
                    moduleSlideAnimations.put(f, slideAnim);
                    moduleFadeAnimations.put(f, fadeAnim);
                } else if (isEnabled && moduleSlideAnimations.get(f).getDirection() != Direction.FORWARDS) {
                    moduleSlideAnimations.get(f).setDirection(Direction.FORWARDS);
                    moduleFadeAnimations.get(f).setDirection(Direction.FORWARDS);
                }
            } else if (moduleSlideAnimations.containsKey(f) && moduleSlideAnimations.get(f).getDirection() != Direction.BACKWARDS) {
                moduleSlideAnimations.get(f).setDirection(Direction.BACKWARDS);
                moduleFadeAnimations.get(f).setDirection(Direction.BACKWARDS);
            }
        }

        moduleSlideAnimations.entrySet().removeIf(entry -> {
            Module module = entry.getKey();
            Animation slideAnim = entry.getValue();
            Animation fadeAnim = moduleFadeAnimations.get(module);
            boolean isDisabled = !currentModuleStates.getOrDefault(module, false);
            return isDisabled && slideAnim.getDirection() == Direction.BACKWARDS && slideAnim.isDone()
                    && fadeAnim != null && fadeAnim.getDirection() == Direction.BACKWARDS && fadeAnim.isDone();
        });

        int textColor = getColorByName("textColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);
        int iconnoColor = getColorByName("iconnoColor");

        int minWidth = Math.max(70, (int) (maxCombinedWidth + 7.9f * padding));
        this.widthDynamic = AnimationMath.fast(this.widthDynamic, minWidth, 25);
        int width = (int) (widthDynamic + 1f);

        StyledFont newcode = Fonts.intl[12];
        float height = tempActiveModules * offset;
        this.heightDynamic = AnimationMath.fast(this.heightDynamic, height, 25);

        if (tempActiveModules > 0 && activeModules == 0 && keyBindsAnimation.getDirection() != Direction.FORWARDS) {
            keyBindsAnimation.setDirection(Direction.FORWARDS);
        } else if (tempActiveModules == 0 && activeModules > 0 && keyBindsAnimation.getDirection() != Direction.BACKWARDS) {
            keyBindsAnimation.setDirection(Direction.BACKWARDS);
        }

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;
        if (isChatOpen && !wasChatOpen && chatAnimation.getDirection() != Direction.FORWARDS) {
            chatAnimation.setDirection(Direction.FORWARDS);
        } else if (!isChatOpen && wasChatOpen && chatAnimation.getDirection() != Direction.BACKWARDS) {
            chatAnimation.setDirection(Direction.BACKWARDS);
        }
        wasChatOpen = isChatOpen;
        activeModules = tempActiveModules;

        if (activeModules < 1 && (isChatOpen || !chatAnimation.isDone())) {
            GLUtils.scaleStart(posX + 34, posY + 12.5f, chatScale);

            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 2.5f, 68, 15, new Vector4f(4f, 0, 4f, 0), fonduoColor, 1);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 3 + 14, 68, 4, new Vector4f(0, 4f, 0, 4f), fonColor, 1);

            RenderUtilka.Render2D.drawRoundedCorner(posX + 70 - 18, posY + 7, 2f, 2f, 2f, iconnoColor);
            RenderUtilka.Render2D.drawRoundedCorner(posX + 70 - 18 + 4, posY + 7, 2f, 2f, 2f, iconnoColor);
            RenderUtilka.Render2D.drawRoundedCorner(posX + 70 - 18 + 8, posY + 7, 2f, 2f, 2f, iconnoColor);

            Fonts.intl[13].drawCenteredString(stack, "Keybinds", posX + 27, posY + 8.3f, infoColor);
            Fonts.iconnew[13].drawCenteredString(stack, "t", posX - 18.5f + 24.5f, posY + 8.9f, iconColor);
            GLUtils.scaleEnd();
        }

        GLUtils.scaleStart(posX + width / 2f, posY + (height + headerHeight) / 2f, moduleScale);
        if (activeModules > 0 || !keyBindsAnimation.isDone()) {
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 2.5f, width - 2, 15, new Vector4f(4f, 0, 4f, 0), fonduoColor, 1);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 3 + 14, width - 2, height + 3, new Vector4f(0, 4f, 0, 4f), fonColor, 1);

            RenderUtilka.Render2D.drawRoundedCorner(posX + width - 18, posY + 7, 2f, 2f, 2f, iconnoColor);
            RenderUtilka.Render2D.drawRoundedCorner(posX + width - 18 + 4, posY + 7, 2f, 2f, 2f, iconnoColor);
            RenderUtilka.Render2D.drawRoundedCorner(posX + width - 18 + 8, posY + 7, 2f, 2f, 2f, iconnoColor);

            Fonts.intl[13].drawCenteredString(stack, "Keybinds", posX + 27, posY + 8.3f, infoColor);
            Fonts.iconnew[13].drawCenteredString(stack, "t", posX - 18.5f + 24.5f, posY + 8.9f, iconColor);

            StencilUtils.initStencilToWrite();
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 3 + 14, width - 2, height + 3, new Vector4f(0, 5f, 0, 5f), fonColor, 1);
            StencilUtils.readStencilBuffer(1);

            int index = 0;
            for (Module f : Manager.FUNCTION_MANAGER.getFunctions()) {
                if (moduleSlideAnimations.containsKey(f)) {
                    String functionName = f.name;
                    String bindString = OtherUtil.getKey(f.bind);
                    if (bindString == null) {
                        bindString = "NONE";
                    }
                    bindString = bindString.replace("MOUSE", "M")
                            .replace("LEFT", "L")
                            .replace("RIGHT", "R")
                            .replace("CONTROL", "C")
                            .replace("_", "");
                    String shortBindString = bindString.substring(0, Math.min(bindString.length(), 4));
                    float bindWidth = newcode.getWidth(shortBindString.toUpperCase());

                    float slideAnimProgress = (float) moduleSlideAnimations.get(f).getOutput();
                    float fadeAnimProgress = (float) moduleFadeAnimations.get(f).getOutput();
                    float translateX = -20 * (1 - slideAnimProgress);
                    int alpha = (int) Math.min(255, Math.max(0, 255 * fadeAnimProgress));

                    GL11.glPushMatrix();
                    GL11.glTranslatef(translateX, 0, 0);

                    Fonts.intl[12].drawString(stack, functionName, posX + padding - 2, posY + headerHeight + padding + (index * offset) + 1.5f, ColorUtils.setAlpha(textColor, alpha));
                    newcode.drawString(stack, shortBindString.toUpperCase(), posX + width - padding - bindWidth - 2, posY + headerHeight + padding + (index * offset) + 0.5f + 1.5f, ColorUtils.setAlpha(textColor, alpha));

                    GL11.glPopMatrix();

                    if (f.bind != 0 && (f.state || !moduleSlideAnimations.get(f).isDone())) {
                        index++;
                    }
                }
            }
            StencilUtils.uninitStencilBuffer();
        }
        GLUtils.scaleEnd();

        keyBinds.setWidth((activeModules < 1 && isChatOpen) ? 68 : width - 4);
        keyBinds.setHeight((activeModules < 1 && isChatOpen) ? 15 + 9 + 3 : activeModules * offset + headerHeight + 1);
    }

    private void renderFirwork(MatrixStack stack, EventRender renderEvent) {
        int fireworkCount = mc.player.inventory.mainInventory.stream()
                .filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() == Items.FIREWORK_ROCKET)
                .mapToInt(ItemStack::getCount)
                .sum();
        fireworkCount += mc.player.inventory.offHandInventory.get(0).getItem() == Items.FIREWORK_ROCKET
                ? mc.player.inventory.offHandInventory.get(0).getCount()
                : 0;

        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);

        float x = FirworkCountDrag.getX() + 7.5f;
        float y = FirworkCountDrag.getY() - 2;

        String countText = String.valueOf(fireworkCount);
        float textWidth = Fonts.intl[14].getWidth(countText);
        float totalWidth = textWidth + 23f;

        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x - 1 - 7.25f, y + 2, 15, 15, new Vector4f(3f, 3f, 0f, 0f), fonduoColor, 1);
        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x - 1 + 7.25f, y + 2, totalWidth - 13, 15, new Vector4f(0f, 0f, 3f, 3f), fonColor, 1);

        int textColor = getColorByName("textColor");

        Fonts.intl[14].drawCenteredString(stack, countText, x + (totalWidth / 2), y + 8.3f, textColor);
        GlStateManager.pushMatrix();
        float scale = 0.5f;
        GlStateManager.scaled(scale, scale, 1.0f);
        drawItemStack(new ItemStack(Items.FIREWORK_ROCKET), (int) ((x - 5f) / scale), (int) ((y + 5.5f) / scale), "", false);
        GlStateManager.popMatrix();

        FirworkCountDrag.setWidth(totalWidth);
        FirworkCountDrag.setHeight(15);
    }

    private void renderTotem(MatrixStack stack, EventRender renderEvent) {
        int totemCount = (int) mc.player.inventory.mainInventory.stream()
                .filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() == Items.TOTEM_OF_UNDYING)
                .count();
        totemCount += mc.player.inventory.offHandInventory.get(0).getItem() == Items.TOTEM_OF_UNDYING ? 1 : 0;
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);

        float x = totemCountDrag.getX() + 8.5f;
        float y = totemCountDrag.getY() - 2;

        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x - 1 - 7.25f, y + 2, 15, 15, new Vector4f(3f, 3f, 0f, 0f), fonduoColor, 1);
        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x - 1 + 7.25f, y + 2, 15, 15, new Vector4f(0f, 0f, 3f, 3f), fonColor, 1);

        int textColor = getColorByName("textColor");

        Fonts.intl[14].drawCenteredString(stack, String.valueOf(totemCount), x + 14, y + 8.3f, textColor);

        GlStateManager.pushMatrix();
        float scale = 0.5f;
        GlStateManager.scaled(scale, scale, 1.0f);
        drawItemStack(new ItemStack(Items.TOTEM_OF_UNDYING), (int) ((x - 5f) / scale), (int) ((y + 5.5f) / scale), "", false);
        GlStateManager.popMatrix();
        totemCountDrag.setWidth(30);
        totemCountDrag.setHeight(15);
    }

    public static void drawItemStack(ItemStack stack, double x, double y, String altText, boolean withoutOverlay) {
        RenderSystem.translated(x, y, 0.0);
        IMinecraft.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        if (!withoutOverlay) {
            IMinecraft.mc.getItemRenderer().renderItemOverlayIntoGUI(IMinecraft.mc.fontRenderer, stack, 0, 0, altText);
        }
        RenderSystem.translated(-x, -y, 0.0);
    }

    public static void drawItemStack(ItemStack stack, double x, double y,  float size, String altText, boolean withoutOverlay) {
        RenderSystem.pushMatrix();
        RenderSystem.translated(x, y, 0.0);
        RenderSystem.scalef(size, size, 1.0f);
        IMinecraft.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        if (!withoutOverlay) {
            IMinecraft.mc.getItemRenderer().renderItemOverlayIntoGUI(IMinecraft.mc.fontRenderer, stack, 0, 0, altText);
        }
        RenderSystem.popMatrix();
    }

    private void renderArmor(final EventRender renderEvent) {
        float xPos = renderEvent.scaledResolution.scaledWidth() / 2f;
        float yPos = renderEvent.scaledResolution.scaledHeight();

        int off = 5;
        if (mc.player.isCreative()) {
            yPos += 14;
        } else {
            if (Manager.FUNCTION_MANAGER.saturationViewer.state && mc.player.getFoodStats().getSaturationLevel() >= 0.5f) {
                yPos -= 9;
            }
            if (mc.player.getAir() < mc.player.getMaxAir() || mc.player.areEyesInFluid(FluidTags.WATER)) {
                if (Manager.FUNCTION_MANAGER.saturationViewer.state && mc.player.getFoodStats().getSaturationLevel() >= 0.5f) {
                    yPos -= 9;
                } else {
                    yPos -= 10;
                }
            }
        }
        for (ItemStack s : mc.player.inventory.armorInventory) {
            drawItemStack(s, xPos - off + 78 * (mc.gameSettings.guiScale / 2f), yPos - 55 * (mc.gameSettings.guiScale / 2f), null, false);
            off += 15;
        }
    }

    private String calculateTimeInList(LocalDateTime joinTime) {
        Duration duration = Duration.between(joinTime, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds) :
                String.format("%d:%02d", minutes, seconds);
    }

    private void onStaffListRender(MatrixStack matrixStack, EventRender render) {
        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);
        int iconnoColor = getColorByName("iconnoColor");
        int infoColor = getColorByName("infoColor");
        float posX = staffList.getX() + 2;
        float posY = staffList.getY();
        int headerHeight = 15;
        int padding = 5;
        int offset = 9;

        Map<String, Float> widthCache = new HashMap<>();
        Map<String, Boolean> currentStaffStates = new HashMap<>();
        float maxWidth = 0;

        for (StaffPlayer staff : staffPlayers.subList(0, Math.min(staffPlayers.size(), 10))) {
            String name = staff.getName();
            currentStaffStates.put(name, true);
            String timeInList = calculateTimeInList(staffJoinTimes.getOrDefault(name, LocalDateTime.now()));
            String fullText = staff.getPrefix().getString() + name + " " + timeInList;
            float totalWidth = Fonts.intl[12].getWidth(fullText) + 25;
            widthCache.put(name, totalWidth);
            maxWidth = Math.max(maxWidth, totalWidth);

            if (!staffSlideAnimations.containsKey(name)) {
                staffSlideAnimations.put(name, new EaseBackIn(400, 1, 1.5f));
                staffSlideAnimations.get(name).setDirection(Direction.FORWARDS);
                staffFadeAnimations.put(name, new EaseBackIn(400, 1, 1.5f));
                staffFadeAnimations.get(name).setDirection(Direction.FORWARDS);
            }
        }

        staffSlideAnimations.entrySet().removeIf(entry -> {
            String name = entry.getKey();
            Animation slideAnim = entry.getValue();
            Animation fadeAnim = staffFadeAnimations.get(name);
            return !currentStaffStates.containsKey(name) && slideAnim.getDirection() == Direction.BACKWARDS && slideAnim.isDone() &&
                    fadeAnim != null && fadeAnim.getDirection() == Direction.BACKWARDS && fadeAnim.isDone();
        });

        float widthStaff = Math.max(maxWidth, 95);
        float height = Math.min(staffPlayers.size(), 10) * offset;
        this.hDynam = AnimationMath.fast(this.hDynam, height, 10);
        this.widthDynamic = AnimationMath.fast(this.widthDynamic, widthStaff, 10);

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;
        if (isChatOpen != wasStaffChatOpen) {
            chatAnimation.setDirection(isChatOpen ? Direction.FORWARDS : Direction.BACKWARDS);
            chatAnimation.reset();
            wasStaffChatOpen = isChatOpen;
        }

        float staffScale = (float) staffListAnimation.getOutput();
        float chatScale = (float) chatAnimation.getOutput();
        int activeStaffCount = Math.min(staffPlayers.size(), 10);

        if (activeStaffCount != activeStaff) {
            staffListAnimation.setDirection(activeStaffCount > 0 ? Direction.FORWARDS : Direction.BACKWARDS);
            staffListAnimation.reset();
            activeStaff = activeStaffCount;
        }

        if (activeStaff < 1 && (isChatOpen || !chatAnimation.isDone())) {
            GLUtils.scaleStart(posX + 34, posY + 12.5f, chatScale);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 2.5f, 68, 15, new Vector4f(4f, 0, 4f, 0), fonduoColor, 1);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 3 + 14, 68, 4, new Vector4f(0, 4f, 0, 4f), fonColor, 1);

            RenderUtilka.Render2D.drawRoundedCorner(posX + 68 - 18, posY + 7, 2f, 2f, 2f, iconnoColor);
            RenderUtilka.Render2D.drawRoundedCorner(posX + 68 - 18 + 4, posY + 7, 2f, 2f, 2f, iconnoColor);
            RenderUtilka.Render2D.drawRoundedCorner(posX + 68 - 18 + 8, posY + 7, 2f, 2f, 2f, iconnoColor);

            Fonts.intl[13].drawCenteredString(matrixStack, "Staffs", posX + 12 + Fonts.intl[13].getWidth("Staffs") / 2, posY + 8.3f, infoColor);
            Fonts.iconnew[13].drawCenteredString(matrixStack, "o", posX - 18.5f + 24.5f, posY + 8.9f, iconColor);
            GLUtils.scaleEnd();

            staffList.setWidth(68);
            staffList.setHeight(15 + 9 + 3);
            return;
        }

        if (activeStaff > 0 || !staffListAnimation.isDone()) {
            GLUtils.scaleStart(posX + widthStaff / 2f, posY + (height + headerHeight) / 2f, staffScale);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 2.5f, widthStaff - 2, 15, new Vector4f(4f, 0, 4f, 0), fonduoColor, 1);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 3 + 14, widthStaff - 2, height + 3, new Vector4f(0, 4f, 0, 4f), fonColor, 1);

            RenderUtilka.Render2D.drawRoundedCorner(posX + widthStaff - 18, posY + 7, 2f, 2f, 2f, iconnoColor);
            RenderUtilka.Render2D.drawRoundedCorner(posX + widthStaff - 18 + 4, posY + 7, 2f, 2f, 2f, iconnoColor);
            RenderUtilka.Render2D.drawRoundedCorner(posX + widthStaff - 18 + 8, posY + 7, 2f, 2f, 2f, iconnoColor);

            String staffText = "Staffs ";
            String staffText2 = (activeStaff > 0 ? "(" + activeStaff + ")" : "");
            Fonts.intl[13].drawCenteredString(matrixStack, staffText, posX + 12 + Fonts.intl[13].getWidth(staffText) / 2, posY + 8.3f, infoColor);
            Fonts.intl[13].drawCenteredString(matrixStack, staffText2, posX + Fonts.intl[13].getWidth(staffText) + 12 + Fonts.intl[13].getWidth(staffText2) / 2, posY + 8.3f, ColorUtils.setAlpha(infoColor, 155));
            Fonts.iconnew[13].drawCenteredString(matrixStack, "o", posX - 18.5f + 24.5f, posY + 8.9f, iconColor);

            StencilUtils.initStencilToWrite();
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 3 + 14, widthStaff - 2, height + 3, new Vector4f(0, 5f, 0, 5f), fonColor, 1);
            StencilUtils.readStencilBuffer(1);

            for (int index = 0; index < Math.min(staffPlayers.size(), 10); index++) {
                StaffPlayer staff = staffPlayers.get(index);
                String name = staff.getName();
                ITextComponent prefix = staff.getPrefix();
                String timeInList = calculateTimeInList(staffJoinTimes.getOrDefault(name, LocalDateTime.now()));

                float slideAnimProgress = (float) staffSlideAnimations.get(name).getOutput();
                float fadeAnimProgress = (float) staffFadeAnimations.get(name).getOutput();
                float translateX = -20 * (1 - slideAnimProgress);
                int alpha = (int) Math.min(255, Math.max(0, 255 * fadeAnimProgress));

                GL11.glPushMatrix();
                GL11.glTranslatef(translateX, 0, 0);

                float prefixWidth = Fonts.intl[12].getWidth(prefix.getString());
                Fonts.intl[12].drawText(matrixStack, prefix, posX + padding - 2, posY + headerHeight + padding + 1 + (index * offset) + 0.4f);
                Fonts.intl[12].drawString(matrixStack, name, posX + padding - 2 + prefixWidth, posY + headerHeight + padding + 1 + (index * offset) + 0.4f, ColorUtils.setAlpha(textColor, alpha));

                int timeColor = staff.getStatus() == NEAR ? new Color(193, 107, 46, 255).getRGB() :
                        staff.getStatus() == SPEC ? new Color(214, 214, 214, 255).getRGB() :
                                staff.getStatus() == VANISHED ? new Color(202, 40, 40, 255).getRGB() :
                                        new Color(92, 218, 19, 255).getRGB();

                Fonts.intl[12].drawString(matrixStack, timeInList, posX + padding - 2 + widthStaff - Fonts.intl[12].getWidth(timeInList) - 10, posY + headerHeight + padding + 1 + (index * offset) + 0.4f, ColorUtils.setAlpha(timeColor, alpha));

                String prefixString = prefix.getString();
                if (prefixString.contains("§a●")) {
                    RenderUtilka.Render2D.drawCircle2(posX + padding - 1f, posY + headerHeight + padding + 0.5f + (index * offset), 1.75F, new Color(84, 252, 84, alpha).getRGB());
                } else if (prefixString.contains("§c●")) {
                    RenderUtilka.Render2D.drawCircle2(posX + padding - 1f, posY + headerHeight + padding + 0.5f + (index * offset), 1.75F, new Color(252, 84, 84, alpha).getRGB());
                } else if (prefixString.contains("§6●")) {
                    RenderUtilka.Render2D.drawCircle2(posX + padding - 1f, posY + headerHeight + padding + 0.5f + (index * offset), 1.75F, new Color(252, 168, 0, alpha).getRGB());
                } else if (prefixString.contains("●")) {
                    RenderUtilka.Render2D.drawCircle2(posX + padding - 1f, posY + headerHeight + padding + 0.5f + (index * offset), 1.75F, new Color(252, 84, 84, alpha).getRGB());
                }

                float headY = posY + headerHeight + padding + (index * offset) - 3;
                float headSize = 8.0f;
                AbstractClientPlayerEntity targetPlayer = mc.world.getPlayers().stream()
                        .filter(player -> player.getName().getString().equals(name))
                        .findFirst()
                        .orElse(null);

                String languageLogoPath = "monoton/images/head.png";
                mc.getTextureManager().bindTexture(new ResourceLocation(languageLogoPath));
                if (targetPlayer != null) {
                    RenderUtilka.Render2D.drawRoundFace(posX + padding + widthStaff + 2 - 12f - (Fonts.intl[12].getWidth(timeInList)) - 10.5f, headY + 1f + 1.1f, headSize - 2, headSize - 2, 2.2f, (float) this.targetInfoAnimation.getOutput(), targetPlayer);
                } else {
                    RenderUtilka.Render2D.drawTexture(posX + padding + widthStaff + 2 - 12f - (Fonts.intl[12].getWidth(timeInList)) - 10.5f, headY + 1f + 1.1f, 6.2f, 6.2f, 2.2f, (float) this.targetInfoAnimation.getOutput());
                }

                GL11.glPopMatrix();
            }

            StencilUtils.uninitStencilBuffer();
            GLUtils.scaleEnd();
        }

        staffJoinTimes.keySet().removeIf(staffName -> !currentStaffStates.containsKey(staffName));
        staffList.setWidth(activeStaff < 1 && isChatOpen ? 68 : widthStaff);
        staffList.setHeight(activeStaff < 1 && isChatOpen ? 15 + 9 + 3 : hDynam + headerHeight);
    }

    private class StaffPlayer {

        @Getter
        String name;
        @Getter
        ITextComponent prefix;
        @Getter
        Status status;

        private StaffPlayer(String name, ITextComponent prefix) {
            this.name = name;
            this.prefix = prefix;

            updateStatus();
        }

        private void updateStatus() {
            for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
                if (player.getName().getString().equals(name)) {
                    status = NEAR;
                    return;
                }
            }

            for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equals(name)) {
                    if (info.getGameType() == GameType.SPECTATOR) {
                        status = SPEC;
                        return;
                    }
                    status = NONE;
                    return;
                }
            }

            status = VANISHED;
        }
    }

    public enum Status {
        NONE(""),
        NEAR(" §e[NEAR]"),
        SPEC(" §c[SPEC]"),
        VANISHED(" §6[VANISHED]");

        @Getter
        final String string;

        Status(String string) {
            this.string = string;
        }
    }

    private void renderCooldown(final MatrixStack matrixStack, final EventRender renderEvent) {
        float width = 56.0F;
        float height = 20.0F;
        float x = this.HUDCooldown.getX();
        float y = this.HUDCooldown.getY();
        StyledFont small = Fonts.intl[13];
        int firstColor = selectedColor;
        int textColor = getColorByName("textColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);
        int iconColor = getColorByName("iconColor");
        int infoColor = getColorByName("infoColor");

        int index2 = 0;
        Map<Integer, Integer> cooldownDurations = new HashMap<>();
        CooldownTracker tracker = mc.player.getCooldownTracker();
        Map<Item, CooldownTracker.Cooldown> cooldowns = tracker.cooldowns;
        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;

        if (isChatOpen != wasChatOpen) {
            chatAnimation.setDirection(isChatOpen ? Direction.FORWARDS : Direction.BACKWARDS);
            wasChatOpen = isChatOpen;
        }
        float chatScale = (float) chatAnimation.getOutput();

        if (cooldowns.isEmpty() && (isChatOpen || !chatAnimation.isDone())) {
            GLUtils.scaleStart(x + 34, y + 12.5f, chatScale);
            float finalY = y;
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(x + 0.25f, y + 0.5f, 21, 21, new Vector4f(5f, 5f, 0f, 0f), fonduoColor, 1);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(x + 20.75f, y + 0.5f, 80 - 4, 21, new Vector4f(0f, 0f, 5f, 5f), fonColor, 1);

            RenderUtilka.Render2D.drawBlurredRoundedRectangle(x + 25, y - 2.5f + 18f, 80 - 13, 1.1f, 1.5f, firstColor, 1);

            Fonts.intl[13].drawCenteredString(matrixStack, "Cooldowns", x + 42, y + 8.3f, infoColor);
            Fonts.intl[13].drawCenteredString(matrixStack, "0:00", x + 84, y + 8.3f, infoColor);
            Fonts.iconnew[20].drawCenteredString(matrixStack, "r", x - 13.5f + 24.5f, y + 9f, iconColor);
            GLUtils.scaleEnd();

            this.HUDCooldown.setWidth(88);
            this.HUDCooldown.setHeight(21);
            return;
        }

        for (Map.Entry<Item, CooldownTracker.Cooldown> entry : cooldowns.entrySet()) {
            Item item = entry.getKey();
            CooldownTracker.Cooldown cooldown = entry.getValue();
            int cooldownId = System.identityHashCode(cooldown);

            long currentTick = tracker.getTicks();
            long expireTick = cooldown.getExpireTicks();
            long remainingTicks = expireTick - currentTick;

            if (remainingTicks <= 0) {
                continue;
            }

            cooldownDurations.putIfAbsent(cooldownId, (int) remainingTicks);
            int initialDuration = cooldownDurations.getOrDefault(cooldownId, (int) remainingTicks);
            initialDuration = Math.max(initialDuration, 1);

            float progress = (float) remainingTicks / initialDuration;
            progress = Math.max(0, Math.min(progress, 1.0f));

            float remainingSeconds = remainingTicks / 20.0f;
            String durationText = formatTime(remainingSeconds);
            String itemName = item.getName().getString();
            if (itemName.length() > 12) {
                itemName = itemName.substring(0, 12) + "..";
            }

            float textWidth = small.getWidth(itemName + durationText) + 21.0F;

            ItemStack itemStack = new ItemStack(item);

            float progressBar = mc.player.getCooldownTracker().getCooldown(item, renderEvent.getPartialTicks());
            float maxCooldownTicks = 20.0f;
            if (item == Items.ENDER_PEARL) {
                maxCooldownTicks = 20.0f;
            }
            float barWidth = (textWidth - 4) * progressBar;

            float drawX = x;
            float finalY = y;
            float finalTextWidth = textWidth;

            RenderUtilka.Render2D.drawBlurredRoundedRectangle(drawX + 0.25f, y + 0.5f, 21, 21, new Vector4f(5f, 5f, 0f, 0f), fonduoColor, 1);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(drawX + 20.75f, y + 0.5f, textWidth - 4, 21, new Vector4f(0f, 0f, 5f, 5f), fonColor, 1);

            RenderUtilka.Render2D.drawRoundedRect(drawX + 25, y - 2.5f + 18f, textWidth - 4 - 10f, 1.1f, 1.5f, ColorUtils.setAlpha(firstColor, 82));
            RenderUtilka.Render2D.drawRoundedRect(drawX + 25, y - 2.5f + 18f, barWidth - 9, 1.1f, 1.5f, firstColor);

            float textX = x + 24.5F;
            small.drawString(matrixStack, itemName + TextFormatting.GRAY + " ", (double) textX, (double) (y + 7.5F), textColor);
            small.drawString(matrixStack, durationText, (double) (x + textWidth - (small.getWidth(durationText) - 12)), (double) (y + 7.5F), textColor);

            GlStateManager.pushMatrix();
            float scale = 0.8f;
            GlStateManager.scaled(scale, scale, 1.0f);
            GlStateManager.disableBlend();
            drawItemStack(itemStack, (x + 6F) / scale - 2f, (y + 6f) / scale - 1.5f, "", true);
            GlStateManager.popMatrix();

            y += 23F;
            index2++;
        }

        cooldownDurations.keySet().removeIf(id -> cooldowns.entrySet().stream().noneMatch(e -> System.identityHashCode(e.getValue()) == id));

        this.HUDCooldown.setWidth(width);
        this.HUDCooldown.setHeight((height + 2f) * index2 - 6f);
    }

    private String formatTime(float seconds) {
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%d:%02d", mins, secs);
    }

    public final Dragging HUDPotion = Monoton.createDrag(this, "Potion", 7, 42);
    private final Map<Integer, Integer> effectDurations = new HashMap<>();
    private void renderPotion(final MatrixStack matrixStack, final EventRender renderEvent) {
        float width = 57.0F;
        float height = 21.0F;
        float x = this.HUDPotion.getX();
        float y = this.HUDPotion.getY();
        StyledFont small = Fonts.intl[13];
        int firstColor2 = selectedColor;
        int textColor = getColorByName("textColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);
        int iconColor = getColorByName("iconColor");
        int infoColor = getColorByName("infoColor");

        int index = 0;
        this.CounterEffects = 0.0F;
        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;

        if (isChatOpen != wasChatOpen) {
            chatAnimation.setDirection(isChatOpen ? Direction.FORWARDS : Direction.BACKWARDS);
            wasChatOpen = isChatOpen;
        }
        float chatScale = (float) chatAnimation.getOutput();

        if (mc.player.getActivePotionEffects().isEmpty() && (isChatOpen || !chatAnimation.isDone())) {
            GLUtils.scaleStart(x + 34, y + 12.5f, chatScale);
            float finalY = y;

            RenderUtilka.Render2D.drawBlurredRoundedRectangle(x + 0.25f, y + 0.5f, 21, 21, new Vector4f(5f, 5f, 0f, 0f), fonduoColor, 1);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(x + 20.75f, y + 0.5f, 70 - 4, 21, new Vector4f(0f, 0f, 5f, 5f), fonColor, 1);

            RenderUtilka.Render2D.drawRoundedRect(x + 25, y - 2.5f + 18f, 70 - 13, 1.1f, 1.5f, firstColor2);

            Fonts.intl[13].drawCenteredString(matrixStack, "Potions", x + 37, y + 8.3f, infoColor);
            Fonts.intl[13].drawCenteredString(matrixStack, "0:00", x + 74, y + 8.3f, infoColor);
            Fonts.iconnew[20].drawCenteredString(matrixStack, "o", x - 13.5f + 24.5f, y + 9f, iconColor);
            GLUtils.scaleEnd();

            this.HUDPotion.setWidth(88);
            this.HUDPotion.setHeight(21);
            return;
        }

        for (EffectInstance effectInstance : mc.player.getActivePotionEffects()) {
            Effect effect = effectInstance.getPotion();
            int effectId = System.identityHashCode(effectInstance);
            int remainingTicks = effectInstance.getDuration();

            effectDurations.putIfAbsent(effectId, remainingTicks);
            int initialDuration = effectDurations.getOrDefault(effectId, remainingTicks);
            initialDuration = Math.max(initialDuration, 1);

            float progress = (float) remainingTicks / initialDuration;
            progress = Math.max(0, Math.min(progress, 1.0f));

            String durationText = EffectUtils.getPotionDurationString(effectInstance, 1.0F);
            String text = I18n.format(effectInstance.getEffectName());
            int levelPotion = effectInstance.getAmplifier() + 1;

            float textWidth = small.getWidth(text + durationText + (effectInstance.getAmplifier() != 0 ? " " + levelPotion : "")) + 21.0F;
            float barWidth = (textWidth - 4) * progress;

            boolean isNegative = !effect.isBeneficial();
            int effectColor = isNegative ? new Color(221, 15, 15, 173).getRGB() : textColor;

            float drawX = x;

            float finalY1 = y;
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(drawX + 0.25f, y + 0.5f, 21, 21, new Vector4f(5f, 5f, 0f, 0f), fonduoColor, 1);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(drawX + 20.75f, y + 0.5f, textWidth - 4, 21, new Vector4f(0f, 0f, 5f, 5f), fonColor, 1);

            RenderUtilka.Render2D.drawRoundedRect(drawX + 25, y - 2.5f + 18f, textWidth - 4 - 10f, 1.1f, 1.5f, ColorUtils.setAlpha(firstColor2, 82));
            RenderUtilka.Render2D.drawRoundedRect(drawX + 25, y - 2.5f + 18f, barWidth - 9, 1.1f, 1.5f, firstColor2);

            float textX = x + 24.5F;
            small.drawString(matrixStack, text + TextFormatting.GRAY + " ", (double) textX, (double) (y + 7.5F), effectColor);
            small.drawString(matrixStack, (effectInstance.getAmplifier() != 0 ? " " + levelPotion : "") + TextFormatting.GRAY + " ", (double) textX - 1 + small.getWidth(text + " "), (double) (y + 7.5F), ColorUtils.setAlpha(textColor, 200));
            small.drawString(matrixStack, durationText, (double) (x + textWidth - (small.getWidth(durationText) - 12)), (double) (y + 7.5F), textColor);

            PotionSpriteUploader uploader = mc.getPotionSpriteUploader();
            TextureAtlasSprite sprite = uploader.getSprite(effect);
            mc.getTextureManager().bindTexture(sprite.getAtlasTexture().getTextureLocation());
            DisplayEffectsScreen.blit(matrixStack, (int) (x + 6F), (int) (y + 6f), 10, 10, 10, sprite);

            y += 23F;
            index++;
        }

        effectDurations.keySet().removeIf(id -> mc.player.getActivePotionEffects().stream().noneMatch(e -> System.identityHashCode(e) == id));

        this.CounterEffects = (float) index;
        this.HUDPotion.setWidth(width);
        this.HUDPotion.setHeight((height + 2f) * this.CounterEffects - 6f);
    }

    private void renderTimer(MatrixStack stack) {
        float x = this.TimerHUD.getX() + 3;
        float y = this.TimerHUD.getY();
        float quotient = Manager.FUNCTION_MANAGER.timer.maxViolation / Manager.FUNCTION_MANAGER.timer.timerAmount.getValue().floatValue();
        float minimumValue = Math.min(Manager.FUNCTION_MANAGER.timer.getViolation(), quotient);
        this.perc = AnimationMath.lerp(this.perc, (quotient - minimumValue) / quotient, 10.0F);

        int percentage = (int) (this.perc * 100);
        percentage = Math.max(0, Math.min(99, percentage));
        int textColor = getColorByName("textColor");

        float width = 72.0F;
        float height = 4.0F;
        int firstColor2 = selectedColor;
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);

        int roundDegree = 4;
        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x - 2.5f, y + 0.5f, width + 5 - 17f, height + 8, new Vector4f(4, 4, 0, 0), fonColor, 1);
        RenderUtilka.Render2D.drawBlurredRoundedRectangle(x - 3 + width + 5 - 16.7f, y + 0.5f, 17, height + 8, new Vector4f(0, 0, 4, 4), fonduoColor, 1);

        RenderUtilka.Render2D.drawRoundedRect(x + 2, y + 5f, width - 2 - 17, 3, 0.8F, ColorUtils.rgba(73, 68, 68, 255));
        RenderUtilka.Render2D.drawRoundedRect(x + 2, y + 5f, (width - 2 - 17) * (percentage / 100.0f), 3, 0.8F, firstColor2);

        Fonts.intl[10].drawString(stack, percentage + "%", x + 7 + width - 2 - 14f - (Fonts.intl[10].getWidth(percentage + "") / 2), y + 6.5f, textColor);

        this.TimerHUD.setWidth(width + 5);
        this.TimerHUD.setHeight(height + 5);
    }

    private void updateCountdown2() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));

        LocalDateTime targetTime = now.withHour(15).withMinute(30).withSecond(0);

        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1);
        }

        long hours = ChronoUnit.HOURS.between(now, targetTime);
        long minutes = ChronoUnit.MINUTES.between(now, targetTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, targetTime) % 60;

        StringBuilder countdownBuilder = new StringBuilder();

        if (hours > 0) {
            countdownBuilder.append(hours).append("h ");
        }
        if (minutes > 0) {
            countdownBuilder.append(minutes).append("m ");
        }
        countdownBuilder.append(seconds).append("s");

        countdownText2 = countdownBuilder.toString().trim();
    }

    private void updateCountdown3() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
        LocalDateTime targetTime = now.withHour((now.getHour() / 6) * 6).withMinute(0).withSecond(0);

        if (!now.isBefore(targetTime)) {
            targetTime = targetTime.plusHours(6);
        }

        long hours = ChronoUnit.HOURS.between(now, targetTime);
        long minutes = ChronoUnit.MINUTES.between(now, targetTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, targetTime) % 60;

        StringBuilder countdownBuilder = new StringBuilder();
        if (hours > 0) {
            countdownBuilder.append(hours).append("h ");
        }
        if (minutes > 0) {
            countdownBuilder.append(minutes).append("m ");
        }
        countdownBuilder.append(seconds).append("s");

        countdownText3 = countdownBuilder.toString().trim();
    }

    private void updateCountdown() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));

        List<LocalDateTime> schedule = Arrays.asList(
                now.withHour(9).withMinute(0).withSecond(0),
                now.withHour(11).withMinute(0).withSecond(0),
                now.withHour(13).withMinute(0).withSecond(0),
                now.withHour(15).withMinute(0).withSecond(0),
                now.withHour(17).withMinute(0).withSecond(0),
                now.withHour(19).withMinute(0).withSecond(0),
                now.withHour(21).withMinute(0).withSecond(0),
                now.withHour(23).withMinute(0).withSecond(0)
        );


        LocalDateTime nextEventTime = schedule.stream()
                .filter(t -> t.isAfter(now))
                .findFirst()
                .orElse(now.withHour(7).plusDays(1));

        long hours = ChronoUnit.HOURS.between(now, nextEventTime);
        long minutes = ChronoUnit.MINUTES.between(now, nextEventTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, nextEventTime) % 60;

        StringBuilder countdownBuilder = new StringBuilder();

        if (hours > 0) {
            countdownBuilder.append(hours).append("h ");
        }
        if (minutes > 0) {
            countdownBuilder.append(minutes).append("m ");
        }
        countdownBuilder.append(seconds).append("s");

        countdownText = countdownBuilder.toString().trim();
    }

    private void renderEvents(MatrixStack stack) {
        float posX = events.getX();
        float posY = events.getY() - 1;
        int headerHeight = 15;
        int padding = 5;
        int firstColor2 = selectedColor;

        int width = 86;
        float height = 27;
        int roundDegree = 6;

        updateCountdown();
        updateCountdown2();
        updateCountdown3();
        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);
        int iconnoColor = getColorByName("iconnoColor");
        int infoColor = getColorByName("infoColor");

        RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 2.5f, width - 2, 15, new Vector4f(4f, 0, 4f, 0), fonduoColor, 1);
        RenderUtilka.Render2D.drawBlurredRoundedRectangle(posX - 1, posY + 3 + 14, width - 2, 30, new Vector4f(0, 4f, 0, 4f), fonColor, 1);

        RenderUtilka.Render2D.drawRoundedCorner(posX + width - 18, posY + 7, 2f, 2f, 2f, iconnoColor);
        RenderUtilka.Render2D.drawRoundedCorner(posX + width - 18 + 4, posY + 7, 2f, 2f, 2f, iconnoColor);
        RenderUtilka.Render2D.drawRoundedCorner(posX + width - 18 + 8, posY + 7, 2f, 2f, 2f, iconnoColor);


        Fonts.intl[13].drawCenteredString(stack, "Events", posX + 12 + Fonts.intl[13].getWidth("Events") / 2, posY + 8.3f, infoColor);
        Fonts.iconnew[13].drawCenteredString(stack, "q", posX - 18.5f + 24.5f, posY + 8.9f, iconColor);


        Fonts.intl[12].drawString(stack, "AirDrop", posX + padding - 2, posY + headerHeight + padding + 1 + 1f, textColor);
        Fonts.intl[12].drawString(stack, countdownText, posX + width - 7.5f - Fonts.intl[12].getWidth(countdownText), posY + headerHeight + padding + 1 + 1f, ColorUtils.setAlpha(iconColor, 185));

        Fonts.intl[12].drawString(stack, "Mascot", posX + padding - 2, posY + headerHeight + padding + 10 + 1.1f, textColor);
        Fonts.intl[12].drawString(stack, countdownText2, posX + width - 7.5f - Fonts.intl[12].getWidth(countdownText2), posY + headerHeight + padding + 10 + 1.1f, ColorUtils.setAlpha(iconColor, 185));

        Fonts.intl[12].drawString(stack, "Chest", posX + padding - 2, posY + headerHeight + padding + 19 + 1.2f, textColor);
        Fonts.intl[12].drawString(stack, countdownText3, posX + width - 7.5f - Fonts.intl[12].getWidth(countdownText3), posY + headerHeight + padding + 19 + 1.2f, ColorUtils.setAlpha(iconColor, 185));

        events.setWidth(width);
        events.setHeight(41.5f);
    }

    private void renderInfo(MatrixStack stack) {
        int targetFPS = mc.debugFPS;
        smoothFPS = lerp(smoothFPS, targetFPS, 0.055f);

        int targetPING = HudUtil.calculatePing();
        smoothPING = lerp(smoothPING, targetPING, 0.055f);

        int targetZ = (int) IMinecraft.mc.player.getPosZ();
        smoothZ = lerp(smoothZ, targetZ, 0.055f);

        int targetY = (int) IMinecraft.mc.player.getPosY();
        smoothY = lerp(smoothY, targetY, 0.055f);

        int targetX = (int) IMinecraft.mc.player.getPosX();
        smoothX = lerp(smoothX, targetX, 0.055f);

        String ping = (int) smoothPING + "ms";
        String fps = (int) smoothFPS + "fps";
        String tps = Monoton.getServerTPS().getTPS() + "tps";
        String bps = HudUtil.calculateBPS() + "bps";
        String login = Profile.getUsername();
        String uid = Profile.getUid() + " (" + Profile.getRole() + ")";
        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");

        int targetYOffset = IMinecraft.mc.currentScreen instanceof ChatScreen ? 0 : 13;
        this.targetYOffset = lerp(this.targetYOffset, targetYOffset, 0.17f);

        Fonts.icon[13].drawCenteredString(stack, "I", 8, 27F + 7.5f + (sr.scaledHeight() - 62) + this.targetYOffset, ColorUtils.setAlpha(iconColor, 200));
        Fonts.intl[13].drawString(stack, ping, 12, 27F + 7 + (sr.scaledHeight() - 62) + this.targetYOffset, textColor);

        Fonts.icon[13].drawCenteredString(stack, "G", 8 + Fonts.intl[13].getWidth(ping) + 11, 27F + 7.5f + (sr.scaledHeight() - 62) + this.targetYOffset, ColorUtils.setAlpha(iconColor, 200));
        Fonts.intl[13].drawString(stack, fps, 12 + Fonts.intl[13].getWidth(ping) + 11, 27F + 7 + (sr.scaledHeight() - 62) + this.targetYOffset, textColor);

        Fonts.icon[13].drawCenteredString(stack, "c", 8, 27F + 14f + (sr.scaledHeight() - 62) + this.targetYOffset, ColorUtils.setAlpha(iconColor, 200));
        Fonts.intl[13].drawString(stack, bps, 12, 27F + 13.5f + (sr.scaledHeight() - 62) + this.targetYOffset, textColor);

        Fonts.icon[13].drawCenteredString(stack, "J", 8 + Fonts.intl[13].getWidth(bps) + 11, 27F + 14f + (sr.scaledHeight() - 62) + this.targetYOffset, ColorUtils.setAlpha(iconColor, 200));
        Fonts.intl[13].drawString(stack, tps, 12 + Fonts.intl[13].getWidth(bps) + 11, 27F + 13.5f + (sr.scaledHeight() - 62) + this.targetYOffset, textColor);


        Fonts.icon[13].drawCenteredString(stack, "b", sr.scaledWidth() - Fonts.intl[13].getWidth(uid) - 12, 27F + 14f + (sr.scaledHeight() - 62) + this.targetYOffset, ColorUtils.setAlpha(iconColor, 200));
        Fonts.intl[13].drawString(stack, uid, sr.scaledWidth() - Fonts.intl[13].getWidth(uid) - 8, 27F + 13.5f + (sr.scaledHeight() - 62) + this.targetYOffset, textColor);

        Fonts.icon[13].drawCenteredString(stack, "v", sr.scaledWidth() - Fonts.intl[13].getWidth(login) - 12, 27F + 7f + (sr.scaledHeight() - 62) + this.targetYOffset, ColorUtils.setAlpha(iconColor, 200));
        Fonts.intl[13].drawString(stack, login, sr.scaledWidth() - Fonts.intl[13].getWidth(login) - 8, 27F + 7.5f + (sr.scaledHeight() - 62) + this.targetYOffset, textColor);
    }

    private void renderTarget(final MatrixStack stack) {
        this.target = getTarget();
        this.targetHudAnimation.setDuration(250);
        this.scale = targetHudAnimation.getOutput();
        int firstColor2 = selectedColor;
        int textColor = getColorByName("textColor");

        if (scale == 0.0F) {
            target = null;
        }

        if (target == null) {
            particles.clear();
            return;
        }

        if (target.hurtTime > 0 && !particlesSpawnedThisHit) {
            spawnHeadParticles();
            particlesSpawnedThisHit = true;
        } else if (target.hurtTime == 0) {
            particlesSpawnedThisHit = false;
        }

        boolean isInvisible = target instanceof LivingEntity && ((LivingEntity) target).isInvisible();

        final double alpha = this.targetInfoAnimation.getOutput() * 255.0;

        updatePlayerHealth();

        final String targetName = this.target.getName().getString();
        float xPosition = targetHUD.getX() - 4, yPosition = targetHUD.getY() - 4, maxWidth = 112, maxHeight = 48;

        this.health = AnimationMath.fast(health, target.getHealth() / target.getMaxHealth(), 12);
        this.health = MathHelper.clamp(this.health, 0, 1);
        this.health2 = AnimationMath.fast(health2, target.getHealth() / target.getMaxHealth(), 4.5f);
        this.health2 = MathHelper.clamp(this.health2, 0, 1);
        this.healthplus2 = AnimationMath.fast(this.healthplus2, target.getAbsorptionAmount() / target.getMaxHealth(), 4.5f);
        this.healthplus2 = MathHelper.clamp(this.healthplus2, 0, 1);
        this.healthplus = AnimationMath.fast(this.healthplus, target.getAbsorptionAmount() / target.getMaxHealth(), 12);
        this.healthplus = MathHelper.clamp(this.healthplus, 0, 1);
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int goldColor = getColorByName("goldColor");

        float hurtProgress = target.hurtTime > 0 ? (float) target.hurtTime / 10.0f : 0.0f;
        hurtTintAnimation.setDirection(target.hurtTime > 0 ? Direction.FORWARDS : Direction.BACKWARDS);
        float tintAlpha = (float) hurtTintAnimation.getOutput() * hurtProgress * 100;
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);
        GlStateManager.pushMatrix();
        AnimationMath.sizeAnimation(xPosition + (maxWidth / 2), yPosition + (maxHeight / 2), scale);

        RenderUtilka.Render2D.drawBlurredRoundedRectangle(xPosition + 3, yPosition + 3, 32f, 31f, new Vector4f(5, 5, 0, 0), fonduoColor, 1);
        RenderUtilka.Render2D.drawBlurredRoundedRectangle(xPosition + 3.5f + 31f, yPosition + 3, 71f, 31f, new Vector4f(0, 0, 5, 5), fonColor, 1);

        RenderUtilka.Render2D.drawRoundedRect(xPosition + 39F, yPosition + 28.0F, 63F, 2f, 0.6F, ColorUtils.setAlpha(firstColor2, 82));
        RenderUtilka.Render2D.drawRoundedRect(xPosition + 39F, yPosition + 28.0F, 63F * health2, 1.9f, 0.8F, ColorUtils.setAlpha(firstColor2, 100));
        RenderUtilka.Render2D.drawRoundedRect(xPosition + 39F, yPosition + 28.0F, 63F * health, 2f, 0.8F, firstColor2);

        RenderUtilka.Render2D.drawRoundedRect(xPosition + 39F, yPosition + 24.5F, 63F, 2f, 0.8F, ColorUtils.rgba(72, 67, 72, 255));
        if (!OtherUtil.isConnectedToServer("funtime")) {
            RenderUtilka.Render2D.drawRoundedRect(xPosition + 39F, yPosition + 24.5F, 63F * healthplus2, 1.9f, 0.8F, ColorUtils.setAlpha(goldColor, 100));
            RenderUtilka.Render2D.drawRoundedRect(xPosition + 39F, yPosition + 24.5F, 63F * healthplus, 2f, 0.8F, ColorUtils.setAlpha(goldColor, 255));
        }

        if (target instanceof AbstractClientPlayerEntity) {
            Color headColor = new Color(255, 0, 0, (int) tintAlpha);
            RenderUtilka.Render2D.drawRoundFace(xPosition + 8.5f - 0.5f, yPosition + 8.2f - 0.5f, 22, 22, 3.5f, (float) this.targetInfoAnimation.getOutput(), (AbstractClientPlayerEntity) this.target);
            RenderUtilka.Render2D.drawRoundedRect(xPosition + 8.2f - 0.5f, yPosition + 7.9f - 0.5f, 22.6f, 22.6f, 3f, headColor.getRGB());
        } else if (target instanceof MobEntity) {
            StencilUtils.initStencilToWrite();
            RenderUtilka.Render2D.drawRoundedRect(xPosition + 8.5f - 0.5f, yPosition + 8.2f - 0.5f, 22, 22, 4, rgba(21, 21, 21, 190));
            StencilUtils.readStencilBuffer(1);
            RenderUtilka.Render2D.drawRoundedRect(xPosition + 8.5f - 0.5f, yPosition + 8.2f - 0.5f, 22, 22, 4f, new Color(23, 23, 23, 50).getRGB());
            RenderUtilka.Render2D.drawImage(new ResourceLocation("monoton/images/vopros.png"), xPosition + 8.5f - 0.5f, yPosition + 8.2f - 0.5f, 22, 22, new Color(255, 255, 255, 255).getRGB());
            StencilUtils.uninitStencilBuffer();
        }
        if (itemCount < 6) {
            Fonts.icon[11].drawString(stack, "s", xPosition + 80.25f, yPosition + 19.25f, new Color(255, 255, 255, 75).getRGB());
        }
        if (itemCount < 5) {
            Fonts.icon[11].drawString(stack, "s", xPosition + 72, yPosition + 19.25f, new Color(255, 255, 255, 75).getRGB());
        }
        if (itemCount < 4) {
            Fonts.icon[11].drawString(stack, "s", xPosition + 63.75f, yPosition + 19.25f, new Color(255, 255, 255, 75).getRGB());
        }
        if (itemCount < 3) {
            Fonts.icon[11].drawString(stack, "s", xPosition + 55.5f, yPosition + 19.25f, new Color(255, 255, 255, 75).getRGB());
        }
        if (itemCount < 2) {
            Fonts.icon[11].drawString(stack, "s", xPosition + 47.25f, yPosition + 19.25f, new Color(255, 255, 255, 75).getRGB());
        }
        if (itemCount < 1) {
            Fonts.icon[11].drawString(stack, "s", xPosition + 39, yPosition + 19.25f, new Color(255, 255, 255, 75).getRGB());
        }
        drawItemStack(xPosition + 39.5f, yPosition + 15, 8f);

        String healthValue = (int) MathUtil.round(this.health * target.getMaxHealth(), 1.0f) + "hp";

        Fonts.intl[13].drawScissorString(stack, targetName, xPosition + 39.5f, yPosition + 10.0f, ColorUtils.setAlpha(textColor, (int)alpha), (int)(35.0f));
        Fonts.intl[13].drawString(stack, (OtherUtil.isConnectedToServer("funtime") && isInvisible ? "**hp" : healthValue), xPosition + 93f - (Fonts.intl[13].getWidth(healthValue) / 2), yPosition + 10.0f, textColor);

        if (particle.get()) {
            particles.removeIf(p -> System.currentTimeMillis() - p.time > 2500L);
            for (HeadParticle p : particles) {
                p.update();
                float size = 1.0f - (float) (System.currentTimeMillis() - p.time) / 2500.0f;
                RenderUtilka.Render2D.drawRoundedCorner((float) p.pos.x, (float) p.pos.y, size * 3.5f, size * 3.5f, ((size * 3.5f) / 2) + 1, ColorUtils.setAlpha(firstColor2, (int) (255.0f * p.alpha * size)));
            }
        }

        GlStateManager.popMatrix();

        this.targetHUD.setWidth(103);
        this.targetHUD.setHeight(30);
        boolean hasPlayerHead = target.getHeldItemMainhand().getItem() == Items.PLAYER_HEAD ||
                target.getHeldItemOffhand().getItem() == Items.PLAYER_HEAD;
        if (target instanceof AbstractClientPlayerEntity) {
            if (mc != null && mc.player != null) {
                if (Manager.FUNCTION_MANAGER.autoMessage.state && !targetName.equals(mc.player.getName().getString())) {
                    String formattedTargetName = targetName != null ? formatTargetName(targetName) : "null";
                    if (Manager.FUNCTION_MANAGER.autoMessage.modes.get("Кидай шар") && hasPlayerHead) {
                        if (timerUtil.hasTimeElapsed(21300) && target != null && target.getHealth() <= Manager.FUNCTION_MANAGER.autoMessage.health.getValue().intValue()) {
                            String[] messages = {
                                    formattedTargetName + (" кидай шар/элитры может быть я тебя отпущу"),
                                    formattedTargetName + (" колобок/элитры дропай и отпущу быстренько"),
                                    formattedTargetName + (" шар выкидывай пока маме не рассказал что ты не спишь"),
                                    formattedTargetName + (" шар скинул пока твоего кота не чпокнул")
                            };

                            String randomMessage = messages[new Random().nextInt(messages.length)];
                            mc.player.sendChatMessage(randomMessage);
                            timerUtil.reset();
                        }
                    }

                    if (Manager.FUNCTION_MANAGER.autoMessage.modes.get("Убил езку")) {
                        if (timerUtil.hasTimeElapsed(550) && target != null && (target.getHealth() <= 0.0f)) {
                            String message = Manager.FUNCTION_MANAGER.autoMessage.killmsg.text.replace("ник", formattedTargetName);
                            mc.player.sendChatMessage("!" + message);
                            timerUtil.reset();
                        }
                    }
                }
            }
        }
    }

    private static String formatTargetName(String name) {
        if (name == null || name.isEmpty()) return "езка";
        long upperCaseCount = name.chars().filter(Character::isUpperCase).count();
        if ((double) upperCaseCount / name.length() > 0.4) {
            return name.toLowerCase();
        }
        return name;
    }

    private void spawnHeadParticles() {
        float headX = targetHUD.getX() + 12.5f;
        float headY = targetHUD.getY() + 13f;
        for (int i = 0; i < 9; ++i) {
            particles.add(new HeadParticle(new Vector3d(headX, headY, 0.0)));
        }
    }

    public static class HeadParticle {
        private Vector3d pos;
        private final Vector3d end;
        private final long time;
        private float alpha;

        public HeadParticle(Vector3d pos) {
            this.pos = pos;
            this.end = pos.add(
                    -ThreadLocalRandom.current().nextFloat(-80.0F, 80.0F),
                    -ThreadLocalRandom.current().nextFloat(-80.0F, 80.0F),
                    -ThreadLocalRandom.current().nextFloat(-80.0F, 80.0F)
            );
            this.time = System.currentTimeMillis();
            this.alpha = 0.0f;
        }

        public void update() {
            this.alpha = MathHelper.lerp(this.alpha, 1.0F, 0.1F);
            this.pos = AnimationMath.fast(this.pos, this.end, 0.5F);
        }
    }

    private int rgba(int r, int g, int b, int a) {
        return new Color(r, g, b, a).getRGB();
    }

    private void updatePlayerHealth() {
        String myPlayerName = mc.player.getName().getString();

        if (target.getName().getString().equals(myPlayerName)) {
            return;
        }

        if (Manager.FUNCTION_MANAGER.scoreboardHealth.state) {
            for (Map.Entry<ScoreObjective, Score> entry : IMinecraft.mc.world.getScoreboard().getObjectivesForEntity(target.getName().getString()).entrySet()) {
                Score score = entry.getValue();
                int newHealth = score.getScorePoints();

                if (newHealth >= 1) {
                    target.setHealth(newHealth);
                } else {
                    target.setHealth(1);
                }
            }
        }
    }

    private void drawItemStack(float x, float y, float offset) {
        List<ItemStack> stackList = new ArrayList<>(Arrays.asList(target.getHeldItemMainhand(), target.getHeldItemOffhand()));
        List<ItemStack> armorItems = (List<ItemStack>) target.getArmorInventoryList();
        stackList.add(armorItems.get(3));
        stackList.add(armorItems.get(2));
        stackList.add(armorItems.get(1));
        stackList.add(armorItems.get(0));

        List<ItemStack> nonEmptyStacks = stackList.stream()
                .filter(stack -> !stack.isEmpty())
                .collect(Collectors.toList());

        itemCount = nonEmptyStacks.size();

        final AtomicReference<Float> posX = new AtomicReference<>(x);
        nonEmptyStacks.forEach(stack -> HudUtil.drawItemStack(stack,
                posX.getAndAccumulate(offset, Float::sum),
                y,
                true,
                true, 0.5f));
    }

    private LivingEntity getTarget() {
        LivingEntity target = null;

        if (Manager.FUNCTION_MANAGER.auraFunction.getTarget() instanceof LivingEntity) {
            target = (LivingEntity) Manager.FUNCTION_MANAGER.auraFunction.getTarget();
            targetHudAnimation.setDirection(Direction.FORWARDS);
        } else if (IMinecraft.mc.currentScreen instanceof ChatScreen) {
            target = IMinecraft.mc.player;
            targetHudAnimation.setDirection(Direction.FORWARDS);
        } else {
            targetHudAnimation.setDirection(Direction.BACKWARDS);
        }
        return target;
    }
}
