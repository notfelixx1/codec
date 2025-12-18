package monoton.module.impl.player;

import monoton.control.handler.impl.PickItemFixHandler;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;
import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import net.minecraft.util.math.MathHelper;

@Annotation(name = "ItemSwapFix", type = TypeList.Player, desc = "Убирает перелаживание предметов античитом")
public class ItemSwapFix extends Module {

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket) {
            onPacket((EventPacket) event);
        }
        return false;
    }

    private void onPacket(EventPacket e) {
        if (!PickItemFixHandler.isFixActive()) {
            if ((e.getPacket() instanceof SHeldItemChangePacket fix) && e.isReceive()) {
                if (fix.getHeldItemHotbarIndex() != mc.player.inventory.currentItem) {
                    int newSlot = MathHelper.clamp(mc.player.inventory.currentItem >= 8 ? mc.player.inventory.currentItem - 1 : mc.player.inventory.currentItem + 1, 0, 8);
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(newSlot));
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                    e.setCancel(true);
                }
            }
        }
    }
}