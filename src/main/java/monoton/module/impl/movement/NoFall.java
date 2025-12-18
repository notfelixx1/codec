package monoton.module.impl.movement;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.utils.math.ViaUtil;
import monoton.utils.other.OtherUtil;
import net.minecraft.util.text.TextFormatting;

@Annotation(name = "NoFall", type = TypeList.Movement, desc = "Убирает урон от падение")
public class NoFall extends Module {

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventUpdate && !this.allowedBypass()) {
            OtherUtil.sendMessage("Зайдите с " + TextFormatting.RED + "1.17+" + TextFormatting.WHITE + " для активаций " + TextFormatting.RED + "NoFall");
            this.toggle();
        }
        if (event instanceof EventUpdate) {
            onUpdate((EventUpdate) event);
        }
        return false;
    }

    public void onUpdate(EventUpdate e) {
        if (ViaUtil.allowedBypass() && mc.player.fallDistance > 2.5) {
            ViaUtil.sendPositionPacket(mc.player.getPosX(), mc.player.getPosY() + 1e-6, mc.player.getPosZ(), mc.player.rotationYaw, mc.player.rotationPitch, false);
            mc.player.fallDistance = 0;
        }
    }

    public boolean allowedBypass() {
        if (!viamcp.florianmichael.vialoadingbase.ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) {
            return false;
        }
        for (UserConnection conn : Via.getManager().getConnectionManager().getConnections()) {
            if (conn == null) {
                return false;
            }
            if (conn.getProtocolInfo().getUsername().equalsIgnoreCase(mc.session.getProfile().getName())) {
                return true;
            }
        }
        return false;
    }
}