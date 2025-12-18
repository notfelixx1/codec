package monoton.control.events.player;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;
import monoton.control.events.client.Event;
import net.minecraft.entity.Entity;

@Data
@AllArgsConstructor
public class EntityRenderMatrixEvent extends Event {
    private final MatrixStack matrix;
    private final Entity entity;
}
