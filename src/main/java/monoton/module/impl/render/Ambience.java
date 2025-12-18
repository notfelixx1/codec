package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ColorSetting;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.render.ColorUtils;
import net.minecraft.block.Blocks;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.gen.Heightmap;

import java.awt.*;
import java.time.LocalTime;

@Annotation(name = "Ambience", type = TypeList.Render, desc = "Изменяет ваш мир")
public class Ambience extends Module {
    private final ModeSetting timeMode = new ModeSetting("Время", "Не менять", "Рассвет", "Утро", "День", "Заход", "Вечер", "Ночь", "Время из жизни", "Не менять");

    private final BooleanOption snow = new BooleanOption("Снег", true);
    private final SliderSetting sizesnow = new SliderSetting("Сила снегопада", 2f, 0.5f, 5f, 0.5f).setVisible(() -> snow.get());

    private final ModeSetting fogMode = new ModeSetting("Туман", "Ничего не делать", "Ничего не делать", "Очистить", "Переопределить");
    public ColorSetting color = new ColorSetting("Цвет тумана", ColorUtils.rgba(128, 115, 225, 255)).setVisible(() -> fogMode.is("Переопределить"));
    private final SliderSetting fogStart = new SliderSetting("Начало тумана", 0.5f, 0.1f, 1.5f, 0.1f).setVisible(() -> fogMode.is("Переопределить"));
    private final SliderSetting fogEnd = new SliderSetting("Конец тумана", 1.0f, 0.1f, 1.5f, 0.1f).setVisible(() -> fogMode.is("Переопределить"));

    public Ambience() {
        addSettings(timeMode, snow, sizesnow, fogMode, color, fogStart, fogEnd);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket eventPacket && ((EventPacket) event).isReceivePacket()) {
            if (eventPacket.getPacket() instanceof SUpdateTimePacket) {
                eventPacket.setCancel(true);
            }
        }
        if (event instanceof EventUpdate) {
            if (snow.get()) {
                int particlesPerTick = (int) (50 * sizesnow.getValue().floatValue());
                if (mc.world.getGameTime() % 2 != 0) return false;

                for (int i = 0; i < particlesPerTick; i++) {
                    double offsetX = (Math.random() - 0.5) * 130;
                    double offsetZ = (Math.random() - 0.5) * 130;
                    double x = mc.player.getPosX() + offsetX;
                    double z = mc.player.getPosZ() + offsetZ;

                    int surfaceY = mc.world.getHeight(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));

                    double minHeightAboveGround = 5.0;
                    double maxHeightAboveGround = 40.0;
                    double y = surfaceY + minHeightAboveGround + Math.random() * (maxHeightAboveGround - minHeightAboveGround);


                    if (y < mc.player.getPosY() - 20) {
                        y = mc.player.getPosY() + 5.0 + Math.random() * 30.0;
                    }

                    mc.world.addParticle(
                            new BlockParticleData(ParticleTypes.FALLING_DUST, Blocks.SNOW.getDefaultState()),
                            x, y, z,
                            0.0D,
                            -0.03D - Math.random() * 0.03D,
                            0.0D
                    );
                }
            }

            if (!timeMode.get().equals("Не менять")) {
                long time;
                if (timeMode.get().equals("Время из жизни")) {
                    time = getRealWorldTime();
                } else {
                    time = switch (timeMode.get()) {
                        case "Рассвет" -> 23000L;
                        case "Утро" -> 1000L;
                        case "День" -> 6000L;
                        case "Вечер" -> 12000L;
                        case "Заход" -> 13000L;
                        case "Ночь" -> 18000L;
                        default -> mc.world.getDayTime();
                    };
                }
                mc.world.setDayTime(time);
            }
        }
        return false;
    }

    private long getRealWorldTime() {
        LocalTime now = LocalTime.now();
        int hours = now.getHour();
        int minutes = now.getMinute();
        int seconds = now.getSecond();

        int totalSeconds = hours * 3600 + minutes * 60 + seconds;
        int offsetSeconds = (totalSeconds - 6 * 3600) % (24 * 3600);
        if (offsetSeconds < 0) offsetSeconds += 24 * 3600;

        return (long) ((offsetSeconds / 86400.0) * 24000);
    }

    public String getFogMode() {
        return fogMode.get();
    }

    public float getFogStart() {
        return fogStart.getValue().floatValue();
    }

    public float getFogEnd() {
        return fogEnd.getValue().floatValue();
    }

    public Color getFogColor() {
        return new Color(color.get());
    }
}
