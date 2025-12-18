package monoton.cmd.impl;

import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.utils.other.OtherUtil;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@CmdInfo(name = "contract", description = "Ставит цель для пойска через автоконтракт")

public class ContractCmd extends Cmd {
    public static String nickName = null;

    @Compile
    public void run(String[] args) throws Exception {
        if (args[1].contains("info")) {
            OtherUtil.sendMessage(TextFormatting.GREEN + "Активная цель: " + nickName);
        } else if (!args[1].isEmpty()) {
            nickName = args[1];
            OtherUtil.sendMessage(TextFormatting.GREEN + "Ваша цель: " + nickName);
        }
    }

    @Compile
    public void error() {
        OtherUtil.sendMessage(TextFormatting.GRAY + "Ошибка в использовании");
        OtherUtil.sendMessage(".contract " + TextFormatting.RED + "<info>");
        OtherUtil.sendMessage(".contract " + TextFormatting.RED + "<name>");
    }
}
