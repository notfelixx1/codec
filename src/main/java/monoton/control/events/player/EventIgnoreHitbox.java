package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import monoton.control.events.client.Event;
import net.minecraft.util.math.BlockPos;

@Data
@AllArgsConstructor
public class EventIgnoreHitbox extends Event {
    private BlockPos pos;
}

