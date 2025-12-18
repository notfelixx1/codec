package monoton.control.events.player;

import lombok.Getter;
import monoton.control.events.client.Event;

@Getter
public class EventAspectRatio extends Event {
    private float aspectRatio;

    public EventAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
}