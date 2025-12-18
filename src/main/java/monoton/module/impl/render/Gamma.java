package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.SliderSetting;

@Annotation(name = "Gamma", type = TypeList.Render, desc = "Динамическое управление освещением в зависимости от времени суток")
public class Gamma extends Module {

    public SliderSetting gammaSlider = new SliderSetting("Яркость", 10.0f, 1.0f, 10.0f, 1f);

    public Gamma() {
        addSettings(gammaSlider);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            applyLightingMode();
        }
        return false;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
    }

    private void applyLightingMode() {
        resetGamma();
        applyDynamicLighting();
    }

    private void applyDynamicLighting() {
        mc.gameSettings.gamma = gammaSlider.getValue().floatValue();
    }


    @Override
    protected void onDisable() {
        resetGamma();
        super.onDisable();
    }

    private void resetGamma() {
        mc.gameSettings.gamma = 1.0f;
    }
}
