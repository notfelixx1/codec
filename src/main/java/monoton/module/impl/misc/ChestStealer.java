package monoton.module.impl.misc;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Item;
import net.minecraft.util.math.vector.Vector2f;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.misc.TimerUtil;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.*;

@Annotation(name = "ChestStealer", type = TypeList.Misc, desc = "Пиздит ресурсы с сундука/мистика")
public class ChestStealer extends Module {

    boolean doClick = false;

    private final BooleanOption chestClose = new BooleanOption("Закрывать пустой", false);
    private final BooleanOption toggle = new BooleanOption("Выкл когда фул инв", false);
    private final SliderSetting stealDelay = new SliderSetting("Задержка лутания", 1, 0, 20, 1);
    public Vector2f rotate = new Vector2f(0, 0);


    private final TimerUtil timerUtil = new TimerUtil();

    public ChestStealer() {
        addSettings(chestClose, toggle, stealDelay);
    }

    @Compile
    private boolean isInventoryFull() {
        for (int i = 0; i < mc.player.inventory.mainInventory.size(); i++) {
            if (mc.player.inventory.mainInventory.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Compile
    private void open(ChestContainer container) {
        for (int r = 0; r < container.inventorySlots.size() / 1.3; ++r) {
            IInventory p = container.getLowerChestInventory();
            int index = new Random().nextInt(0, container.inventorySlots.size());
            if (index <= container.inventorySlots.size()) {
                if (timerUtil.hasTimeElapsed(stealDelay.getValue().longValue() - 1)) {
                    if (p.getStackInSlot(index).getItem() != Item.getItemById(0)) {
                        doClick = true;
                        if (doClick) {
                            mc.playerController.windowClick(container.windowId, index, 0, ClickType.QUICK_MOVE, mc.player);
                        } else {
                            timerUtil.reset();
                            continue;
                        }
                        timerUtil.reset();
                        continue;
                    } else {
                        timerUtil.reset();
                        continue;
                    }
                }
                if (toggle.get() && isInventoryFull()) {
                    mc.player.closeScreen();
                }
                if (container.getLowerChestInventory().isEmpty() && chestClose.get()) {
                    mc.player.closeScreen();
                }
            }
        }
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventUpdate) {
            if (mc.player.openContainer instanceof ChestContainer container) {
                open(container);
            }
        }
        return false;
    }
}
