package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;

@Annotation(name = "IRC", type = TypeList.Misc, desc = "Внутри игровой чат межу юзерами чита")
public class IRC extends Module {

    public IRC() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}

