package monoton.control.events.player;


import lombok.AllArgsConstructor;
import lombok.Getter;
import monoton.control.events.client.Event;

@Getter
@AllArgsConstructor
public class EventTotemParticle extends Event {
    private double x;
    private double y;
    private double z;
    private double xSpeed;
    private double ySpeed;
    private double zSpeed;
}