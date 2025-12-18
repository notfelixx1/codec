package monoton.cmd;

import monoton.cmd.impl.*;
import monoton.control.Manager;
import monoton.utils.other.OtherUtil;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdManager {
    public List<Cmd> cmdList = new ArrayList<>();
    public boolean isMessage;
    private String prefix = ".";

    @Compile
    @VMProtect(type = VMProtectType.MUTATION)
    public void init() {
        cmdList.addAll(Arrays.asList(
                new TargetCmd(),
                new ContractCmd(),
                new BotCmd(),
                new ParseCmd(),
                new WayCmd(),
                new RctCmd(),
                new HClipCmd(),
                new VClipCmd(),
                new HelpCmd(),
                new MacroCmd(),
                new BindCmd(),
                new PartyCmd(),
                new ConfigCmd(),
                new FriendCmd(),
                new PanicCmd(),
                new RgCmd(),
                new StaffCmd(),
                new PrefixCmd(),
                new GPSCmd()
        ));
    }

    public void setPrefix(String newPrefix) {
        this.prefix = newPrefix;
    }

    public void runCommands(String message) {
        if (Manager.FUNCTION_MANAGER.noCommands.state) {
            isMessage = false;
            return;
        }
        if (message.startsWith(prefix)) {
            for (Cmd cmd : Manager.COMMAND_MANAGER.getCommands()) {
                if (message.startsWith(prefix + cmd.command)) {
                    try {
                        cmd.run(message.split(" "));
                    } catch (Exception ex) {
                        cmd.error();
                        ex.printStackTrace();
                    }
                    isMessage = true;
                    return;
                }
            }
            OtherUtil.sendMessage(TextFormatting.RED + "Команда не найдена!");
            OtherUtil.sendMessage(TextFormatting.GRAY + "Используйте " + TextFormatting.RED + prefix + "help" + TextFormatting.GRAY + " для списка всех команд.");
            isMessage = true;
        } else {
            isMessage = false;
        }
    }

    public List<Cmd> getCommands() {
        return cmdList;
    }
}