package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import monoton.control.events.client.Event;
import net.minecraft.entity.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class EventAttack extends Event {
    private final Entity targetEntity;

    public Entity getTarget() {
        return targetEntity;
    }
}