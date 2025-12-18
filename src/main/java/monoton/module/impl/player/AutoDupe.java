package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.utils.misc.TimerUtil;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Annotation(name = "AutoDupe", type = TypeList.Player, desc = "Автоматически фармит предметы на BravoHvH")
public class AutoDupe extends Module {
    private static final class Kit {
        final String name;
        final long cooldown;

        Kit(String name, long cooldown) {
            this.name = name;
            this.cooldown = cooldown;
        }
    }

    private static final List<Kit> KIT_LIST = List.of(
            new Kit("free", 46_000),
            new Kit("hero", 61_000),
            new Kit("prince", 46_000),
            new Kit("killer", 81_000),
            new Kit("krushitel", 111_000),
            new Kit("kraken", 81_000),
            new Kit("alpha", 81_000),
            new Kit("rabbit", 61_000),
            new Kit("sponsor", 81_000)
    );

    private final MultiBoxSetting kits = new MultiBoxSetting("Киты",
            new BooleanOption("Кит free", true),
            new BooleanOption("Кит hero", false),
            new BooleanOption("Кит prince", false),
            new BooleanOption("Кит killer", false),
            new BooleanOption("Кит krushitel", false),
            new BooleanOption("Кит kraken", false),
            new BooleanOption("Кит alpha", false),
            new BooleanOption("Кит rabbit", false),
            new BooleanOption("Кит sponsor", false));

    private final MultiBoxSetting item = new MultiBoxSetting("Предметы",
            new BooleanOption("Чарки", true),
            new BooleanOption("Зелье Викинга", true),
            new BooleanOption("Восстановить эффекты", true),
            new BooleanOption("Эндер жемчуг", true));

    private final TimerUtil kitTimer = new TimerUtil();
    private final TimerUtil cleanTimer = new TimerUtil();
    private final TimerUtil sequentialTimer = new TimerUtil();
    private boolean storeKit = false;
    private String lastKitRequested = null;
    private final Map<String, Long> kitLastRequestTimes = new HashMap<>();
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("Кит (\\w+) на кулдауне \\((\\d+) сек\\)");

    public AutoDupe() {
        addSettings(kits, item);
    }

    @Compile
    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket && ((EventPacket) event).getPacket() instanceof SChatPacket) {
            SChatPacket packet = (SChatPacket) ((EventPacket) event).getPacket();
            if (packet.getType() == ChatType.CHAT) {
                ITextComponent message = packet.getChatComponent();
                String messageText = message.getString();
                Matcher matcher = COOLDOWN_PATTERN.matcher(messageText);
                if (matcher.matches()) {
                    String kitName = matcher.group(1);
                    long cooldownSeconds = Long.parseLong(matcher.group(2));
                    long cooldownMillis = cooldownSeconds * 1000;
                    long currentTime = System.currentTimeMillis();
                    kitLastRequestTimes.put(kitName, currentTime - (KIT_LIST.stream()
                            .filter(k -> k.name.equals(kitName))
                            .findFirst()
                            .map(k -> k.cooldown)
                            .orElse(61_000L) - cooldownMillis));
                    lastKitRequested = null;
                    sequentialTimer.reset();
                }
            }
        }

        if (!(event instanceof EventUpdate) || mc.player == null || mc.world == null) {
            return false;
        }

        try {
            if (!invEmpty()) {
                if (haveTargetItems() && storeKit && cleanTimer.hasTimeElapsed(750)) {
                    if (mc.currentScreen instanceof ContainerScreen) {
                        int startIndex = mc.player.openContainer.getInventory().size() - 36;
                        int endIndex = mc.player.openContainer.getInventory().size() - 1;

                        boolean movedItem = false;
                        for (int i = startIndex; i <= endIndex; i++) {
                            ItemStack itemStack = mc.player.openContainer.getSlot(i).getStack();
                            if (isTargetItem(itemStack)) {
                                mc.playerController.windowClick(mc.player.openContainer.windowId, i, 0, ClickType.QUICK_MOVE, mc.player);
                                movedItem = true;
                            }
                        }

                        if (movedItem) {
                            storeKit = false;
                            mc.player.closeScreen();
                        }
                        cleanTimer.reset();
                    } else {
                        mc.player.sendChatMessage("/ec");
                        cleanTimer.reset();
                    }
                } else if (!haveTargetItems() && cleanTimer.hasTimeElapsed(750)) {
                    mc.player.sendChatMessage("/clear -confirmed");
                    cleanTimer.reset();
                }
                return false;
            }

            if (!sequentialTimer.hasTimeElapsed(3200) && lastKitRequested != null) {
                return false;
            }

            String kitToRequest = getNextKit();
            if (kitToRequest == null) {
                return false;
            }

            mc.player.sendChatMessage("/kit " + kitToRequest);
            kitLastRequestTimes.put(kitToRequest, System.currentTimeMillis());
            lastKitRequested = kitToRequest;
            storeKit = true;
            sequentialTimer.reset();
            kitTimer.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Compile
    private String getNextKit() {
        long currentTime = System.currentTimeMillis();
        List<String> enabledKits = new ArrayList<>();

        for (Kit kit : KIT_LIST) {
            if (kits.get("Кит " + kit.name)) {
                enabledKits.add(kit.name);
            }
        }

        if (enabledKits.isEmpty()) {
            return null;
        }

        for (String kitName : enabledKits) {
            long lastRequestTime = kitLastRequestTimes.getOrDefault(kitName, 0L);
            long cooldown = KIT_LIST.stream()
                    .filter(k -> k.name.equals(kitName))
                    .findFirst()
                    .map(k -> k.cooldown)
                    .orElse(61_000L);

            if (currentTime - lastRequestTime >= cooldown) {
                return kitName;
            }
        }

        return null;
    }

    @Compile
    private boolean haveTargetItems() {
        try {
            for (int i = 0; i < mc.player.container.getInventory().size(); i++) {
                ItemStack stack = mc.player.container.getInventory().get(i);
                if (!stack.isEmpty()) {
                    boolean isTarget = isTargetItem(stack);
                    if (isTarget) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Compile
    private boolean isTargetItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (item.get("Чарки") && stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            return true;
        }

        if (item.get("Эндер жемчуг") && stack.getItem() == Items.ENDER_PEARL) {
            return true;
        }

        if (stack.getItem() == Items.POTION) {
            String displayName = stack.getDisplayName().getString();
            if (item.get("Зелье Викинга") && displayName.equals("Зелье Викинга")) {
                return true;
            }
            if (item.get("Восстановить эффекты") && displayName.equals("Восстановить эффекты")) {
                return true;
            }
        }

        return false;
    }

    private boolean invEmpty() {
        try {
            for (int i = 0; i < mc.player.container.getInventory().size(); i++) {
                ItemStack stack = mc.player.container.getInventory().get(i);
                if (!stack.isEmpty()) {
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    protected void onEnable() {
        kitTimer.reset();
        cleanTimer.reset();
        sequentialTimer.reset();
        storeKit = false;
        lastKitRequested = null;
        kitLastRequestTimes.clear();
    }

    @Override
    protected void onDisable() {
        storeKit = false;
        lastKitRequested = null;
    }
}