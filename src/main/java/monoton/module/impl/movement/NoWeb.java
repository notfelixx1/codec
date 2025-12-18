package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventIgnoreHitbox;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.utils.move.MoveUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

@Annotation(
        name = "NoWeb",
        type = TypeList.Movement,
        desc = "Делает вас быстрее в паутине"
)
public class NoWeb extends Module {
    public final BooleanOption nobreak = new BooleanOption("Игнор ломание", false);

    public NoWeb() {
        addSettings(nobreak);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventIgnoreHitbox e && nobreak.getValue()) {
            onBlockCollide(e);
        }

        if (event instanceof EventUpdate) {
            boolean inWeb = mc.player.isInWeb();

            if (inWeb) {
                mc.player.setVelocity(mc.player.getMotion().x, 0, mc.player.getMotion().z);

                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    mc.player.setVelocity(mc.player.getMotion().x, 0.9, mc.player.getMotion().z);
                }

                if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.player.setVelocity(mc.player.getMotion().x, -0.9, mc.player.getMotion().z);
                }
                
                MoveUtil.setMotion(0.21);
            }
        }
        return false;
    }

    public void onBlockCollide(EventIgnoreHitbox e) {
        if (mc.world.getBlockState(e.getPos()).getBlock() == Blocks.COBWEB) e.setCancel(true);
    }
}