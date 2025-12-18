package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;

@Annotation(name = "BetterChat", type = TypeList.Misc)
public class BetterChat extends Module {
    public final BooleanOption chatHistory = new BooleanOption("История чата", true);
    public final BooleanOption antiSpam = new BooleanOption("АнтиСпам в чате", true);

    public BetterChat() {
        addSettings(chatHistory, antiSpam);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
