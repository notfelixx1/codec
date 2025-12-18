package monoton.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;

import monoton.control.events.client.Event;
import monoton.control.events.render.EventRenderChunk;
import monoton.control.events.render.EventRenderChunkContainer;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.render.shader.Easing;
import monoton.utils.render.shader.Easings;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Annotation(name = "Animation", type = TypeList.Render, desc = "Растягивает ваш экран")

public class Animation extends Module {
    public MultiBoxSetting mode = new MultiBoxSetting("Что анимировать", new BooleanOption("Список игроков", false), new BooleanOption("Инвентарь", false), new BooleanOption("Приближение камеры", false), new BooleanOption("Обновление чанков", false), new BooleanOption("Изменение перспективы", false));
    public ModeSetting chunkanim = new ModeSetting("Анимация чанков", "Quart", "Quart", "Circ", "Sine", "Cubic").setVisible(() -> mode.get("Обновление чанков"));
    private final SliderSetting chunkSpeed = new SliderSetting("Скорость", 6.0F, 2.0F, 10.0F, 1.0F).setVisible(() -> mode.get("Обновление чанков"));

    public Animation() {
        addSettings(mode, chunkanim, chunkSpeed);
    }

    private final WeakHashMap<ChunkRenderDispatcher.ChunkRender, AtomicLong> renderChunkMap = new WeakHashMap<>();

    private double applySelectedEasing(double t) {
        String modeName = chunkanim.get();
        Easing easing;
        if ("Circ".equalsIgnoreCase(modeName)) {
            easing = Easings.CIRC_OUT;
        } else if ("Sine".equalsIgnoreCase(modeName)) {
            easing = Easings.SINE_OUT;
        } else if ("Cubic".equalsIgnoreCase(modeName)) {
            easing = Easings.CUBIC_OUT;
        } else {
            easing = Easings.QUART_OUT;
        }
        return easing.ease(t);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRenderChunk e) {
            onRenderChunk(e);
        }
        else if (event instanceof EventRenderChunkContainer e) {
            onRenderChunkContainer(e);
        }
        return false;
    }

    private void onRenderChunk(EventRenderChunk e) {
        if (!Boolean.TRUE.equals(mode.get("Обновление чанков"))) {
            return;
        }
        if (mc.player != null && mc.world != null) {
            if (!renderChunkMap.containsKey(e.getChunkRender())) {
                renderChunkMap.put(e.getChunkRender(), new AtomicLong(-1L));
            }
        }
    }

    private void onRenderChunkContainer(EventRenderChunkContainer e) {
        if (!Boolean.TRUE.equals(mode.get("Обновление чанков"))) {
            return;
        }
        if (renderChunkMap.containsKey(e.getChunkRender())) {
            AtomicLong timeAlive = renderChunkMap.get(e.getChunkRender());
            long timeClone = timeAlive.get();
            if (timeClone == -1L) {
                timeClone = System.currentTimeMillis();
                timeAlive.set(timeClone);
            }

            long timeDifference = System.currentTimeMillis() - timeClone;
            double durationMs = chunkSpeed.getValue().floatValue() * 100;
            if (timeDifference <= durationMs) {
                double chunkY = e.getChunkRender().getPosition().getY();
                double t = timeDifference / durationMs;
                double offsetY = chunkY * applySelectedEasing(t);
                RenderSystem.translated(0.0D, -chunkY + offsetY, 0.0D);
            }
        }
    }

}
