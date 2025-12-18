package monoton.module.impl.combat;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventEntityHitBox;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.SliderSetting;
import net.minecraft.entity.LivingEntity;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "HitBox",
        type = TypeList.Combat, desc = "Увеличивает хитбокс энтити"
)
public class HitBox extends Module {

    public final SliderSetting size = new SliderSetting("Размер", 0.2f, 0F, 1, 0.05F, 0.2f);
    public final BooleanOption showHitBox = new BooleanOption("Невидимые", false);

    public HitBox() {
        addSettings(size, showHitBox);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventEntityHitBox e) {
            handleEvent(e);
        }
        return false;
    }

    @Compile
    private void handleEvent(EventEntityHitBox event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        event.setSize(size.getValue().floatValue());
    }

    public boolean shouldShowHitBox() {
        return isState() && !showHitBox.get();
    }
}
