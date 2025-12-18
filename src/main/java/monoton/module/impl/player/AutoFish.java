package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.utils.misc.TimerUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.Hand;

@Annotation(name = "AutoFish", type = TypeList.Player, desc = "Автоматически ловит рыбу")
public class AutoFish extends Module {
    private final TimerUtil delay = new TimerUtil();
    private boolean isHooked = false;
    private boolean needToHook = false;

    @Override
    public boolean onEvent(Event event) {
        if (mc.player != null && mc.world != null) {
            if (event instanceof EventPacket) {
                EventPacket e = (EventPacket)event;
                IPacket var4 = e.getPacket();
                if (var4 instanceof SPlaySoundEffectPacket) {
                    SPlaySoundEffectPacket p = (SPlaySoundEffectPacket)var4;
                    if (p.getSound().getName().getPath().equals("entity.fishing_bobber.splash")) {
                        this.isHooked = true;
                        this.delay.reset();
                    }
                }
            }

            if (event instanceof EventUpdate) {
                EventUpdate e = (EventUpdate)event;
                if (this.isHooked) {
                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    this.isHooked = false;
                    this.needToHook = true;
                }

                if (this.needToHook) {
                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    this.needToHook = false;
                }
            }

        }
        return false;
    }

    protected void onDisable() {
        super.onDisable();
        this.delay.reset();
        this.isHooked = false;
        this.needToHook = false;
    }
}
