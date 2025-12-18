package monoton.control.events.game;

import lombok.AllArgsConstructor;
import monoton.control.events.client.Event;

@AllArgsConstructor
public class InventoryCloseEvent extends Event {

    public int windowId;

}

