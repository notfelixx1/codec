package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.control.events.game.EventKey;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BindSetting;
import monoton.utils.other.OtherUtil;
import net.minecraft.entity.player.PlayerEntity;

import static monoton.module.impl.combat.Aura.target;

@Annotation(name = "CrashCrack", desc = "Крашит кряка юзеров", type = TypeList.Misc)
public class CrashCrack extends Module {
    private BindSetting crash = new BindSetting("Кнопка краша", 0);
    private PlayerEntity lastTarget = null;

    public CrashCrack() {
        addSettings(crash);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKey e) {
            handleKeyEvent(e);
        }
        return false;
    }

    private void handleKeyEvent(EventKey e) {
        if (e.key == crash.getKey()) {
            if (target != null) {
                lastTarget = (PlayerEntity) target;
            }

            if (lastTarget != null) {
                mc.player.sendChatMessage(lastTarget.getUniqueID() + " " + "убить кряко-юзера " + lastTarget.getName().getString());
            } else {
                OtherUtil.sendMessage("Вы ещё не таргетите кряко юзера");
            }
        }
    }
}