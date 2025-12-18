package monoton.module.impl.player;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.game.EventKey;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BindSetting;
import monoton.module.settings.imp.BooleanOption;
import monoton.utils.misc.TimerUtil;
import monoton.utils.other.OtherUtil;
import monoton.utils.world.InventoryUtils;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.util.text.TextFormatting;

@Annotation(name = "ElytraHelper", type = TypeList.Player, desc = "Помогает свапать элитры с инвентаря")
public class ElytraHelper extends Module {
    private final BindSetting swapChestKey = new BindSetting("Элитры", 0);
    private final BindSetting fireWorkKey = new BindSetting("Фейерверк", 0);
    private final BooleanOption autoFly = new BooleanOption("Авто взлёт", true);
    private final BooleanOption autoJump = new BooleanOption("Авто прыжок", true);
    private final BooleanOption autofireWork = new BooleanOption("Авто фейерверк", false);
    public final BooleanOption swap = new BooleanOption("Фейр в левую руку", true);
    private final BooleanOption autofireWorkstart = new BooleanOption("Только при взлёте", false).setVisible(() -> autofireWork.get());
    ItemStack currentStack = ItemStack.EMPTY;
    private final TimerUtil stopWatch = new TimerUtil();
    boolean fireworkUsed;
    private final TimerUtil timerUtil = new TimerUtil();
    private boolean recentlySwapped = false;
    private final TimerUtil swapCooldownTimer = new TimerUtil();
    private boolean hasFiredOnStart = false;

    public ElytraHelper() {
        addSettings(swapChestKey, fireWorkKey, autoJump, autoFly, autofireWork, autofireWorkstart, swap);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (this.autoJump.get() && !mc.player.abilities.isFlying && mc.player.isOnGround() && ((ItemStack)mc.player.inventory.armorInventory.get(2)).getItem() == Items.ELYTRA && !mc.gameSettings.keyBindJump.isKeyDown() && !mc.player.isInWater() && !mc.player.isInLava()) {
                mc.player.jump();
            }

            if (this.autoFly.get() && !mc.player.abilities.isFlying && !mc.player.isInWater() && !mc.player.isOnGround() && !mc.player.isElytraFlying() && ((ItemStack)mc.player.inventory.armorInventory.get(2)).getItem() == Items.ELYTRA) {
                mc.player.startFallFlying();
                mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                if (autofireWork.get() && autofireWorkstart.get() && !hasFiredOnStart) {
                    if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) != -1) {
                        InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
                        hasFiredOnStart = true;
                    } else {
                        OtherUtil.sendMessage(TextFormatting.WHITE + "У вас не были найдены" + TextFormatting.RED + " фейерверки");
                    }
                }
            }

            if (mc.player.isOnGround() || mc.player.isInWater() || mc.player.isInLava()) {
                hasFiredOnStart = false;
            }

            if (mc.player.isElytraFlying() && autofireWork.get() && !autofireWorkstart.get() && timerUtil.hasTimeElapsed(570L)) {
                if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) != -1) {
                    InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
                } else {
                    OtherUtil.sendMessage(TextFormatting.WHITE + "У вас не были найдены" + TextFormatting.RED + " фейерверки");
                }
                timerUtil.reset();
            }

            this.currentStack = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST);

            if (recentlySwapped && swapCooldownTimer.hasTimeElapsed(2000L)) {
                recentlySwapped = false;
            }

            if (fireworkUsed) {
                useFirework();
                fireworkUsed = false;
            }
        }

        if (event instanceof EventKey e) {
            if (e.key == swapChestKey.getKey() && stopWatch.hasTimeElapsed(150L)) {
                if (getItemSlot(Items.ELYTRA) == -1) {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMoveFunction.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(2);
                    }
                    changeChestPlate(currentStack);
                    stopWatch.reset();
                } else {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMoveFunction.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(2);
                    }
                    changeChestPlate(currentStack);
                    stopWatch.reset();
                }
                recentlySwapped = true;
                swapCooldownTimer.reset();
            }

            if (e.key == fireWorkKey.getKey()) {
                fireworkUsed = true;
            }
        }
        return false;
    }

    private void changeChestPlate(ItemStack stack) {
        if (stack.getItem() != Items.ELYTRA) {
            int elytraSlot = getItemSlot(Items.ELYTRA);
            int freeSlot = findFreeInventorySlot();
            if (elytraSlot >= 0) {
                InventoryUtils.moveItem(elytraSlot, 6);
            } else if (freeSlot >= 0) {
            }
            return;
        }
        int armorSlot = getChestPlateSlot();
        int freeSlot = findFreeInventorySlot();
        if (armorSlot >= 0) {
            InventoryUtils.moveItem(armorSlot, 6);
        } else if (freeSlot >= 0) {
            InventoryUtils.moveItem(6, freeSlot);
        }
    }

    private int findFreeInventorySlot() {
        for (int i = 10; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int getChestPlateSlot() {
        Item[] items = {
                Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
                Items.IRON_CHESTPLATE, Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE
        };

        for (Item item : items) {
            for (int i = 0; i < 36; ++i) {
                Item stack = mc.player.inventory.getStackInSlot(i).getItem();
                if (stack == item) {
                    if (i < 9) {
                        i += 36;
                    }
                    return i;
                }
            }
        }
        return -1;
    }

    private int getItemSlot(Item item) {
        int finalSlot = -1;

        for (int i = 0; i < 36; ++i) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                finalSlot = i;
                break;
            }
        }

        if (finalSlot < 9 && finalSlot != -1) {
            finalSlot += 36;
        }

        return finalSlot;
    }

    private void useFirework() {
        if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) == -1) {
            if (mc.player.isElytraFlying()) {
                OtherUtil.sendMessage(TextFormatting.WHITE + "У вас не были найдены" + TextFormatting.RED + " фейерверки");
            }
        } else {
            InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
        }
    }

    @Override
    public void onDisable() {
        stopWatch.reset();
        timerUtil.reset();
        hasFiredOnStart = false;
        super.onDisable();
    }
}