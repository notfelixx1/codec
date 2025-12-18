package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;

@Annotation(name = "SaturationViewer", type = TypeList.Render, desc = "Показывает настоящие насыщение")
public class SaturationViewer extends Module {

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}