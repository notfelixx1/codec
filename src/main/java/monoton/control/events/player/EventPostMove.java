package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import monoton.control.events.client.Event;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class EventPostMove extends Event {
    private double horizontalMove;
}