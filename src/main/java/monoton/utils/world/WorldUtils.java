package monoton.utils.world;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import monoton.utils.IMinecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorldUtils implements IMinecraft {

    public static class TotemUtil {
        public static BlockPos getBlock(float distance, Block block) {
            BlockPos playerPos = getPlayerPosLocal();
            return getSphere(playerPos, distance, 6, false, true, 0)
                    .stream()
                    .filter(position -> mc.world.getBlockState(position).getBlock() == block)
                    .min(Comparator.comparing(blockPos -> getDistanceOfEntityToBlock(mc.player, blockPos)))
                    .orElse(null);
        }

        public static BlockPos getBlock(float distance) {
            BlockPos playerPos = getPlayerPosLocal();
            return getSphere(playerPos, distance, 6, false, true, 0)
                    .stream()
                    .filter(position -> mc.world.getBlockState(position).getBlock() != net.minecraft.block.Blocks.AIR)
                    .min(Comparator.comparing(blockPos -> getDistanceOfEntityToBlock(mc.player, blockPos)))
                    .orElse(null);
        }

        public static List<BlockPos> getSphere(final BlockPos blockPos, final float radius, final int height, final boolean hollow,
                                               final boolean includeY, final int yOffset) {
            List<BlockPos> sphere = new ArrayList<>();
            int x = blockPos.getX();
            int y = blockPos.getY();
            int z = blockPos.getZ();

            int radiusInt = (int) radius;
            for (int dx = -radiusInt; dx <= radiusInt; dx++) {
                for (int dz = -radiusInt; dz <= radiusInt; dz++) {
                    for (int dy = includeY ? -radiusInt : 0; dy < (includeY ? radiusInt : height); dy++) {
                        double distanceSq = dx * dx + dz * dz + (includeY ? dy * dy : 0);
                        if (distanceSq < radius * radius && (!hollow || distanceSq >= (radius - 1) * (radius - 1))) {
                            sphere.add(new BlockPos(x + dx, y + dy + yOffset, z + dz));
                        }
                    }
                }
            }
            return sphere;
        }

        public static BlockPos getPlayerPosLocal() {
            if (mc.player == null) {
                return BlockPos.ZERO;
            }
            return new BlockPos(Math.floor(mc.player.getPosX()), Math.floor(mc.player.getPosY()), Math.floor(mc.player.getPosZ()));
        }

        public static double getDistanceOfEntityToBlock(final Entity entity, final BlockPos blockPos) {
            return getDistance(entity.getPosX(), entity.getPosY(), entity.getPosZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }

        public static double getDistance(final double x1, final double y1, final double z1, final double x2,
                                         final double y2, final double z2) {
            double dx = x1 - x2;
            double dy = y1 - y2;
            double dz = z1 - z2;
            return MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public static class Blocks {
        public static List<BlockPos> getAllInBox(BlockPos from, BlockPos to) {
            List<BlockPos> blocks = new ArrayList<>();
            int minX = Math.min(from.getX(), to.getX());
            int minY = Math.min(from.getY(), to.getY());
            int minZ = Math.min(from.getZ(), to.getZ());
            int maxX = Math.max(from.getX(), to.getX());
            int maxY = Math.max(from.getY(), to.getY());
            int maxZ = Math.max(from.getZ(), to.getZ());

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        blocks.add(new BlockPos(x, y, z));
                    }
                }
            }
            return blocks;
        }

        public static CopyOnWriteArrayList<BlockPos> getAllInBoxA(BlockPos from, BlockPos to) {
            CopyOnWriteArrayList<BlockPos> blocks = new CopyOnWriteArrayList<>();
            int minX = Math.min(from.getX(), to.getX());
            int minY = Math.min(from.getY(), to.getY());
            int minZ = Math.min(from.getZ(), to.getZ());
            int maxX = Math.max(from.getX(), to.getX());
            int maxY = Math.max(from.getY(), to.getY());
            int maxZ = Math.max(from.getZ(), to.getZ());

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (mc.world.getBlockState(pos).getBlock() != net.minecraft.block.Blocks.AIR) {
                            blocks.add(pos);
                        }
                    }
                }
            }
            return blocks;
        }
    }
}