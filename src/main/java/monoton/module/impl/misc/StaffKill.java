package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.control.events.player.Listener;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.utils.other.OtherUtil;
import net.minecraft.network.play.server.SChatPacket;

@Annotation(name = "StaffKill", type = TypeList.Misc, desc = "Уклон проверки через AnyDesk")
public class StaffKill extends Module {

    private boolean waitingForResponse = false;
    private long lastSentTime = 0L;
    private static final long DELAY = 1000L;

    private final Listener<EventUpdate> onUpdate = (event) -> {
        if (waitingForResponse) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSentTime >= DELAY) {
            mc.player.sendChatMessage("/mod 1368934220");
            lastSentTime = currentTime;
            waitingForResponse = true;
        }
    };

    private final Listener<EventPacket> onPacket = (event) -> {
        if (!event.isReceivePacket()) return;
        if (!(event.getPacket() instanceof SChatPacket chatPacket)) return;

        String message = chatPacket.getChatComponent().getString();

        if (!waitingForResponse) return;

        if (message.contains("Команда не найдена") ||
                message.contains("неизвестная команда")) {

            OtherUtil.sendMessage("§cВы не на проверки модуль отключён");
            this.toggle();
            waitingForResponse = false;
            return;
        }

        if (message.contains("отправ")) {

            OtherUtil.sendMessage("§a§lAnyDesk успешно отправлен модератору! §fЕсли он спросит, почему не может подключиться — скажите что-то вроде: §e«Я не знаю, у меня всё нормально, проблема с вашей стороны»");
            this.toggle();
            waitingForResponse = false;
        }
    };

    public StaffKill() {
        this.addSettings();
    }

    @Override
    public void onEnable() {
        waitingForResponse = false;
        lastSentTime = 0L;
    }

    @Override
    public void onDisable() {
        waitingForResponse = false;
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            onUpdate.onEvent((EventUpdate) event);
        } else if (event instanceof EventPacket) {
            onPacket.onEvent((EventPacket) event);
        }
        return false;
    }
}