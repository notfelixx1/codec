package monoton.utils;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Framebuffer;

public interface IMinecraft {
    Tessellator TESSELLATOR = Tessellator.getInstance();
    BufferBuilder BUFFER = TESSELLATOR.getBuffer();
    Minecraft mc = Minecraft.getInstance();
    MainWindow sr = mc.getMainWindow();
    Framebuffer FRAMEBUFFER = mc.getFramebuffer();
    FontRenderer fr = mc.fontRenderer;
    MainWindow mw = mc.getMainWindow();
}
