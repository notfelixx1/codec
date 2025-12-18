package monoton.module.impl.combat;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventCrystalEntity;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.impl.player.GuiMove;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.misc.TimerUtil;
import monoton.utils.world.InventoryUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.item.minecart.TNTMinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.*;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Annotation(name = "AutoTotem",
        type = TypeList.Combat, desc = "Берёт тотемы при низком значение здоровья"
)
public class AutoTotem extends Module {
    private final SliderSetting health = new SliderSetting("Здоровье", 5.0f, 1.0f, 20.0f, 0.5f);
    private final SliderSetting healthelytra = new SliderSetting("Здоровье на элитрах", 9f, 1.0f, 20.0f, 0.5f);
    private final SliderSetting healthbronya = new SliderSetting("Без полной брони", 8f, 1.0f, 20.0F, 0.5F);
    private final MultiBoxSetting mode = new MultiBoxSetting("Проверки на", new BooleanOption[]{
            new BooleanOption("Золотые сердца", true),
            new BooleanOption("Кристаллы", true),
            new BooleanOption("Падение", true),
            new BooleanOption("Кристалл в руке", true)
    });
    private final TimerUtil stopWatch = new TimerUtil();
    private Item backItem = Items.AIR;
    private ItemStack backItemStack;
    private int nonEnchantedTotems;
    private boolean totemIsUsed;
    private int itemInMouse = -1;
    private int totemCount = 0;
    ItemStack currentStack = ItemStack.EMPTY;
    private int oldItem = -1;
    private final BooleanOption swapBack = new BooleanOption("Возвращать предмет", true);
    private final BooleanOption noBallSwitch = new BooleanOption("Не сменять шар", false);
    private final BooleanOption saveEnchanted = new BooleanOption("Сохранять зачарованный", true);

