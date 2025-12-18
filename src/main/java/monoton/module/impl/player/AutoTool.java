package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.control.events.player.EventAttack;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;

@Annotation(name = "AutoTool", type = TypeList.Player, desc = "Автоматически берет нужный предмет или меч")
public class AutoTool extends Module {
    public static BooleanOption sword = new BooleanOption("Брать меч из хотбара", true);
    private int oldSlot = -1;
    private boolean status;
    private long lastActionTime = 0;
    private static final long DELAY_MS = 300;

    public AutoTool() {
        addSettings(sword);
    }

    @Override
    public boolean onEvent(Event event) {
        if (mc.player != null && mc.world != null && mc.player.connection != null) {
            if (event instanceof EventUpdate) {
                if (mc.objectMouseOver != null && mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver instanceof BlockRayTraceResult) {
                    BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) mc.objectMouseOver;
                    Block block = mc.world.getBlockState(blockRayTraceResult.getPos()).getBlock();
                    if (block != Blocks.AIR) {
                        int bestSlot = this.findBestSlot();
                        if (bestSlot == -1) {
                            return false;
                        }

                        this.status = true;
                        this.lastActionTime = System.currentTimeMillis();
                        if (this.oldSlot == -1) {
                            this.oldSlot = mc.player.inventory.currentItem;
                        }
                        mc.player.inventory.currentItem = bestSlot;
                    } else if (this.status && System.currentTimeMillis() - this.lastActionTime >= DELAY_MS) {
                        if (this.oldSlot != -1) {
                            mc.player.inventory.currentItem = this.oldSlot;
                        }
                        this.reset();
                    }
                } else if (this.status && System.currentTimeMillis() - this.lastActionTime >= DELAY_MS) {
                    if (this.oldSlot != -1) {
                        mc.player.inventory.currentItem = this.oldSlot;
                    }
                    this.reset();
                }
            } else if (event instanceof EventAttack && sword.getValue()) {
                EventAttack attackEvent = (EventAttack) event;
                Entity target = attackEvent.getTarget();

                if (target != null) {
                    if (target instanceof net.minecraft.entity.item.EnderCrystalEntity) {
                        return false;
                    }

                    ItemStack heldItem = mc.player.getHeldItemMainhand();

                    if (heldItem.getItem() == Items.GOLDEN_APPLE
                            || heldItem.getItem() == Items.ENCHANTED_GOLDEN_APPLE
                            || heldItem.getItem().isFood()
                            || heldItem.getItem() instanceof PotionItem) {
                        return false;
                    }

                    int bestSwordSlot = findSwordSlot();

                    if (bestSwordSlot != -1 && !(heldItem.getItem() instanceof SwordItem)) {
                        this.lastActionTime = System.currentTimeMillis();
                        mc.player.inventory.currentItem = bestSwordSlot;
                        mc.player.connection.sendPacket(new CHeldItemChangePacket(bestSwordSlot));
                    }
                }
            }
        }
        return false;
    }

    private void reset() {
        this.oldSlot = -1;
        this.status = false;
        this.lastActionTime = 0;
    }

    private int findBestSlot() {
        if (mc.objectMouseOver instanceof BlockRayTraceResult) {
            BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) mc.objectMouseOver;
            Block block = mc.world.getBlockState(blockRayTraceResult.getPos()).getBlock();
            int bestSlot = -1;
            float bestSpeed = 1.0F;

            for (int slot = 0; slot < 9; ++slot) {
                ItemStack stack = mc.player.inventory.getStackInSlot(slot);
                float speed = stack.getDestroySpeed(block.getDefaultState());
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = slot;
                }
            }

            return bestSlot;
        } else {
            return -1;
        }
    }

    private int findSwordSlot() {
        int bestSwordSlot = -1;
        float bestDamage = -1.0F;

        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = mc.player.inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof SwordItem) {
                float damage = ((SwordItem) stack.getItem()).getAttackDamage();
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSwordSlot = slot;
                }
            }
        }

        return bestSwordSlot;
    }

    @Override
    protected void onDisable() {
        this.reset();
        super.onDisable();
    }
}