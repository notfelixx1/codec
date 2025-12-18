package monoton.module.impl.misc;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.friend.Friend;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.Arrays;

@Annotation(name = "AutoTpaccept", type = TypeList.Misc, desc = "Принимает телепортацию")
public class AutoTpaccept extends Module {
    private final BooleanOption onlyfriends = new BooleanOption("Только друзья", false);

    private final String[] teleportMessages = new String[]{"has requested teleport", "просит телепортироваться", "хочет к вам телепортироваться", "просит к вам телепортироваться", "хочет телепортироваться"};

    private boolean teleportMessageProcessed = false;

    public AutoTpaccept() {
        addSettings(onlyfriends);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket packetEvent) {
            if (packetEvent.isReceivePacket()) {
                if (packetEvent.getPacket() instanceof SChatPacket packetChat) {
                    handleReceivePacket(packetChat);
                }
            }
        }
        return false;
    }

    /**
     * Обрабатывает полученный пакет чата.
     *
     * @param packet Пакет чата
     */
    private void handleReceivePacket(SChatPacket packet) {
        String message = TextFormatting.getTextWithoutFormattingCodes(packet.getChatComponent().getString());

        if (isTeleportMessage(message)) {
            teleportMessageProcessed = true;

            if (onlyFriendsEnabled()) {
                handleTeleportWithFriends(message);
                return;
            }
            acceptTeleport();
        } else if (message.contains("Не спамь!") && teleportMessageProcessed) {
            handleAntiSpamMessage();
            teleportMessageProcessed = false;
        }
    }

    /**
     * Проверяет, является ли сообщение пакетом телепортации.
     *
     * @param message Сообщение чата
     * @return true, если сообщение является пакетом телепортации, иначе false
     */
    private boolean isTeleportMessage(String message) {
        return Arrays.stream(this.teleportMessages)
                .map(String::toLowerCase)
                .anyMatch(message::contains);
    }

    /**
     * Проверяет, включена ли опция "только для друзей".
     *
     * @return true, если опция "только для друзей" включена, иначе false
     */
    private boolean onlyFriendsEnabled() {
        return onlyfriends.get();
    }

    /**
     * Обрабатывает пакет телепортации, когда включена опция "только для друзей".
     *
     * @param message Сообщение чата
     */
    private void handleTeleportWithFriends(String message) {
        for (Friend friend : Manager.FRIEND_MANAGER.getFriends()) {

            StringBuilder builder = new StringBuilder();
            char[] buffer = message.toCharArray();
            for (int w = 0; w < buffer.length; w++) {
                char c = buffer[w];
                if (c == '§') {
                    w++;
                } else {
                    builder.append(c);
                }
            }

            if (builder.toString().contains(friend.getName()))
                acceptTeleport();
        }
    }

    /**
     * Отправляет команду для принятия телепортации.
     */
    private void acceptTeleport() {
        mc.player.sendChatMessage("/tpaccept");
        teleportMessageProcessed = false;
    }

    /**
     * Обрабатывает сообщение "Не спамь!" и отправляет команду /tpaccept через 1.5 секунды.
     */
    private void handleAntiSpamMessage() {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                mc.player.sendChatMessage("/tpaccept");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}