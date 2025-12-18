package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.TypeList;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.misc.TimerUtil;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(
        name = "TapeMouse",
        type = TypeList.Misc,
        desc = "Авто кликер"
)
public class TapeMouse extends Module {
    public SliderSetting speed = new SliderSetting("Задержка", 1, 0, 10, 1);
    public ModeSetting mode = new ModeSetting("Выбор режимов", "Левый клик", new String[]{"Левый клик", "Правый клик"});
    private final TimerUtil timerUtil = new TimerUtil();

    public TapeMouse() {
        addSettings(mode, speed);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate e) {
            if (mc.player.isHandActive() && !mc.player.isBlocking()) return false;
            if (timerUtil.hasTimeElapsed(speed.getValue().longValue() * 100L)) {
                if (mode.is("Левый клик")) {
                    mc.gameSettings.keyBindAttack.setPressed(true);
                    mc.gameSettings.keyBindAttack.setPressed(false);
                    mc.clickMouse();
                } else if (mode.is("Правый клик")) {
                    mc.gameSettings.keyBindUseItem.setPressed(true);
                    mc.gameSettings.keyBindUseItem.setPressed(false);
                    mc.rightClickMouse();
                }
                timerUtil.reset();
            }
        }
        return false;
    }
}