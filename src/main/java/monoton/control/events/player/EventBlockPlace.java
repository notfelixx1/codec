package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import monoton.control.events.client.Event;
import net.minecraft.util.math.BlockPos;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class EventBlockPlace extends Event {
    private BlockPos pos;
}