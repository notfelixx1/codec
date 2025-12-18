package monoton.control.events.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.control.events.client.Event;
import net.minecraft.item.ItemStack;

public class EventRenderTooltip extends Event {
    public final MatrixStack matrixStack;
    public final ItemStack stack;
    public final int mouseX;
    public final int mouseY;

    public EventRenderTooltip(MatrixStack matrixStack, ItemStack stack, int mouseX, int mouseY) {
        this.matrixStack = matrixStack;
        this.stack = stack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
} 