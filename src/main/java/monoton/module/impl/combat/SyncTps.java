package monoton.module.impl.combat;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.util.math.MathHelper;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "SyncTps", type = TypeList.Combat, desc = "Синхронизирует чит под лаги сервера")
public class SyncTps extends Module {
    public static float TPS = 20.0F;
    public static float adjustTicks = 0.0F;
    public static float tickAdjustmentFactor = 1.0F;
    private long timestamp;
    private float emaTPS = 20.0F;
    private static final float SMOOTHING_FACTOR = 0.1F;
    private static final float MAX_TPS = 20.0F;

    public SyncTps() {
        this.timestamp = System.nanoTime();
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket e) {
            if (e.getPacket() instanceof SUpdateTimePacket) {
                long delay = System.nanoTime() - this.timestamp;
                float rawTPS = MAX_TPS * (1.0E9F / (float) delay);
                float boundedTPS = MathHelper.clamp(rawTPS, 0.0F, MAX_TPS);
                this.emaTPS = this.emaTPS + SMOOTHING_FACTOR * (boundedTPS - this.emaTPS);
                TPS = (float) this.round((double) this.emaTPS);
                adjustTicks = this.emaTPS - MAX_TPS;
                tickAdjustmentFactor = MAX_TPS / TPS;
                this.timestamp = System.nanoTime();
            }
        }
        return false;
    }

    public double round(double input) {
        return (double) Math.round(input * 100.0) / 100.0;
    }

    @Compile
    public void onDisable() {
        mc.timer.timerSpeed = 1.0F;
        super.onDisable();
    }
}