package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;

import static monoton.module.TypeList.*;

@Annotation(name = "ItemPhysics", type = Render, desc = "Добавляет физику предметом при выбрасывание")
public class ItemPhysics extends Module {

    public final BooleanOption size = new BooleanOption("Уменьшить предметы", true);

    public ItemPhysics() {
        addSettings(size);
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}

