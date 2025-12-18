package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import monoton.control.events.client.Event;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.vector.Vector3d;

@AllArgsConstructor
@Getter
@Setter
public class EventFireworkMotion extends Event {
    private LivingEntity entity;
    private FireworkRocketEntity fireworkRocketEntity;
    private Vector3d vector3d;
}

