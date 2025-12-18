package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import monoton.control.events.client.Event;
import net.minecraft.entity.LivingEntity;

@Getter
@AllArgsConstructor
public class EventKill extends Event {

    private final LivingEntity target;

}