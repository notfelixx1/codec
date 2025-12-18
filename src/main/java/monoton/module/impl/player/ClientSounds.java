package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Module;
import monoton.module.api.Annotation;
import monoton.module.settings.imp.SliderSetting;

@Annotation(name = "ClientSounds", type = TypeList.Player, desc = "Воспроизводит звук при включение модуля по бинду")
public class ClientSounds extends Module {

    public final SliderSetting voulme = new SliderSetting("Громкость", 65f, 5f, 100f, 5f);

    public ClientSounds() {
        super();
        addSettings(voulme);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
