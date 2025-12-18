package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;

@Annotation(name = "NoPlaceSphere", type = TypeList.Player, desc = "Не даёт ставить шары")
public class NoPlaceSpher extends Module {

    public NoPlaceSpher() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}
