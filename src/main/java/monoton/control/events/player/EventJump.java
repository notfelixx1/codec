package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import monoton.control.events.client.Event;

@Getter
@Setter
@AllArgsConstructor
public class EventJump extends Event {
    private float motion, yaw;
}
