package monoton.control.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import monoton.control.events.client.Event;
import net.minecraft.util.math.BlockPos;

@Getter
@AllArgsConstructor
public class PotionExplosionEvent extends Event {
    private final int rgbColor;
    private final BlockPos position;
}

