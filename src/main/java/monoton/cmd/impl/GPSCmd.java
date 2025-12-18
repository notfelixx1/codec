package monoton.cmd.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import monoton.control.Manager;
import monoton.utils.render.RenderUtilka;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.utils.other.OtherUtil;
import monoton.utils.IMinecraft;
import monoton.utils.font.Fonts;
import monoton.utils.render.ColorUtils;
import org.joml.Vector4i;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import static monoton.ui.clickgui.Panel.selectedColor;

@CmdInfo(
        name = "gps", description = "Прокладывает путь до координат")
public class GPSCmd extends Cmd {
    public static boolean enabled;
    public static Vector3d vector3d;

    public GPSCmd() {
    }

    @Compile
    public void run(String[] args) throws Exception {
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("off")) {
                OtherUtil.sendMessage("Навигатор выключен");
                enabled = false;
                vector3d = null;
                return;
            }
            int positionX = mc.player.getPosition().getX();
            int positionZ = mc.player.getPosition().getZ();
            if (args[1].equalsIgnoreCase("death")) {
                int x = Integer.parseInt(String.valueOf(positionX));
                int y = Integer.parseInt(String.valueOf(positionZ));
                enabled = true;
                vector3d = new Vector3d((double) x, 0.0, (double) y);
                OtherUtil.sendMessage(TextFormatting.GRAY + "Навигатор включен! Координаты " + x + ";" + y);
            }

            if (args.length == 3) {
                int x = Integer.parseInt(args[1]);
                int y = Integer.parseInt(args[2]);
                enabled = true;
                vector3d = new Vector3d((double) x, 0.0, (double) y);
                OtherUtil.sendMessage(TextFormatting.GRAY + "Навигатор включен! Координаты " + x + ";" + y);
            }
        } else {
            this.error();
        }

    }

    public static void drawArrow(MatrixStack stack) {
        if (enabled) {
            double x = vector3d.x - IMinecraft.mc.getRenderManager().info.getProjectedView().getX();
            double z = vector3d.z - IMinecraft.mc.getRenderManager().info.getProjectedView().getZ();
            Minecraft var10000 = mc;
            double cos = (double) MathHelper.cos((double) mc.player.rotationYaw * 0.017453292519943295);
            var10000 = mc;
            double sin = (double) MathHelper.sin((double) mc.player.rotationYaw * 0.017453292519943295);
            double rotY = -(z * cos - x * sin);
            double rotX = -(x * cos + z * sin);
            float angle = (float) (Math.atan2(rotY, rotX) * 180.0 / Math.PI);
            double x2 = (double) (0 * MathHelper.cos(Math.toRadians((double) angle)) + (float) IMinecraft.mc.getMainWindow().getScaledWidth() / 2.0F);
            double y2 = (double) (0 * MathHelper.sin(Math.toRadians((double) angle)) + (float) IMinecraft.mc.getMainWindow().getScaledHeight() / 4.3F);
            GlStateManager.pushMatrix();
            GlStateManager.disableBlend();
            GlStateManager.translated(x2, y2, 0.0);
            GlStateManager.rotatef(angle, 0.0F, 0.0F, 1.0F);
            Minecraft var10001 = IMinecraft.mc;
            double var21 = Math.pow(vector3d.x - mc.player.getPosX(), 2.0);
            Minecraft var10002 = IMinecraft.mc;
            double dst = Math.sqrt(var21 + Math.pow(vector3d.z - mc.player.getPosZ(), 2.0));
            int clr = selectedColor;
            GlStateManager.rotatef(90.0F, 0.0F, 0.0F, 1.0F);
            Fonts.intl[13].drawCenteredStringWithOutline(stack, (int) dst + "m", -1, 6.5f, -1);
            RenderUtilka.Render2D.drawImageAlph(new ResourceLocation("monoton/images/arrows.png"), 1f - (Manager.FUNCTION_MANAGER.arrowsFunction.size3.getValue().floatValue() / 2), 7f, Manager.FUNCTION_MANAGER.arrowsFunction.size3.getValue().floatValue(), Manager.FUNCTION_MANAGER.arrowsFunction.size3.getValue().floatValue(), new Vector4i(ColorUtils.setAlpha(clr, 200), ColorUtils.setAlpha(clr, 200), ColorUtils.setAlpha(clr, 200), ColorUtils.setAlpha(clr, 200)));
            GlStateManager.enableBlend();
            GlStateManager.popMatrix();

        }
    }

    @Compile
    public void error() {
        this.sendMessage(TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        this.sendMessage(TextFormatting.WHITE + ".gps " + TextFormatting.GRAY + "<" + TextFormatting.RED + "x, z" + TextFormatting.GRAY + ">");
        this.sendMessage(TextFormatting.WHITE + ".gps " + TextFormatting.GRAY + "<" + TextFormatting.RED + "death" + TextFormatting.GRAY + ">");
        this.sendMessage(TextFormatting.WHITE + ".gps " + TextFormatting.GRAY + "<" + TextFormatting.RED + "off" + TextFormatting.GRAY + ">");
    }
}
