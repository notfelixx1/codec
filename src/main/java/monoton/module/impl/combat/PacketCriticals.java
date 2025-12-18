package monoton.module.impl.combat;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.ModeSetting;
import monoton.utils.math.MathUtil;
import monoton.utils.other.OtherUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import static monoton.module.TypeList.Combat;
import static net.minecraft.client.Minecraft.player;

@Annotation(name = "PacketCriticals", type = Combat, desc = "Всегда даёт критический удар")
public class PacketCriticals extends Module {

    public final ModeSetting mode = new ModeSetting("Режим", "ReallyWorld", "ReallyWorld", "Grim 1.17+");
    public static boolean cancelCrit;

    public PacketCriticals() {
        addSettings(mode);
    }

    @Override
    public boolean onEvent(Event event) {
        if (!this.allowedBypass() && mode.is("Grim 1.17")) {
            OtherUtil.sendMessage("Зайдите с " + TextFormatting.RED + "1.17+" + TextFormatting.WHITE + " для активаций " + TextFormatting.RED + "Criticals" + TextFormatting.WHITE + " или включите " + TextFormatting.RED + "мод ReallyWorld");
            this.toggle();
        }
        if (event instanceof EventPacket e) {
            onPacket(e);
        }
        return false;
    }

    @Compile
    public void onPacket(EventPacket e) {
        if (e.isSendPacket() && e.getPacket() instanceof CUseEntityPacket packet) {
            if (packet.getAction() == CUseEntityPacket.Action.ATTACK) {
                Entity entity = packet.getEntityFromWorld(mc.world);
                if (entity == null || entity instanceof EnderCrystalEntity || cancelCrit) {
                    return;
                }
                sendGrimCrit();
            }
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

    @Compile
    public void sendGrimCrit() {
        if (Manager.FUNCTION_MANAGER.auraFunction.target == null) return;
        if (mode.is("ReallyWorld")) {
            if (!mc.player.isInWeb()) return;
            if (mc.player.isOnGround()) return;
            double y = mc.player.getPosY();
            if (y == (int) y) return;
            mc.player.fallDistance = 0.001f;
            mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(mc.player.getPosX(), mc.player.getPosY() + (-(mc.player.fallDistance = MathUtil.randomizeFloat(1e-7F, 1e-6F))), mc.player.getPosZ(), Aura.rotate.x, Aura.rotate.y, false));
        } else {
            if (mc.player.isOnGround()) return;
            double y = mc.player.getPosY();
            if (y == (int) y) return;
            mc.player.fallDistance = 0.001f;
            mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(mc.player.getPosX(), mc.player.getPosY() + (-(mc.player.fallDistance = MathUtil.randomizeFloat(1e-7F, 1e-6F))), mc.player.getPosZ(), Aura.rotate.x, Aura.rotate.y, false));
        }
    }
}
