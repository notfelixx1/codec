package monoton.utils.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import monoton.utils.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class HudUtil implements IMinecraft {

    public static String calculateBPS() {
        double distance = Math.sqrt(Math.pow(mc.player.getPosX() - mc.player.prevPosX, 2) +
                Math.pow(mc.player.getPosY() - mc.player.prevPosY, 2) +
                Math.pow(mc.player.getPosZ() - mc.player.prevPosZ, 2));
        float bps = (float) (distance * mc.timer.timerSpeed * 20.0D);
        return String.valueOf((float) (Math.round(bps * 10) / 10.0f));
    }

    public static double getBps(Entity entity) {
        double x = entity.getPosX() - entity.prevPosX;
        double y = entity.getPosY() - entity.prevPosY;
        double z = entity.getPosZ() - entity.prevPosZ;
        return Math.sqrt((x * x) + (y * y) + (z * z)) * 20.0D;
    }

    public static void drawItemStack(ItemStack stack, float x, float y, boolean drawOverlay, boolean scale, float scaleValue) {
        if (stack == null || stack.isEmpty()) return;

        RenderSystem.pushMatrix();
        RenderSystem.translatef(x, y, 0);

        if (scale && scaleValue != 1.0f) {
            GL11.glScalef(scaleValue, scaleValue, scaleValue);
        }

        mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);

        if (drawOverlay) {
            mc.getItemRenderer().renderItemOverlayIntoGUI(mc.fontRenderer, stack, 0, 0, null);
        }

        RenderSystem.popMatrix();
    }

    public static int calculatePing() {
        return mc.player.connection.getPlayerInfo(mc.player.getUniqueID()) != null ?
                mc.player.connection.getPlayerInfo(mc.player.getUniqueID()).getResponseTime() : 0;
    }

    public static String serverIP() {
        return mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null && !mc.isSingleplayer() ? mc.getCurrentServerData().serverIP : "";
    }

}
