package monoton.module.impl.misc;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.game.EventKey;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.impl.player.GuiMove;
import monoton.module.settings.imp.*;
import monoton.utils.misc.TimerUtil;
import monoton.utils.world.InventoryUtils;
import net.minecraft.item.*;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

@Annotation(name = "AutoSwap", type = TypeList.Misc, desc = "Авто свап предметов левой руки")
public class AutoSwap extends Module {
    private final MultiBoxSetting mode = new MultiBoxSetting("Свап", new BooleanOption[]{new BooleanOption("Рука", true), new BooleanOption("Голова", false)});
    public InfoSetting left = new InfoSetting("Левая рука", () -> {
    }).setVisible(() -> mode.get("Рука"));
    private final BindSetting swapKey = new BindSetting("Свап", 0).setVisible(() -> mode.get("Рука"));
    private final ModeSetting itemType = new ModeSetting("Свапать с", "Щит", "Щит", "Геплы", "Тотем", "Шар ").setVisible(() -> mode.get("Рука"));
    private final ModeSetting swapType = new ModeSetting("Свапать на", "Геплы", "Щит", "Геплы", "Тотем", "Шар ").setVisible(() -> mode.get("Рука"));
    public InfoSetting head = new InfoSetting("Голова", () -> {
    }).setVisible(() -> mode.get("Голова"));
    private final BindSetting swapKey2 = new BindSetting("Свaп", 0).setVisible(() -> mode.get("Голова"));
    private final ModeSetting itemType2 = new ModeSetting("Свапать c", "Шар ", "Шлем", "Шар ").setVisible(() -> mode.get("Голова"));
    private final ModeSetting swapType2 = new ModeSetting("Свапать нa", "Шлем", "Шлем", "Шар ").setVisible(() -> mode.get("Голова"));

    private boolean waitingForSwap = false;
    private int targetSlot = -1;
    private int targetInventorySlot = -1;

    private final TimerUtil stopWatch = new TimerUtil();
    private final TimerUtil attackSpeedTimer = new TimerUtil();
    private boolean hadAttackSpeedRecently = false;
    public final BooleanOption totemswap = new BooleanOption("Свап топора на меч", true);
    public final BooleanOption notif = new BooleanOption("Отображать свап предмов", true);
    private int axeSlot = -1;

    public AutoSwap() {
        addSettings(mode, totemswap, left, swapKey, itemType, swapType, head, swapKey2, itemType2, swapType2, notif);
    }

