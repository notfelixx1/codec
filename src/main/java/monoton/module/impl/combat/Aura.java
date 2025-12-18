package monoton.module.impl.combat;

import lombok.Getter;
import monoton.cmd.impl.TargetCmd;
import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.*;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.math.*;
import monoton.utils.misc.TimerUtil;
import monoton.utils.move.MoveUtil;
import monoton.utils.other.OtherUtil;
import monoton.utils.other.RandUtils;
import monoton.utils.other.StopWatch;
import monoton.utils.world.InventoryUtils;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.*;
import static net.minecraft.client.Minecraft.player;
import static net.minecraft.util.math.MathHelper.wrapDegrees;
import static net.optifine.CustomColors.random;

@SuppressWarnings("all")
@Annotation(name = "Aura", type = TypeList.Combat, desc = "Бьёт женщин и детей")
public class Aura extends Module {
    private static final Log log = LogFactory.getLog(Aura.class);
    @Getter
    public static LivingEntity target = null;
    public static Vector2f rotate = new Vector2f(0, 0);
    public Vector2f visualRotate = new Vector2f(0, 0);

    private boolean isSpinning = false;
    private boolean spinDirection = true;
    private float spinProgress = 0.0f;

    private final ModeSetting sortMode = new ModeSetting("Сортировать", "Умная", "Умная", "Поле зрения", "Дистанция", "Здоровье");
    private final TimerUtil speedtopTimer = new TimerUtil();
    public final ModeSetting rotationMode = new ModeSetting("Режим рот", "Grim", "Grim", "Snap", "FunTime", "Spooky", "HolyWorld");

    private final MultiBoxSetting targets = new MultiBoxSetting("Выбор целей",
            new BooleanOption("Игроки", true),
            new BooleanOption("Голые", true),
            new BooleanOption("Мобы", false),
            new BooleanOption("Друзья", false),
            new BooleanOption("Невидимки", true),
            new BooleanOption("Голые невидимки", true)
    );

    public static boolean obgon;

    public final ModeSetting sprintMode = new ModeSetting("Режим спринта", "Обычный", "Обычный", "Легитный");
    public static SliderSetting distance = new SliderSetting("Дистанция аттаки", 3.0f, 2.5f, 6f, 0.1f, 3.2f);
    private final SliderSetting rotateDistance = new SliderSetting("Доп ротация", 0.0f, 0.0f, 3.0f, 0.1f);
    private final SliderSetting elytrarotate = new SliderSetting("Ротация на элитре", 12.5F, 0.0F, 64.0F, 0.5F).setVisible(() -> !rotationMode.is("FunTime"));
    private final SliderSetting elytradist = new SliderSetting("Элитра дистанция", 0.7F, 0.0F, 0.7F, 0.05F).setVisible(() -> !rotationMode.is("FunTime"));
    public final ModeSetting correction = new ModeSetting("Корекция", "Свободная", "Свободная", "Приследование", "Сфокусированная");
    boolean isRotated;

    @Getter
    private final StopWatch stopWatch = new StopWatch();

    public final MultiBoxSetting settings = new MultiBoxSetting("Настройки",
            new BooleanOption("Только критами", true),
            new BooleanOption("Отжимать щит", true),
            new BooleanOption("Ломать щит", true),
            new BooleanOption("Умные криты", false),
            new BooleanOption("Бить через стены", true),
            new BooleanOption("Не бить если ешь", true));

    public final BooleanOption rwbupas = new BooleanOption("Бить через стены RW", true).setVisible(() -> settings.get("Бить через стены"));

    float ticksUntilNextAttack;
    private long cpsLimit = 0;
    public float speedtop = 0.0f;
    private boolean activatedBooster = false;
    public double speed = (double) 0.0F;
    private int attackCount = 0;
    private boolean isSnappingUp = false;
    private long snapStartTime = 0L;
    private final long snapDuration = 150L;
    private Vector2f snapStartRotation;
    private int attacksBeforeSnap = 19;
    private float lastYawJitter = 0;
    private float lastPitchJitter = 0;

    public Aura() {
        this.addSettings(targets, sortMode, rotationMode, correction, distance, rotateDistance, sprintMode, elytrarotate, elytradist, settings, rwbupas);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventInteractEntity entity) {
            if (target != null) entity.setCancel(true);
        }

        if (event instanceof EventPacket e) {
            onPacket(e);
            if (e.getPacket() instanceof net.minecraft.network.play.server.SPlayerPositionLookPacket) {
                activatedBooster = true;
            }
        }

        if (event instanceof EventInput eventInput) {
            if (Manager.FUNCTION_MANAGER.freeCam.player == null) {
                if (correction.is("Свободная")) {
                    MoveUtil.fixMovement(eventInput, Manager.FUNCTION_MANAGER.autoPotionFunction.isActivePotion ? Minecraft.getInstance().player.rotationYaw : rotate.x);
                } else if (correction.is("Приследование")) {
                    MoveUtil.fixMovementToTarget(eventInput, target);
                }
            }
        }

