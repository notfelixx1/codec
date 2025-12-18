package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import monoton.control.events.client.Event;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;

@Data
@AllArgsConstructor
public class EventClickBlockRight extends Event {
    private final ClientPlayerEntity player;
    private final ClientWorld world;
    private final Hand hand;
    private final BlockRayTraceResult result;
}
