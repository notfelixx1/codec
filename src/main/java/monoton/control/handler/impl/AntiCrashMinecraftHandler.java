package monoton.control.handler.impl;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import net.minecraft.network.play.server.*;

import static monoton.utils.IMinecraft.mc;

public class AntiCrashMinecraftHandler {

    public boolean onEvent(Event event) {
        if (event instanceof EventPacket) {
            onPacket((EventPacket) event);
        }
        return false;
    }

    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.world == null) return;
        Object p = e.getPacket();
        if (p instanceof SExplosionPacket exp) {
            if (Math.abs(exp.getX()) > 1e9 || Math.abs(exp.getY()) > 1e9 || Math.abs(exp.getZ()) > 1e9
                    || Math.abs(exp.getStrength()) > 1e9) {
                e.setCancel(true);
            }
        } else if (p instanceof SSpawnParticlePacket part) {
            if (Math.abs(part.getXCoordinate()) > 1e9 || Math.abs(part.getYCoordinate()) > 1e9
                    || Math.abs(part.getZCoordinate()) > 1e9 || Math.abs(part.getXOffset()) > 1e9
                    || Math.abs(part.getYOffset()) > 1e9 || Math.abs(part.getZOffset()) > 1e9
                    || Math.abs(part.getParticleSpeed()) > 1e9) {

                e.setCancel(true);
            }
        } else if (p instanceof SPlayerPositionLookPacket pos) {
            if (Math.abs(pos.getX()) > 1e9 || Math.abs(pos.getY()) > 1e9 || Math.abs(pos.getZ()) > 1e9
                    || Math.abs(pos.getYaw()) > 1e9 || Math.abs(pos.getPitch()) > 1e9) {
                e.setCancel(true);
            }
        } else if (p instanceof SChangeGameStatePacket change) {
            int id = change.func_241776_b_().field_241778_b_;
            if (id == 4 || id == 5) {
                e.setCancel(true);
            }
        } else if (p instanceof SEntityTeleportPacket entity) {
            if (Math.abs(entity.getX()) > 1e6 || Math.abs(entity.getY()) > 1e6 || Math.abs(entity.getZ()) > 1e6) {
                e.setCancel(true);
            }
        }
    }
}
