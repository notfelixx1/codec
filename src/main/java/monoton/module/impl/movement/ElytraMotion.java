package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.SliderSetting;

@Annotation(name = "ElytraMotion", type = TypeList.Movement, desc = "Позволяет зависнуть возле цели на элитрах")
public class ElytraMotion extends Module {

    public final SliderSetting distancie = new SliderSetting("Дист до цели", 2.5F, 1.5F, 3F, 0.1F);

    public ElytraMotion() {
        addSettings(distancie);
    }

    @Override
    public boolean onEvent(final Event event) {
        return false;
    }
}