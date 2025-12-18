package monoton.cmd.impl;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.utils.other.OtherUtil;

@CmdInfo(name = "vclip", description = "Телепортирует вверх/вниз с прилипанием к потолку")
public class VClipCmd extends Cmd {

    private final Minecraft mc = Minecraft.getInstance();
    private static final double HEAD_CLEARANCE = 0.015;

    @Override
    public void run(String[] args) throws Exception {
        if (args.length < 2) {
            OtherUtil.sendMessage("Использование: .vclip <up/down/число>");
            return;
        }

        String firstArg = args[1].toLowerCase();

        try {
            double offset = Double.parseDouble(args[1]);
            double newY = mc.player.getPosY() + offset;
            mc.player.setPosition(mc.player.getPosX(), newY, mc.player.getPosZ());

            String sign = offset > 0 ? "вверх" : "вниз";
            OtherUtil.sendMessage("Телепортирован на " + TextFormatting.RED +
                    String.format("%.2f", Math.abs(offset)) + TextFormatting.WHITE +
                    " блоков " + sign);
            return;
        } catch (NumberFormatException ignored) {
        }

        if (firstArg.equals("up")) {
            if (args.length >= 3) {
                teleportUpManual(args[2]);
            } else {
                teleportUpAuto();
            }
        } else if (firstArg.equals("down")) {
            if (args.length > 2) {
                OtherUtil.sendMessage("Для .vclip down высота не нужна");
                return;
            }
            teleportDownToCeiling();
        } else {
            OtherUtil.sendMessage("Укажите направление: up или down (или просто число)");
        }
    }

    private void teleportUpManual(String arg) {
        try {
            double offset = Double.parseDouble(arg);
            double newY = mc.player.getPosY() + offset;
            mc.player.setPosition(mc.player.getPosX(), newY, mc.player.getPosZ());
            OtherUtil.sendMessage("Телепортирован на " + TextFormatting.RED + offset + TextFormatting.WHITE + " блоков вверх");
        } catch (NumberFormatException e) {
            OtherUtil.sendMessage("Укажите корректное число");
        }
    }

    private void teleportUpAuto() {
        double playerY = mc.player.getPosY();
        int startY = (int) Math.ceil(playerY);
        double playerHeight = getPlayerHeight();

        for (int y = startY + 1; y <= mc.world.getHeight(); y++) {
            if (canFitAtFloor(y, playerHeight)) {
                mc.player.setPosition(mc.player.getPosX(), y, mc.player.getPosZ());
                return;
            }
        }
        OtherUtil.sendMessage(TextFormatting.RED + "Свободное место выше не найдено");
    }

    private void teleportDownToCeiling() {
        double playerY = mc.player.getPosY();
        int startY = (int) Math.floor(playerY);
        double playerHeight = getPlayerHeight();

        int cavityStart = -1;
        for (int y = startY - 1; y >= 0; y--) {
            BlockPos pos = new BlockPos(mc.player.getPosX(), y, mc.player.getPosZ());
            if (isPassable(pos)) {
                cavityStart = y;
                break;
            }
        }

        if (cavityStart == -1) {
            OtherUtil.sendMessage(TextFormatting.RED + "Свободное место выше не найдено");
            return;
        }

        int cavityTop = cavityStart;
        for (int y = cavityStart + 1; y <= mc.world.getHeight(); y++) {
            BlockPos pos = new BlockPos(mc.player.getPosX(), y, mc.player.getPosZ());
            if (!isPassable(pos)) {
                cavityTop = y - 1;
                break;
            }
            if (y > cavityStart + 100) break;
        }


        double targetY = (cavityTop + 0.98) - playerHeight + HEAD_CLEARANCE;

        mc.player.setPosition(mc.player.getPosX(), targetY, mc.player.getPosZ());
    }

    private double getPlayerHeight() {
        return mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY;
    }

    private boolean canFitAtFloor(int floorY, double height) {
        int needed = (int) Math.ceil(height);
        for (int i = 0; i < needed; i++) {
            if (!isPassable(new BlockPos(mc.player.getPosX(), floorY + i, mc.player.getPosZ()))) {
                return false;
            }
        }
        return mc.world.getBlockState(new BlockPos(mc.player.getPosX(), floorY - 1, mc.player.getPosZ())).getMaterial().blocksMovement();
    }

    private boolean isPassable(BlockPos pos) {
        if (!mc.world.isBlockLoaded(pos)) return false;
        BlockState state = mc.world.getBlockState(pos);
        Material mat = state.getMaterial();
        return state.isAir() || mat == Material.WATER || mat == Material.LAVA;
    }

    @Override
    public void error() {}
}