package monoton.module.impl.combat;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventMotion;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;

import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

@Annotation(name = "Velocity", type = TypeList.Combat, desc = "Умный Velocity с обходом GrimAC")
public class Velocity extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Grim", "Grim", "Обычный");
    private final BooleanOption countScirr = new BooleanOption("Счётчик ударов", false);
    private final SliderSetting untilCount = new SliderSetting("До счётчика", 4, 1, 10, 1);
    private final SliderSetting afterCount = new SliderSetting("После счётчика", 2, 1, 10, 1);
    private final BooleanOption onlyNetherite = new BooleanOption("Только в незеритовой броне", false);
    private final BooleanOption debug = new BooleanOption("Логи", false);

    private int hitCount = 0;
    private Vector3d lastKnockback = Vector3d.ZERO;

    public Velocity() {
        addSettings(mode, countScirr, untilCount, afterCount, onlyNetherite, debug);
    }

    @Override
    public boolean onEvent(Event event) {
        if (mc.player == null || mc.world == null) return false;
        if (mode.is("Обычный")) {
            if (event instanceof EventPacket ep && ep.isReceivePacket()) {
                if (ep.getPacket() instanceof SEntityVelocityPacket velocity) {
                    if (velocity.getEntityID() == mc.player.getEntityId()) {
                        lastKnockback = new Vector3d(
                                velocity.getMotionX() / 8000.0,
                                velocity.getMotionY() / 8000.0,
                                velocity.getMotionZ() / 8000.0
                        );

                        ep.setCancel(true);
                        return true;


                    }
                }
            }
        }

        // Grim / SpookyTime режим — компенсируем движение вручную
        if (event instanceof EventMotion e && mode.is("Grim")) {
            if (mc.player.hurtTime > 0 && canApplyVelocity()) {
                Vector3d playerPos = mc.player.getPositionVec();
                Vector3d predicted = playerPos.add(lastKnockback.scale(1.5));
                Vector2f rotation = calculateAngle(playerPos.add(0, mc.player.getEyeHeight(), 0), predicted);

                float yawDiff = MathHelper.wrapDegrees(mc.player.rotationYaw - rotation.x);

                // Компенсация движения в зависимости от направления нокбэка
                if (Math.abs(yawDiff) <= 60) {
                    mc.player.motion.x += lastKnockback.x * -1.2;
                    mc.player.motion.z += lastKnockback.z * -1.2;
                    if (mc.player.isOnGround()) mc.player.jump();
                } else if (yawDiff > 120 || yawDiff < -120) {
                    mc.player.motion.x += lastKnockback.x * 1.3;
                    mc.player.motion.z += lastKnockback.z * 1.3;
                } else if (yawDiff > 60 && yawDiff <= 150) {
                    mc.player.motion.x += lastKnockback.x * -0.8;
                    mc.player.motion.z += lastKnockback.z * -0.8;
                } else if (yawDiff < -60 && yawDiff >= -150) {
                    mc.player.motion.x += lastKnockback.x * -0.8;
                    mc.player.motion.z += lastKnockback.z * -0.8;
                }
            }
        }

        // Счётчик ударов
        if (event instanceof EventMotion && mc.player.hurtTime == 9) {
            hitCount++;

            if (countScirr.get() && hitCount > untilCount.getValue().intValue() + afterCount.getValue().intValue()) {
                hitCount = 0;
            }
        }

        return false;
    }

    private boolean canApplyVelocity() {
        if (countScirr.get() && hitCount > untilCount.getValue().intValue()) return false;
        if (onlyNetherite.get() && !isWearingNetherite()) return false;
        if (mc.player.isInWater() || mc.player.isInLava() || mc.player.isElytraFlying()) return false;
        return lastKnockback.length() > 0.1;
    }

    private boolean isWearingNetherite() {
        for (ItemStack stack : mc.player.getArmorInventoryList()) {
            if (stack.getItem() instanceof ArmorItem armor && armor.getArmorMaterial() == ArmorMaterial.NETHERITE) {
                return true;
            }
        }
        return false;
    }

    private Vector2f calculateAngle(Vector3d from, Vector3d to) {
        Vector3d diff = to.subtract(from);
        double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (Math.atan2(diff.z, diff.x) * 180 / Math.PI) - 90F;
        float pitch = (float) (-(Math.atan2(diff.y, distance) * 180 / Math.PI));
        return new Vector2f(yaw, pitch);
    }

    @Override
    public void onDisable() {
        hitCount = 0;
        lastKnockback = Vector3d.ZERO;
    }
}