package monoton.module.settings.imp;

import lombok.Getter;
import lombok.Setter;
import monoton.module.settings.Setting;
import net.minecraft.client.util.InputMappings;

import java.util.function.Supplier;

@Getter
@Setter
public class BindSetting extends Setting {

    private int key;

    public BindSetting(String name, int defaultKey) {
        super(name);
        key = defaultKey;
    }
    public BindSetting setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    @Override
    public SettingType getType() {
        return SettingType.BIND_SETTING;
    }

    public String getKeyName() {
        InputMappings.Input input = InputMappings.getInputByCode(key, 0);
        return input.getTranslationKey().replace("key.keyboard.", "").toUpperCase();
    }
}
