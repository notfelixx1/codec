package monoton.control.events.player;

import monoton.control.events.client.Event;
import net.minecraft.entity.Entity;

public class EventDestroyTotem extends Event {
    public Entity entity;

    public EventDestroyTotem(Entity entity) {
        this.entity = entity;
    }
}
