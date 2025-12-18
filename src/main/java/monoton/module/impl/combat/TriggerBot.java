package monoton.module.impl.combat;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "TriggerBot", type = TypeList.Combat)
public class TriggerBot extends Module {


    private final BooleanOption onlyCritical = new BooleanOption("Только криты", true);
    private final BooleanOption onlySpaceCritical = new BooleanOption("Только с пробелом", false)
            .setVisible(onlyCritical::get);

    public TriggerBot() {
        addSettings(onlyCritical, onlySpaceCritical);
    }

    private long cpsLimit = 0;

    @Compile
    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate e) {
            if (cpsLimit > System.currentTimeMillis()) {
                cpsLimit--;
            }

            if (mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY) {
                if (whenFalling() && (cpsLimit <= System.currentTimeMillis())) {
                    cpsLimit = System.currentTimeMillis() + 550;
                    if (mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY) {
                        mc.playerController.attackEntity(mc.player, ((EntityRayTraceResult) mc.objectMouseOver).getEntity());
                        mc.player.swingArm(Hand.MAIN_HAND);
                    }
                }
            }
        }
        return false;
    }

    public boolean reasonForCancelCritical;

    @Compile
    public boolean whenFalling() {
        boolean critWater = mc.player.areEyesInFluid(FluidTags.WATER);

        reasonForCancelCritical = mc.player.isPotionActive(Effects.BLINDNESS)
                || mc.player.isOnLadder()
                || mc.player.isInWater() && critWater
                || mc.player.isRidingHorse()
                || mc.player.abilities.isFlying
                || mc.player.isElytraFlying();

        final boolean onSpace = onlySpaceCritical.get()
                && mc.player.isOnGround()
                && !mc.gameSettings.keyBindJump.isKeyDown();

        if (mc.player.getCooledAttackStrength(1.5F) < 0.92F)
            return false;
        if (!reasonForCancelCritical && onlyCritical.get()) {
            return onSpace || !mc.player.isOnGround() && mc.player.fallDistance > 0.0F;
        }

        return true;
    }

}
