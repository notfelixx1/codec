package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;

@Annotation(name = "NoCommands", type = TypeList.Misc, desc = "Отключает команды чита такие как баритон и тд")
public class NoCommands extends Module {

    public NoCommands() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
