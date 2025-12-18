package monoton.control.events.game;

import lombok.Getter;
import monoton.control.events.client.Event;

public class TickEvent extends Event {
    @Getter
    private static final TickEvent instance = new TickEvent();
}
