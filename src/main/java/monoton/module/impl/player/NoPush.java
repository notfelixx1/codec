package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.module.api.Module;
import monoton.module.api.Annotation;
import monoton.module.TypeList;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;


@Annotation(name = "NoPush", type = TypeList.Player, desc = "Отключает отталкивание от определённых действий")
public class NoPush extends Module {

    public final MultiBoxSetting modes = new MultiBoxSetting("Тип",
            new BooleanOption("Игроки", true),
            new BooleanOption("Блоки", true),
            new BooleanOption("Удочка", true));

    public NoPush() {
        addSettings(modes);
    }

    @Override
    public boolean onEvent(final Event event) {
        return false;
    }
}
