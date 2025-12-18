package monoton.module.impl.misc;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventDropItem;
import monoton.control.events.player.EventUpdate;
import monoton.control.events.render.EventHotbarRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.utils.font.Fonts;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;

import static monoton.utils.render.ColorUtils.rgba;

@Annotation(name = "LockSlot", type = TypeList.Misc, desc = "Отключает выброс предметов из выбранных слотов")
public class LockSlot extends Module {

    private final MultiBoxSetting lockSlots = new MultiBoxSetting("Блок слотов",
            new BooleanOption("Слот 1", true),
            new BooleanOption("Слот 2", false),
            new BooleanOption("Слот 3", false),
            new BooleanOption("Слот 4", false),
            new BooleanOption("Слот 5", false),
            new BooleanOption("Слот 6", false),
            new BooleanOption("Слот 7", false),
            new BooleanOption("Слот 8", false),
            new BooleanOption("Слот 9", false)
    );
    public static BooleanOption render = new BooleanOption("Отображать блок", true);

    public LockSlot() {
        addSettings(lockSlots, render);
    }

    @Override
    public boolean onEvent(Event event) {
        if (mc.player == null || mc.player.openContainer == null) return false;
        if (event instanceof EventDropItem e) {
            int currentSlot = mc.player.inventory.currentItem;

            if (lockSlots.get("Слот " + (currentSlot + 1))) {
                e.cancel();
            }
        }

        if (event instanceof EventHotbarRender.Post e && render.get()) {
            int centerX = mc.getMainWindow().getScaledWidth() / 2;
            int baseY = mc.getMainWindow().getScaledHeight() - 16 - 3;

            for (int i = 0; i < 9; i++) {
                if (!lockSlots.get(i)) continue;
                float iconWidth = Fonts.icon[11].getWidth("K");
                float x = centerX - 90 + i * 20 + (20f - iconWidth) / 2f - 4.5f;
                float y = baseY + 3.5f;
                Fonts.icon[11].drawString(e.getStack(), "K", x, y, rgba(205, 70, 70, 220));
            }
        }
        return false;
    }
}