package monoton.cmd.impl;

import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.control.Manager;
import monoton.control.config.ConfigManager;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.io.IOException;


@CmdInfo(name = "cfg", description = "Через эту команду можно управлять конфигами")
public class ConfigCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        if (args.length > 1) {
            ConfigManager configManager = Manager.CONFIG_MANAGER;
            switch (args[1]) {
                case "save" -> {
                    String configName = args[2];
                    configManager.saveConfiguration(configName);
                    sendMessage("Конфигурация " + TextFormatting.GRAY + args[2] + TextFormatting.RESET + " успешно сохранена.");
                }
                case "load" -> {
                    String configName = args[2];
                    configManager.loadConfiguration(configName, false);
                }
                case "remove" -> {
                    String configName = args[2];
                    try {
                        configManager.deleteConfig(configName);
                        sendMessage("Конфигурация " + TextFormatting.GRAY + args[2] + TextFormatting.RESET + " успешно удалена.");
                    } catch (Exception e) {
                        sendMessage("Не удалось удалить конфигурацию с именем " + configName + " возможно её просто нету.");
                    }
                }
                case "list" -> {
                    if (configManager.getAllConfigurations().isEmpty()) {
                        sendMessage("Список конфигов пуст.");
                        return;
                    }
                    for (String s : configManager.getAllConfigurations()) {
                        sendMessage(s.replace(".cfg", ""));
                    }
                }
                case "dir" -> {
                    try {
                        Runtime.getRuntime().exec("explorer " + ConfigManager.CONFIG_DIR.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            error();
        }
    }

    @Compile
    @Override
    public void error() {
        sendMessage(TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        sendMessage(TextFormatting.WHITE + "." + "cfg load " + TextFormatting.GRAY + "<name>");
        sendMessage(TextFormatting.WHITE + "." + "cfg save " + TextFormatting.GRAY + "<name>");
        sendMessage(TextFormatting.WHITE + "." + "cfg remove " + TextFormatting.GRAY + "<name>");
        sendMessage(TextFormatting.WHITE + "." + "cfg list" + TextFormatting.GRAY
                + (" - показать список конфигов"));
        sendMessage(TextFormatting.WHITE + "." + "cfg dir" + TextFormatting.GRAY
                + (" - открыть папку с конфигами"));
    }
}
