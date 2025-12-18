package monoton.utils.math;

import monoton.control.Manager;
import monoton.utils.other.RandUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import monoton.utils.IMinecraft;

import static net.minecraft.util.math.MathHelper.clamp;

public class AuraUtil implements IMinecraft {

    public static Vector3d getvector(Vector3d vec, AxisAlignedBB AABB) {
        return new Vector3d(MathHelper.clamp(vec.getX(), AABB.minX, AABB.maxX), MathHelper.clamp(vec.getY(), AABB.minY, AABB.maxY), MathHelper.clamp(vec.getZ(), AABB.minZ, AABB.maxZ));
    }
    public static boolean isPlayerFalling(boolean onlyCrit, boolean onlySpace) {

        boolean cancelReason = mc.player.areEyesInFluid(FluidTags.WATER) && mc.player.movementInput.jump || mc.player.areEyesInFluid(FluidTags.LAVA) && mc.player.movementInput.jump || mc.player.isOnLadder() || mc.world.getBlockState(new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ())).getBlock() == Blocks.COBWEB || mc.player.isPassenger() || mc.player.abilities.isFlying || mc.player.isPotionActive(Effects.LEVITATION) || mc.player.isPotionActive(Effects.BLINDNESS);
        boolean onSpace = !mc.gameSettings.keyBindJump.isKeyDown() && mc.player.isOnGround() && onlySpace;
        float attackStrength = mc.player.getCooledAttackStrength(
                        Manager.FUNCTION_MANAGER.syncTps.state ?
                        (Manager.FUNCTION_MANAGER.syncTps.adjustTicks) :
                        (Manager.FUNCTION_MANAGER.auraFunction.rotationMode.is("Spooky") ? RandUtils.LegitFloat(0.97f, 1.0f, RandUtils.PatternMode.MICRO_CORRECTIONS) : 1.5F));
        if (attackStrength < 0.92) return false;
        if (!cancelReason && onlyCrit) {
            return onSpace || !mc.player.isOnGround() && mc.player.fallDistance > 0;
        }

        return true;
    }
    public static Vector3d getvector(Vector3d vec, Entity entity) {
        return getvector(vec, entity.getBoundingBox());
    }

    public static Vector3d getVector(Entity entity) {
        Vector3d eyePosVec = mc.player.getEyePosition(mc.getRenderPartialTicks());
        return getvector(eyePosVec, entity).subtract(eyePosVec);
    }

    public static Vector3d getVec(Vector3d vec, AxisAlignedBB AABB) {
        return new Vector3d(MathHelper.clamp(vec.getX(), AABB.minX, AABB.maxX), MathHelper.clamp(vec.getY(), AABB.minY, AABB.maxY), MathHelper.clamp(vec.getZ(), AABB.minZ, AABB.maxZ));
    }

    public static Vector3d calculateTargetVector(LivingEntity target) {
        Vector3d targetEyePosition = target.getPositionVec().add(0, target.getEyeHeight() - 0.24, 0);
        return targetEyePosition.subtract(mc.player.getEyePosition(1.0F));
    }
    public static Vector3d getVectorHoly(LivingEntity target) {
        double wHalf = (double)(target.getWidth() / 2.0F);
        double yExpand = MathHelper.clamp(target.getPosYEye() - target.getPosY(), (double)0.0F, (double)target.getHeight());
        double xExpand = MathHelper.clamp(mc.player.getPosX() - target.getPosX(), -wHalf, wHalf);
        double zExpand = MathHelper.clamp(mc.player.getPosZ() - target.getPosZ(), -wHalf, wHalf);
        return new Vector3d(target.getPosX() - mc.player.getPosX() + xExpand, target.getPosY() - mc.player.getPosYEye() + yExpand, target.getPosZ() - mc.player.getPosZ() + zExpand);
    }
}