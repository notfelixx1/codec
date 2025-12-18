package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Module;
import monoton.module.api.Annotation;

@Annotation(name = "Chams", type = TypeList.Render, desc = "Просвечивает игроков сквозь блоки")
public class Chams extends Module {

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}


