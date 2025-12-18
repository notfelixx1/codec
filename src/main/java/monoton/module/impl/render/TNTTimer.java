package monoton.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.utils.font.Fonts;
import monoton.utils.glu.Project;
import monoton.utils.math.MathUtil;
import monoton.utils.render.RenderUtilka;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import org.joml.Vector2f;

import static monoton.module.TypeList.Render;
import static monoton.utils.render.ColorUtils.rgba;

@Annotation(name = "TNTTimer", type = Render, desc = "Время до взрыва динамита")
public class TNTTimer extends Module {

    @Override
    public boolean onEvent(Event event) {
        if (!(event instanceof EventRender render) || mc.player == null || mc.world == null) {
            return false;
        }
        if (render.isRender2D()) {
            timerRender(render.matrixStack);
        }
        return false;
    }

    private boolean timerRender(MatrixStack stack) {
        Project project = new Project();
        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof TNTEntity tnt) {
                final String name = MathUtil.round(tnt.getFuse() / 20.0F, 1) + " сек";
                Vector3d pos = RenderUtilka.interpolate(tnt, mc.getRenderPartialTicks());
                Vector2f vec = project.project2D(pos.x, pos.y + tnt.getHeight() + 0.5, pos.z);
                if (vec.x == Float.MAX_VALUE && vec.y == Float.MAX_VALUE) return false;


                float width = Fonts.intl[13].getWidth(name) + 3;

                float halfWidth = width / 2.0F;
                int colorsbox2 = rgba(15, 15, 16, 125);

                RenderUtilka.Render2D.drawRoundedRect(vec.x - halfWidth - 1f, vec.y, width, Fonts.intl[13].getFontHeight(), 0, colorsbox2);
                Fonts.intl[13].drawStringWithShadow(stack, TextFormatting.RED + name, 0.5F + vec.x - halfWidth, 3.5F + vec.y, -1);
            }
        }
        return false;
    }
}
