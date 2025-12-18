package monoton.module.impl.combat;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CUseEntityPacket;


@Annotation(name = "NoFriendDamage", type = TypeList.Combat, desc = "Отключает урон по друзьям")
public class NoFriendDamage extends Module {
    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket packet) {
            if (packet.getPacket() instanceof CUseEntityPacket useEntityPacket) {
                Entity entity = useEntityPacket.getEntityFromWorld(mc.world);
                if (entity instanceof RemoteClientPlayerEntity && Manager.FRIEND_MANAGER.isFriend(entity.getName().getString()) && useEntityPacket.getAction() == CUseEntityPacket.Action.ATTACK) {
                    event.setCancel(true);
                }
            }
        }
        return false;
    }
}
