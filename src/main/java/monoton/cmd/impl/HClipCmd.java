package monoton.cmd.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.utils.other.OtherUtil;

@CmdInfo(name = "hclip", description = "Телепортирует вас вперед")
public class HClipCmd extends Cmd {
    @Override
    public void run(String[] args) throws Exception {
        if (args.length < 2) {
            OtherUtil.sendMessage("Укажите дистанцию телепортации");
            return;
        }

        try {
            double distance = Double.parseDouble(args[1]);
            Vector3d tp = Minecraft.getInstance().player.getLook(1F).scale(distance);
            Minecraft.getInstance().player.setPosition(
                    Minecraft.getInstance().player.getPosX() + tp.x,
                    Minecraft.getInstance().player.getPosY(),
                    Minecraft.getInstance().player.getPosZ() + tp.z
            );
            OtherUtil.sendMessage("Вы телепортированы на " + TextFormatting.RED + distance + TextFormatting.WHITE + " блоков вперед");
        } catch (NumberFormatException e) {
            OtherUtil.sendMessage("Укажите корректное число для дистанции");
        }
    }

    @Override
    public void error() {
    }
}
