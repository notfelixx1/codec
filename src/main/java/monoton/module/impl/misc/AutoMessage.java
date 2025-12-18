package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.TypeList;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.module.settings.imp.TextSetting;


@Annotation(
        name = "AutoMessage",
        type = TypeList.Player,
        desc = "Автоматически выписывает после убиства таргета"
)
public class AutoMessage extends Module {
    public static final MultiBoxSetting modes = new MultiBoxSetting("Выбор целей",
            new BooleanOption("Кидай шар", true),
            new BooleanOption("Убил езку", true)
    );
    public SliderSetting health = new SliderSetting("Уровень хп", 6f, 1f, 20f, 1f).setVisible(() -> modes.get("Кидай шар"));

    public static TextSetting killmsg = new TextSetting("Убил", "-ник ez").setVisible(() -> modes.get("Убил езку"));

    public AutoMessage() {
        this.addSettings(modes, health, killmsg);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