        if (event instanceof EventUpdate updateEvent) {
            String priorityTargetName = TargetCmd.getPriorityTargetName();
            LivingEntity priorityTarget = null;

            if (priorityTargetName != null) {
                for (Entity entity : mc.world.getAllEntities()) {
                    if (entity instanceof PlayerEntity player && player.getName().getString().equalsIgnoreCase(priorityTargetName) && isValidTarget((LivingEntity) entity)) {
                        priorityTarget = (LivingEntity) entity;
                        break;
                    }
                }
            }

            if (priorityTarget != null) {
                target = priorityTarget;
            } else if (!(target != null && isValidTarget(target))) {
                target = findTarget();
            }
            if (target == null) {
                cpsLimit = System.currentTimeMillis();
                rotate = new Vector2f(player.rotationYaw, player.rotationPitch);
                speedtop = 0.0f;
                return false;
            }

            if (speedtopTimer.hasTimeElapsed(35) && player.isElytraFlying()) {
                if (!activatedBooster) {
                    speedtop += 0.0001f;
                }
                if (player.ticksExisted % 10 == 0) {
                    speedtop -= 0.0005f;
                }
                if (speedtop > 0.0011f) {
                    speedtop = 0.0f;
                }
                if (activatedBooster) {
                    speedtop = 0.0f;
                }
                speedtopTimer.reset();
            }
            attackAndRotateOnEntity(target);
        }


        if (event instanceof EventMotion motionEvent) {
            handleMotionEvent(motionEvent);
        }

