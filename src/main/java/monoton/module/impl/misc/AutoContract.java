package monoton.module.impl.misc;

import monoton.cmd.impl.ContractCmd;
import monoton.control.events.client.Event;
import monoton.control.events.game.EventKey;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.Setting;
import monoton.module.settings.imp.BindSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.misc.TimerUtil;
import monoton.utils.other.OtherUtil;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.util.InputMappings;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "AutoContract", type = TypeList.Misc, desc = "Автоматически ищет контракт на ReallyWorld")
public class AutoContract extends Module {
    private final TimerUtil hubTimer = new TimerUtil();
    private final TimerUtil actionTimer = new TimerUtil();
    private boolean isInHub = false;
    private boolean hasJoinedGame = false;
    private final SliderSetting griefServerNumber = new SliderSetting("Номер грифа", 1F, 1F, 54F, 1F);
    private BindSetting offBind = new BindSetting("Кнопка отключение", InputMappings.getInputByName("key.keyboard.insert").getKeyCode());

    public AutoContract() {
        addSettings(new Setting[]{griefServerNumber, offBind});
    }

    @Compile
    @Override
    protected void onEnable() {
        if (ContractCmd.nickName != null) {
            if (!OtherUtil.isPvP()) {
                mc.player.sendChatMessage("/hub");
            }

            OtherUtil.sendMessage("Для отключения функции нажмите кнопку " + TextFormatting.RED + offBind.getKeyName());

            selectCompassAndUse();
        }

        super.onEnable();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket packetEvent) {
            if (handlePacketEvent(packetEvent)) {
                return false;
            }
        }

        if (event instanceof EventUpdate) {
            handleUpdateEvent();
        }

        if (event instanceof EventKey keyEvent) {
            handleKeyEvent(keyEvent);
        }

        return false;
    }

    private boolean handleKeyEvent(EventKey event) {
        if (event.key == offBind.getKey()) {
            toggle();
            return true;
        }
        return false;
    }

    private boolean handlePacketEvent(EventPacket packetEvent) {
        if (OtherUtil.isPvP()) {
            OtherUtil.sendMessage("Вы в режиме пвп!");
            toggle();
            return true;
        }

        if (ContractCmd.nickName == null) {
            OtherUtil.sendMessage("Напишите ник вашей цели! .contract <name>");
            toggle();
            return true;
        }

        IPacket<?> packet = packetEvent.getPacket();
        if (packet instanceof SJoinGamePacket) {
            hasJoinedGame = true;
            actionTimer.reset();
            return false;
        }

        if (packet instanceof SChatPacket chatPacket) {
            return handleChatPacket(chatPacket);
        }

        return false;
    }

    private boolean handleChatPacket(SChatPacket chatPacket) {
        String message = TextFormatting.getTextWithoutFormattingCodes(chatPacket.getChatComponent().getString());
        if (message.contains("Ваша цель") && message.contains(ContractCmd.nickName)) {
            OtherUtil.sendMessage("Ваша цель найдена!");
            toggle();
            return true;
        }

        if (message.contains("Ваша цель") || message.contains("Не спамь!") || message.contains("Не получилось")) {
            hasJoinedGame = false;
            isInHub = true;
            hubTimer.reset();
            actionTimer.reset();
            return false;
        }

        if (message.contains("К сожалению сервер переполнен") ||
                message.contains("Подождите 20 секунд!") ||
                message.contains("большой поток игроков")) {
            selectCompassAndUse();
        }

        return false;
    }

    @Compile
    private void handleUpdateEvent() {
        boolean isTargetOnline = false;
        for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equals(ContractCmd.nickName)) {
                isTargetOnline = true;
                break;
            }
        }

        if (hasJoinedGame && isTargetOnline) {
            mc.player.sendChatMessage("/contract get");
            hasJoinedGame = false;
        }

        if (isInHub && hubTimer.hasTimeElapsed(700L)) {
            isInHub = false;
            mc.player.sendChatMessage("/hub");
            hubTimer.reset();
        }

        if (mc.currentScreen == null) {
            if (mc.player.ticksExisted < 5) {
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            }
        } else if (mc.currentScreen instanceof ChestScreen chestScreen) {
            handleChestScreen(chestScreen);
        }
    }

    private void handleChestScreen(ChestScreen chestScreen) {
        try {
            int targetGriefNumber = griefServerNumber.getValue().intValue();
            ContainerScreen container = chestScreen;

            for (int i = 0; i < container.getContainer().inventorySlots.size(); i++) {
                Slot slot = container.getContainer().inventorySlots.get(i);
                String displayName = slot.getStack().getDisplayName().getString();
                String message = TextFormatting.getTextWithoutFormattingCodes(slot.getStack().getTextComponent().getString());

                if ((displayName.contains("ГРИФЕРСКОЕ ВЫЖИВАНИЕ") || message.equals("[ГРИФ #" + targetGriefNumber + " (1.16.5+)]")) && actionTimer.hasTimeElapsed(50L)) {
                    mc.playerController.windowClick(mc.player.openContainer.windowId, i, 0, ClickType.PICKUP, mc.player);
                    actionTimer.reset();
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Compile
    private void selectCompassAndUse() {
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.inventory.getStackInSlot(slot).getItem() == Items.COMPASS) {
                mc.player.inventory.currentItem = slot;
                mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                break;
            }
        }
    }
}