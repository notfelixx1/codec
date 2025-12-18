package monoton.module.impl.misc;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventMotion;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@Annotation(name = "WebTrap", type = TypeList.Misc, desc = "Траперит игроков в паутину")
public class WebTrap extends Module {

    private long lastPlaceTime = 0;
    private final long placeDelay = 250;
    public Vector2f server;
    private BlockPos targetPos;
    private Entity targetEntity;
    private boolean rotationSet = false;

    public WebTrap() {
        addSettings();
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate update) {
            onUpdate(update);
        } else if (event instanceof EventMotion motion) {
            handleMotion(motion);
        }
        return false;
    }

    private void handleMotion(EventMotion e) {
        if (check()) {
            e.setYaw(server.x);
            e.setPitch(server.y);
            mc.player.rotationYawHead = server.x;
            mc.player.renderYawOffset = server.x;
            mc.player.rotationPitchHead = server.y;
            rotationSet = true; // Mark rotation as set
        }
    }

    public boolean check() {
        return server != null && targetPos != null && targetEntity != null && targetEntity.isAlive();
    }

    private boolean isCobwebItem(Item item) {
        return item == Items.COBWEB;
    }

    private int getCobwebHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (isCobwebItem(mc.player.inventory.getStackInSlot(i).getItem())) {
                return i;
            }
        }
        return -1;
    }

    private int getCobwebInventoryIndex() {
        for (int i = 9; i < 36; i++) {
            if (isCobwebItem(mc.player.inventory.getStackInSlot(i).getItem())) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasCobweb() {
        return getCobwebHotbarSlot() != -1 || getCobwebInventoryIndex() != -1;
    }

    private boolean isEntityInCobweb(Entity entity) {
        boolean headInWeb = false;
        boolean feetInWeb = false;
        Vector3d entityPos = entity.getPositionVec();

        for (double x = -0.31; x <= 0.31; x += 0.31) {
            for (double z = -0.31; z <= 0.31; z += 0.31) {
                for (double y = entity.getEyeHeight(); y >= 0.0; y -= 0.1) {
                    BlockPos headPos = new BlockPos(entityPos.x + x, entityPos.y + y, entityPos.z + z);
                    if (mc.world.getBlockState(headPos).getBlock() == Blocks.COBWEB) {
                        headInWeb = true;
                        break;
                    }
                }
                if (headInWeb) break;
            }
            if (headInWeb) break;
        }

        for (double x = -0.31; x <= 0.31; x += 0.31) {
            for (double z = -0.31; z <= 0.31; z += 0.31) {
                BlockPos feetPos = new BlockPos(entityPos.x + x, entityPos.y, entityPos.z + z);
                if (mc.world.getBlockState(feetPos).getBlock() == Blocks.COBWEB) {
                    feetInWeb = true;
                    break;
                }
            }
            if (feetInWeb) break;
        }

        return headInWeb || feetInWeb;
    }

    private BlockPos getTargetPosition(Entity entity, boolean feetInWeb, boolean headInWeb) {
        Vector3d pos = entity.getPositionVec();
        BlockPos feetPos = new BlockPos(pos);
        BlockPos headPos = feetPos.up();

        if (feetInWeb && !headInWeb) {
            if (mc.world.getBlockState(headPos).getBlock() == Blocks.AIR &&
                    mc.world.getBlockState(feetPos).getMaterial().isSolid()) {
                return headPos;
            }
        } else if (!feetInWeb && !headInWeb) {
            if (mc.world.getBlockState(feetPos).getBlock() == Blocks.AIR &&
                    mc.world.getBlockState(feetPos.down()).getMaterial().isSolid()) {
                return feetPos;
            }
        }
        return null;
    }

    private void reset() {
        targetPos = null;
        server = null;
        targetEntity = null;
        rotationSet = false; // Reset rotation flag
    }

    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        if (!hasCobweb() || Manager.FUNCTION_MANAGER.auraFunction.target != null) {
            reset();
            return;
        }

        // Проверяем, валидна ли текущая цель
        if (targetEntity != null && (!targetEntity.isAlive() ||
                targetEntity.getPositionVec().squareDistanceTo(mc.player.getPositionVec()) > 25.0 ||
                Manager.FRIEND_MANAGER.isFriend(targetEntity.getName().getString()))) {
            reset();
        }

        Vector3d playerPos = mc.player.getPositionVec();
        double radius = 4.0;
        AxisAlignedBB searchBox = new AxisAlignedBB(
                playerPos.x - radius, playerPos.y - radius, playerPos.z - radius,
                playerPos.x + radius, playerPos.y + radius, playerPos.z + radius
        );

        targetEntity = mc.world.getEntitiesWithinAABB(PlayerEntity.class, searchBox, entity ->
                entity.isAlive() &&
                        entity != mc.player &&
                        entity.isOnGround() &&
                        Math.abs(entity.getPosX() - entity.lastTickPosX) < 0.01 &&
                        Math.abs(entity.getPosZ() - entity.lastTickPosZ) < 0.01 &&
                        !Manager.FRIEND_MANAGER.isFriend(entity.getName().getString())
        ).stream().min((e1, e2) -> Double.compare(
                e1.getPositionVec().squareDistanceTo(playerPos),
                e2.getPositionVec().squareDistanceTo(playerPos)
        )).orElse(null);

        if (targetEntity != null) {
            boolean headInWeb = false;
            boolean feetInWeb = false;
            Vector3d entityPos = targetEntity.getPositionVec();

            for (double x = -0.31; x <= 0.31; x += 0.31) {
                for (double z = -0.31; z <= 0.31; z += 0.31) {
                    BlockPos headPos = new BlockPos(entityPos.x + x, entityPos.y + targetEntity.getEyeHeight(), entityPos.z + z);
                    BlockPos feetPos = new BlockPos(entityPos.x + x, entityPos.y, entityPos.z + z);
                    if (mc.world.getBlockState(headPos).getBlock() == Blocks.COBWEB) {
                        headInWeb = true;
                    }
                    if (mc.world.getBlockState(feetPos).getBlock() == Blocks.COBWEB) {
                        feetInWeb = true;
                    }
                    if (headInWeb && feetInWeb) break;
                }
                if (headInWeb && feetInWeb) break;
            }

            targetPos = getTargetPosition(targetEntity, feetInWeb, headInWeb);
            if (targetPos != null) {
                updateRotation(); // Set rotation first

                // Only place cobweb if rotation was set in the previous motion event
                if (rotationSet) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastPlaceTime >= placeDelay) {
                        int originalSlot = mc.player.inventory.currentItem;
                        boolean swapped = false;
                        int blockIndex = -1;
                        int targetSlot = -1;

                        int hotbarSlot = getCobwebHotbarSlot();
                        if (hotbarSlot != -1) {
                            if (hotbarSlot != mc.player.inventory.currentItem) {
                                mc.player.connection.sendPacket(new CHeldItemChangePacket(hotbarSlot));
                            }
                            targetSlot = hotbarSlot;
                        } else {
                            blockIndex = getCobwebInventoryIndex();
                            if (blockIndex != -1) {
                                targetSlot = (mc.player.inventory.currentItem + 1) % 9;
                                mc.playerController.windowClick(0, blockIndex, targetSlot, ClickType.SWAP, mc.player);
                                mc.player.connection.sendPacket(new CHeldItemChangePacket(targetSlot));
                                swapped = true;
                            } else {
                                reset();
                                return;
                            }
                        }

                        BlockPos belowPos = targetPos.down();
                        BlockRayTraceResult rayTraceResult = new BlockRayTraceResult(
                                new Vector3d(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5),
                                Direction.UP,
                                belowPos,
                                false
                        );

                        boolean placed = mc.playerController.processRightClickBlock(
                                mc.player,
                                mc.world,
                                Hand.MAIN_HAND,
                                rayTraceResult
                        ).isSuccess();

                        lastPlaceTime = currentTime;
                        mc.player.swingArm(Hand.MAIN_HAND);

                        mc.player.connection.sendPacket(new CHeldItemChangePacket(originalSlot));
                        if (swapped) {
                            mc.playerController.windowClick(0, blockIndex, targetSlot, ClickType.SWAP, mc.player);
                        }
                    }
                }
            } else {
                reset();
            }
        } else {
            reset();
        }
    }

    @Compile
    private void updateRotation() {
        if (targetPos == null) return;

        Vector3d targetVec = new Vector3d(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        Vector3d vec = targetVec.subtract(mc.player.getEyePosition(1.0F));

        float yawToTarget = wrapDegrees((float) Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));

        if (server == null) {
            server = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        }

        float yaw = yawToTarget;
        float pitch = clamp(pitchToTarget, -89.0F, 89.0F);

        float gcd = getGCDValue();
        yaw -= (yaw - server.x) % gcd;
        pitch -= (pitch - server.y) % gcd;

        server = new Vector2f(yaw, pitch);
    }

    private float getGCDValue() {
        float sensitivity = (float) (mc.gameSettings.mouseSensitivity * 0.6F + 0.2F);
        float gcd = sensitivity * sensitivity * sensitivity * 8.0F;
        return gcd * 0.15F;
    }
}