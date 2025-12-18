package monoton.module.impl.combat;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventInput;
import monoton.control.events.player.EventMotion;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.math.SensUtil;
import monoton.utils.move.MoveUtil;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@Annotation(name = "ProjectileHelper", type = TypeList.Combat, desc = "Прицеливание для метательного оружия с траекторным расчетом")
public class ProjectileHelper extends Module {
    private final MultiBoxSetting weapons = new MultiBoxSetting("Оружие",
            new BooleanOption("Лук", true),
            new BooleanOption("Трезубец", false),
            new BooleanOption("Арбалет", true));
    private final BooleanOption predictPosition = new BooleanOption("Предсказание позиции", true);
    private final BooleanOption autoShot = new BooleanOption("Авто выстрел", false);
    private final SliderSetting aimRange = new SliderSetting("Дальность прицеливания", 50.0f, 10.0f, 100.0f, 1.0f);
    private final SliderSetting fov = new SliderSetting("Поле зрения", 90.0f, 1.0f, 180.0f, 1.0f);

    public LivingEntity target;
    public Vector2f rotate = new Vector2f(0, 0);
    public Vector2f server;
    private boolean aiming;
    private boolean initialAim;
    private boolean wasCharging;
    private int chargeTicks = 0;
    private final Map<LivingEntity, List<Vector3d>> positionHistory = new WeakHashMap<>();
    private final Map<LivingEntity, List<Long>> positionHistoryTimestamps = new WeakHashMap<>();

    public ProjectileHelper() {
        this.addSettings(weapons, aimRange, fov, autoShot, predictPosition);
        this.rotate = new Vector2f(0, 0);
        this.server = new Vector2f(0, 0);
    }

    public boolean check() {
        return state && aiming && server != null;
    }

    @Override
    public boolean onEvent(Event event) {
        if (mc.player != null && mc.player.isElytraFlying()) {
            reset();
            return false;
        }
        if (event instanceof EventInput e) {
            handleInput(e);
        }
        if (mc.player == null || mc.world == null) {
            reset();
            return false;
        }
        if (event instanceof EventUpdate) {
            boolean charging = validItem();

            if (!charging && wasCharging) {
                reset();
            }
            wasCharging = charging;

            if (!charging) return false;

            updateTarget();

            if (target != null && target.isAlive()) {
                if (!aiming) initialAim = true;
                aim();
                if (autoShot.get()) {
                    handleAutoShot();
                }
            }
            if (target != null && target.isAlive()) {
                synchronized (positionHistory) {
                    List<Vector3d> positions = positionHistory.computeIfAbsent(target, k -> new ArrayList<>(5));
                    List<Long> timestamps = positionHistoryTimestamps.computeIfAbsent(target, k -> new ArrayList<>(5));
                    positions.add(0, target.getPositionVec());
                    timestamps.add(0, System.currentTimeMillis());

                    if (positions.size() > 5) {
                        positions.remove(positions.size() - 1);
                        timestamps.remove(timestamps.size() - 1);
                    }
                }
            }
        }

        if (event instanceof EventMotion motionEvent) {
            if (target == null || !target.isAlive() || !validItem()) {
                return false;
            }

            motionEvent.setYaw(rotate.x);
            motionEvent.setPitch(rotate.y);
            mc.player.rotationYawHead = rotate.x;
            mc.player.renderYawOffset = rotate.x;
            mc.player.rotationPitchHead = rotate.y;
        }

        return false;
    }

    private void handleInput(EventInput event) {
        if (check()) {
            MoveUtil.fixMovement(event, server.x);
        }
    }

    private void handleAutoShot() {
        if (!autoShot.get() || target == null || !target.isAlive() || !isAimingAtTarget() || !canAttackThroughWalls(target)) {
            chargeTicks = 0;
            return;
        }

        ItemStack item = mc.player.getHeldItemMainhand();
        if (isCrossbow()) {
            if (CrossbowItem.isCharged(item)) {
                chargeTicks++;
                if (chargeTicks >= 3) {
                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    chargeTicks = 0;
                }
            }
        } else if (isBow()) {
            if (mc.player.getItemInUseMaxCount() >= getChargeTime(item)) {
                chargeTicks++;
                if (chargeTicks >= 3) {
                    mc.playerController.onStoppedUsingItem(mc.player);
                    chargeTicks = 0;
                }
            }
        } else if (isTrident()) {
            if (mc.player.getItemInUseMaxCount() >= getChargeTime(item)) {
                chargeTicks++;
                if (chargeTicks >= 3) {
                    mc.playerController.onStoppedUsingItem(mc.player);
                    chargeTicks = 0;
                }
            }
        }
    }

