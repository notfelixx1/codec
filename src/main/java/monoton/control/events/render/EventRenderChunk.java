package monoton.control.events.render;

import lombok.AllArgsConstructor;
import lombok.Data;
import monoton.control.events.client.Event;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

@Data
@AllArgsConstructor
public class EventRenderChunk extends Event {
    private ChunkRenderDispatcher.ChunkRender chunkRender;
}
