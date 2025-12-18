package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.control.events.game.EventLeavePlayer;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.utils.other.OtherUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "LeaveTracker", type = TypeList.Misc, desc = "Пишет кординаты ливнутых игроков из прогрузки")
public class LeaveTracker extends Module {

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventLeavePlayer e) {
            this.leave_tracker(e.entity);
        }
        return false;
    }

    @Compile
    public void leave_tracker(Entity entity) {
        if (entity instanceof PlayerEntity && entity.getEntityId() != mc.player.getEntityId() && entity != mc.player) {
            int x = (int) entity.getPosX();
            int y = (int) entity.getPosY();
            int z = (int) entity.getPosZ();
            if (entity != null && mc.player.getDistance(entity) > 200.0F && entity.getUniqueID().equals(PlayerEntity.getOfflineUUID(entity.getName().getString()))) {
                OtherUtil.sendMessage(TextFormatting.WHITE + "Игрок " + TextFormatting.RED + entity.getName().getString() + TextFormatting.WHITE + " ливнул на" + TextFormatting.RED + " x: " + x + " y: " + y + " z: " + z);
            }
        }

    }
}
