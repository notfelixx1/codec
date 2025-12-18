package monoton.cmd;

import monoton.utils.other.OtherUtil;
import monoton.utils.IMinecraft;

public abstract class Cmd implements IMinecraft {
    public final String command;
    public final String description;

    public Cmd() {
        command = this.getClass().getAnnotation(CmdInfo.class).name();
        description = this.getClass().getAnnotation(CmdInfo.class).description();
    }

    public abstract void run(String[] args) throws Exception;
    public abstract void error();

    public void sendMessage(String message) {
        OtherUtil.sendMessage(message);
    }
}
