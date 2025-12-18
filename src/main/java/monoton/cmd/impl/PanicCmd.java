package monoton.cmd.impl;

import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.control.Manager;
import monoton.utils.other.OtherUtil;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@CmdInfo(name = "panic", description = "Выключает все функции чита")

public class PanicCmd extends Cmd {
    @Compile
    @Override
    public void run(String[] args) throws Exception {
        if (args.length == 1) {
            Manager.FUNCTION_MANAGER.getFunctions().stream().filter(function -> function.state).forEach(function -> function.setState(false));
            OtherUtil.sendMessage("Выключил все модули!");
        } else error();
    }

    @Override
    public void error() {

    }
}
