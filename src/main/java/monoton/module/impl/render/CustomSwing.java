package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.TypeList;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;

@Annotation(name = "SwingAnimation", type = TypeList.Render, desc = "Меняет анимацию атаки")
public class CustomSwing extends Module {
    public static final ModeSetting swordAnim = new ModeSetting("Мод", "Мод 1", "Мод 1", "Мод 2", "Мод 3", "Мод 4", "Мод 5", "Мод 6", "Мод 7");
    public final SliderSetting angle = new SliderSetting("Угол", 60, 0, 60, 5).setVisible(() -> swordAnim.is("Мод 2"));
    public final SliderSetting swipePower = new SliderSetting("Сила взмаха", 35, 10, 100, 1).setVisible(() -> !swordAnim.is("Мод 5") && !swordAnim.is("Мод 6") && !swordAnim.is("Мод 7"));
    public final SliderSetting swipePower2 = new SliderSetting("Сила взмaха", 35, 10, 35, 1).setVisible(() -> swordAnim.is("Мод 5"));
    public final SliderSetting swipeSpeed = new SliderSetting("Плавность взмаха", 6, 1, 10, 1).setVisible(() -> !swordAnim.is("Мод 5") && !swordAnim.is("Мод 6"));
    public final SliderSetting swipePowerd = new SliderSetting("Сила вращение", 1.05f, 1f, 2f, 0.05f).setVisible(() -> swordAnim.is("Мод 6"));
    public static BooleanOption item360 = new BooleanOption("Вращать на 360", false).setVisible(() -> !swordAnim.is("Мод 6"));
    public static BooleanOption onlyaura = new BooleanOption("Только с Aura", false);

    public CustomSwing() {
        addSettings(swordAnim, angle, swipePower, swipePower2, swipeSpeed, swipePowerd, item360, onlyaura);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
