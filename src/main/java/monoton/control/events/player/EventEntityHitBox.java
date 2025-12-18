package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import monoton.control.events.client.Event;
import net.minecraft.entity.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class EventEntityHitBox extends Event {
    private Entity entity;
    private float size;
}