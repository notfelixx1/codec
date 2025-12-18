package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventDeath;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "AutoRespawn", type = TypeList.Player, desc = "Моментально жмёт кнопку возрождение")
public class AutoRespawn extends Module {

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventDeath) {
            death((EventDeath) event);
            return true;
        }
        return false;
    }

    @Compile
    public void death(EventDeath event) {
        if (mc.player == null) return;
        mc.player.respawnPlayer();
        mc.displayGuiScreen(null);
    }
}