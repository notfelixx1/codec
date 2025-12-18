package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventOverlaysRender;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;

@Annotation(name = "NoRender", type = TypeList.Render, desc = "Убирает не нужный рендер")
public class NoRender extends Module {

    public MultiBoxSetting element = new MultiBoxSetting("Элементы",
            new BooleanOption("Оверлей огня", true),
            new BooleanOption("Плохие эффекты", true),
            new BooleanOption("Босс бар", false),
            new BooleanOption("Скорборд", false),
            new BooleanOption("Заголовки", false),
            new BooleanOption("Аним тотема", false),
            new BooleanOption("Дождь", true),
            new BooleanOption("Камера клип", true),
            new BooleanOption("Затемнение фона", false),
            new BooleanOption("Тряска от урона", true),
            new BooleanOption("Зачарование", true),
            new BooleanOption("Размытие под водой", true),
            new BooleanOption("Интерполяцию рук", true));

    public NoRender() {
        addSettings(element);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventOverlaysRender) {
            handleEventOverlaysRender((EventOverlaysRender) event);
        } else if (event instanceof EventUpdate) {
            handleEventUpdate((EventUpdate) event);
        }
        return false;
    }

    private void handleEventOverlaysRender(EventOverlaysRender event) {
        EventOverlaysRender.OverlayType overlayType = event.getOverlayType();

        boolean cancelOverlay = switch (overlayType) {
            case FIRE_OVERLAY -> element.get(0);
            case BOSS_LINE -> element.get(2);
            case SCOREBOARD -> element.get(3);
            case TITLES -> element.get(4);
            case TOTEM -> element.get(5);
        };

        if (cancelOverlay) {
            event.setCancel(true);
        }
    }

    private void handleEventUpdate(EventUpdate event) {

        boolean isRaining = element.get(6) && mc.world.isRaining();

        if (isRaining) {
            mc.world.setRainStrength(0);
            mc.world.setThunderStrength(0);
        }

    }
}
