package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;

import static monoton.module.TypeList.Render;

@Annotation(name = "TargetEsp", type = Render, desc = "Отображает таргет")
public class TargetEsp extends Module {
    public static ModeSetting targetesp = new ModeSetting("Мод", "Кольцо", "Ромб", "Кольцо", "Призраки");
    public final SliderSetting size = new SliderSetting("Дистанция", 85f, 60F, 100F, 5F).setVisible(() -> targetesp.is("Ромб"));
    public final SliderSetting distancee = new SliderSetting("Дистанция", 0.6f, 0.3F, 0.8F, 0.05F).setVisible(() -> targetesp.is("Призраки"));
    public final SliderSetting sizee = new SliderSetting("Размер", 0.3f, 0.1F, 0.3F, 0.05F).setVisible(() -> targetesp.is("Призраки"));
    public final SliderSetting distance = new SliderSetting("Длина", 9f, 1F, 20F, 1F).setVisible(() -> targetesp.is("Призраки"));
    public final SliderSetting alpha = new SliderSetting("Прозрачность", 6f, 0F, 20F, 1F).setVisible(() -> targetesp.is("Призраки"));
    public final SliderSetting speed = new SliderSetting("Скорость", 40f, 30F, 80F, 5F).setVisible(() -> targetesp.is("Призраки"));
    public final BooleanOption hitred = new BooleanOption("Краснеть при ударе", true).setVisible(() -> !targetesp.is("Кольцо"));

    public TargetEsp() {
        this.addSettings(targetesp, size, alpha, sizee, distance, speed, distancee, hitred);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}