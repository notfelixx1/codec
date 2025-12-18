package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;

@Annotation(name = "ArmorDurability", type = TypeList.Render, desc = "Меняет цвет брони в зависимости от её прочности")
public class ArmorDurability extends Module {

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}


