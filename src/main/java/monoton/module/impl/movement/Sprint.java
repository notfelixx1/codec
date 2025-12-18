package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;

@Annotation(
        name = "Sprint",
        type = TypeList.Movement,
        desc = "Автоматически включает бег при движении вперед"
)
public class Sprint extends Module {

    public Sprint() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}
