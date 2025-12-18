package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.utils.render.ProjectionUtils;
import monoton.utils.render.RenderUtilka;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.joml.Vector2d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static monoton.ui.clickgui.Panel.selectedColor;
import static monoton.utils.render.RenderUtilka.Render2D.*;

@Annotation(name = "FireworkEsp", type = TypeList.Render, desc = "Создаёт метку там где был запущен фейрверк")
public class FireworkEsp extends Module {

    private static final int MAX_FIREWORKS = 15;
    private static final int ITEM_RENDER_OFFSET = 8;
    private static final long FIREWORK_FADE_TIME = 1400;

    private final Map<FireworkRocketEntity, FireworkData> fireworks = new ConcurrentHashMap<>();

    private static class FireworkData {
        final double x;
        final double y;
        final double z;
        final long spawnTime;

        FireworkData(double x, double y, double z, long spawnTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.spawnTime = spawnTime;
        }
    }

    public FireworkEsp() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        if (!(event instanceof EventRender render) || mc.player == null || mc.world == null) {
            return false;
        }

        try {
            if (render.isRender3D()) {
                updateFireworks();
            }

            if (render.isRender2D()) {
                renderFireworks(render.partialTicks);
            }
        } catch (Exception e) {
            System.err.println("Error in ItemEsp rendering: " + e.getMessage());
        }

        return false;
    }

    private void updateFireworks() {
        long currentTime = System.currentTimeMillis();
        fireworks.entrySet().removeIf(entry -> currentTime - entry.getValue().spawnTime > FIREWORK_FADE_TIME);

        if (fireworks.size() < MAX_FIREWORKS) {
            for (Entity entity : mc.world.getAllEntities()) {
                if (entity instanceof FireworkRocketEntity fireworkEntity && !fireworks.containsKey(fireworkEntity)) {
                    fireworks.put(fireworkEntity, new FireworkData(fireworkEntity.getPosX(), fireworkEntity.getPosY(), fireworkEntity.getPosZ(), currentTime));
                    if (fireworks.size() >= MAX_FIREWORKS) {
                        break;

                    }
                }
            }
        }
    }

    private void renderFireworks(float partialTicks) {
        long currentTime = System.currentTimeMillis();
        int count = 0;

        for (Map.Entry<FireworkRocketEntity, FireworkData> entry : fireworks.entrySet()) {
            if (count >= MAX_FIREWORKS) break;

            FireworkRocketEntity firework = entry.getKey();
            FireworkData data = entry.getValue();
            long timeAlive = currentTime - data.spawnTime;

            if (timeAlive > FIREWORK_FADE_TIME) {
                continue;
            }

            try {
                Vector2d screenPos = ProjectionUtils.project(
                        data.x,
                        data.y,
                        data.z
                );

                if (screenPos != null) {
                    float fade = 1.0f - (timeAlive / (float)FIREWORK_FADE_TIME);
                    int firstColor2 = selectedColor;
                    RenderUtilka.Render2D.drawRoundedRect((int) screenPos.x - ITEM_RENDER_OFFSET + 2, (int) screenPos.y - ITEM_RENDER_OFFSET + 2, 13, 13, 5, new Color(25, 25, 28, 172).getRGB());
                    drawCircle(
                            (int) screenPos.x - ITEM_RENDER_OFFSET + 8, (int) screenPos.y - ITEM_RENDER_OFFSET + 8,
                            0,
                            360 * fade,
                            6.5f,
                            1,
                            false,
                            firstColor2
                    );
                    GL11.glPushMatrix();
                    GL11.glTranslated(screenPos.x, screenPos.y, 0);
                    GL11.glScaled(0.6, 0.6, 0.6);
                    GL11.glTranslated(-screenPos.x, -screenPos.y, 0);

                    GL11.glColor4f(1, 1, 1, fade);

                    mc.getItemRenderer().renderItemAndEffectIntoGUI(
                            firework.getItem(),
                            (int) screenPos.x - ITEM_RENDER_OFFSET,
                            (int) screenPos.y - ITEM_RENDER_OFFSET
                    );
                    GL11.glColor4f(1, 1, 1, 1);
                    GL11.glPopMatrix();
                    count++;
                }
            } catch (Exception e) {
                System.err.println("Failed to render firework: " + e.getMessage());
            }
        }
    }
}
