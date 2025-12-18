package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import net.minecraft.item.UseAction;

@Annotation(name = "AutoEat", type = TypeList.Player, desc = "Автоматически использует еду")
public class AutoEat extends Module {
    private boolean isEating = false;

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            mc.gameSettings.keyBindUseItem.setPressed(this.isEating);
            if (mc.player.getFoodStats().getFoodLevel() <= 16) {
                if (mc.player.getHeldItemMainhand().getUseAction() == UseAction.EAT || mc.player.getHeldItemOffhand().getUseAction() == UseAction.EAT) {
                    this.isEating = true;
                }
            } else {
                this.isEating = mc.player.getFoodStats().needFood();
            }
        }

        return false;
    }
}

