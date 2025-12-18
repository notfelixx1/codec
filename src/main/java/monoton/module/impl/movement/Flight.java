package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventMotion;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.misc.TimerUtil;
import monoton.utils.move.MoveUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.math.vector.Vector3d;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.concurrent.CopyOnWriteArrayList;

@Annotation(name = "Flight", type = TypeList.Movement, desc = "Полёт пакетами или тому подобное")
public class Flight extends Module {
    public static ModeSetting mode = new ModeSetting("Мод", "Глайд", "Глайд", "Элитра Глайд", "Пакет");
    private final SliderSetting motion = new SliderSetting("Скорость", 0.5F, 0F, 2F, 0.05F).setVisible(() -> mode.is("Глайд"));
    private final SliderSetting speed = new SliderSetting("Скорoсть", 0.2F, 0.1F, 0.5F, 0.05F).setVisible(() -> mode.is("Пакет"));
    private final TimerUtil ticks = new TimerUtil();
    private int ticksTwo = 0;
    private final TimerUtil timerUtil = new TimerUtil();
    private final CopyOnWriteArrayList<IPacket<?>> packets = new CopyOnWriteArrayList<>();

    public Flight() {
        this.addSettings(mode, motion, speed);
    }

    @Compile
    @Override
    public boolean onEvent(Event event) {
        if (mc.player == null || mc.world == null) return false;

        if (mode.is("Глайд")) {
            if (event instanceof EventUpdate) {
                handleGlideFly();
            }
        } else if (mode.is("Пакет")) {
            if (event instanceof EventUpdate) {
                if (timerUtil.hasTimeElapsed(1450)) {
                    double x = mc.player.getPosX();
                    double y = mc.player.getPosY();
                    double z = mc.player.getPosZ();
                    float yaw = mc.player.rotationYaw;
                    float pitch = mc.player.rotationPitch;
                    mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x, y, z, yaw, pitch, mc.player.isOnGround()));
                    if (!packets.isEmpty()) {
                        for (IPacket<?> packet : packets) {
                            mc.player.connection.sendPacketWithoutEvent(packet);
                        }
                        packets.clear();
                    }
                    timerUtil.reset();
                }
            }
            if (event instanceof EventPacket eventReceivePacket) {
                IPacket<?> packet = eventReceivePacket.getPacket();
                if (packet instanceof CPlayerPacket) {
                    packets.add(packet);
                    eventReceivePacket.setCancel(true);
                }
            }
            handleRWFly();
        } else if (mode.is("Элитра Глайд")) {
            if (event instanceof EventMotion) {
                if (!mc.player.isElytraFlying()) return false;

                ticksTwo++;
                Vector3d pos = mc.player.getPositionVec();
                float yaw = mc.player.rotationYaw;
                double forward = 0.085;

                double dx = -Math.sin(Math.toRadians(yaw)) * forward;
                double dz = Math.cos(Math.toRadians(yaw)) * forward;
                mc.player.setVelocity(dx * 1.25f, mc.player.getMotion().y - 0.01f, dz * 1.25f);

                if (ticks.isReached(45)) {
                    mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                    ticks.reset();
                }
                mc.player.setVelocity(dx * 1.25f, mc.player.getMotion().y + 0.015f, dz * 1.25f);
            }
        }
        return false;
    }

    @Compile
    private void handleRWFly() {
        if (mc.player.movementInput == null) return;
        mc.player.setMotion(0, 0, 0);
        MoveUtil.setMotion(speed.getValue().floatValue() + 0.005f);
    }

    @Compile
    private void handleGlideFly() {
        if (mc.player.movementInput == null) return;
        mc.player.setVelocity(0, 0, 0);
        MoveUtil.setMotion(motion.getValue().floatValue());
    }
}