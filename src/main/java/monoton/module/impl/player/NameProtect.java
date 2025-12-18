package monoton.module.impl.player;

import monoton.module.settings.imp.TextSetting;
import net.minecraft.client.Minecraft;
import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Module;
import monoton.module.api.Annotation;
import monoton.module.settings.imp.BooleanOption;

@Annotation(name = "NameProtect", type = TypeList.Player, desc = "Изменяет ваш реальный ник")
public class NameProtect extends Module {

    public BooleanOption friends = new BooleanOption("Друзья", false);
    public static TextSetting name = new TextSetting("Никнейм", "monotondlc");

    public NameProtect() {
        addSettings(name, friends);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }

    public String patch(String text) {
        String out = text;
        if (this.state) {
            out = text.replaceAll(Minecraft.getInstance().session.getUsername(), name.get());
        }
        return out;
    }

    public static void setNameProtect(String newName) {
        name.text = newName;
    }

    public static String getNameProtect() {
        return name.get();
    }
}