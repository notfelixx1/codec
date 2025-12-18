package monoton.control.events.game;

import monoton.control.events.client.Event;

public class EventKey extends Event {

    public int key;

    public EventKey(int key) {
        this.key = key;
    }
}