    private ItemStack lastOffhandStack = null;
    private boolean lastAttackSpeedResult = false;
    private boolean isSwapping = false;

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventKey e) {
            if (mode.get("Рука") && e.key == swapKey.getKey() && stopWatch.isReached(300) && !isSwapping && !waitingForSwap) {
                isSwapping = true;

                ItemStack offhandItemStack = mc.player.getHeldItemOffhand();
                Item selectedItem = getSelectedItem();
                Item swapItem = getSwapItem();

                int fromSlot = -1;
                int toSlot = 45;

                if (offhandItemStack.getItem() == selectedItem) {
                    fromSlot = getSlot(swapItem);
                } else {
                    fromSlot = getSlot(selectedItem);
                }

                if (fromSlot >= 0 && swapItem != Items.AIR && selectedItem != Items.AIR) {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMoveFunction.state && !GuiMove.mode.is("Vanila")) {
                        waitingForSwap = true;
                        targetSlot = fromSlot;
                        targetInventorySlot = toSlot;

                        GuiMove.stopMovementTemporarily(2);

                        new Thread(() -> {
                            try {
                                Thread.sleep(40);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }

                            mc.execute(() -> {
                                if (mc.player != null && waitingForSwap) {
                                    InventoryUtils.moveItem(targetSlot, targetInventorySlot);
                                    stopWatch.reset();

                                    ItemStack newItem = targetInventorySlot == 45
                                            ? mc.player.getHeldItemOffhand()
                                            : mc.player.inventory.armorInventory.get(3);

                                    if (notif.get() && newItem.getItem() != Items.AIR) {
                                        Manager.NOTIFICATION_MANAGER.add(
                                                newItem.getDisplayName().getString(),
                                                "Function Debug",
                                                3,
                                                this.category,
                                                newItem.getDisplayName()
                                        );
                                    }

                                    waitingForSwap = false;
                                    isSwapping = false;
                                }
                            });
                        }).start();
                    } else {
                        InventoryUtils.moveItem(fromSlot, toSlot);
                        stopWatch.reset();
                        notifySwap(toSlot == 45 ? mc.player.getHeldItemOffhand() : mc.player.inventory.armorInventory.get(3));
                        isSwapping = false;
                    }
                } else {
                    isSwapping = false;
                }
            }

            if (mode.get("Голова") && e.key == swapKey2.getKey() && stopWatch.isReached(300) && !isSwapping && !waitingForSwap) {
                isSwapping = true;

                ItemStack helmetItemStack = mc.player.inventory.armorInventory.get(3);
                Item selectedItem = getSelectedItem2();
                Item swapItem = getSwapItem2();

                int fromSlot = -1;
                int toSlot = 5;

                if (helmetItemStack.getItem() == selectedItem) {
                    fromSlot = getSlot(swapItem);
                } else {
                    fromSlot = getSlot(selectedItem);
                }

                if (fromSlot >= 0 && swapItem != Items.AIR && selectedItem != Items.AIR) {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMoveFunction.state && !GuiMove.mode.is("Vanila")) {
                        waitingForSwap = true;
                        targetSlot = fromSlot;
                        targetInventorySlot = toSlot;

                        GuiMove.stopMovementTemporarily(3);

                        new Thread(() -> {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }

                            mc.execute(() -> {
                                if (mc.player != null && waitingForSwap) {
                                    InventoryUtils.moveItem(targetSlot, targetInventorySlot);
                                    stopWatch.reset();

                                    ItemStack newItem = mc.player.inventory.armorInventory.get(3);
                                    if (notif.get() && newItem.getItem() != Items.AIR) {
                                        Manager.NOTIFICATION_MANAGER.add(
                                                newItem.getDisplayName().getString(),
                                                "Function Debug",
                                                3,
                                                this.category,
                                                newItem.getDisplayName()
                                        );
                                    }

                                    waitingForSwap = false;
                                    isSwapping = false;
                                }
                            });
                        }).start();
                    } else {
                        InventoryUtils.moveItem(fromSlot, toSlot);
                        stopWatch.reset();
                        notifySwap(mc.player.inventory.armorInventory.get(3));
                        isSwapping = false;
                    }
                } else {
                    isSwapping = false;
                }
            }
        }

        if (mc.player.ticksExisted % 0.5f == 0) {
            ItemStack offhandItemStack = mc.player.getHeldItemOffhand();
            if (offhandItemStack != lastOffhandStack) {
                lastAttackSpeedResult = hasAttackSpeedDescription(offhandItemStack);
                lastOffhandStack = offhandItemStack;
            }

            if (lastAttackSpeedResult) {
                hadAttackSpeedRecently = true;
                attackSpeedTimer.reset();
            } else if (attackSpeedTimer.isReached(500)) {
                hadAttackSpeedRecently = false;
            }

            if (totemswap.get() && stopWatch.isReached(300)) {
                ItemStack mainHandItemStack = mc.player.getHeldItemMainhand();
                if (mainHandItemStack.getItem() instanceof SwordItem &&
                        offhandItemStack.getItem() == Items.PLAYER_HEAD &&
                        lastAttackSpeedResult &&
                        stopWatch.isReached(300)) {
                    int axeSlot = getAxeSlot();
                    if (axeSlot != -1) {
                        mc.player.inventory.currentItem = axeSlot;
                        stopWatch.reset();
                    }
                }

                boolean hasAxeInHotbar = getAxeSlot() != -1;
                boolean hasSwordInHotbar = getSwordSlot() != -1;

                if (hasAxeInHotbar && hasSwordInHotbar) {
                    if (!lastAttackSpeedResult && hadAttackSpeedRecently && mainHandItemStack.getItem() instanceof AxeItem) {
                        axeSlot = mc.player.inventory.currentItem;
                        int swordSlot = getSwordSlot();
                        if (swordSlot != -1) {
                            mc.player.inventory.currentItem = swordSlot;
                            stopWatch.reset();
                        }
                    } else if (lastAttackSpeedResult && mainHandItemStack.getItem() instanceof SwordItem && axeSlot != -1) {
                        int axeCheckSlot = getAxeSlot();
                        if (axeCheckSlot != -1) {
                            mc.player.inventory.currentItem = axeCheckSlot;
                            axeSlot = -1;
                            stopWatch.reset();
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void onDisable() {
        axeSlot = -1;
        hadAttackSpeedRecently = false;
        super.onDisable();
    }

    private void notifySwap(ItemStack stack) {
        if (notif.get() && stack.getItem() != Items.AIR) {
            Manager.NOTIFICATION_MANAGER.add(
                    stack.getDisplayName().getString(),
                    "Function Debug",
                    3,
                    this.category,
                    stack.getDisplayName()
            );
        }
    }

    private Item getSwapItem() {
        return getItemByType(swapType.get());
    }

    private Item getSelectedItem() {
        return getItemByType(itemType.get());
    }

    private Item getSwapItem2() {
        return getItemByType(swapType2.get());
    }

    private Item getSelectedItem2() {
        return getItemByType(itemType2.get());
    }

    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Щит" -> Items.SHIELD;
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Геплы" -> Items.GOLDEN_APPLE;
            case "Шар " -> Items.PLAYER_HEAD;
            case "Шлем" -> Items.NETHERITE_HELMET;
            default -> Items.AIR;
        };
    }

    private int getSlot(Item item) {
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

    private int getSwordSlot() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof SwordItem) {
                return i;
            }
        }
        return -1;
    }

    private int getAxeSlot() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }



    private boolean hasAttackSpeedDescription(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        if (itemStack.hasDisplayName() && itemStack.getDisplayName().getString().contains("Кобры")) {
            return true;
        }
        List<ITextComponent> tooltip = itemStack.getTooltip(mc.player, net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL);
        for (ITextComponent component : tooltip) {
            if (component.getString().contains("Скорости Атаки") || component.getString().contains("Скорость атаки") || component.getString().contains("Attack Speed")) {
                return true;
            }
        }
        return false;
    }
}