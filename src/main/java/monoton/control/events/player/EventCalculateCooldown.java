package monoton.control.events.player;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.item.Item;
import monoton.control.events.client.Event;

@Data
@EqualsAndHashCode(callSuper = true)
public class EventCalculateCooldown extends Event {

    public Item itemStack;
    public float cooldown;

    public EventCalculateCooldown(Item item) {
        this.itemStack = item;
    }
}

