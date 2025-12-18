package monoton.module.impl.movement;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "AntiTarget", type = TypeList.Movement, desc = "Не даёт вас за таргетить на элитрах")
public class AntiTarget extends Module {
    public static final ModeSetting mod = new ModeSetting("Режим", "Обычный", "Обычный", "Быстрый");

    public static SliderSetting gradus = new SliderSetting("Наклон", 35f, 30f, 50f, 1f).setVisible(() -> mod.is("Обычный"));
    public static SliderSetting speedantitarget = new SliderSetting("Скорость", 1.95f, 1.9f, 2.7f, 0.01f, 2F).setVisible(() -> mod.is("Обычный"));

    public AntiTarget() {
        addSettings(mod, gradus, speedantitarget);
    }

    @Override
    public boolean onEvent(Event event) {
        if (Manager.FUNCTION_MANAGER.auraFunction.target != null) {
            return false;
        }

        if (mc.player.isElytraFlying()) {
            float targetPitch = -gradus.getValue().floatValue();
            if (mod.is("Обычный")) {
                mc.player.rotationPitch = targetPitch;
            } else {
                mc.player.rotationPitch = -42.5f;
                mc.player.rotationYaw = 45;
            }
        }
        return false;
    }
}