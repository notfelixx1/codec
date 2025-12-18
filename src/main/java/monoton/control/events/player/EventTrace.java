package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import monoton.control.events.client.Event;

@Setter
@Getter
@AllArgsConstructor
public class EventTrace extends Event {

    private float yaw, pitch;

}
