package monoton.module.impl.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.game.EventKey;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.impl.player.GuiMove;
import monoton.module.settings.imp.BindSetting;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.InfoSetting;
import monoton.module.settings.imp.ModeSetting;
import monoton.utils.font.Fonts;
import monoton.utils.font.styled.StyledFont;
import monoton.utils.other.OtherUtil;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.ProjectionUtils;
import monoton.utils.render.RenderUtilka;
import monoton.utils.world.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.AirItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.client.CResourcePackStatusPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.network.play.server.SSendResourcePackPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static monoton.ui.clickgui.Panel.getColorByName;
import static monoton.ui.clickgui.Panel.selectedColor;

@Annotation(name = "ServerHelper", type = TypeList.Misc, desc = "Автоматизирует действие")
public class ServerHelper extends Module {
    public ModeSetting mode = new ModeSetting("Выбор режимов", "FunTime", "FunTime", "HolyWorld", "ReallyWorld");
    public InfoSetting binding = new InfoSetting("Бинды", () -> {
    });
    private BindSetting trap = new BindSetting("Трапа", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting diz = new BindSetting("Дезорент", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting plast = new BindSetting("Пласт", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting yaw = new BindSetting("Явная пыль", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting aura = new BindSetting("Божья аура", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting death = new BindSetting("Огнен смерч", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting snowball = new BindSetting("Снежок заморозка", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting stick = new BindSetting("Взрыв штучка", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting gul = new BindSetting("Прощальный гул", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting stun = new BindSetting("Стан", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting trapka = new BindSetting("Взрыв трап", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting snow = new BindSetting("Ком снега", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting trapk = new BindSetting("Трaпкa", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting antipolet = new BindSetting("Анти Полет", 0).setVisible(() -> mode.is("ReallyWorld"));
    private BindSetting horus = new BindSetting("Хорус", 0);
    private BindSetting charGapple = new BindSetting("Чарка", 0);
    private BindSetting potion = new BindSetting("Все взр зелье", 0);
    private BindSetting dropKey = new BindSetting("Выброс корд", 0);
    public BooleanOption blockmsg = new BooleanOption("Блок запрет слова", true).setVisible(() -> mode.is("ReallyWorld"));
    public BooleanOption coloserp = new BooleanOption("Не скачивать рп", true).setVisible(() -> mode.is("ReallyWorld"));
    public BooleanOption sell = new BooleanOption("Выделять цены", true).setVisible(() -> mode.is("FunTime"));
    public BooleanOption time_indicator = new BooleanOption("Время предметов", true).setVisible(() -> mode.is("FunTime"));
    InventoryUtils.Hands handUtil = new InventoryUtils.Hands();
    long delay;
    private final String[] forbiddenWords = {
            "экспа", "экспенсив", "экспой", "нуриком", "целкой", "нурлан", "newcode", "ньюкод",
            "нурсултан", "целестиал", "целка", "нурик", "атернос", "aternos", "celka", "nurik",
            "expensive", "celestial", "nursultan", "фанпей", "funpay", "fluger", "акриен", "akrien",
            "фантайм", "funtime", "безмамный", "rich", "рич", "wild", "вилд", "excellent",
            "экселлент", "matix", "impact", "матикс", "импакт", "wurst", "бесплатно донат"};
    int x = -1, z = -1;
    private int prevChorusSlot = -1;
    private int originalChorusInventorySlot = -1;
    private boolean isChorusSelected = false;
    private int prevGappleSlot = -1;
    private int originalGappleInventorySlot = -1;
    private boolean isGappleSelected = false;
    public static CopyOnWriteArrayList<Usables> usables = new CopyOnWriteArrayList<>();
    private final Map<Integer, List<String>> playerEffects = new HashMap<>();
    private final Timer effectTimer = new Timer();
    public static int oldItem;
    public static Item lastItem;
    public static boolean invSwap;
    public static int invSlot;

    public ServerHelper() {
        addSettings(mode, blockmsg, coloserp, sell, time_indicator, binding, trap, diz, plast, yaw, aura, death, snowball, stick, gul, stun, snow, trapka, trapk, charGapple, horus, potion, antipolet, dropKey);
    }

    @Override
    public boolean onEvent(Event event) {
        if (mode.is("HolyWorld")) {
            if (event instanceof EventKey e) {
                if (e.key == trapka.getKey()) {
                    holyworldUse(Items.PRISMARINE_SHARD, "Взрывная трапка");
                }

                if (e.key == gul.getKey()) {
                    holyworldUse(Items.FIREWORK_STAR, "Прощальный гул");
                }

                if (e.key == stun.getKey()) {
                    holyworldUse(Items.NETHER_STAR, "Стан");
                }

                if (e.key == snow.getKey()) {
                    holyworldUse(Items.SNOWBALL, "Ком снега");
                }

                if (e.key == stick.getKey()) {
                    holyworldUse(Items.FIRE_CHARGE, "Взрывная штучка");
                }

                if (e.key == trapk.getKey()) {
                    holyworldUse(Items.POPPED_CHORUS_FRUIT, "Трапка");
                }
            }

            if (event instanceof EventUpdate && (oldItem != -1 || invSwap)) {
                holyworldFinalUse();
            }
        }

        if (event instanceof EventKey e) {
            handleKeyEvent(e);
        } else if (event instanceof EventPacket e) {
            handlePacketEvent(e);
        } else if (event instanceof EventRender e && mode.is("FunTime") && time_indicator.get()) {
            handleRenderEvent(e);
        }
        return false;
    }

    private void handleKeyEvent(EventKey e) {
        if (mode.is("FunTime")) {
            if (e.key == snowball.getKey()) {
                if (OtherUtil.isConnectedToServer("funtime") || OtherUtil.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("снежок заморозка", true);
                    int invSlot = getItemForName("снежок заморозка", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Снежок заморозка" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.SNOWBALL != null && !mc.player.getCooldownTracker().hasCooldown(Items.SNOWBALL)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            OtherUtil.sendMessage(TextFormatting.WHITE + "Снежок заморозка был" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.SNOWBALL) == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Снежок заморозка" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.SNOWBALL, false);
                    }
                }
            }
            if (e.key == diz.getKey()) {
                if (OtherUtil.isConnectedToServer("funtime") || OtherUtil.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("дезориентация", true);
                    int invSlot = getItemForName("дезориентация", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Дезориентация" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.ENDER_EYE != null && !mc.player.getCooldownTracker().hasCooldown(Items.ENDER_EYE)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            OtherUtil.sendMessage(TextFormatting.WHITE + "Дезориентация была" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.ENDER_EYE) == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Дезориентация" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.ENDER_EYE, false);
                    }
                }
            }
            if (e.key == trap.getKey()) {
                if (OtherUtil.isConnectedToServer("funtime") || OtherUtil.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("трапка", true);
                    int invSlot = getItemForName("трапка", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Трапка" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.NETHERITE_SCRAP != null && !mc.player.getCooldownTracker().hasCooldown(Items.NETHERITE_SCRAP)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            OtherUtil.sendMessage(TextFormatting.WHITE + "Трапка была" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.NETHERITE_SCRAP) == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Трапка" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.NETHERITE_SCRAP, false);
                    }
                }
            }
            if (e.key == plast.getKey()) {
                if (OtherUtil.isConnectedToServer("funtime") || OtherUtil.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("пласт", true);
                    int invSlot = getItemForName("пласт", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Пласт" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найден");
                    }

                    if (Items.DRIED_KELP != null && !mc.player.getCooldownTracker().hasCooldown(Items.DRIED_KELP)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            OtherUtil.sendMessage(TextFormatting.WHITE + "Пласт был" + TextFormatting.RED + " использован");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.DRIED_KELP) == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Пласт" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найден");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.DRIED_KELP, false);
                    }
                }
            }
            if (e.key == yaw.getKey()) {
                if (OtherUtil.isConnectedToServer("funtime") || OtherUtil.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("явная", true);
                    int invSlot = getItemForName("явная", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Явная пыль" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.SUGAR != null && !mc.player.getCooldownTracker().hasCooldown(Items.SUGAR)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            OtherUtil.sendMessage(TextFormatting.WHITE + "Явная пыль была" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.SUGAR) == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Явная пыль" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.SUGAR, false);
                    }
                }
            }
            if (e.key == aura.getKey()) {
                if (OtherUtil.isConnectedToServer("funtime") || OtherUtil.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("божья", true);
                    int invSlot = getItemForName("божья", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Божья аура" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.PHANTOM_MEMBRANE != null && !mc.player.getCooldownTracker().hasCooldown(Items.PHANTOM_MEMBRANE)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            OtherUtil.sendMessage(TextFormatting.WHITE + "Божья аура была" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.PHANTOM_MEMBRANE) == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Божья аура" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.PHANTOM_MEMBRANE, false);
                    }
                }
            }
            if (e.key == death.getKey()) {
                if (OtherUtil.isConnectedToServer("funtime") || OtherUtil.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("огненный", true);
                    int invSlot = getItemForName("огненный", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Огненный смерч" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найден");
                    }

                    if (Items.FIRE_CHARGE != null && !mc.player.getCooldownTracker().hasCooldown(Items.FIRE_CHARGE)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            OtherUtil.sendMessage(TextFormatting.WHITE + "Огненный смерч был" + TextFormatting.RED + " использован");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.FIRE_CHARGE) == -1) {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Огненный смерч" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найден");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.FIRE_CHARGE, false);
                    }
                }
            }
        }
        if (mode.is("ReallyWorld")) {
            if (e.key == antipolet.getKey()) {
                antipolet();
            }
        }
        if (e.key == potion.getKey()) {
            potion();
        }
        if (e.key == horus.getKey()) {
            handleChorusSwitch();
        }
        if (e.key == charGapple.getKey()) {
            handleGappleSwitch();
        }
        if (e.key == dropKey.getKey()) {
            mc.player.sendChatMessage("!корды " + (int) mc.player.getPosX() + " " + (int) mc.player.getPosZ());
        }
    }

