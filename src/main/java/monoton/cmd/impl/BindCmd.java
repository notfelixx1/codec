package monoton.cmd.impl;

import net.minecraft.util.text.TextFormatting;
import monoton.cmd.Cmd;
import monoton.module.api.Module;
import org.lwjgl.glfw.GLFW;
import monoton.cmd.CmdInfo;
import monoton.control.Manager;
import monoton.utils.math.KeyMappings;
import ru.kotopushka.compiler.sdk.annotations.Compile;


@CmdInfo(name = "bind", description = "Позволяет забиндить модуль на определенную клавишу")
public class BindCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        try {
            if (args.length >= 2) {
                switch (args[1].toLowerCase()) {
                    case "list" -> listBoundKeys();
                    case "clear" -> clearAllBindings();
                    case "add" -> {
                        if (args.length >= 4) {
                            addKeyBinding(args[2], args[3]);
                        } else {
                            error();
                        }
                    }
                    case "remove" -> {
                        if (args.length >= 2) {
                            removeKeyBinding(args[2]);
                        } else {
                            error();
                        }
                    }
                    default -> error();
                }
            } else {
                error();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод для вывода списка модулей с привязанными клавишами
     */
    @Compile
    private void listBoundKeys() {
        sendMessage(TextFormatting.GRAY + "Список всех модулей с привязанными клавишами:");
        for (Module f : Manager.FUNCTION_MANAGER.getFunctions()) {
            if (f.bind == 0) continue;
            sendMessage(f.name + " [" + TextFormatting.GRAY + (GLFW.glfwGetKeyName(f.bind, -1) == null ? "" : GLFW.glfwGetKeyName(f.bind, -1)) + TextFormatting.RESET + "]");
        }
    }

    /**
     * Метод для очистки всех привязок клавиш
     */
    @Compile
    private void clearAllBindings() {
        for (Module f : Manager.FUNCTION_MANAGER.getFunctions()) {
            f.bind = 0;
        }
        sendMessage(TextFormatting.GREEN + "Все клавиши были отвязаны от модулей");
    }

    /**
     * Метод для добавления привязки клавиши к модулю
     *
     * @param moduleName имя модуля
     * @param keyName    название клавиши
     */
    private void addKeyBinding(String moduleName, String keyName) {
        Integer key = null;

        try {
            key = KeyMappings.keyMap.get(keyName.toUpperCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Module module = Manager.FUNCTION_MANAGER.get(moduleName);
        if (key != null) {
            if (module != null) {
                module.bind = key;
                sendMessage("Клавиша" + TextFormatting.GRAY + keyName + TextFormatting.WHITE + " была привязана к модулю " + TextFormatting.RED + module.name);
            } else {
                sendMessage("Модуль " + moduleName + " не был найден");
            }
        } else {
            sendMessage("Клавиша " + keyName + " не была найдена!");
        }
    }

    /**
     * Метод для удаления привязки клавиши
     *
     * @param moduleName имя модуля
     */
    private void removeKeyBinding(String moduleName) {
        for (Module f : Manager.FUNCTION_MANAGER.getFunctions()) {
            if (f.name.equalsIgnoreCase(moduleName)) {
                f.bind = 0;
                sendMessage("Клавиша " + TextFormatting.RESET + " была отвязана от модуля " + TextFormatting.GRAY + f.name);
            }
        }
    }

    /**
     * Метод для обработки ошибки неверного синтаксиса команды
     */
    @Compile
    @Override
    public void error() {
        sendMessage(TextFormatting.WHITE + "Неверный синтаксис команды. " + TextFormatting.GRAY + "Используйте:");
        sendMessage(TextFormatting.WHITE + ".bind add " + TextFormatting.DARK_GRAY + "<name> <key>");
        sendMessage(TextFormatting.WHITE + ".bind remove " + TextFormatting.DARK_GRAY + "<name>");
        sendMessage(TextFormatting.WHITE + ".bind list");
        sendMessage(TextFormatting.WHITE + ".bind clear");
    }
}