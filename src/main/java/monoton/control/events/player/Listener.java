package monoton.control.events.player;


import monoton.control.events.client.Event;

public interface Listener<T extends Event> {
    void onEvent(T var1);
}

