package monoton.cmd.impl;

import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.utils.other.OtherUtil;
import net.minecraft.client.entity.player.ClientPlayerEntity;

@CmdInfo(name = "rg", description = "Приватит место на указанных координатах с радиусом. .rg <x> <y> <z>")
public class RgCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        if (args.length < 4) {
            OtherUtil.sendMessage("§cУкажите координаты привата: .rg <x> <y> <z>");
            return;
        }

        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        try {
            int x = Integer.parseInt(args[1]);
            int y = Integer.parseInt(args[2]);
            int z = Integer.parseInt(args[3]);
            int radius = 5;

            int y1 = y - 5;
            int y2 = y + 5;

            int x1 = x - radius;
            int z1 = z - radius;
            int x2 = x + radius;
            int z2 = z + radius;

            String rgrandomname = String.valueOf((long) (Math.random() * 1_000_000_000L));
            while (rgrandomname.length() < 9) rgrandomname = "0" + rgrandomname;
            String regionName = "mon" + rgrandomname;

            OtherUtil.sendMessage("§aСоздаю регион §e" + regionName + " §aцентр: §e" + x + " " + y + " " + z);

            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    p.sendChatMessage("//pos1 " + x1 + "," + y1 + "," + z1);
                }
            }, 850);

            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    p.sendChatMessage("//pos2 " + x2 + "," + y2 + "," + z2);
                }
            }, 1700);

            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    p.sendChatMessage("/rg claim " + regionName);
                    OtherUtil.sendMessage("§aРегион §e" + regionName + " §aуспешно создан!");
                    OtherUtil.sendMessage("§7Размер: §f" + (radius*2+1) + "x" + (radius*2+1) + "x" + (y2-y1+1));
                }
            }, 2550);

        } catch (NumberFormatException e) {
            OtherUtil.sendMessage("§cУкажите корректные координаты (целые числа)!");
        }
    }

    @Override
    public void error() {
        OtherUtil.sendMessage("§cИспользование: .rg <x> <y> <z>");
    }
}