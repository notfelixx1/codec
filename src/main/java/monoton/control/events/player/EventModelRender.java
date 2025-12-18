package monoton.control.events.player;

import net.minecraft.client.renderer.entity.PlayerRenderer;
import monoton.control.events.client.Event;

public class EventModelRender extends Event {

    public PlayerRenderer renderer;
    private Runnable entityRenderer;

    public EventModelRender(PlayerRenderer renderer, Runnable entityRenderer) {
        this.renderer = renderer;
        this.entityRenderer = entityRenderer;
    }

    public void render() {
        entityRenderer.run();
    }
}
