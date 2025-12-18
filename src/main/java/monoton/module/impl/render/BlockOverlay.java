package monoton.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import mods.baritone.utils.IRenderer;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.shader.ColorUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static monoton.ui.clickgui.Panel.selectedColor;

@Annotation(name = "BlockOverlay", type = TypeList.Render, desc = "Обводит блок на который вы навились")
public class BlockOverlay extends Module {

    public BlockOverlay() {
        this.addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender) {
            if (((EventRender) event).isRender3D()) {
                onRender3D((EventRender) event);
            }
        }
        return false;
    }

    public void onRender3D(EventRender event) {
        if (mc.objectMouseOver == null) return;
        if (mc.objectMouseOver.getType() != RayTraceResult.Type.BLOCK) return;

        BlockRayTraceResult bhr = (BlockRayTraceResult) mc.objectMouseOver;

        BlockPos pos = bhr.getPos();
        AxisAlignedBB aabb = new AxisAlignedBB(pos, pos.add(1, 1, 1)).grow(0.002D);

        MatrixStack ms = new MatrixStack();
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(ms.getLast().getMatrix());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.depthMask(false);

        RenderSystem.shadeModel(7425);
        RenderSystem.disableDepthTest();
        drawFilledBox(ms, aabb, ColorUtils.setAlpha(selectedColor, 60));
        RenderSystem.enableDepthTest();
        RenderSystem.shadeModel(7424);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        drawOutline(ms, aabb, ColorUtils.setAlpha(selectedColor, 60));
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        RenderSystem.popMatrix();
    }

    private void drawFilledBox(MatrixStack stack, AxisAlignedBB aabb, int colorInt) {
        int topColor = ColorUtil.boostColor(colorInt, 10);
        int bottomColor = ColorUtil.darken(colorInt, 0.5f);

        float tr = ColorUtil.red(topColor) / 255f;
        float tg = ColorUtil.green(topColor) / 255f;
        float tb = ColorUtil.blue(topColor) / 255f;
        float ta = ColorUtil.alpha(topColor) / 255f;

        float br = ColorUtil.red(bottomColor) / 255f;
        float bg = ColorUtil.green(bottomColor) / 255f;
        float bb = ColorUtil.blue(bottomColor) / 255f;
        float ba = ColorUtil.alpha(bottomColor) / 255f;

        AxisAlignedBB toDraw = aabb.offset(-mc.getRenderManager().renderPosX(), -mc.getRenderManager().renderPosY(), -mc.getRenderManager().renderPosZ());
        var matrix4f = stack.getLast().getMatrix();

        BUFFER.begin(7, DefaultVertexFormats.POSITION_COLOR);

        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.minZ).color(br, bg, bb, ba).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.minZ).color(br, bg, bb, ba).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.maxZ).color(br, bg, bb, ba).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.maxZ).color(br, bg, bb, ba).endVertex();

        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.minZ).color(tr, tg, tb, ta).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.maxZ).color(tr, tg, tb, ta).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.maxZ).color(tr, tg, tb, ta).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.minZ).color(tr, tg, tb, ta).endVertex();

        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.minZ).color(br, bg, bb, ba).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.minZ).color(tr, tg, tb, ta).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.minZ).color(tr, tg, tb, ta).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.minZ).color(br, bg, bb, ba).endVertex();

        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.maxZ).color(br, bg, bb, ba).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.maxZ).color(br, bg, bb, ba).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.maxZ).color(tr, tg, tb, ta).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.maxZ).color(tr, tg, tb, ta).endVertex();

        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.minZ).color(br, bg, bb, ba).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.minY, (float) toDraw.maxZ).color(br, bg, bb, ba).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.maxZ).color(tr, tg, tb, ta).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.minX, (float) toDraw.maxY, (float) toDraw.minZ).color(tr, tg, tb, ta).endVertex();

        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.minZ).color(br, bg, bb, ba).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.minZ).color(tr, tg, tb, ta).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.maxY, (float) toDraw.maxZ).color(tr, tg, tb, ta).endVertex();
        BUFFER.pos(matrix4f, (float) toDraw.maxX, (float) toDraw.minY, (float) toDraw.maxZ).color(br, bg, bb, ba).endVertex();

        TESSELLATOR.draw();
    }

    private void drawOutline(MatrixStack stack, AxisAlignedBB aabb, int colorInt) {
        float a = ColorUtil.alpha(colorInt) / 255f;
        Color awt = new Color(ColorUtil.red(colorInt), ColorUtil.green(colorInt), ColorUtil.blue(colorInt));
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        IRenderer.startLines(awt, a, 1f, true);
        IRenderer.drawAABB(stack, aabb);
        IRenderer.endLines(true);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }
}
