package monoton.module.impl.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.game.EventKey;
import monoton.control.events.player.EventInput;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BindSetting;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.misc.TimerUtil;
import monoton.utils.move.MoveUtil;
import monoton.utils.world.InventoryUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;

@Annotation(name = "TargetPearl", type = TypeList.Player, desc = "Кидает перл точно за перлом врага (через всё)")
@Getter
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetPearl extends Module {

    private final ModeSetting mode = new ModeSetting("Тип", "Автоматический", "По клавише", "Автоматический");
    private final BindSetting bind = new BindSetting("Клавиша", 0).setVisible(() -> mode.is("По клавише"));
    private final BooleanOption onlyTarget = new BooleanOption("Только за таргетом", false);
    private final SliderSetting distance = new SliderSetting("Мин дистанция", 10, 8, 20, 1);
    private final TimerUtil timerUtil = new TimerUtil();

    private EnderPearlEntity targetPearl = null;
    private Vector3d cachedLanding = null;
    private long lastPearlScan = 0;
    private long lastThrowTime = 0;
    private boolean isThrowing = false;
    public Vector2f server = null;

    private static final int PEARL_SCAN_INTERVAL = 70;
    private static final int THROW_COOLDOWN = 2000;
    private static final int MAX_SIM_TICKS = 160;
    private static final double MAX_ACCEPTABLE_ERROR = 1.8;
    private static final double FINAL_CHECK_ERROR = 3.2;

    public TargetPearl() {
        addSettings(mode, bind, onlyTarget, distance);
    }

    public boolean check() {
        return state && isThrowing && server != null;
    }

    @Override
    public boolean onEvent(Event event) {
        if (event == null || mc.player == null || mc.world == null) return false;

        if (event instanceof EventInput e && check()) {
            MoveUtil.fixMovement(e, server.x);
        }

        if (event instanceof EventKey e && mode.is("По клавише") && e.key == bind.getKey()) {
            tryThrow();
        }

        if (mode.is("Автоматический") && event instanceof EventUpdate) {
            tryThrow();
        }

        return false;
    }

    private void tryThrow() {
        if (System.currentTimeMillis() - lastThrowTime < THROW_COOLDOWN) return;
        if (!canThrow()) return;

        updateTargetPearl();
        if (cachedLanding == null) return;

        float[] rot = calculateYawPitch(cachedLanding);
        if (rot == null) {
            rot = findAnyWorkingRotation(cachedLanding);
            if (rot == null) return;
        }

        Vector3d predicted = simulateTrajectory(rot[0], rot[1]);
        if (predicted != null && cachedLanding.distanceTo(predicted) > FINAL_CHECK_ERROR) {
            rot = findAnyWorkingRotation(cachedLanding);
            if (rot == null) return;
        }

        isThrowing = true;
        server = new Vector2f(rot[0], rot[1]);
        sendPlayerRotationPacket(rot[0], rot[1], mc.player.isOnGround());

        if (InventoryUtils.hasItem(Items.ENDER_PEARL)) {

            InventoryUtils.inventorySwapClick(Items.ENDER_PEARL, false);
            useItem(Hand.MAIN_HAND);
            timerUtil.reset();
            lastThrowTime = System.currentTimeMillis();
        }

        isThrowing = false;
        server = null;
    }

    private boolean canThrow() {
        return !mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL);
    }

    private void updateTargetPearl() {
        long now = System.currentTimeMillis();
        if (now - lastPearlScan < PEARL_SCAN_INTERVAL) return;

        lastPearlScan = now;
        cachedLanding = null;
        targetPearl = findBestPearl();

        if (targetPearl != null && targetPearl.isAlive()) {
            cachedLanding = predictLanding(targetPearl);
            if (cachedLanding != null) {
                BlockPos pos = new BlockPos(cachedLanding);

                if (mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()).isEmpty() &&
                        mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty()) {
                    cachedLanding = null;
                }
            }
        }
    }

    private EnderPearlEntity findBestPearl() {
        List<Entity> entities = mc.world.getEntitiesWithinAABBExcludingEntity(
                mc.player, mc.player.getBoundingBox().grow(130));

        EnderPearlEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double minDist = distance.getValue().doubleValue();
        Entity auraTarget = Manager.FUNCTION_MANAGER.auraFunction.getTarget();

        for (Entity e : entities) {
            if (!(e instanceof EnderPearlEntity pearl) || !pearl.isAlive()) continue;
            if (onlyTarget.get() && (pearl.getShooter() == null || !pearl.getShooter().equals(auraTarget))) continue;

            Vector3d landing = predictLanding(pearl);
            if (landing == null) continue;

            double dist = mc.player.getPositionVec().distanceTo(landing);
            if (dist >= minDist && dist <= 80 && dist < bestDist) {
                best = pearl;
                bestDist = dist;
            }
        }
        return best;
    }

    // Точная копия ванильной физики перла
    private Vector3d predictLanding(EnderPearlEntity pearl) {
        Vector3d pos = pearl.getPositionVec();
        Vector3d vel = pearl.getMotion();

        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            Vector3d next = pos.add(vel);
            vel = vel.scale(0.99).subtract(0, 0.03, 0);

            if (next.y <= 0) {
                return snapToBlockCenter(next);
            }

            BlockPos bp = new BlockPos(next);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return snapToBlockCenter(next);
            }
            pos = next;
        }
        return null;
    }

    private Vector3d snapToBlockCenter(Vector3d vec) {
        return new Vector3d(
                MathHelper.floor(vec.x) + 0.5,
                MathHelper.floor(vec.y),
                MathHelper.floor(vec.z) + 0.5
        );
    }

    private float[] calculateYawPitch(Vector3d target) {
        Vector3d eye = mc.player.getEyePosition(1.0f);
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

        float bestPitch = 0;
        int bestTicks = Integer.MAX_VALUE;
        double bestError = Double.MAX_VALUE;

        for (float pitch = 85.0f; pitch >= -30.0f; pitch -= 0.42f) {
            SimulationResult res = simulateWithTicks(yaw, pitch, target);
            if (res != null && res.error <= MAX_ACCEPTABLE_ERROR) {
                if (res.ticks < bestTicks || (res.ticks == bestTicks && res.error < bestError)) {
                    bestTicks = res.ticks;
                    bestPitch = pitch;
                    bestError = res.error;
                }
            }
        }

        if (bestTicks != Integer.MAX_VALUE) {
            return new float[]{yaw, MathHelper.clamp(bestPitch, -90f, 90f)};
        }
        return null;
    }

    private float[] findAnyWorkingRotation(Vector3d target) {
        Vector3d eye = mc.player.getEyePosition(1.0f);
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

        for (float pitch = 80.0f; pitch >= -40.0f; pitch -= 1.8f) {
            Vector3d landing = simulateTrajectory(yaw, pitch);
            if (landing != null && target.distanceTo(landing) <= FINAL_CHECK_ERROR + 1.5) {
                return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
            }
        }
        return null;
    }

    private static class SimulationResult {
        final double error;
        final int ticks;
        SimulationResult(double error, int ticks) {
            this.error = error;
            this.ticks = ticks;
        }
    }

    private SimulationResult simulateWithTicks(float yaw, float pitch, Vector3d target) {
        Vector3d pos = getThrowPos(yaw, pitch);
        Vector3d motion = getThrowMotion(yaw, pitch);

        for (int tick = 0; tick < MAX_SIM_TICKS; tick++) {
            pos = pos.add(motion);
            motion = motion.scale(0.99).subtract(0, 0.03, 0);

            if (pos.y <= 0) {
                return new SimulationResult(snapToBlockCenter(pos).distanceTo(target), tick + 1);
            }

            BlockPos bp = new BlockPos(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return new SimulationResult(snapToBlockCenter(pos).distanceTo(target), tick + 1);
            }
        }
        return null;
    }

    private Vector3d simulateTrajectory(float yaw, float pitch) {
        Vector3d pos = getThrowPos(yaw, pitch);
        Vector3d motion = getThrowMotion(yaw, pitch);

        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            pos = pos.add(motion);
            motion = motion.scale(0.99).subtract(0, 0.03, 0);

            if (pos.y <= 0) {
                return snapToBlockCenter(pos);
            }

            BlockPos bp = new BlockPos(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return snapToBlockCenter(pos);
            }
        }
        return null;
    }

    private Vector3d getThrowPos(float yaw, float pitch) {
        float yr = (float) Math.toRadians(yaw);
        double x = mc.player.getPosX() - MathHelper.cos(yr) * 0.16;
        double y = mc.player.getPosY() + mc.player.getEyeHeight() - 0.1;
        double z = mc.player.getPosZ() - MathHelper.sin(yr) * 0.16;
        return new Vector3d(x, y, z);
    }

    private Vector3d getThrowMotion(float yaw, float pitch) {
        double v = 1.5;
        float yr = (float) Math.toRadians(yaw);
        float pr = (float) Math.toRadians(pitch);
        double vx = -MathHelper.sin(yr) * MathHelper.cos(pr) * v;
        double vy = -MathHelper.sin(pr) * v;
        double vz = MathHelper.cos(yr) * MathHelper.cos(pr) * v;
        return new Vector3d(vx, vy, vz);
    }

    private void sendPlayerRotationPacket(float yaw, float pitch, boolean onGround) {
        if (mc.getConnection() != null) {
            mc.getConnection().sendPacket(new CPlayerPacket.RotationPacket(yaw, pitch, onGround));
        }
    }

    private void useItem(Hand hand) {
        if (mc.getConnection() != null) {
            mc.getConnection().sendPacket(new CPlayerTryUseItemPacket(hand));
            var cooldown = Manager.FUNCTION_MANAGER.itemsCooldown;
            if (cooldown != null && cooldown.state) {
                cooldown.onItemUsed(Items.ENDER_PEARL);
            }
        }
    }

    @Override
    protected void onDisable() {
        isThrowing = false;
        targetPearl = null;
        cachedLanding = null;
        server = null;
        timerUtil.reset();
        super.onDisable();
    }
}