package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.TypeList;
import monoton.module.settings.imp.SliderSetting;

@Annotation(name = "ItemScroller", type = TypeList.Player, desc = "Быстрое перелаживание предметов при зажатие Shift + ЛКМ")
public class ItemScroller extends Module {

    public SliderSetting delay = new SliderSetting("Задержка", 80, 0, 1000, 1);


    public ItemScroller() {
        addSettings(delay);
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}
