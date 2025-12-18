package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.Setting;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.other.StopWatch;
import net.minecraft.entity.LivingEntity;

@Annotation(
        name = "ElytraPredict",
        type = TypeList.Movement,
        desc = "Смещает хитбокс противника во время полёта на элитрах для перегона на элитрах"
)
public class ElyrtaPredict extends Module {
    public final SliderSetting elytradistance = new SliderSetting("Дистанция обгона", 3.0F, 0.0F, 4.25F, 0.05F);
    public final StopWatch timer = new StopWatch();
    public boolean disabled = false;

    public ElyrtaPredict() {
        this.addSettings(elytradistance);
    }

    public boolean onEvent(Event event) {
        return false;
    }

    public double getElytraDistance(LivingEntity target) {
        return elytradistance.getValue().floatValue();
    }

    public boolean canPredict(LivingEntity target) {
        if (mc.player.hurtTime > 0 && !target.lastSwing.finished(500)) {
            disabled = true;
            timer.resetes();
        }
        if (timer.finished(500)) disabled = false;
        return !disabled;
    }
}

