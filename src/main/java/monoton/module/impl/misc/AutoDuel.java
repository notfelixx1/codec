package monoton.module.impl.misc;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import monoton.module.settings.imp.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SChatPacket;
import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.control.events.player.Listener;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.Setting;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.utils.other.Counter;
import monoton.control.Manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.HashMap;

@Annotation(
        name = "AutoDuel",
        type = TypeList.Misc,
        desc = "Автоматически вызывает игрока на дуэль"
)
public class AutoDuel extends Module {
    private static final Pattern pattern = Pattern.compile("^\\w{3,16}$");
    private final MultiBoxSetting mode = new MultiBoxSetting("Кит",
            new BooleanOption("Щит", false),
            new BooleanOption("Шипы 3", false),
            new BooleanOption("Лук", false),
            new BooleanOption("Тотемы", false),
            new BooleanOption("Исцеление", false),
            new BooleanOption("Шары", true),
            new BooleanOption("Классик", false),
            new BooleanOption("Читерский рай", false),
            new BooleanOption("Незерка", false)
    );
    public final BooleanOption money = new BooleanOption("На деньги", false);
    public SliderSetting moneys = new SliderSetting("Сумма в тысячах", 50, 1, 500, 1).setVisible(() -> money.get());

    private double lastPosX;
    private double lastPosY;
    private double lastPosZ;
    private final List<String> sent = Lists.newArrayList();
    private final Counter counter = Counter.create();
    private final Counter counter2 = Counter.create();
    private final Counter counterChoice = Counter.create();
    private final Counter counterTo = Counter.create();
    private final HashMap<String, Long> duelTimestamps = new HashMap<>();

    private final Listener<EventUpdate> onUpdate = (event) -> {
        List<String> players = this.getOnlinePlayers();
        double var10000 = Math.pow(this.lastPosX - Minecraft.player.getPosX(), 2.0);
        var10000 += Math.pow(this.lastPosY - Minecraft.player.getPosY(), 2.0);
        double distance = Math.sqrt(var10000 + Math.pow(this.lastPosZ - Minecraft.player.getPosZ(), 2.0));
        if (distance > 500.0) {
            this.toggle();
        }

        this.lastPosX = Minecraft.player.getPosX();
        this.lastPosY = Minecraft.player.getPosY();
        this.lastPosZ = Minecraft.player.getPosZ();
        if (this.counter2.hasReached(80L * (long) players.size())) {
            this.sent.clear();
            this.counter2.reset();
        }

        for (String player : players) {
            if (!this.sent.contains(player) && !player.equals(mc.session.getProfile().getName()) && this.counter.hasReached(600L)) {
                if (Manager.FRIEND_MANAGER.isFriend(player)) {
                    continue;
                }

                if (duelTimestamps.containsKey(player)) {
                    long lastDuelTime = duelTimestamps.get(player);
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastDuelTime < 31000) {
                        continue;
                    }
                }

                Minecraft.player.sendChatMessage("/duel " + player + (money.get() ? " " + (moneys.getValue().intValue() * 1000) : ""));
                this.sent.add(player);
                duelTimestamps.put(player, System.currentTimeMillis());
                this.counter.reset();
            }
        }

        Container patt3185$temp = Minecraft.player.openContainer;
        if (patt3185$temp instanceof ChestContainer chest) {
            if (mc.currentScreen.getTitle().getString().contains("Выбор набора (1/1)")) {
                for (int i = 0; i < chest.getLowerChestInventory().getSizeInventory(); ++i) {
                    List<Integer> slotsID = new ArrayList<>();
                    int index = 0;

                    for (BooleanOption value : this.mode.getValues()) {
                        if (!value.getValue()) {
                            ++index;
                        } else {
                            slotsID.add(index);
                            ++index;
                        }
                    }

                    Collections.shuffle(slotsID);
                    int slotID = slotsID.get(0);
                    if (this.counterChoice.hasReached(80L)) {
                        mc.playerController.windowClick(chest.windowId, slotID, 0, ClickType.QUICK_MOVE, Minecraft.player);
                        this.counterChoice.reset();
                    }
                }
            } else if (mc.currentScreen.getTitle().getString().contains("Настройка поединка") && this.counterTo.hasReached(80L)) {
                mc.playerController.windowClick(chest.windowId, 0, 0, ClickType.QUICK_MOVE, Minecraft.player);
                this.counterTo.reset();
            }
        }
    };

    private final Listener<EventPacket> onPacket = (event) -> {
        if (event.isReceivePacket()) {
            IPacket<?> packet = event.getPacket();
            if (packet instanceof SChatPacket) {
                SChatPacket chat = (SChatPacket) packet;
                String text = chat.getChatComponent().getString().toLowerCase();
                if (text.contains("начало") && text.contains("через") && text.contains("секунд!") || text.equals("дуэли » во время поединка запрещено использовать команды")) {
                    this.toggle();
                }
            }
        }
    };

    public AutoDuel() {
        this.addSettings(this.mode, money, moneys);
    }

    private List<String> getOnlinePlayers() {
        return Minecraft.player.connection.getPlayerInfoMap().stream()
                .map(NetworkPlayerInfo::getGameProfile)
                .map(GameProfile::getName)
                .filter((profileName) -> pattern.matcher(profileName).matches())
                .collect(Collectors.toList());
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            this.onUpdate.onEvent((EventUpdate) event);
        } else if (event instanceof EventPacket) {
            this.onPacket.onEvent((EventPacket) event);
        }

        return false;
    }
}