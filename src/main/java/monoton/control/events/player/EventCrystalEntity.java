package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.entity.Entity;
import monoton.control.events.client.Event;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventCrystalEntity extends Event {

    private Entity entity;
}