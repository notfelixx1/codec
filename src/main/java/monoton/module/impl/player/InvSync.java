package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SConfirmTransactionPacket;

@Annotation(name = "InvSync", type = TypeList.Player, desc = "Исправляет десинхронизацию вашего инвентаря с сервером")
public class InvSync extends Module {
    public short action;

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket packetEvent) {
            invSync(packetEvent);
        }
        return false;
    }

    public void invSync(EventPacket event) {
        final IPacket<?> packet = event.getPacket();
        if (mc.player == null || mc.world == null) return;

        if (packet instanceof SConfirmTransactionPacket wrapper) {
            final Container inventory = mc.player.container;

            if (wrapper.getWindowId() == inventory.windowId) {
                this.action = wrapper.getActionNumber();

                if (this.action > 0 && this.action < inventory.getTransactionID()) {
                    inventory.setTransactionID((short) (this.action + 1));
                }
            }
        }
    }
}