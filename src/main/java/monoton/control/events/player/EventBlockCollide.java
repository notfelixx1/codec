package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import monoton.control.events.client.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

@Data
@AllArgsConstructor
public class EventBlockCollide extends Event {
    private BlockState blockState;
    private BlockPos pos;
    private AxisAlignedBB boundingBox;
}
