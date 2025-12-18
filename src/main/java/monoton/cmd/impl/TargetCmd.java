package monoton.cmd.impl;

import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@CmdInfo(name = "target", description = "Ставит цель в приоритет")
public class TargetCmd extends Cmd {
    private static String priorityTargetName = null;

    @Compile
    @Override
    public void run(String[] args) throws Exception {
        int startIndex = (args.length > 0 && args[0].equalsIgnoreCase(".target")) ? 1 : 0;

        if (startIndex >= args.length) {
            error();
            return;
        }

        switch (args[startIndex].toLowerCase()) {
            case "clear" -> {
                priorityTargetName = null;
                sendMessage("Приоритет был " + TextFormatting.RED + "сброшена");
            }
            default -> {
                priorityTargetName = args[startIndex];
                sendMessage("Приоритет установлен на " + TextFormatting.RED + priorityTargetName);
            }
        }
    }

    @Compile
    @Override
    public void error() {
        sendMessage(TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        sendMessage(TextFormatting.WHITE + ".target " + TextFormatting.GRAY + "<ник>");
        sendMessage(TextFormatting.WHITE + ".target clear" + TextFormatting.GRAY + " - сбросить приоритетную цель");
    }

    public static String getPriorityTargetName() {
        return priorityTargetName;
    }
}