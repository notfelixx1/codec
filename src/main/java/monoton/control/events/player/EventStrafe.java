package monoton.control.events.player;

import monoton.control.events.client.Event;

public class EventStrafe extends Event {

    public float yaw;

    public EventStrafe(float yaw) {
        this.yaw = yaw;
    }

}
