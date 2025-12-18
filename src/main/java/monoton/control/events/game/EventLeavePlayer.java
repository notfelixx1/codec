package monoton.control.events.game;

import monoton.control.events.client.Event;
import net.minecraft.entity.Entity;

public class EventLeavePlayer extends Event {
    public Entity entity;

    public EventLeavePlayer(Entity entity) {
        this.entity = entity;
    }
}

