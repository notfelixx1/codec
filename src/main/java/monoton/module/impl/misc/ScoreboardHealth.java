package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;

@Annotation(name = "ScoreboardHealth", type = TypeList.Misc, desc = "Обход показа хп где хп игроков скрыто")
public class ScoreboardHealth extends Module {

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
