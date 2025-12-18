package monoton.module.impl.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.player.*;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting; // Добавлено
import monoton.utils.misc.TimerUtil;
import monoton.utils.move.MoveUtil;
import monoton.utils.other.OtherUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.List;

import static net.minecraft.util.math.MathHelper.clamp;

@Getter
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Annotation(name = "AutoExplosion", type = TypeList.Combat, desc = "Автоматически размещает и взрывает кристаллы")
public class AutoExplosion extends Module {

    private BlockPos position = null;
    private Entity crystalEntity = null;
    private int oldSlot = -1;
    private final TimerUtil timerUtil = new TimerUtil();
    private final TimerUtil placeDelayTimer = new TimerUtil();

    private BlockPos pendingCrystalPlace = null;

    private final MultiBoxSetting protection = new MultiBoxSetting("Не взрывать",
            new BooleanOption("Себя", true),
            new BooleanOption("Друзей", true),
            new BooleanOption("Предметы", true));

    public Vector2f server;

    private static final double MAX_ROTATION_DISTANCE = 4.0;
    private static final double MAX_ATTACK_DISTANCE = 4.0;
    private static final double MAX_PLACE_DISTANCE = 4.0;
    private static final long CRYSTAL_PLACE_DELAY = 1;

    public AutoExplosion() {
        addSettings(protection);
    }

    public boolean check() {
        return state && server != null && crystalEntity != null && position != null;
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventWorldChanged) {
            resetCrystalState();
            return false;
        }

        if (event instanceof EventInput e) {
            handleInput(e);
        }

        if (mc.player == null || mc.world == null) return false;

        if (check() && isValidForRotation(crystalEntity)) {
            OtherUtil.look(event, server, OtherUtil.Correction.FULL, null);
        }

        if (event instanceof EventPlace eventPlace && eventPlace.getBlock() == Blocks.OBSIDIAN) {
            handleObsidianPlace(eventPlace.getPos());
        } else if (event instanceof EventUpdate updateEvent) {
            handleUpdateEvent(updateEvent);
        } else if (event instanceof EventMotion motionEvent) {
            handleMotionEvent(motionEvent);
        }

