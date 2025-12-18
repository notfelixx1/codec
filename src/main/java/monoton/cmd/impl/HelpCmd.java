package monoton.cmd.impl;

import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.control.Manager;
import monoton.utils.other.OtherUtil;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@CmdInfo(name = "help", description = "Список команд чита")
public class HelpCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        for (Cmd cmd : Manager.COMMAND_MANAGER.getCommands()) {
            if (cmd instanceof HelpCmd) continue;
            OtherUtil.sendMessage(TextFormatting.WHITE + cmd.description + TextFormatting.GRAY + " " + TextFormatting.RED + cmd.command);
        }
    }

    @Override
    public void error() {
    }
}
