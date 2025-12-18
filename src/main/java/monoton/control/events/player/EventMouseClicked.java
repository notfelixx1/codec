package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import monoton.control.events.client.Event;

@Getter
@AllArgsConstructor
public class EventMouseClicked extends Event {
    private final int key;
    private final float mouseX, mouseY;
}