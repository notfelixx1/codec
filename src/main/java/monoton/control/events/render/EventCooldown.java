package monoton.control.events.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import monoton.control.events.client.Event;
import net.minecraft.item.Item;

@Setter
@Getter
@AllArgsConstructor
public class EventCooldown extends Event {
    private final Item item;
    private int ticks;
}
