package monoton.module.impl.combat;

import com.mojang.blaze3d.platform.GlStateManager;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventMotion;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.render.RenderUtilka;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.util.ResourceLeakDetector.isEnabled;

// Улучшенная логика: хранение позиций до 1 секунды
@Annotation(name = "BackTrack", type = TypeList.Combat, desc = "Задерживает хитбокс игроков для увеличения дальности удара")
public class BackTrack extends Module {

    // Слайдер теперь позволяет устанавливать время в миллисекундах (от 100 до 1000 мс)
    private final SliderSetting time = new SliderSetting("Время (мс)", 600, 100, 1000, 50);
    public final BooleanOption render = new BooleanOption("Отображать", true);

    // Карта для хранения прошлых позиций каждого игрока
    private final Map<PlayerEntity, List<Position>> backtrackPositions = new HashMap<>();

    public BackTrack() {
        this.addSettings(time, render);
    }

    @Override
    public boolean onEvent(Event event) {
        if (mc == null || mc.player == null || mc.world == null) return false;

        if (event instanceof EventMotion) {
            handleMotion();
        } else if (event instanceof EventRender e && e.isRender3D()) {
            handleRender(e);
        }
        return false;
    }

    // --- Логика обработки движения и сохранения позиций ---

    private void handleMotion() {
        // Максимальная задержка в миллисекундах, берется из слайдера
        final long maxDelay = time.getValue().longValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            List<Position> positions = backtrackPositions.computeIfAbsent(player, k -> new ArrayList<>());

            // 1. Добавляем текущую позицию
            positions.add(new Position(
                    new Vector3d(player.getPosX(), player.getPosY(), player.getPosZ()),
                    System.currentTimeMillis()
            ));

            // 2. Удаляем устаревшие позиции
            // Удаляем позиции, время которых превышает установленную задержку
            positions.removeIf(pos -> (System.currentTimeMillis() - pos.getTime()) > maxDelay);
        }
    }

    // --- Методы для использования другими модулями (KillAura) ---

    /**
     * Возвращает самую старую позицию, которая все еще находится в пределах времени задержки.
     * Эта позиция является лучшей целью для "замораживания" хитбокса.
     */
    public Position getOldestPosition(PlayerEntity player) {
        if (!isEnabled()) return null;
        List<Position> positions = backtrackPositions.get(player);
        if (positions == null || positions.isEmpty()) return null;

        // Самая старая позиция находится в начале списка после очистки.
        return positions.get(0);
    }

    /**
     * Создает AABB игрока, смещенный к его сохраненной BackTrack позиции.
     * Это можно использовать для изменения хитбокса во время удара.
     */
    public AxisAlignedBB getBackTrackBox(PlayerEntity player, Position backtrackPos) {
        if (backtrackPos == null) return null;

        // Получаем текущие размеры игрока
        float width = player.getWidth();
        float height = player.getHeight();

        Vector3d pos = backtrackPos.getPos();

        // Создаем новый AABB в сохраненной позиции
        // Стандартные размеры хитбокса игрока: 0.6x1.8 (ширина x высота)
        return new AxisAlignedBB(
                pos.x - width / 2.0,
                pos.y,
                pos.z - width / 2.0,
                pos.x + width / 2.0,
                pos.y + height,
                pos.z + width / 2.0
        );
    }

    // --- Логика рендеринга ---

    private void handleRender(EventRender e) {
        if (!render.get()) return;

        EntityRendererManager renderManager = mc.getRenderManager();
        if (renderManager == null || renderManager.info == null) return;

        final long maxDelay = time.getValue().longValue();

        GL11.glPushMatrix();
        // Настройка рендеринга для 3D бокса
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();
        GlStateManager.enableDepthTest();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_DST_ALPHA);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            List<Position> positions = backtrackPositions.getOrDefault(player, new ArrayList<>());
            for (Position position : positions) {
                try {
                    Vector3d pos = position.getPos();
                    Vector3d cameraPos = renderManager.info.getProjectedView();

                    // Вычисление смещения относительно камеры
                    double x = pos.x - cameraPos.x;
                    double y = pos.y - cameraPos.y;
                    double z = pos.z - cameraPos.z;

                    // Вычисление прозрачности (fade-out эффект)
                    float timePassed = (float) (System.currentTimeMillis() - position.getTime());
                    // Альфа = 1 - (прошедшее время / общее время)
                    float alpha = 1.0f - (timePassed / maxDelay);
                    alpha = Math.max(0, Math.min(alpha, 0.5f)); // Ограничение максимальной прозрачности до 0.5


                    GL11.glPushMatrix();
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glLineWidth(2);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);

                    // Создание AABB для отрисовки
                    float width = player.getWidth();
                    float height = player.getHeight();
                    AxisAlignedBB box = new AxisAlignedBB(
                            x - width / 2.0,
                            y,
                            z - width / 2.0,
                            x + width / 2.0,
                            y + height,
                            z + width / 2.0
                    );

                    // Отрисовка бокса: белый цвет с расчетом прозрачности
                    int color = new Color(1.0f, 1.0f, 1.0f, alpha).getRGB();
                    RenderUtilka.Render3D.drawBox(box, color);

                    // Восстановление состояния GL
                    GL11.glLineWidth(1);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glPopMatrix();

                } catch (Exception ex) {
                    // Игнорирование ошибок рендеринга
                }
            }
        }

        // Восстановление общего состояния GL
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GL11.glPopMatrix();
    }

    @Override
    public void onEnable() {
        backtrackPositions.clear();
    }

    @Override
    public void onDisable() {
        backtrackPositions.clear();
    }

    // --- Внутренний класс для хранения позиции и времени ---

    static class Position {
        private final Vector3d pos;
        private final long time; // Время сохранения позиции

        public Position(Vector3d pos, long time) {
            this.pos = pos;
            this.time = time;
        }

        public Vector3d getPos() {
            return pos;
        }

        public long getTime() {
            return time;
        }
    }
}