package monoton.module.settings.imp;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import monoton.module.settings.Setting;

import java.util.function.Supplier;

public class SliderSetting extends Setting {
    private float value;
    @Getter
    private final float min;
    @Getter
    private final float max;
    @Getter
    private final float increment;
    @Getter
    private final float safeMax;

    public SliderSetting(String name, float value, float min, float max, float increment, float safeMax) {
        super(name);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.safeMax = safeMax;
    }

    public SliderSetting(String name, float value, float min, float max, float increment) {
        this(name, value, min, max, increment, max);
    }

    public SliderSetting setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    public Number getValue() {
        return MathHelper.clamp(value, getMin(), getMax());
    }

    public void setValue(float value) {
        this.value = MathHelper.clamp(value, getMin(), getMax());
    }

    public boolean isSafe() {
        return value <= safeMax;
    }

    @Override
    public SettingType getType() {
        return SettingType.SLIDER_SETTING;
    }
}