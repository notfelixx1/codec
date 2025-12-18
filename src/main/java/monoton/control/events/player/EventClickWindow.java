package monoton.control.events.player;

import monoton.control.events.client.Event;
import net.minecraft.inventory.container.ClickType;

public class EventClickWindow extends Event {
    public int slotId;
    public int clickType;
    public int windowId;
    public ClickType type;

    public EventClickWindow(int windowIdIn, int slotIdIn, int usedButtonIn, ClickType modeIn) {
        this.windowId = windowIdIn;
        this.slotId = slotIdIn;
        this.clickType = usedButtonIn;
        this.type = modeIn;
    }
}
