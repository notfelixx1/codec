package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import monoton.control.events.client.Event;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

@Getter
@AllArgsConstructor
public class EventPlace extends Event {
    private final Block block;
    private final BlockPos pos;
}
