package monoton.control.events.player;

import monoton.control.events.client.Event;

public class EventTravel extends Event {

    public float speed;

    public EventTravel(float speed) {
        this.speed = speed;
    }

}