        return false;
    }


    public double getScale(Vector3d position, double size) {
        Vector3d cam = mc.getRenderManager().info.getProjectedView();
        double distance = cam.distanceTo(position);
        double fov = mc.gameRenderer.getFOVModifier(mc.getRenderManager().info, mc.getRenderPartialTicks(), true);
        return max(10.0, 1000.0 / distance) * (size / 30.0) / (fov == 70.0 ? 1.0 : fov / 70.0);
    }

    public Vector2f clientRot = null;

    private void handleMotionEvent(EventMotion motionEvent) {
        if (target == null || Manager.FUNCTION_MANAGER.autoPotionFunction.isActivePotion) return;

        float yaw = rotate.x;
        float pitch = rotate.y;

        motionEvent.setYaw(yaw);
        motionEvent.setPitch(pitch);

        mc.player.rotationYawHead = yaw;
        mc.player.renderYawOffset = yaw;
        mc.player.rotationPitchHead = pitch;

    }

    float lastYaw, lastPitch;

    private void attackAndRotateOnEntity(LivingEntity target) {
        this.isRotated = true;
        Vector3d vec = AuraUtil.calculateTargetVector(target);
        if (Manager.FUNCTION_MANAGER.elytraMotion.state
                && mc.player.isElytraFlying()
                && getDistance(target) <= Manager.FUNCTION_MANAGER.elytraMotion.distancie.getValue().floatValue()) {
            boolean shouldPredictElytra = target.isElytraFlying()
                    && Manager.FUNCTION_MANAGER.elyrtaPredict.state
                    && Manager.FUNCTION_MANAGER.elyrtaPredict.canPredict(target);
            if (!shouldPredictElytra) {
                mc.player.setVelocity(0, 0, 0);
            }
        }

        if (player.isElytraFlying() && target.isElytraFlying() && Manager.FUNCTION_MANAGER.elyrtaPredict.state) {
            rotationAngles(target);
        } else {
            float yawToTarget = (float) wrapDegrees(toDegrees(atan2(vec.z, vec.x)) - 90.0);
            float pitchToTarget = (float) (-toDegrees(atan2(vec.y, hypot(vec.x, vec.z))));

            float yawDelta = wrapDegrees(yawToTarget - this.rotate.x);
            float pitchDelta = wrapDegrees(pitchToTarget - this.rotate.y);

            int roundedYaw = (int) yawDelta;
            boolean elytraFly = false;
            float rotationYawSpeed = 300;
            float rotationPitchSpeed = 300;
            float clampedYaw = min(max(abs((long) yawDelta), 0.0f), rotationYawSpeed);
            float clampedPitch = max(abs((long) pitchDelta), 0.0f);
            float yaw;
            float pitch;
            float gcd = SensUtil.getGCDValue();
            vec = vec.add(
                    (target.getPosX() - target.lastTickPosX),
                    (target.getPosY() - target.lastTickPosY),
                    (target.getPosZ() - target.lastTickPosZ)
            );

            if (player.isElytraFlying()) {
                if (this.shouldAttack(target)) {
                    if (settings.get(5) && player.isHandActive() && !player.isBlocking()) return;
                    attackEntityAndSwing(target);
                }

                yaw = rotate.x + (yawDelta > 0 ? clampedYaw : -clampedYaw);
                pitch = MathHelper.clamp(rotate.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);
                yaw -= (yaw - rotate.x) % gcd;
                pitch -= (pitch - rotate.y) % gcd;
                rotate = new Vector2f(yaw, pitch);
                this.lastYaw = clampedYaw;
                this.lastPitch = clampedPitch;
            } else {
                switch (rotationMode.getIndex()) {
                    case 5 -> {
                        if (this.shouldAttack(target)) {
                            if (settings.get(5) && player.isHandActive() && !player.isBlocking()) return;
                            attackEntityAndSwing(target);
                        }
                        rotate = new Vector2f(player.rotationYaw, player.rotationPitch);
                    }
                    case 4 -> {
                        if (shouldAttack(target)) {
                            if (settings.get(5) && player.isHandActive() && !player.isBlocking()) return;
                            attackEntityAndSwing(target);
                        }
                        if (ticksUntilNextAttack > 0) {
                            setRotationHolyWorld(target, false);
                            --ticksUntilNextAttack;
                        } else {
                            rotate.x = player.rotationYaw;
                            rotate.y = player.rotationPitch;
                        }

                    }
                    case 3 -> {
                        if (this.shouldAttack(target)) {
                            attackEntityAndSwing(target);
                        }
                        spookyTimeRotation();
                    }
                    case 2 -> {
                        if (shouldAttack(target)) {
                            attackEntityAndSwing(target);
                        }
                        if (attackCount >= attacksBeforeSnap && !isSnappingUp) {
                            isSnappingUp = true;
                            snapStartTime = System.currentTimeMillis();
                            snapStartRotation = new Vector2f(rotate.x, rotate.y);
                            attackCount = 0;
                            attacksBeforeSnap = 20 + ThreadLocalRandom.current().nextInt(6);
                        }
                        if (isSnappingUp) {
                            long currentTime = System.currentTimeMillis();
                            float progress = (currentTime - snapStartTime) / (float) snapDuration;

                            if (progress >= 1.0f) {
                                isSnappingUp = false;
                                rotate = new Vector2f(rotate.x, -90.0f);
                            } else {
                                float targetPitch = -90.0f;
                                float currentPitch = snapStartRotation.y + (targetPitch - snapStartRotation.y) * progress;
                                rotate = new Vector2f(rotate.x, MathHelper.clamp(currentPitch, -90.0f, 90.0f));
                            }
                            break;
                        }
                        clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 0.5f), 99f);
                        clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 0.5f), 95f);
                        float targetYaw = rotate.x + (yawDelta > 0 ? clampedYaw : -clampedYaw);
                        float targetPitch = MathHelper.clamp(rotate.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -70f, 70f);

                        if (!shouldPlayerFalling()) {
                            float smoothYaw = (mc.player.rotationYaw - rotate.x) * 0.14f;
                            float smoothPitch = (mc.player.rotationPitch - rotate.y) * 0.2f;
                            targetYaw = rotate.x + smoothYaw;
                            targetPitch = MathHelper.clamp(rotate.y + smoothPitch, -70f, 70f);
                        }

                        float lookAroundYaw = (float) Math.sin(System.currentTimeMillis() / 55.0) * 16.5f;
                        float lookAroundPitch = (float) Math.cos(System.currentTimeMillis() / 650.0) * 2.0f;

                        yaw = targetYaw + lookAroundYaw;
                        pitch = targetPitch + lookAroundPitch;

                        float gcd2 = SensUtil.getGCDValue();
                        yaw -= (yaw - rotate.x) % gcd2;
                        pitch -= (pitch - rotate.y) % gcd2;

                        rotate = new Vector2f(yaw, pitch);
                    }
                    case 1 -> {
                        if (shouldAttack(target)) {
                            if (settings.get(5) && player.isHandActive() && !player.isBlocking()) return;
                            attackEntityAndSwing(target);
                        }
                        if (ticksUntilNextAttack > 0) {
                            setRotation(target, false);
                            --ticksUntilNextAttack;
                        } else {
                            rotate = new Vector2f(player.rotationYaw, player.rotationPitch);
                        }
                    }
                    case 0 -> {
                        if (shouldAttack(target)) {
                            attackEntityAndSwing(target);
                        }
                        yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
                        pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));

                        yawDelta = MathHelper.wrapDegrees(yawToTarget - this.rotate.x);
                        pitchDelta = MathHelper.wrapDegrees(pitchToTarget - this.rotate.y);

                        clampedYaw = Math.max(Math.abs(yawDelta), 0.0f);
                        clampedPitch = Math.max(Math.abs(pitchDelta), 0.0f);

                        yaw = rotate.x + (yawDelta > 0 ? clampedYaw : -clampedYaw);
                        pitch = MathHelper.clamp(rotate.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);
                        gcd = SensUtil.getGCDValue();
                        yaw -= (yaw - rotate.x) % gcd;
                        pitch -= (pitch - rotate.y) % gcd;

                        rotate = new Vector2f(yaw, pitch);
                    }
                }
            }
        }
    }

    @Compile
    private void spookyTimeRotation() {
        Vector3d eyePos = mc.player.getEyePosition(mc.getRenderPartialTicks());
        Vector3d targetPos;
        targetPos = target.getPositionVec().add(0, target.getHeight() / 2.0F, 0);
        Vector3d vecToTarget = targetPos.subtract(eyePos);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-vecToTarget.x, vecToTarget.z));
        float targetPitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vecToTarget.y, Math.hypot(vecToTarget.x, vecToTarget.z))), -90F, 90F);

        float currentYaw = rotate.x;
        float currentPitch = rotate.y;

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;

        float yawAbs = Math.abs(yawDelta);
        float pitchAbs = Math.abs(pitchDelta);

        float yawFraction = MathHelper.clamp(yawAbs / 180.0F, 0.0F, 1.0F);
        float pitchFraction = MathHelper.clamp(pitchAbs / 90.0F, 0.0F, 1.0F);

        float yawSpeed = MathHelper.lerp(yawFraction, 42.2F, 55.03F);
        float pitchSpeed = MathHelper.lerp(pitchFraction, 9.2F, 32.2F);

        float cooldown = 1.0F - MathHelper.clamp(mc.player.getCooledAttackStrength(1.5F), 0.0F, 1.0F);
        float slowdown = MathHelper.lerp(cooldown, 1.0F, 1.0F);
        yawSpeed *= slowdown;
        pitchSpeed *= slowdown;

        float randomScaleYaw = 1.0F + ((random.nextFloat() * 2.0F - 1.0F) * 0.3F);
        float randomScalePitch = 1.0F + ((random.nextFloat() * 2.0F - 1.0F) * 0.3F);

        yawSpeed = MathHelper.clamp(yawSpeed * randomScaleYaw, 42.2F, 55.03F);
        pitchSpeed = MathHelper.clamp(pitchSpeed * randomScalePitch, 9.2F, 32.2F);

        float yawStep = MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float pitchStep = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        lastYawJitter = randomJitter(3.0F, lastYawJitter);
        lastPitchJitter = randomJitter(0.0F, lastPitchJitter);

        float newYaw = currentYaw + yawStep + lastYawJitter;
        float newPitch = MathHelper.clamp(currentPitch + pitchStep + lastPitchJitter, -89.0F, 90.0F);

        rotate = new Vector2f(newYaw, newPitch);

        float gcd = 0.125F;
        rotate = new Vector2f(
                rotate.x - (rotate.x - lastYaw) % gcd,
                rotate.y - (rotate.y - lastPitch) % gcd
        );

        lastYaw = rotate.x;
        lastPitch = rotate.y;

    }

    private float randomJitter(float bound, float previous) {
        if (bound <= 0.0F) return 0.0F;
        float next = (random.nextFloat() * 2.0F - 1.0F) * bound;
        return MathHelper.lerp(0.35F, previous, next);
    }

    public float flags[] = new float[10];

    private void LonyGrief() {
        Vector3d targetPosition = AuraUtil.calculateTargetVector(target);

        float t = mc.player.ticksExisted + mc.getRenderPartialTicks();
        float smoothYaw = (float) (float) (Math.sin(t * 0.50) * 12);
        float smoothPitch = (float) (Math.sin(t * 0.65) * 2 + Math.sin(t * 0.83 + 54.1) * 14.5);


        float yawToTarget = (float) wrapDegrees(Math.toDegrees(Math.atan2(targetPosition.z, targetPosition.x)) - 90);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(targetPosition.y, hypot(targetPosition.x, targetPosition.z))));
        if ((flags[4] <= 0 && canAttack())) flags[4] = 1;

        float finalYaw = 0, finalPitch = 0;

        if (flags[4] > 0) {
            finalYaw = yawToTarget;
            finalPitch = pitchToTarget;
        } else {
            finalYaw = smoothStep(mc.player.rotationYaw, yawToTarget, (float) ((float) (Math.sin(t * 5))));
            finalPitch = smoothStep(mc.player.rotationPitch, pitchToTarget, 0.2F);
        }

        rotate = new Vector2f(finalYaw, mc.player.rotationPitch);
        flags[4]--;
    }

    public float smoothStep(float start, float end, float amount) {
        float a = MathHelper.clamp(amount, 0f, 1f), d = wrapDegrees(end - start);
        if (Math.abs(d) < 0.5f) return end;
        return wrapDegrees(start + d * a);
    }

    private Vector3d calculateDynamicTargetVector(Entity target, Entity player) {
        double playerEyeY = player.getPosY() + player.getEyeHeight(player.getPose());

        double targetY = target.getPosY();
        double targetHeight = target.getHeight();

        double targetFeetY = targetY;
        double targetBodyY = targetY + targetHeight / 2.0;
        double targetHeadY = targetY + targetHeight * 0.9;

        double targetAimY;
        if (Math.abs(playerEyeY - targetFeetY) < 0.5) {
            targetAimY = targetFeetY;
        } else if (Math.abs(playerEyeY - targetBodyY) < 0.5) {
            targetAimY = targetBodyY;
        } else {
            targetAimY = targetHeadY;
        }

        double playerX = player.getPosX();
        double playerZ = player.getPosZ();
        double targetX = target.getPosX();
        targetY = targetAimY;
        double targetZ = target.getPosZ();

        return new Vector3d(targetX - playerX, targetY - playerEyeY, targetZ - playerZ);
    }

    public void rotationAngles(LivingEntity target) {
        if (this.shouldAttack(target)) {
            if (settings.get(5) && player.isHandActive() && !player.isBlocking()) return;
            attackEntityAndSwing(target);
        }

        double elytraDistance = Manager.FUNCTION_MANAGER.elyrtaPredict.getElytraDistance(target);

        Vector3d add = new Vector3d(0, MathHelper.clamp(target.getPosY() - target.getHeight(), 0, target.getHeight() / 2.0f), 0);

        Vector3d targetPos = target.getPositionVec()
                .add(add);
        Vector3d playerPos = mc.player.getEyePosition(1.0f);
        Vector3d motionOffset = target.getMotion().mul(
                elytraDistance,
                elytraDistance,
                elytraDistance
        );
        Vector3d pos = targetPos.subtract(playerPos);
        if (Manager.FUNCTION_MANAGER.elyrtaPredict.canPredict(target)) {
            pos = pos.add(motionOffset);
        }


        float shortestYawPath = (float) ((((((Math.toDegrees(Math.atan2(pos.z, pos.x)) - 90) - mc.player.rotationYaw) % 360) + 540) % 360) - 180);

        float findPitch = (float) Math.min(90, -Math.toDegrees(Math.atan2(pos.y, Math.hypot(pos.x, pos.z))));

        float targetYaw = mc.player.rotationYaw + shortestYawPath;
        float targetPitch = MathHelper.clamp(findPitch, -90, 90);

        Vector2f correctedRotation = correctRotation(
                targetYaw,
                targetPitch
        );

        rotate = new Vector2f(correctedRotation.x, correctedRotation.y);

    }

    public static Vector2f correctRotation(float yaw, float pitch) {
        if ((yaw == -90 && pitch == 90) || yaw == -180)
            return new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);

        float gcd = SensUtil.getGCDValue();
        yaw -= yaw % gcd;
        pitch -= pitch % gcd;

        return new Vector2f(yaw, pitch);
    }

    private boolean isPlayerBetweenPoints(Vector3d playerPos, Vector3d point1, Vector3d point2) {
        double distToPoint1 = playerPos.distanceTo(point1);
        double distToPoint2 = playerPos.distanceTo(point2);
        double distBetweenPoints = point1.distanceTo(point2);

        double tolerance = 1.0;
        double totalDist = distToPoint1 + distToPoint2;

        return Math.abs(totalDist - distBetweenPoints) < tolerance;
    }

    public boolean shouldPlayerFalling() {
        return AuraUtil.isPlayerFalling(settings.get("Только критами"), settings.get("Умные криты"));
    }

    public double attackDistance() {
        return max(mc.playerController.extendedReach() ? 6.0D : 3.0D, distance.getValue().floatValue());
    }

    private void findTarget(CUseEntityPacket packet) {
        if (mc.world != null && player != null) {
            if (packet.getEntityFromWorld(mc.world) instanceof PlayerEntity player && player != target) {
                target = player;
            }
        }
    }

    @Compile
    private void setRotation(final LivingEntity base, final boolean attack) {
        Vector3d vec3d = AuraUtil.getVector(target);
        double diffX = vec3d.x;
        double diffY = vec3d.y;
        double diffZ = vec3d.z;
        float[] rotations = new float[]{(float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F, (float) (-Math.toDegrees(Math.atan2(diffY, Math.hypot(diffX, diffZ))))};
        float deltaYaw = MathHelper.wrapDegrees(MathUtil.calculateDelta(rotations[0], rotate.x));
        float deltaPitch = MathUtil.calculateDelta(rotations[1], rotate.y);
        float limitedYaw = Math.min(Math.max(Math.abs(deltaYaw), 1.0F), 360.0F);
        float limitedPitch = Math.min(Math.max(Math.abs(deltaPitch), 1.0F), 90.0F);
        float finalYaw = rotate.x + (deltaYaw > 0.0F ? limitedYaw : -limitedYaw) + RandUtils.LegitFloat(-1.0F, 1.0F, RandUtils.PatternMode.HUMAN_TREMOR);
        float finalPitch = MathHelper.clamp(rotate.y + (deltaPitch > 0.0F ? limitedPitch : -limitedPitch) + RandUtils.LegitFloat(-1.0F, 1.0F, RandUtils.PatternMode.HUMAN_TREMOR), -90.0F, 90.0F);
        float gcd = GCDUtil.getGCDValue();
        finalYaw = (float) ((double) finalYaw - (double) (finalYaw - rotate.x) % (double) gcd);
        finalPitch = (float) ((double) finalPitch - (double) (finalPitch - rotate.y) % (double) gcd);
        rotate = new Vector2f(finalYaw, finalPitch);
    }

    private void setRotationHolyWorld(final LivingEntity base, final boolean attack) {
        Vector3d vec3d = AuraUtil.getVector(target);
        double diffX = vec3d.x;
        double diffY = vec3d.y;
        double diffZ = vec3d.z;
        float[] rotations = new float[]{(float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F, (float) (-Math.toDegrees(Math.atan2(diffY, Math.hypot(diffX, diffZ))))};
        float deltaYaw = MathHelper.wrapDegrees(MathUtil.calculateDelta(rotations[0], rotate.x));
        float deltaPitch = MathUtil.calculateDelta(rotations[1], rotate.y);
        float limitedYaw = Math.min(Math.max(Math.abs(deltaYaw), 1.0F), 360.0F);
        float limitedPitch = Math.min(Math.max(Math.abs(deltaPitch), 1.0F), 90.0F);
        float finalYaw = rotate.x + (deltaYaw > 0.0F ? limitedYaw : -limitedYaw) + RandUtils.LegitFloat(-1.0F, 1.0F, RandUtils.PatternMode.HUMAN_TREMOR);
        float finalPitch = MathHelper.clamp(rotate.y + (deltaPitch > 0.0F ? limitedPitch : -limitedPitch) + RandUtils.LegitFloat(-1.0F, 1.0F, RandUtils.PatternMode.HUMAN_TREMOR), -90.0F, 90.0F);
        float gcd = GCDUtil.getGCDValue();
        finalYaw = (float) ((double) finalYaw - (double) (finalYaw - rotate.x) % (double) gcd);
        finalPitch = (float) ((double) finalPitch - (double) (finalPitch - rotate.y) % (double) gcd);
        rotate = new Vector2f(finalYaw, finalPitch);
    }

    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof CUseEntityPacket packet) {
            findTarget(packet);
        }
    }

    private float[] calculateRotations(LivingEntity target) {
        double deltaX = target.getPosX() - player.getPosX();
        double deltaY = target.getPosY() + target.getEyeHeight() - (player.getPosY() + player.getEyeHeight());
        double deltaZ = target.getPosZ() - player.getPosZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) toDegrees(atan2(deltaZ, deltaX)) - 90.0F;
        float pitch = (float) -toDegrees(atan2(deltaY, distance));
        return new float[]{yaw, pitch};
    }

    public float wrapLerp(float step, float input, float target) {
        return input + step * wrapDegrees(target - input);
    }

    public float randomLerp(float min, float max) {
        return Interpolator.lerp(max, min, new SecureRandom().nextFloat());
    }


    private void attackEntityAndSwing(final LivingEntity targetEntity) {
        if (settings.get(5) && player.isHandActive() && !player.isBlocking()) return;
        if (settings.get(1) && player.isBlocking()) {
            mc.playerController.onStoppedUsingItem(player);
        }

        boolean sprint = false;
        boolean isInLiquid = player.isActualySwimming() || player.isSwimming() && player.areEyesInFluid(FluidTags.WATER) || player.areEyesInFluid(FluidTags.LAVA);

        if (sprintMode.is("Обычный")) {
            if (CEntityActionPacket.lastUpdatedSprint && !player.isInWater()) {
                player.connection.sendPacket(new CEntityActionPacket(player, CEntityActionPacket.Action.STOP_SPRINTING));
                sprint = true;
            }
        }

        ticksUntilNextAttack = 3f;

        if ((rotationMode.is("FunTime") || rotationMode.is("Spooky") || rotationMode.is("HolyWorld"))) {
            if (!rayTrace()) return;
        }

        BackTrack backTrackModule = (BackTrack) Manager.FUNCTION_MANAGER.backTrack;
        Vector3d targetPos = targetEntity.getPositionVec();

        if (targetEntity instanceof PlayerEntity playerTarget
                && backTrackModule != null
                && backTrackModule.state) {
            BackTrack.Position oldestPos = backTrackModule.getOldestPosition((PlayerEntity) targetEntity);

            if (oldestPos != null) {
                targetPos = oldestPos.getPos();
            }
        }

        if (rwbupas.get() && !canAttackThroughWalls(targetEntity)) {
            try {
                if (Manager.FUNCTION_MANAGER.auraFunction.target != null) {
                    Vector3d eye = mc.player.getEyePosition(1.0F);

                    Vector3d aimPoint = getAimPointForRotation(targetEntity);
                    Vector3d rayEnd = aimPoint != null ? aimPoint : eye.add(mc.player.getLookVec().scale(6.0));

                    Vector3d dirVec = rayEnd.subtract(eye).normalize();
                    double dirX = dirVec.x;
                    double dirY = dirVec.y;
                    double dirZ = dirVec.z;

                    Vector3d currentStart = eye;
                    int maxSteps = 2;

                    for (int i = 0; i < maxSteps; i++) {
                        RayTraceResult rr = mc.world.rayTraceBlocks(
                                new RayTraceContext(currentStart, rayEnd, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player)
                        );
                        if (rr == null || rr.getType() != RayTraceResult.Type.BLOCK) {
                            break;
                        }

                        BlockRayTraceResult br = (BlockRayTraceResult) rr;

                        mc.playerController.spoofInstantDig(br.getPos(), br.getFace());

                        Vector3d hit = br.getHitVec();
                        currentStart = new Vector3d(
                                hit.x + dirX * 0.05,
                                hit.y + dirY * 0.05,
                                hit.z + dirZ * 0.05
                        );

                        if (currentStart.squareDistanceTo(rayEnd) < 0.0025) {
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (this.settings.get(2)) {
            this.breakShield(targetEntity);
        }

        this.cpsLimit = System.currentTimeMillis() + 480;

        mc.playerController.attackEntity(mc.player, targetEntity);
        mc.player.swingArm(Hand.MAIN_HAND);

        if (sprint) {
            player.connection.sendPacket(new CEntityActionPacket(player, CEntityActionPacket.Action.START_SPRINTING));
        }
    }

    private Vector3d getAimPointForRotation(LivingEntity targetEntity) {
        if (targetEntity == null) return null;

        Vector3d base = targetEntity.getPositionVec().add(0.0, targetEntity.getHeight() / 1.3f, 0.0);

        base = base.add(
                (targetEntity.getPosX() - targetEntity.lastTickPosX),
                (targetEntity.getPosY() - targetEntity.lastTickPosY),
                (targetEntity.getPosZ() - targetEntity.lastTickPosZ)
        );

        return base;
    }

    public boolean rayTraceNone() {
        return (RayTraceUtil.rayTraceEntity(player.rotationYaw, player.rotationPitch, attackDistance(), target));
    }

    public boolean rayTrace() {
        return (RayTraceUtil.rayTraceEntity(rotate.x, rotate.y, attackDistance(), target));
    }

    private void breakShield(LivingEntity target) {
        if (target instanceof PlayerEntity entity) {
            if (target.isActiveItemStackBlocking(3) && !entity.isSpectator() && !entity.isCreative()) {
                Item item = null;

                for (int i = 0; i < 36; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() instanceof AxeItem) {
                        item = mc.player.inventory.getStackInSlot(i).getItem();
                        break;
                    }
                }

                if (item != null) {
                    InventoryUtils.inventorySwapClick(item, false, InventoryUtils.UseType.ATTACK, (PlayerEntity) target, (BlockPos) null);
                }
            }
        }
    }

    private boolean shouldAttack(LivingEntity targetEntity) {
        return this.canAttack()
                && targetEntity != null
                && this.cpsLimit <= System.currentTimeMillis()
                && mc.player.getCooledAttackStrength(0.5f) >= 0.95f;
    }

    private long timeOnGround = 0;
    private boolean wasOnGround = false;
    private long lastJumpTime = 0;

    public boolean canAttack() {
        boolean inSnow = false;
        boolean inBerryBush = false;
        double x;
        double z;

        BlockPos feetPos = new BlockPos(player.getPosX(), player.getPosY(), player.getPosZ());
        if (mc.world.getBlockState(feetPos).getBlock() == Blocks.SNOW) {
            int snowLayers = mc.world.getBlockState(feetPos).get(SnowBlock.LAYERS);
            if (snowLayers == 2) {
                inSnow = true;
            }
        }

        boolean currentlyOnGround = player.isOnGround();
        if (currentlyOnGround && !wasOnGround) {
            timeOnGround = System.currentTimeMillis();
        } else if (!currentlyOnGround && wasOnGround && mc.gameSettings.keyBindJump.isKeyDown()) {
            lastJumpTime = System.currentTimeMillis();
        } else if (!currentlyOnGround) {
            timeOnGround = 0;
        }
        wasOnGround = currentlyOnGround;

        BlockPos headPosAbove = new BlockPos(player.getPosX(), player.getPosY() + 1.5, player.getPosZ());
        boolean blockAbove = mc.world.getBlockState(headPosAbove).getBlock() != Blocks.AIR;
        BlockPos blockBelow = new BlockPos(player.getPosX(), player.getPosY() - 1, player.getPosZ());
        boolean standingOnTrapdoor = mc.world.getBlockState(blockBelow).getBlock() instanceof TrapDoorBlock;
        boolean trapdoorAbove = mc.world.getBlockState(headPosAbove).getBlock() instanceof TrapDoorBlock;

        final boolean onSpace = settings.get(3)
                && mc.player.isOnGround()
                && !mc.gameSettings.keyBindJump.isKeyDown();

        float elytradistance1 = 0.0F;
        float elytraSpeed = 0.0F;
        if (player.isElytraFlying()) {
            elytraSpeed = (float) Math.sqrt(player.getMotion().x * player.getMotion().x + player.getMotion().z * player.getMotion().z);
            elytradistance1 = this.elytradist.getValue().floatValue() - elytraSpeed * 0.1f;
        }

        float attackStrength = mc.player.getCooledAttackStrength(Manager.FUNCTION_MANAGER.syncTps.state ? Manager.FUNCTION_MANAGER.syncTps.adjustTicks : 0.95F);

        boolean antisync = mc.player.fallDistance < 0.76F || mc.player.fallDistance > 1.15F || mc.player.isElytraFlying();
        double yDiff = (double) ((int) mc.player.getPosY()) - mc.player.getPosY();
        boolean bl4 = yDiff == -0.01250004768371582;
        boolean bl5 = yDiff == -0.1875;

        boolean reasonForAttack = player.isOnLadder() || (player.isInWater() && player.areEyesInFluid(FluidTags.WATER)) || player.isRidingHorse() || (!player.isInWater() && (bl5 || bl4)) && !mc.player.isSneaking() || player.abilities.isFlying || player.isElytraFlying() || player.isPotionActive(Effects.LEVITATION) || (mc.player.isInLava() && mc.player.areEyesInFluid(FluidTags.LAVA)) || mc.player.isInWeb() || inSnow || inBerryBush || (standingOnTrapdoor && blockAbove);

        if (!(this.getDistance(target) >= (double) distance.getValue().floatValue() - elytradistance1) && !(player.getCooledAttackStrength(attackStrength) < 0.95F)) {
            if (Manager.FUNCTION_MANAGER.freeCam.player != null) {
                return true;
            } else if (!mc.player.isSneaking() && blockAbove && player.isForcedDown() && !player.collidedVertically) {
                return true;
            } else if (!reasonForAttack && this.settings.get(0)) {
                return onSpace || (!inBerryBush && ((Manager.FUNCTION_MANAGER.packetCriticals.state && Manager.FUNCTION_MANAGER.packetCriticals.mode.is("Grim 1.17+") ? !player.isOnGround() : (!player.isOnGround() && player.fallDistance > (player.collidedVertically ? 0.1 : 0)) && antisync)));
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static float bpstarget() {
        double distance = Math.sqrt(pow(target.getPosX() - target.prevPosX, 2) + pow(target.getPosY() - target.prevPosY, 2) + pow(target.getPosZ() - target.prevPosZ, 2));
        float bps = (float) (distance * mc.timer.timerSpeed * 20.0D);
        return (float) (round(bps * 10) / 10.2f);
    }

    private final List<LivingEntity> validTargets = new ArrayList<>();
    private long lastTargetUpdate = 0;
    private static final long TARGET_UPDATE_INTERVAL = 100;

    private LivingEntity findTarget() {
        long now = System.currentTimeMillis();

        if (now - lastTargetUpdate > TARGET_UPDATE_INTERVAL || target == null || !isValidTarget(target)) {
            validTargets.clear();
            updateValidTargets();
            lastTargetUpdate = now;
        }

        if (validTargets.isEmpty()) return null;

        String priority = TargetCmd.getPriorityTargetName();
        if (priority != null) {
            for (LivingEntity e : validTargets) {
                if (e instanceof PlayerEntity p && p.getName().getString().equalsIgnoreCase(priority)) {
                    return e;
                }
            }
        }

        if (validTargets.size() > 1 && !"Дистанция".equals(sortMode.get())) {
            sortTargets(validTargets);
        }

        return validTargets.get(0);
    }

    private void sortTargets(List<LivingEntity> targets) {
        switch (sortMode.get()) {
            case "Умная" -> {
                targets.sort(
                        Comparator.comparingDouble((LivingEntity e) -> {
                                    if (e instanceof PlayerEntity p) {
                                        if (p.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {
                                            return Double.MIN_VALUE;
                                        }
                                        return -getEntityArmorCached(p);
                                    }
                                    return -e.getTotalArmorValue();
                                })
                                .thenComparingDouble(e -> getEntityHealth(e))
                                .thenComparingDouble(this::getDistance)
                );
            }
            case "Поле зрения" -> {
                targets.sort(Comparator.comparingDouble(this::getFOV));
            }
            case "Дистанция" -> {
                targets.sort(Comparator.comparingDouble(this::getDistance));
            }
            case "Здоровье" -> {
                targets.sort(Comparator.comparingDouble(this::getHealth));
            }
        }
    }

    private final Map<PlayerEntity, Double> armorCache = new ConcurrentHashMap<>();
    private final Map<PlayerEntity, Long> armorCacheTime = new ConcurrentHashMap<>();
    private static final long ARMOR_CACHE_TIME = 500;

    private double getEntityArmorCached(PlayerEntity player) {
        Long time = armorCacheTime.get(player);
        if (time != null && System.currentTimeMillis() - time < ARMOR_CACHE_TIME) {
            Double cached = armorCache.get(player);
            if (cached != null) return cached;
        }

        double armor = 0.0;
        for (ItemStack stack : player.inventory.armorInventory) {
            if (stack.getItem() instanceof ArmorItem a) {
                armor += a.getDamageReduceAmount();
                armor += EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25;
            }
        }

        armorCache.put(player, armor);
        armorCacheTime.put(player, System.currentTimeMillis());
        return armor;
    }

    private void updateValidTargets() {
        double maxDist = distance.getValue().floatValue() + rotateDistance.getValue().floatValue() + (player.isElytraFlying() ? elytrarotate.getValue().floatValue() : 0);
        maxDist *= maxDist;

        for (Entity entity : mc.world.getEntitiesWithinAABB(LivingEntity.class, player.getBoundingBox().grow(maxDist))) {
            if (entity instanceof LivingEntity living && isValidTarget(living)) {
                validTargets.add(living);
            }
        }
    }

    private double getHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    private double getFOV(LivingEntity entity) {
        float[] rotations = calculateRotations(entity);
        float yawDiff = Math.abs(wrapDegrees(rotations[0] - player.rotationYaw));
        float pitchDiff = Math.abs(wrapDegrees(rotations[1] - player.rotationPitch));
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private boolean isValidTarget(final LivingEntity base) {
        if (base instanceof ClientPlayerEntity) return false;

        if (base.ticksExisted < 3) return false;
        if (!settings.get(4) && !canAttackThroughWalls(base))
            return false;

        float elytrarotate1 = 0.0F;
        if (player.isElytraFlying()) {
            elytrarotate1 = this.elytrarotate.getValue().floatValue();
        }

        if (!player.isElytraFlying()) {
            elytrarotate1 = 0.0F;
        }

        if (mc.player.getDistanceEyePos(base) > (distance.getValue().floatValue() + rotateDistance.getValue().floatValue() + elytrarotate1))
            return false;

        if (base instanceof PlayerEntity p) {
            if (AntiBot.checkBot(base)) {
                return false;
            }
            String playerName = base.getName().getString();
            if (!targets.get("Друзья") && Manager.FRIEND_MANAGER.isFriend(playerName)) {
                return false;
            }
            if (p.getName().getString().equalsIgnoreCase(mc.player.getName().getString())) return false;
        }

        if (base instanceof PlayerEntity && !targets.get("Игроки")) {
            return false;
        }
        if (base instanceof PlayerEntity && base.getTotalArmorValue() == 0 && !targets.get("Голые")) {
            return false;
        }
        if (base instanceof PlayerEntity && base.isInvisible() && base.getTotalArmorValue() == 0 && !targets.get("Голые невидимки")) {
            return false;
        }
        if (base instanceof PlayerEntity && base.isInvisible() && !targets.get("Невидимки")) {
            return false;
        }

        if (!targets.get("Мобы") && base instanceof MobEntity) {
            return false;
        }

        return !base.isInvulnerable() && base.isAlive() && !(base instanceof ArmorStandEntity);
    }

    private boolean canAttackThroughWalls(LivingEntity targetEntity) {
        Vector3d targetVec = targetEntity.getPositionVec().add(0.0, (double) targetEntity.getEyeHeight(), 0.0);
        Vector3d playerVec = player.getPositionVec().add(0.0, (double) player.getEyeHeight(), 0.0);
        RayTraceResult result = mc.world.rayTraceBlocks(new RayTraceContext(playerVec, targetVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
        return ((RayTraceResult) result).getType() == RayTraceResult.Type.MISS;
    }

    private double getDistance(LivingEntity entity) {
        return AuraUtil.getVector(entity).length();
    }

    public double getEntityArmor(PlayerEntity target) {
        double totalArmor = 0.0;
        for (ItemStack armorStack : target.inventory.armorInventory) {
            if (armorStack != null && armorStack.getItem() instanceof ArmorItem) {
                totalArmor += getProtectionLvl(armorStack);
            }
        }
        return totalArmor;
    }

    public double getEntityHealth(Entity ent) {
        if (ent instanceof PlayerEntity player) {
            double armorValue = getEntityArmor(player) / 20.0;
            return (player.getHealth() + player.getAbsorptionAmount()) * armorValue;
        } else if (ent instanceof LivingEntity livingEntity) {
            return livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
        }
        return 0.0;
    }

    private double getProtectionLvl(ItemStack stack) {
        ArmorItem armor = (ArmorItem) stack.getItem();
        double damageReduce = armor.getDamageReduceAmount();
        if (stack.isEnchanted()) {
            damageReduce += (double) EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25;
        }
        return damageReduce;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.visualRotate = new Vector2f(player.rotationYaw, player.rotationPitch);
    }

    @Override
    public void onDisable() {
        this.rotate = new Vector2f(player.rotationYaw, player.rotationPitch);
        this.visualRotate = new Vector2f(player.rotationYaw, player.rotationPitch);
        target = null;
        obgon = false;
        isSpinning = false;
        spinProgress = 0.0f;
        super.onDisable();
    }
}