package monoton.module.impl.combat;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import java.util.Comparator;

@Annotation(name = "CrystalOptimizer", type = TypeList.Combat, desc = "Взрывает кристаллы на которые навёлся")
public class CrystallOptimizer extends Module {
    private long lastAttackTime = 0;
    private int attackTicks = 0;
    private final int BASE_ATTACK_TICKS = 1;

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            handleUpdate();
        }
        return false;
    }

    @Override
    public void onDisable() {
        lastAttackTime = 0;
        attackTicks = 0;
    }

    private void handleUpdate() {
        boolean holdingCrystal = mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL ||
                mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL;
        if (!holdingCrystal || mc.player.getCooledAttackStrength(1.0F) < 1.0F) {
            return;
        }

        float tps = SyncTps.TPS;
        float tickRate = Math.max(tps, 1.0F);
        float ticksPerAttack = BASE_ATTACK_TICKS * (20.0F / tickRate);

        attackTicks++;
        if (attackTicks < (int) Math.ceil(ticksPerAttack)) {
            return;
        }

        Entity crystal = mc.world.getEntitiesWithinAABB(EnderCrystalEntity.class,
                        mc.player.getBoundingBox().grow(mc.playerController.getBlockReachDistance()))
                .stream()
                .filter(this::canBeSeen)
                .min(Comparator.comparingDouble(e -> mc.player.getDistanceSq(e)))
                .orElse(null);

        if (crystal != null) {
            mc.playerController.attackEntity(mc.player, crystal);
            mc.player.swingArm(Hand.MAIN_HAND);
            lastAttackTime = System.currentTimeMillis();
            attackTicks = 0;
        }
    }

    private boolean canBeSeen(Entity entity) {
        double reach = mc.playerController.getBlockReachDistance();
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        AxisAlignedBB bb = entity.getBoundingBox().grow(0.1);

        Vector3d[] corners = {
                new Vector3d(bb.minX, bb.minY, bb.minZ),
                new Vector3d(bb.minX, bb.minY, bb.maxZ),
                new Vector3d(bb.minX, bb.maxY, bb.minZ),
                new Vector3d(bb.minX, bb.maxY, bb.maxZ),
                new Vector3d(bb.maxX, bb.minY, bb.minZ),
                new Vector3d(bb.maxX, bb.minY, bb.maxZ),
                new Vector3d(bb.maxX, bb.maxY, bb.minZ),
                new Vector3d(bb.maxX, bb.maxY, bb.maxZ)
        };

        for (Vector3d corner : corners) {
            RayTraceContext context = new RayTraceContext(
                    eyePos, corner,
                    RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE,
                    mc.player
            );
            RayTraceResult result = mc.world.rayTraceBlocks(context);
            if (result.getType() == RayTraceResult.Type.MISS) {
                if (eyePos.squareDistanceTo(corner) <= reach * reach) {
                    return true;
                }
            }
        }
        return false;
    }
}