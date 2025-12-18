package monoton.module.settings.imp;

import lombok.Getter;
import lombok.Setter;
import monoton.module.settings.Setting;

import java.util.function.Supplier;

@Getter
@Setter
public class ButtonSetting extends Setting {

    private Runnable run;
    private String toggleText;
    private boolean isToggled;

    public ButtonSetting(String name, Runnable run) {
        super(name);
        this.run = run;
        this.isToggled = false;
    }

    public ButtonSetting(String name, String toggleText, Runnable run) {
        super(name);
        this.run = run;
        this.toggleText = toggleText;
        this.isToggled = false;
    }

    public ButtonSetting setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    public void toggle() {
        if (toggleText != null) {
            isToggled = !isToggled;
        }
    }

    public String getCurrentToggleText() {
        if (toggleText == null) {
            return name;
        }
        return isToggled ? toggleText : name;
    }

    @Override
    public SettingType getType() {
        return SettingType.BUTTON_SETTING;
    }
}