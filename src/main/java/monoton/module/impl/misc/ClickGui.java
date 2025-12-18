package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.TypeList;
import monoton.module.settings.imp.BooleanOption;

@Annotation(name = "ClickGui", type = TypeList.Misc, desc = "Гуйшка чита")
public class ClickGui extends Module {
    public final BooleanOption sounds = new BooleanOption("Звуки гуй", true);

    public ClickGui() {
        addSettings(sounds);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        setState(false);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
