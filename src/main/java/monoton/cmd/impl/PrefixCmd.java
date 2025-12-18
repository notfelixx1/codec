package monoton.cmd.impl;

import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.control.Manager;
import monoton.utils.other.OtherUtil;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@CmdInfo(name = "prefix", description = "Установить префикс для команд")
public class PrefixCmd extends Cmd {
    @Compile
    @Override
    public void run(String[] args) {
        if (args.length != 2) {
            OtherUtil.sendMessage(TextFormatting.RED + "Использование: .prefix <новый_префикс>");
            return;
        }

        String newPrefix = args[1];
        if (!isValidPrefix(newPrefix)) {
            OtherUtil.sendMessage(TextFormatting.RED + "Недопустимый символ в префиксе!");
            return;
        }

        Manager.COMMAND_MANAGER.setPrefix(newPrefix);
        OtherUtil.sendMessage(TextFormatting.GREEN + "Префикс установлен на: " + newPrefix);
    }

    private boolean isValidPrefix(String prefix) {
        return prefix.length() == 1;
    }

    @Override
    public void error() {
    }
}
