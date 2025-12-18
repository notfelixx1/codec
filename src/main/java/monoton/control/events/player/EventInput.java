package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import monoton.control.events.client.Event;
import monoton.utils.move.MoveUtil;
import net.minecraft.util.math.MathHelper;

import static monoton.utils.IMinecraft.mc;

@Getter
@Setter
@AllArgsConstructor
public class EventInput extends Event {

    private float forward, strafe;
    private boolean jump, sneak;
    private double sneakSlowDownMultiplier;

    public void setYaw(float yaw, float direction) {
        final float forward = this.getForward();
        final float strafe = this.getStrafe();

        final double angle = MathHelper.wrapDegrees(Math.toDegrees(MoveUtil.direction(direction, forward, strafe)));

        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                final double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(MoveUtil.direction(yaw, predictedForward, predictedStrafe)));
                final double difference = Math.abs(angle - predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        this.setForward(closestForward);
        this.setStrafe(closestStrafe);
    }

    public void setYaw(final float yaw) {
        setYaw(yaw, mc.player.rotationYaw);
    }
}
