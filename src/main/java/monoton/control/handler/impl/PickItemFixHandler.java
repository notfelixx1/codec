package monoton.control.handler.impl;

import lombok.Getter;
import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.utils.IMinecraft;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;
import net.minecraft.util.math.MathHelper;

public class PickItemFixHandler implements IMinecraft {
    @Getter
    public static boolean fixActive = false;
    public static int tickCounter = 0;

    public boolean onEvent(Event event) {
        if (event instanceof EventPacket) {
            onPacketEvent((EventPacket) event);
        }

        if (event instanceof EventUpdate) {
            onUpdate((EventUpdate) event);
        }

        return false;
    }


    public static void activateFix() {
        fixActive = true;
        tickCounter = 0;
    }

    public void onUpdate(EventUpdate event) {
        if (fixActive) {
            ++tickCounter;
            if (tickCounter >= 6) {
                fixActive = false;
                tickCounter = 0;
            }
        }
    }

    public void onPacketEvent(EventPacket e) {
        if (fixActive && mc.player != null && mc.world != null) {
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
