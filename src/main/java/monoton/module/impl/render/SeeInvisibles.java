package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.SliderSetting;

@Annotation(name = "SeeInvisibles",
        type = TypeList.Render, desc = "Убирает прозрачность у невидимых игроков")

public class SeeInvisibles extends Module {
    public SliderSetting alpha = new SliderSetting("Прозрачность", 0.5F, 0.3F, 1.0F, 0.1F);
    public SeeInvisibles() {
        this.addSettings(alpha);
    }

    public boolean onEvent(Event event) {
        return false;
    }
}
