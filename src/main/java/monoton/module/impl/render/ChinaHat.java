package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.TypeList;

@Annotation(name = "ChinaHat", type = TypeList.Render, desc = "Создаёт китайскую шляпу над головой")
public class ChinaHat extends Module {

    public ChinaHat() {
        this.addSettings();
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}