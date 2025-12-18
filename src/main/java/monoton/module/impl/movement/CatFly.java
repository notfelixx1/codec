package monoton.module.impl.movement;

import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.util.text.TextFormatting;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.TypeList;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.other.OtherUtil;
import monoton.utils.misc.TimerUtil;
import monoton.utils.world.InventoryUtils;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(
        name = "CatFly",
        type = TypeList.Movement, desc = "Полёт использующий элитры при этом летя на нагруднике"
)
public class CatFly extends Module {
    private final TimerUtil timerUtil = new TimerUtil();
    private final TimerUtil timerUtil1 = new TimerUtil();
    private final TimerUtil timerUtil2 = new TimerUtil();
    private final SliderSetting timerStartFireWork = new SliderSetting("Задержка фейер", 4F, 1F, 15F, 1F);
    int oldItem = -1;

    public CatFly() {
        this.addSettings(timerStartFireWork);
    }

    @Compile
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) == -1) {
                return false;
            }
            int timeSwap = 550;
            for (int i = 0; i < 9; ++i) {
                if (mc.player.inventory.getStackInSlot(i).getItem() == Items.ELYTRA && !mc.player.isOnGround() && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.isElytraFlying()) {
                    if (this.timerUtil1.hasTimeElapsed((long) timeSwap)) {
                        this.timerUtil2.reset();
                        mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                        mc.player.startFallFlying();
                        mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                        mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                        this.oldItem = i;
                        this.timerUtil1.reset();
                    }

                    if (this.timerUtil.hasTimeElapsed(timerStartFireWork.getValue().intValue() * 40) && mc.player.isElytraFlying()) {
                        if (mc.player.isHandActive() && !mc.player.isBlocking()) return false;
                        this.useFirework();
                        this.timerUtil.reset();
                    }
                }
            }
        }
        return false;
    }

    @Compile
    private void useFirework() {
        if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) == -1) {
            OtherUtil.sendMessage(TextFormatting.RED + "У тебя нету фейерверков!");
        } else {
            InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
        }
    }

    @Compile
    public void onDisable() {
        super.onDisable();
        if (this.oldItem != -1) {
            if (((ItemStack) mc.player.inventory.armorInventory.get(2)).getItem() == Items.ELYTRA && mc.player.inventory.getStackInSlot(this.oldItem).getItem() instanceof ArmorItem) {
                mc.playerController.windowClick(0, 6, this.oldItem, ClickType.SWAP, mc.player);
            }

            this.oldItem = -1;
        }

        mc.gameSettings.keyBindSneak.setPressed(false);
    }
}
