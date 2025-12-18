package monoton.module.impl.combat;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.module.settings.imp.SliderSetting;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.TridentItem;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "ItemRelease",
        type = TypeList.Combat, desc = "Автоматически выпускает предмет, когда он полностью натянут"
)
public class ItemRelease extends Module {

    private final MultiBoxSetting items = new MultiBoxSetting("Предметы",
            new BooleanOption("Лук", true),
            new BooleanOption("Трезубец", false),
            new BooleanOption("Арбалет", true));

    private final SliderSetting delay = new SliderSetting("Сила выстрела", 4.0F, 2.0F, 20.0F, 0.5F).setVisible(() -> items.get("Лук"));

    public ItemRelease() {
        this.addSettings(items, delay);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate e) {
            handleUpdateEvent(e);
        }
        return false;
    }

    @Compile
    private void handleUpdateEvent(EventUpdate e) {
        if (items.get("Лук")) {
            if (mc.player.inventory.getCurrentItem().getItem() instanceof BowItem) {
                if (mc.player.inventory.getCurrentItem().getItem() instanceof BowItem && mc.player.isHandActive() && mc.player.getItemInUseMaxCount() >= delay.getValue().floatValue()) {
                    mc.playerController.onStoppedUsingItem(mc.player);
                }
            }
        }

        if (items.get("Трезубец")) {
            if (mc.player.inventory.getCurrentItem().getItem() instanceof TridentItem) {
                if (mc.player.isHandActive() && mc.player.getItemInUseMaxCount() >= 10) {
                    mc.playerController.onStoppedUsingItem(mc.player);
                }
            }
        }

        if (items.get("Арбалет")) {
            if (mc.player.inventory.getCurrentItem().getItem() instanceof CrossbowItem) {
                if (mc.player.isHandActive() && mc.player.getItemInUseMaxCount() >= CrossbowItem.getChargeTime(mc.player.inventory.getCurrentItem())) {
                    mc.playerController.onStoppedUsingItem(mc.player);
                }
            }
        }
    }
}