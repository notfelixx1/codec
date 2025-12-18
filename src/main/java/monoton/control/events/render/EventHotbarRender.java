package monoton.control.events.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;
import monoton.control.events.client.Event;

@Data
@AllArgsConstructor
public class EventHotbarRender extends Event {
    private MatrixStack stack;
    private float partialTicks;

    public static class Pre extends EventHotbarRender {
        public Pre(MatrixStack stack, float partialTicks) {
            super(stack, partialTicks);
        }
    }

    public static class Post extends EventHotbarRender {
        public Post(MatrixStack stack, float partialTicks) {
            super(stack, partialTicks);
        }
    }
}