        return false;
    }

    private void handleInput(EventInput event) {
        if (check()) {
            MoveUtil.fixMovement(event, server.x);
        }
    }

    private void handleUpdateEvent(EventUpdate updateEvent) {
        if (pendingCrystalPlace != null && placeDelayTimer.finished(CRYSTAL_PLACE_DELAY)) {
            final int crystalSlot = getSlotWithCrystal();
            if (crystalSlot == -1) {
                pendingCrystalPlace = null;
                return;
            }

            Vector3d placeTarget = new Vector3d(
                    pendingCrystalPlace.getX() + 0.5,
                    pendingCrystalPlace.getY() + 1.0,
                    pendingCrystalPlace.getZ() + 0.5
            );

            oldSlot = mc.player.inventory.currentItem;
            mc.player.inventory.currentItem = crystalSlot;

            BlockRayTraceResult rayTrace = new BlockRayTraceResult(
                    placeTarget,
                    Direction.UP,
                    pendingCrystalPlace,
                    false
            );

            ActionResultType result = mc.playerController.processRightClickBlock(
                    mc.player, mc.world, Hand.MAIN_HAND, rayTrace
            );

            if (result == ActionResultType.SUCCESS) {
                mc.player.swingArm(Hand.MAIN_HAND);
            }

            if (oldSlot != -1) {
                mc.player.inventory.currentItem = oldSlot;
            }
            oldSlot = -1;

            this.position = pendingCrystalPlace;
            this.pendingCrystalPlace = null;
            this.placeDelayTimer.reset();
        }

        if (position != null) {
            List<Entity> crystals = mc.world.getEntitiesWithinAABBExcludingEntity(null,
                            new AxisAlignedBB(
                                    position.getX(), position.getY(), position.getZ(),
                                    position.getX() + 1.0, position.getY() + 2.0, position.getZ() + 1.0
                            ))
                    .stream()
                    .filter(e -> e instanceof EnderCrystalEntity && e.isAlive())
                    .toList();

            if (crystalEntity != null && !crystalEntity.isAlive()) {
                resetCrystalState();
                return;
            }

            if (!crystals.isEmpty()) {
                crystalEntity = crystals.get(0);
            }

            if (crystalEntity != null && isValidForAttack(crystalEntity)) {
                attackEntity(crystalEntity);
            }
        }

        if (crystalEntity != null && !crystalEntity.isAlive()) {
            resetCrystalState();
        }
    }

    @Compile
    private void handleObsidianPlace(BlockPos pos) {
        if (pos == null || !isPlaceDistanceValid(pos)) return;

        this.pendingCrystalPlace = pos;
        this.placeDelayTimer.reset();
        this.position = null;
    }

    @Compile
    private void handleMotionEvent(EventMotion motionEvent) {
        if (crystalEntity == null || !isValidForRotation(crystalEntity)) return;

        Vector3d crystalCenter = crystalEntity.getPositionVec().add(0, 0.5, 0);
        server = OtherUtil.get(crystalCenter);

        motionEvent.setYaw(server.x);
        motionEvent.setPitch(server.y);
        mc.player.rotationYawHead = server.x;
        mc.player.renderYawOffset = server.x;
        mc.player.rotationPitchHead = server.y;
    }

    @Compile
    private void attackEntity(Entity base) {
        if (!isValidForAttack(base)) return;
        if (mc.player.getCooledAttackStrength(1.0f) < 1.0f) return;
        if (!timerUtil.finished(80)) return;

        mc.playerController.attackEntity(mc.player, base);
        mc.player.swingArm(Hand.MAIN_HAND);
        timerUtil.reset();
    }

    private int getSlotWithCrystal() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == Items.END_CRYSTAL) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPlaceDistanceValid(BlockPos pos) {
        return mc.player.getPositionVec().distanceTo(
                new Vector3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5)
        ) <= MAX_PLACE_DISTANCE;
    }

    private boolean shouldNotExplodeBecauseOfSelf() {
        return protection.get("Себя") && crystalEntity.getPosY() <= mc.player.getPosY() + 0.1;
    }

    private boolean shouldNotExplodeBecauseOfFriend() {
        if (!protection.get("Друзей")) return false;

        return mc.world.getEntitiesWithinAABB(PlayerEntity.class, crystalEntity.getBoundingBox().grow(6.0))
                .stream()
                .anyMatch(entity -> entity != mc.player
                        && entity.isAlive()
                        && Manager.FRIEND_MANAGER.isFriend(entity.getName().getString()));
    }

    private boolean shouldNotExplodeBecauseOfItems() {
        if (!protection.get("Предметы")) return false;

        return mc.world.getEntitiesWithinAABB(ItemEntity.class, crystalEntity.getBoundingBox().grow(6.0))
                .stream()
                .map(e -> ((ItemEntity) e).getItem().getItem())
                .anyMatch(item -> item == Items.TOTEM_OF_UNDYING ||
                        item == Items.END_CRYSTAL ||
                        item == Items.ENCHANTED_GOLDEN_APPLE ||
                        item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE ||
                        item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS ||
                        item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD ||
                        item == Items.ELYTRA || item == Items.TRIDENT);
    }

    private boolean hasProtectionBlock() {
        return shouldNotExplodeBecauseOfSelf() ||
                shouldNotExplodeBecauseOfFriend() ||
                shouldNotExplodeBecauseOfItems();
    }

    @Compile
    private boolean isValidForRotation(Entity base) {
        if (base == null || !base.isAlive()) return false;
        if (hasProtectionBlock()) return false;
        return mc.player.getDistance(base) <= MAX_ROTATION_DISTANCE;
    }

    @Compile
    private boolean isValidForAttack(Entity base) {
        if (!isValidForRotation(base)) return false;
        if (hasProtectionBlock()) return false;
        return mc.player.getDistance(base) <= MAX_ATTACK_DISTANCE;
    }


    private void resetCrystalState() {
        crystalEntity = null;
        position = null;
        pendingCrystalPlace = null;
        server = null;
        oldSlot = -1;
        placeDelayTimer.reset();
    }

    @Override
    public void onDisable() {
        resetCrystalState();
        super.onDisable();
    }
}