    public AutoTotem() {
        addSettings(mode, health, healthelytra, healthbronya, swapBack, noBallSwitch, saveEnchanted);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventCrystalEntity) {
            handleCrystalEvent((EventCrystalEntity) event);
        } else if (event instanceof EventUpdate) {
            handleUpdateEvent();
        } else if (event instanceof EventPacket) {
            handlePacketEvent((EventPacket) event);
        }
        return false;
    }

    private void handleCrystalEvent(EventCrystalEntity event) {
        Entity entity = event.getEntity();
        if (entity instanceof EnderCrystalEntity && swapBack.get() && entity.getDistance(mc.player) <= 6.0f) {
            swapToTotem();
        }
    }

    private void handleUpdateEvent() {
        totemCount = countTotems(true);
        nonEnchantedTotems = (int) IntStream.range(0, 36)
                .mapToObj(i -> mc.player.inventory.getStackInSlot(i))
                .filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING && !s.isEnchanted())
                .count();

        int slot = getSlotInInventory();
        boolean handNotNull = !(mc.player.getHeldItemOffhand().getItem() instanceof AirItem);

        if (shouldToSwapTotem()) {
            if (slot != -1 && !isTotemInHands()) {
                if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMoveFunction.state && !GuiMove.mode.is("Vanila")) {
                    GuiMove.stopMovementTemporarily(2);
                }
                InventoryUtils.swapHand(slot, Hand.OFF_HAND, false);
                if (handNotNull && oldItem == -1) {
                    oldItem = slot;
                }
            }
        } else if (oldItem != -1) {
            if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMoveFunction.state && !GuiMove.mode.is("Vanila")) {
                GuiMove.stopMovementTemporarily(2);
            }
            InventoryUtils.swapHand(oldItem, Hand.OFF_HAND, false);
            oldItem = -1;
        }
    }

    private void handlePacketEvent(EventPacket event) {
        if (event.isReceivePacket()) {
            IPacket packet = event.getPacket();
            if (packet instanceof SEntityStatusPacket statusPacket) {
                if (statusPacket.getOpCode() == 35 && statusPacket.getEntity(mc.world) == mc.player) {
                    totemIsUsed = true;
                }
            }
        }
    }

    private void swapToTotem() {
        int totemSlot = getSlotInInventory();
        if (totemSlot == -1 || isTotemInHands()) return;

        Item currentOffhand = mc.player.getHeldItemOffhand().getItem();
        if (currentOffhand == Items.TOTEM_OF_UNDYING) return;

        if (itemInMouse == -1) {
            itemInMouse = totemSlot;
            backItem = currentOffhand;
            backItemStack = mc.player.getHeldItemOffhand().copy();
        }
        if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMoveFunction.state && !GuiMove.mode.is("Vanila")) {
            GuiMove.stopMovementTemporarily(2);
        }
        mc.playerController.windowClick(mc.player.container.windowId, totemSlot, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(mc.player.container.windowId, Hand.OFF_HAND.ordinal(), 0, ClickType.PICKUP, mc.player);

        syncOffhandToServer();

        if (totemCount > 1 && totemIsUsed) {
            backItemInMouse();
            totemIsUsed = false;
        } else if (backItem != Items.AIR) {
            scheduledSwapBack();
        }

        stopWatch.reset();
    }

    private void syncOffhandToServer() {
        mc.getConnection().sendPacket(new CPlayerDiggingPacket(
                CPlayerDiggingPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO,
                Direction.DOWN
        ));

        mc.getConnection().sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
    }

    private void scheduledSwapBack() {
        if (backItem == Items.AIR || !swapBack.get()) return;

        mc.execute(() -> {
            if (mc.player == null || mc.world == null) return;

            int backSlot = findItemInHotbar(backItem);
            if (backSlot == -1) {
                backSlot = findItemInInventory(backItem);
            }

            if (backSlot != -1) {
                mc.playerController.windowClick(mc.player.container.windowId, backSlot, 0, ClickType.PICKUP, mc.player);
                mc.playerController.windowClick(mc.player.container.windowId, Hand.OFF_HAND.ordinal(), 0, ClickType.PICKUP, mc.player);
                syncOffhandToServer();
            }

            itemInMouse = -1;
            backItem = Items.AIR;
        });
    }
    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                return i + 36;
            }
        }
        return -1;
    }

    private int findItemInInventory(Item item) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }
    private int countTotems(boolean includeEnchanted) {
        return (int) IntStream.range(0, mc.player.inventory.getSizeInventory())
                .mapToObj(i -> mc.player.inventory.getStackInSlot(i))
                .filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING && (includeEnchanted || !s.isEnchanted()))
                .count();
    }

    private void backItemInMouse() {
        if (itemInMouse != -1) {
            mc.playerController.windowClick(mc.player.container.windowId, itemInMouse, 0, ClickType.PICKUP, mc.player);
        }
    }

    private boolean isTotemInHands() {
        Hand[] hands = Hand.values();

        for (Hand hand : hands) {
            ItemStack heldItem = mc.player.getHeldItem(hand);
            if (heldItem.getItem() == Items.TOTEM_OF_UNDYING && !isSaveEnchanted(heldItem)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSaveEnchanted(ItemStack itemStack) {
        return saveEnchanted.get() && itemStack.isEnchanted() && nonEnchantedTotems > 0;
    }

    private boolean shouldToSwapTotem() {
        this.currentStack = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        final float absorptionAmount = mc.player.isPotionActive(Effects.ABSORPTION) ? mc.player.getAbsorptionAmount() : 0.0f;
        float currentHealth = mc.player.getHealth();
        if (this.mode.get(0)) {
            currentHealth += absorptionAmount;
        }

        boolean hasFullArmor = true;
        for (int i = 0; i < mc.player.inventory.armorInventory.size(); i++) {
            ItemStack armor = mc.player.inventory.armorInventory.get(i);
            if (armor.isEmpty()) {
                if (i == 3 && hasJackHeadInInventory()) {
                    continue;
                }
                hasFullArmor = false;
                break;
            }
        }

        float healthThreshold = hasFullArmor ? this.health.getValue().floatValue() : this.healthbronya.getValue().floatValue();

        return (!this.isOffhandItemBall() && this.isInDangerousSituation()) ||
                currentHealth <= (currentStack.getItem() == Items.ELYTRA ? healthelytra.getValue().floatValue() : healthThreshold) ||
                checkFall();
    }

    private boolean isInDangerousSituation() {
        return checkCrystal() || checkPlayerWithCrystalNearObsidian();
    }

    private boolean checkFall() {
        if (!this.mode.get(2)) {
            return false;
        }
        if (mc.player.isInWater() || mc.player.isElytraFlying()) {
            return false;
        }
        float fallDistance = mc.player.fallDistance;
        float fallDamage = calculateFallDamage(fallDistance);

        float currentHealth = mc.player.getHealth();

        return fallDamage >= currentHealth / 1.92f;
    }

    private float calculateFallDamage(float fallDistance) {
        if (fallDistance <= 3.0f) return 0;

        float fallDamage = (fallDistance - 3.0f) / 2;

        float armorReduction = 0;
        for (ItemStack armor : mc.player.inventory.armorInventory) {
            if (armor.getItem() instanceof ArmorItem) {
                armorReduction += ((ArmorItem) armor.getItem()).getDamageReductionAmount();
            }
        }

        ItemStack boots = mc.player.inventory.armorInventory.get(0);
        if (boots.getItem() instanceof ArmorItem) {
            int featherFallingLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FEATHER_FALLING, boots);
            if (featherFallingLevel > 0) {
                float reductionFactor = 1.0f - (Math.min(featherFallingLevel, 4) * 0.171f);
                fallDamage *= reductionFactor;
            }
        }

        if (hasProtectionAura()) {
            fallDamage *= 0.2f;
        }

        float absorption = mc.player.isPotionActive(Effects.ABSORPTION) ? mc.player.getAbsorptionAmount() : 0.0f;
        fallDamage = Math.max(0, fallDamage - absorption);

        return Math.min(fallDamage, mc.player.getMaxHealth());
    }

    private boolean hasProtectionAura() {
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.hasDisplayName() && "Аура Защиты От Падения".equals(stack.getDisplayName().getString())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCrystal() {
        if (!mode.get(1))
            return false;

        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof EnderCrystalEntity && mc.player.getDistance(entity) <= 6.0f) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPlayerWithCrystalNearObsidian() {
        if (!mode.get(3))
            return false;

        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof PlayerEntity && entity != mc.player && mc.player.getDistance(entity) <= 5.0f) {
                PlayerEntity otherPlayer = (PlayerEntity) entity;
                ItemStack mainHand = otherPlayer.getHeldItemMainhand();
                ItemStack offHand = otherPlayer.getHeldItemOffhand();
                if (mainHand.getItem() == Items.END_CRYSTAL || offHand.getItem() == Items.END_CRYSTAL) {
                    BlockPos obsidianPos = getBlock(5.0f, Blocks.OBSIDIAN);
                    if (obsidianPos != null && getDistanceOfEntityToBlock(otherPlayer, obsidianPos) <= 5.0f) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOffhandItemBall() {
        return noBallSwitch.get() && mc.player.getHeldItemOffhand().getItem() == Items.PLAYER_HEAD;
    }

    private BlockPos getBlock(float distance, Block block) {
        return getSphere(getPlayerPosLocal(), distance, 6, false, true, 0).stream()
                .filter(position -> mc.world.getBlockState(position).getBlock() == block)
                .min(Comparator.comparing(blockPos -> getDistanceOfEntityToBlock(mc.player, blockPos)))
                .orElse(null);
    }

    private List<BlockPos> getSphere(BlockPos center, float radius, int height, boolean hollow, boolean fromBottom, int yOffset) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        for (int x = centerX - (int) radius; x <= centerX + radius; ++x) {
            for (int z = centerZ - (int) radius; z <= centerZ + radius; ++z) {
                int yStart = fromBottom ? (centerY - (int) radius) : centerY;
                for (int yEnd = fromBottom ? (centerY + (int) radius) : (centerY + height), y = yStart; y < yEnd; ++y) {
                    if (isPositionWithinSphere(centerX, centerY, centerZ, x, y, z, radius, hollow)) {
                        positions.add(new BlockPos(x, y + yOffset, z));
                    }
                }
            }
        }
        return positions;
    }

    private BlockPos getPlayerPosLocal() {
        if (mc.player == null) {
            return BlockPos.ZERO;
        }
        return new BlockPos(Math.floor(mc.player.getPosX()), Math.floor(mc.player.getPosY()), Math.floor(mc.player.getPosZ()));
    }

    private double getDistanceOfEntityToBlock(Entity entity, BlockPos blockPos) {
        return getDistance(entity.getPosX(), entity.getPosY(), entity.getPosZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    private double getDistance(double n, double n2, double n3, double n4, double n5, double n6) {
        double n7 = n - n4;
        double n8 = n2 - n5;
        double n9 = n3 - n6;
        return MathHelper.sqrt(n7 * n7 + n8 * n8 + n9 * n9);
    }

    private static boolean isPositionWithinSphere(int centerX, int centerY, int centerZ, int x, int y, int z, float radius, boolean hollow) {
        double distanceSq = Math.pow(centerX - x, 2.0) + Math.pow(centerZ - z, 2.0) + Math.pow(centerY - y, 2.0);
        return distanceSq < Math.pow(radius, 2.0) && (!hollow || distanceSq >= Math.pow(radius - 1.0f, 2.0));
    }

    private int getSlotInInventory() {
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (itemStack.getItem() == Items.TOTEM_OF_UNDYING && !isSaveEnchanted(itemStack)) {
                return adjustSlotNumber(i);
            }
        }
        return -1;
    }

    private int adjustSlotNumber(int slot) {
        return (slot < 9) ? (slot + 36) : slot;
    }

    private boolean hasJackHeadInInventory() {
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == Items.PLAYER_HEAD && stack.hasDisplayName()) {
                String displayName = stack.getDisplayName().getString();
                if ("Голова Джека".equals(displayName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onDisable() {
        oldItem = -1;
        super.onDisable();
    }
}