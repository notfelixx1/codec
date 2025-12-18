package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;


@Annotation(name = "SantaHat", type = TypeList.Movement, desc = "ВАНЮЧАЯ ШАПКА")
public class SantaHat extends Module {

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
