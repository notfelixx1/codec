package monoton.module.impl.player;

import monoton.control.events.game.EventKey;
import monoton.module.settings.imp.BooleanOption;
import monoton.utils.other.OtherUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import monoton.control.events.client.Event;
import monoton.module.api.Module;
import monoton.module.api.Annotation;
import monoton.module.TypeList;
import monoton.module.settings.imp.BindSetting;
import monoton.utils.world.InventoryUtils;

@Annotation(name = "ClickPearl", type = TypeList.Player, desc = "Кидает пёрл при нажатие на клавишу")
public class ClickPearl extends Module {
    private BindSetting clickKey = new BindSetting("Кнопка", -98);
    private BooleanOption legit = new BooleanOption("Легитный", false);
    InventoryUtils.Hands handUtil = new InventoryUtils.Hands();
    long delay;

    public ClickPearl() {
        addSettings(clickKey, legit);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventKey e) {
            if (e.key == clickKey.getKey()) {
                handleMouseTickEvent();
            }
        }
        return false;
    }

    private void handleMouseTickEvent() {
        if (!mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL) && InventoryUtils.getItemSlot(Items.ENDER_PEARL) != -1) {
            int originalSlot = mc.player.inventory.currentItem;
            int pearlSlot = InventoryUtils.findItemSlot(Items.ENDER_PEARL);
            if (legit.get()) {
                if (pearlSlot > 8 || pearlSlot == -1) return;

                mc.player.inventory.currentItem = pearlSlot;
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        useItem(Hand.MAIN_HAND);
                    }
                }, 160);
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        mc.player.inventory.currentItem = originalSlot;
                    }
                }, 250);
            } else {
                if (OtherUtil.isConnectedToServer("funtime") || OtherUtil.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);

                    int hbSlot = findItem(Items.ENDER_PEARL, true);
                    int invSlot = findItem(Items.ENDER_PEARL, false);

                    if (Items.ENDER_PEARL != null) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                        }
                    }
                    useItem(Hand.MAIN_HAND);

                } else {
                    InventoryUtils.inventorySwapClick(Items.ENDER_PEARL, false);
                    useItem(Hand.MAIN_HAND);

                }
            }

        }
    }

    private int findItem(Item item, boolean hotbarOnly) {
        for (int i = hotbarOnly ? 0 : 9; i < (hotbarOnly ? 9 : 36); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findAndTrowItem(int hbSlot, int invSlot) {
        if (hbSlot != -1) {
            this.handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return hbSlot;
        }
        if (invSlot != -1) {
            handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.playerController.pickItem(invSlot);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return invSlot;
        }
        return -1;
    }


    private void useItem(Hand hand) {
        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
        mc.player.swingArm(hand);
    }
}

