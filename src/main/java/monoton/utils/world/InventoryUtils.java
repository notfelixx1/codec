package monoton.utils.world;

import lombok.Getter;
import monoton.module.impl.player.GuiMove;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import monoton.control.Manager;
import monoton.module.impl.combat.Aura;
import monoton.utils.IMinecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.stream.IntStream;

import static net.minecraft.inventory.InventoryHelper.getItemIndex;

public class InventoryUtils implements IMinecraft {
    public static UseType lastTypeUse;
    public static int oldItemHW = -1;
    public static int oldItem = -1;
    public static Item lastItem;
    public static boolean invSwap;
    public static int invSlot;
    public static BlockPos lastPosition;

    @Getter
    private static InventoryUtils instance = new InventoryUtils();




    public static boolean stackHasAnyEffect(ItemStack stack, boolean includeRegular, boolean includeSplash, boolean includeLingering, Effect... effects) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        boolean typeOk = (includeRegular && item == Items.POTION) || (includeSplash && item == Items.SPLASH_POTION) || (includeLingering && item == Items.LINGERING_POTION);
        if (!typeOk) return false;

        for (EffectInstance instance : PotionUtils.getEffectsFromStack(stack)) {
            for (Effect effect : effects) {
                if (instance.getPotion() == effect) return true;
            }
        }
        return false;
    }






    public static int getHotBarSlot(Item input) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == input) {
                return i;
            }
        }
        return -1;
    }

    public static int getItem(Item item, boolean hotbar) {
        for (int i = 0; i < (hotbar ? 9 : 45); ++i) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int getItemSlot(Item input) {
        for (ItemStack stack : mc.player.getArmorInventoryList()) {
            if (stack.getItem() == input) {
                return -2;
            }
        }

        ItemStack cursorStack = mc.player.inventory.getItemStack();
        if (!cursorStack.isEmpty() && cursorStack.getItem() == input) {
            return -3;
        }

        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s.getItem() == input) {
                slot = i;
                break;
            }
        }

        if (slot < 9 && slot != -1) {
            slot += 36;
        }

        return slot;
    }

    public static void swapHand(int slotId, Hand hand, boolean packet) {
        if (slotId == -1) return;
        int button = hand.equals(Hand.MAIN_HAND) ? mc.player.inventory.currentItem : 40;

        clickSlotId(slotId, button, ClickType.SWAP, packet);
    }

    public static void moveItem(int from, int to) {
        if (from == to || from == -1)
            return;

        from = from < 9 ? from + 36 : from;

        clickSlotId(from, 0, ClickType.SWAP, false);
        clickSlotId(to, 0, ClickType.SWAP, false);
        clickSlotId(from, 0, ClickType.SWAP, false);
    }

    public static void clickSlotId(int slotId, int buttonId, ClickType clickType, boolean packet) {
        clickSlotId(mc.player.openContainer.windowId, slotId, buttonId, clickType, packet);
    }

    public static void clickSlotId(int windowId, int slotId, int buttonId, ClickType clickType, boolean packet) {
        if (packet) {
            mc.player.connection.sendPacket(new CClickWindowPacket(windowId, slotId, buttonId, clickType, ItemStack.EMPTY, mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
        } else {
            mc.playerController.windowClick(windowId, slotId, buttonId, clickType, mc.player);
        }
    }

    public static void pickupItem(int slot, int button) {
        mc.playerController.windowClick(0, slot, button, ClickType.PICKUP, mc.player);
    }

    public static void dropItem(int slot) {
        mc.playerController.windowClick(0, slot, 0, ClickType.THROW, mc.player);
    }

    public static boolean doesHotbarHaveItem(Item item) {
        for(int i = 0; i < 9; ++i) {
            mc.player.inventory.getStackInSlot(i);
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                return true;
            }
        }

        return false;
    }

    public static void antipolet(Item item) {
        if (getItemIndex(item) != -1) {
            for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
                if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                    int originalSlot = i < 9 ? 36 + i : i;
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMoveFunction.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(2);
                    }

                    InventoryUtils.moveItem(originalSlot, 45);

                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            if (!mc.gameSettings.keyBindSneak.isKeyDown()) {
                                mc.gameSettings.keyBindSneak.setPressed(true);
                            }
                        }
                    }, 150);

                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                                mc.gameSettings.keyBindSneak.setPressed(false);
                            }
                        }
                    }, 200);

                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMoveFunction.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(2);
                    }

                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            InventoryUtils.moveItem(45, originalSlot);
                        }
                    }, 300);

                    break;
                }
            }
        }
    }

    public static void inventorySwapClick(Item item, boolean rotation) {
        if (getItemIndex(item) != -1) {
            int i;
            if (doesHotbarHaveItem(item)) {
                for (i = 0; i < 9; i++) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                        if (i != mc.player.inventory.currentItem) {
                            mc.player.connection.sendPacket(new CHeldItemChangePacket(i));
                        }

                        if (rotation && Manager.FUNCTION_MANAGER.auraFunction.target != null) {
                            mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(mc.player.rotationYaw, mc.player.rotationPitch, false));
                        }

                        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));

                        if (i != mc.player.inventory.currentItem) {
                            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                        }
                        break;
                    }
                }
            }


            if (!doesHotbarHaveItem(item)) {
                for (i = 0; i < 36; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {

                        mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);

                        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem % 8 + 1));

                        if (rotation && Aura.target != null) {
                            mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(mc.player.rotationYaw, mc.player.rotationPitch, false));
                        }

                        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));

                        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                        mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);
                        break;
                    }
                }
            }
        }
    }

    public static void inventorySwapClick(Item item, boolean rotation, UseType useType, PlayerEntity playerEntity, BlockPos position) {
        if (getItemIndex(item) != -1) {
                int slot = getItemSlot(item);
                if (Manager.FUNCTION_MANAGER.serverHelper.mode.is("HolyWorld") && oldItemHW == -1) {
                    boolean hotbar = false;

                    for (int i = 0; i < 9; ++i) {
                        if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                            oldItem = mc.player.inventory.currentItem;
                            lastItem = mc.player.inventory.getStackInSlot(i).getItem();
                            mc.player.inventory.currentItem = i;
                            lastTypeUse = useType;
                            lastPosition = position;
                            hotbar = true;
                            break;
                        }
                    }

                    if (!hotbar) {
                        for (int i = 0; i < 36; ++i) {
                            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                                mc.playerController.windowClick(0, i, mc.player.inventory.currentItem, ClickType.SWAP, mc.player);
                                invSwap = true;
                                invSlot = i;
                                break;
                            }
                        }
                    }
                }

                if (!Manager.FUNCTION_MANAGER.serverHelper.mode.is("HolyWorld")) {
                    int i;
                    if (doesHotbarHaveItem(item)) {
                        for (i = 0; i < 9; i++) {
                            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                                if (i != mc.player.inventory.currentItem) {
                                    mc.player.connection.sendPacket(new CHeldItemChangePacket(i));
                                }

                                use(useType, Hand.MAIN_HAND, position);

                                if (i != mc.player.inventory.currentItem) {
                                    mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                                }
                                break;
                            }
                        }
                    }


                    if (!doesHotbarHaveItem(item)) {
                        for (i = 0; i < 36; ++i) {
                            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {

                                mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);

                                mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem % 8 + 1));

                                use(useType, Hand.MAIN_HAND, position);

                                mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                                mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);
                                break;
                            }
                        }
                    }
                }
            }
    }

    public static void use(UseType useType, Hand hand, BlockPos position) {
        if (useType == InventoryUtils.UseType.USE_ITEM) {
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
        }

        if (useType == InventoryUtils.UseType.ATTACK) {
            mc.playerController.attackEntity(mc.player, Aura.target);
        }

        if (useType == InventoryUtils.UseType.PLACE_BLOCK) {
            BlockRayTraceResult rayTraceResult = new BlockRayTraceResult(new Vector3d((double) ((float) position.getX() + 0.5F), (double) ((float) position.getY() + 0.5F), (double) ((float) position.getZ() + 0.5F)), Direction.UP, position, false);
            if (mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, rayTraceResult) == ActionResultType.SUCCESS) {
                mc.player.swingArm(Hand.MAIN_HAND);
            }
        }

    }

    public static void inventorySwapClickFF(Item item, boolean rotation) {
        if (getItemIndex(item) != -1) {
            int i;
            if (doesHotbarHaveItem(item)) {
                for (i = 0; i < 9; i++) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                        if (Manager.FUNCTION_MANAGER.elytraHelper.state && Manager.FUNCTION_MANAGER.elytraHelper.swap.get() && mc.player.isHandActive() && !mc.player.isBlocking()) {
                            if (i != mc.player.inventory.currentItem) {
                                int originalSlot = i;
                                InventoryUtils.moveItem(i, 45);
                                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
                                InventoryUtils.moveItem(originalSlot, 45);
                            }
                        } else {
                            if (i != mc.player.inventory.currentItem) {
                                mc.player.connection.sendPacket(new CHeldItemChangePacket(i));
                            }
                            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                            if (i != mc.player.inventory.currentItem) {
                                mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                            }
                        }
                        break;
                    }
                }
            } else {
                for (i = 0; i < 36; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                        if (Manager.FUNCTION_MANAGER.elytraHelper.state && Manager.FUNCTION_MANAGER.elytraHelper.swap.get() && mc.player.isHandActive() && !mc.player.isBlocking()) {
                            if (i != mc.player.inventory.currentItem) {
                                int originalSlot = i;
                                InventoryUtils.moveItem(i, 45);
                                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
                                InventoryUtils.moveItem(originalSlot, 45);
                            }
                        } else {
                            mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);
                            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem % 8 + 1));
                            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                            mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);
                        }
                        break;
                    }
                }
            }
        }
    }


    public static boolean hasItem(Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    public static int findItemSlot(Item item) {
        if (item == null) return -1;

        for (int i = 0; i < 9; i++) {
            if (Minecraft.getInstance().player.inventory.getStackInSlot(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findChestPlate() {
        Item[] items = {Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE};
        for (Item item : items) {
            int slot = InventoryUtils.findItemSlot(item);
            if (slot != -1) {
                return slot;
            }
        }
        return -1;
    }


    public static int getSlot(Item item) {
        if (mc == null || mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static class Hands {
        public static boolean isEnabled;
        private boolean isChangingItem;
        private int originalSlot = -1;

        public void handleItemChange(boolean resetItem) {
            if (this.isChangingItem && this.originalSlot != -1) {
                isEnabled = true;
                mc.player.inventory.currentItem = this.originalSlot;
                if (resetItem) {
                    this.isChangingItem = false;
                    this.originalSlot = -1;
                    isEnabled = false;
                }
            }
        }

        public void setOriginalSlot(int slot) {
            this.originalSlot = slot;
        }
    }

    public static enum UseType {
        ATTACK,
        USE_ITEM,
        PLACE_BLOCK;
    }
}
