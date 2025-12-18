package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import monoton.control.events.client.Event;
import net.minecraft.util.math.BlockPos;

@AllArgsConstructor
public class EventCollision extends Event {
    @Getter
    private final BlockPos blockPos;
}

