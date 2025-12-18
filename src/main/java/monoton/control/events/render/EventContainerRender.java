package monoton.control.events.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;
import monoton.control.events.client.Event;
import net.minecraft.inventory.container.Container;

@Data
@AllArgsConstructor
public class EventContainerRender extends Event {
    private MatrixStack stack;
    private int guiLeft;
    private int guiTop;
    private Container container;

    public static class Pre extends EventContainerRender {
        public Pre(MatrixStack stack, int guiLeft, int guiTop, Container container) {
            super(stack, guiLeft, guiTop, container);
        }
    }

    public static class Post extends EventContainerRender {
        public Post(MatrixStack stack, int guiLeft, int guiTop, Container container) {
            super(stack, guiLeft, guiTop, container);
        }
    }
}


