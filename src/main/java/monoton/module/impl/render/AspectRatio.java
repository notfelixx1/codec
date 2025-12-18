package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventAspectRatio;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.Setting;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;

@Annotation(name = "AspectRatio", type = TypeList.Render, desc = "Растягивает ваш экран")
public class AspectRatio extends Module {

    public final ModeSetting mode = new ModeSetting("Соотношение", "Кастомное", "4:3", "16:9", "1:1", "16:10", "Кастомное");
    public final SliderSetting aspectRatio = new SliderSetting("Значение", 1.0f, 0.1f, 5, 0.1f).setVisible(() -> mode.is("Кастомное"));

    public AspectRatio() {
        addSettings(mode, aspectRatio);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventAspectRatio e) {
            onAspectRatio(e);
        }
        return false;
    }

    public void onAspectRatio(EventAspectRatio e) {
        float value = switch (mode.get()) {
            case "4:3" -> 4.0f / 3.0f;
            case "16:9" -> 16.0f / 9.0f;
            case "1:1" -> 1.0f;
            case "16:10" -> 16.0f / 10.0f;
            default -> aspectRatio.getValue().floatValue();
        };

        e.setAspectRatio(value);
    }
}