    private boolean isAimingAtTarget() {
        if (target == null || server == null || rotate == null) return false;
        float yawDelta = MathHelper.wrapDegrees(server.x - rotate.x);
        float pitchDelta = MathHelper.wrapDegrees(server.y - rotate.y);
        float fovToTarget = (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
        return fovToTarget < 5.0f;
    }

    private boolean canAttackThroughWalls(LivingEntity targetEntity) {
        Vector3d targetVec = targetEntity.getPositionVec().add(0.0, (double) targetEntity.getEyeHeight(), 0.0);
        Vector3d playerVec = mc.player.getPositionVec().add(0.0, (double) mc.player.getEyeHeight(), 0.0);
        RayTraceResult result = mc.world.rayTraceBlocks(new RayTraceContext(playerVec, targetVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player));
        return result.getType() == RayTraceResult.Type.MISS;
    }

    private void aim() {
        if (target == null || !target.isAlive()) return;

        float gravity = getGravity();
        Vector3d targetPos;


        if (predictPosition.get()) {
            float modif = 9.5f;
            targetPos = new Vector3d(
                    target.getPosX() + (target.getPosX() - target.lastTickPosX) * modif,
                    target.getPosY() + target.getEyeHeight() - 0.2f,
                    target.getPosZ() + (target.getPosZ() - target.lastTickPosZ) * modif
            );
        } else {
            targetPos = target.getPositionVec().add(0, target.getEyeHeight() - 0.2f, 0);
        }

        if (targetPos == null) return;

        Vector3d playerEyePos = mc.player.getEyePosition(1.0f);
        Vector3d vec = targetPos.subtract(playerEyePos);

        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
        float horizontalDistance = (float) Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, horizontalDistance)));

        float gravityCorrection = (float) (mc.player.getDistance(target) * gravity);
        pitchToTarget -= gravityCorrection;
        pitchToTarget = clamp(pitchToTarget, -89.0f, 89.0f);

        float currentYaw = mc.player.rotationYaw;
        float yawDelta = MathHelper.wrapDegrees(yawToTarget - currentYaw);
        yawToTarget = currentYaw + yawDelta;

        server = new Vector2f(yawToTarget, pitchToTarget);

        float gcd = SensUtil.getGCDValue();
        if (gcd > 0) {
            yawToTarget -= yawToTarget % gcd;
            pitchToTarget -= pitchToTarget % gcd;
        }

        rotate = new Vector2f(yawToTarget, pitchToTarget);
        aiming = true;
    }

    private float getGravity() {
        ItemStack item = mc.player.getHeldItemMainhand();
        if (item.getItem() instanceof BowItem && weapons.get("Лук")) {
            return 0.18f;
        } else if (item.getItem() instanceof CrossbowItem && weapons.get("Арбалет")) {
            return 0.16f;
        } else if (item.getItem() instanceof TridentItem && weapons.get("Трезубец")) {
            return 0.22f;
        }
        return 0.0f;
    }

    private boolean isBow() {
        ItemStack item = mc.player.getHeldItemMainhand();
        return item.getItem() instanceof BowItem && weapons.get("Лук") && mc.player.isHandActive();
    }

    private boolean isTrident() {
        ItemStack item = mc.player.getHeldItemMainhand();
        return item.getItem() instanceof TridentItem && weapons.get("Трезубец") && mc.player.isHandActive();
    }

    private boolean isCrossbow() {
        ItemStack item = mc.player.getHeldItemMainhand();
        return item.getItem() instanceof CrossbowItem && weapons.get("Арбалет") && CrossbowItem.isCharged(item);
    }

    private void updateTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.world.getAllEntities()) {
            if (isValidTarget(entity)) {
                targets.add((LivingEntity) entity);
            }
        }

        targets.sort(Comparator.comparingDouble(this::getFovToEntity));
        target = targets.isEmpty() ? null : targets.get(0);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        PlayerEntity player = (PlayerEntity) entity;

        if (player instanceof ClientPlayerEntity) return false;
        if (player.ticksExisted < 3) return false;
        if (!canAttackThroughWalls(player)) return false;
        if (mc.player.getDistance(player) > aimRange.getValue().floatValue()) return false;
        if (AntiBot.checkBot(player)) return false;
        if (Manager.FRIEND_MANAGER.isFriend(player.getName().getString())) return false;
        if (player.getName().getString().equalsIgnoreCase(mc.player.getName().getString())) return false;
        if (getFovToEntity(player) > (fov.getValue().floatValue() * 2)) return false;
        if (!player.isAlive()) return false;

        return true;
    }

    private double getFovToEntity(LivingEntity entity) {
        Vector3d playerPos = mc.player.getEyePosition(1.0F);
        Vector3d targetPos = entity.getPositionVec().add(0, entity.getHeight() * 0.5, 0);
        Vector3d vec = targetPos.subtract(playerPos);

        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - mc.player.rotationYaw);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - mc.player.rotationPitch);

        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    private boolean validItem() {
        if (mc.player == null) return false;
        ItemStack item = mc.player.getHeldItemMainhand();
        if (item.isEmpty()) return false;
        return isBow() || isTrident() || isCrossbow();
    }

    public static int getChargeTime(ItemStack item) {
        if (item.getItem() instanceof BowItem) {
            return 20;
        } else if (item.getItem() instanceof TridentItem) {
            return 10;
        }
        return 0;
    }

    private void reset() {
        if (mc.player != null) {
            rotate = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
            server = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        }
        target = null;
        aiming = false;
        initialAim = false;
        chargeTicks = 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
    }
}