    private void handlePacketEvent(EventPacket e) {
        if (mode.is("ReallyWorld") && coloserp.get()) {
            if (e.getPacket() instanceof SSendResourcePackPacket) {
                Minecraft.getInstance().getConnection().sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.ACCEPTED));
                Minecraft.getInstance().getConnection().sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.SUCCESSFULLY_LOADED));
                e.setCancel(true);
            }
        }

        if (e.getPacket() instanceof CChatMessagePacket) {
            CChatMessagePacket chatPacket = (CChatMessagePacket) e.getPacket();
            String message = chatPacket.getMessage().toLowerCase();

            if (mode.is("ReallyWorld") && blockmsg.get()) {
                for (String word : forbiddenWords) {
                    if (message.matches("(?i)\\b" + Pattern.quote(word) + "\\b")) {
                        OtherUtil.sendMessage("" + TextFormatting.GRAY + "Ваше сообщение содержит " + TextFormatting.RED + "запрещенное" + TextFormatting.GRAY + " слово, сообщение не было отправлено");
                        e.setCancel(true);
                        return;
                    }
                }
            }
        }

        if (mode.is("FunTime") && time_indicator.get()) {
            if (e.getPacket() instanceof SPlaySoundEffectPacket wrapper) {
                float x = (float) wrapper.getX();
                float y = (float) wrapper.getY();
                float z = (float) wrapper.getZ();
                if (wrapper.getSound().getName().getPath().equals("block.piston.extend")) {
                    usables.add(new Usables(System.currentTimeMillis(), 15000, x, y, z, "Трапка"));
                } else if (wrapper.getSound().getName().getPath().equals("block.anvil.place")) {
                    usables.add(new Usables(System.currentTimeMillis(), 20000, x, y, z, "Пласт"));
                }
            }
        }
    }

    private void handleRenderEvent(EventRender e) {
        if (e.isRender2D()) {
            for (Usables usable : usables) {
                if (System.currentTimeMillis() - usable.time > (long) usable.time2) {
                    usables.remove(usable);
                    continue;
                }

                float x1 = usable.spawnX;
                float y1 = usable.spawnY + 1.5F;
                float z1 = usable.spawnZ;
                Item item = null;
                if (usable.text.equals("Трапка")) {
                    x1 += 0.5F;
                    z1 += 0.5F;
                    item = Items.NETHERITE_SCRAP;
                } else if (usable.text.equals("Пласт")) {
                    item = Items.DRIED_KELP;
                }

                Vector2f vector2d = ProjectionUtils.project2f((double) x1, (double) y1, (double) z1);
                if (vector2d == null) continue;

                float time1 = (float) ((long) usable.time2 - (System.currentTimeMillis() - usable.time));
                float totalTime = (float) usable.time2;
                float progress = Math.max(0, Math.min(1, time1 / totalTime)); // 0.0 — 1.0

                int time3 = (int) (time1 / 1000.0F);
                String timeText = String.format("%.1fс", time1 / 1000.0F);

                StyledFont font = Fonts.intl[12];
                String nameText = usable.text;
                String fullText = nameText + " " + timeText;

                float width = font.getWidth(fullText) + 8.0F;
                float height = 12.0F;
                float x = vector2d.x - width / 2.0F;
                float y = vector2d.y;

                int textColor = getColorByName("textColor");
                int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
                int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);
                int firstColor2 = selectedColor;

                RenderUtilka.Render2D.drawBlurredRoundedRectangle(x - 7.75f, y, 16, 16, new Vector4f(4f, 4f, 0f, 0f), fonduoColor, 1);
                RenderUtilka.Render2D.drawBlurredRoundedRectangle(x + 8f, y, width, 16, new Vector4f(0f, 0f, 4f, 4f), fonColor, 1);
                RenderUtilka.Render2D.drawBlurredRoundedRectangle((x + ((width + 16 - 7.75f) / 2)) - 2.5f, y + 14, 5, 5, new Vector4f(0f, 3f, 0f, 3f), fonColor, 1);

                float progressBarWidth = width - 6.5f;
                float progressBarX = x + 10f;
                float progressBarY = y + 11f;
                RenderUtilka.Render2D.drawRoundedCorner(progressBarX, progressBarY, progressBarWidth, 1.2f, 1.5f, ColorUtils.setAlpha(firstColor2, 82));

                float filledWidth = progressBarWidth * progress;
                if (filledWidth > 0) {
                    RenderUtilka.Render2D.drawRoundedCorner(progressBarX, progressBarY, filledWidth, 1.2f, 1.5f, firstColor2);
                }

                GL11.glPushMatrix();

                font.drawString(e.matrixStack, nameText,
                        (double) (x + 9.5f),
                        (double) (y + height / 2.0F - 1.6F),
                        textColor);

                float timeX = x + 12 + font.getWidth(nameText + " ");
                font.drawString(e.matrixStack, timeText,
                        (double) timeX,
                        (double) (y + height / 2.0F - 1.6F),
                        textColor);

                if (item != null) {
                    float scale = 0.6667f;
                    float itemPosX = vector2d.x - width / 2.0F - 5f;
                    float itemPosY = vector2d.y + 2.5f;

                    RenderSystem.pushMatrix();
                    RenderSystem.scaled(scale, scale, 1.0);
                    RenderSystem.translated(itemPosX / scale, itemPosY / scale, 0.0);
                    mc.getItemRenderer().renderItemAndEffectIntoGUI(
                            new ItemStack(item),
                            0,
                            0
                    );
                    RenderSystem.popMatrix();
                }

                GL11.glPopMatrix();
            }
        }
    }

    @Compile
    private void potion() {
        if (OtherUtil.isConnectedToServer("funtime") || OtherUtil.isConnectedToServer("spooky")) {
            handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = findItem(Items.SPLASH_POTION, true);
            int invSlot = findItem(Items.SPLASH_POTION, false);

            if (invSlot == -1 && hbSlot == -1) {
                OtherUtil.sendMessage(TextFormatting.WHITE + "Взрывное зелье не было найдено");
            }

            if (Items.SPLASH_POTION != null) {
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot != -1 && slot > 8) {
                    mc.playerController.pickItem(slot);
                    OtherUtil.sendMessage(TextFormatting.WHITE + "Взрывное зелье было" + TextFormatting.RED + " использовано");
                }
            }
        } else {
            handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack.getItem() == Items.SPLASH_POTION) {
                    InventoryUtils.inventorySwapClick(Items.SPLASH_POTION, false);
                    return;
                }
            }
            OtherUtil.sendMessage(TextFormatting.WHITE + "Взрывное зелье не было найдено");
        }
    }

    private int findItem(Item item, boolean hotbarOnly) {
        for (int i = hotbarOnly ? 0 : 9; i < (hotbarOnly ? 9 : 36); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private void antipolet() {
        if (InventoryUtils.getItemSlot(Items.FIREWORK_STAR) == -1) {
            OtherUtil.sendMessage(TextFormatting.RED + "У вас отсутствует анти полет!");
        } else {
            InventoryUtils.antipolet(Items.FIREWORK_STAR);
        }
    }

    @Compile
    public static void holyworldUse(Item item, String nameItem) {
        if (oldItem == -1) {
            boolean hotbar = false;

            for (int i = 0; i < 9; ++i) {
                if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                    oldItem = mc.player.inventory.currentItem;
                    lastItem = mc.player.inventory.getStackInSlot(i).getItem();
                    mc.player.inventory.currentItem = i;
                    hotbar = true;
                    break;
                }
            }

            if (!hotbar) {
                for (int i = 0; i < 36; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                        GuiMove.stopMovementTemporarily(2);
                        mc.playerController.windowClick(0, i, mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
                        invSwap = true;
                        invSlot = i;
                        break;
                    }
                }
            }
        }

    }

    public static void holyworldFinalUse() {
        if (invSwap && invSlot != -1) {
            if (mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem).getItem() == lastItem) {
                GuiMove.stopMovementTemporarily(2);
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                mc.playerController.windowClick(0, invSlot, mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
                invSwap = false;
                invSlot = -1;
            }
        } else {
            if (mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem).getItem() == lastItem) {
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                mc.player.swingArm(Hand.MAIN_HAND);
            }

            mc.player.inventory.currentItem = oldItem;
            oldItem = -1;
        }

    }


    private int getItemForName(String name, boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (itemStack.getItem() instanceof AirItem) {
                continue;
            }
            String displayName = TextFormatting.getTextWithoutFormattingCodes(itemStack.getDisplayName().getString());
            if (displayName != null && displayName.toLowerCase().contains(name)) {
                return i;
            }
        }
        return -1;
    }

    private int findAndTrowItem(int hbSlot, int invSlot) {
        if (hbSlot != -1) {
            this.handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return hbSlot;
        }
        if (invSlot != -1) {
            handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.playerController.pickItem(invSlot);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return invSlot;
        }
        return -1;
    }

    private void handleChorusSwitch() {
        if (isChorusSelected) {
            ItemStack currentStack = mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem);
            if (currentStack.getItem() == Items.CHORUS_FRUIT) {
                if (originalChorusInventorySlot != -1) {
                    InventoryUtils.moveItem(mc.player.inventory.currentItem, originalChorusInventorySlot);
                } else if (prevChorusSlot != -1) {
                    mc.player.inventory.currentItem = prevChorusSlot;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(prevChorusSlot));
                }
                isChorusSelected = false;
                prevChorusSlot = -1;
                originalChorusInventorySlot = -1;
                return;
            } else {
                isChorusSelected = false;
                prevChorusSlot = -1;
                originalChorusInventorySlot = -1;
            }
        }

        int chorusSlot = findItem(Items.CHORUS_FRUIT, true);
        int currentHotbarSlot = mc.player.inventory.currentItem;

        if (chorusSlot != -1) {
            if (chorusSlot != currentHotbarSlot) {
                prevChorusSlot = currentHotbarSlot;
                mc.player.inventory.currentItem = chorusSlot;
                mc.player.connection.sendPacket(new CHeldItemChangePacket(chorusSlot));
            }
            isChorusSelected = true;
            originalChorusInventorySlot = -1;
        } else {
            chorusSlot = findItem(Items.CHORUS_FRUIT, false);
            if (chorusSlot != -1) {
                int targetHotbarSlot = currentHotbarSlot + 36;
                InventoryUtils.moveItem(chorusSlot, targetHotbarSlot);
                mc.player.connection.sendPacket(new CHeldItemChangePacket(currentHotbarSlot));
                isChorusSelected = true;
                originalChorusInventorySlot = chorusSlot;
                prevChorusSlot = -1;
            } else {
                OtherUtil.sendMessage(TextFormatting.RED + "Хорус не найден!");
                isChorusSelected = false;
                prevChorusSlot = -1;
                originalChorusInventorySlot = -1;
            }
        }
    }

    private void handleGappleSwitch() {
        if (isGappleSelected) {
            ItemStack currentStack = mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem);
            if (currentStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                if (originalGappleInventorySlot != -1) {
                    InventoryUtils.moveItem(mc.player.inventory.currentItem, originalGappleInventorySlot);
                } else if (prevGappleSlot != -1) {
                    mc.player.inventory.currentItem = prevGappleSlot;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(prevGappleSlot));
                }
                isGappleSelected = false;
                prevGappleSlot = -1;
                originalGappleInventorySlot = -1;
                return;
            } else {
                isGappleSelected = false;
                prevGappleSlot = -1;
                originalGappleInventorySlot = -1;
            }
        }

        int gappleSlot = findItem(Items.ENCHANTED_GOLDEN_APPLE, true);
        int currentHotbarSlot = mc.player.inventory.currentItem;

        if (gappleSlot != -1) {
            if (gappleSlot != currentHotbarSlot) {
                prevGappleSlot = currentHotbarSlot;
                mc.player.inventory.currentItem = gappleSlot;
                mc.player.connection.sendPacket(new CHeldItemChangePacket(gappleSlot));
            }
            isGappleSelected = true;
            originalGappleInventorySlot = -1;
        } else {
            gappleSlot = findItem(Items.ENCHANTED_GOLDEN_APPLE, false);
            if (gappleSlot != -1) {
                int targetHotbarSlot = currentHotbarSlot + 36;
                InventoryUtils.moveItem(gappleSlot, targetHotbarSlot);
                mc.player.connection.sendPacket(new CHeldItemChangePacket(currentHotbarSlot));
                isGappleSelected = true;
                originalGappleInventorySlot = gappleSlot;
                prevGappleSlot = -1;
            } else {
                OtherUtil.sendMessage(TextFormatting.RED + "Чарка не найдена!");
                isGappleSelected = false;
                prevGappleSlot = -1;
                originalGappleInventorySlot = -1;
            }
        }
    }

    public static class Usables {
        public long time;
        public int time2;
        public float spawnX;
        public float spawnY;
        public float spawnZ;
        public String text;

        public Usables(long time, int time2, float spawnX, float spawnY, float spawnZ, String text) {
            this.time = time;
            this.time2 = time2;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.spawnZ = spawnZ;
            this.text = text;
        }
    }
}