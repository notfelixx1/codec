package monoton.cmd.impl;

import net.minecraft.util.text.TextFormatting;
import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.control.Manager;
import monoton.control.staff.StaffManager;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.List;

@CmdInfo(name = "staff", description = "Добавляет игрока в Staff-List")
public class StaffCmd extends Cmd {
    @Compile
    @Override
    public void run(String[] args) throws Exception {
        if (args.length >= 2) {
            switch (args[1].toLowerCase()) {
                case "add" -> {
                    if (args.length == 3) {
                        addStaffName(args[2], "");
                    } else if (args.length == 4) {
                        String formattedPrefix = getFormattedPrefix(args[3]);
                        if (formattedPrefix == null) {
                            sendMessage(TextFormatting.RED + "Недопустимый префикс! Доступные префиксы: YT, ML.MODER, MODER, ST.MODER, GL.MODER, ADMIN, ML.ADMIN");
                            return;
                        }
                        addStaffName(args[2], formattedPrefix);
                    } else {
                        error();
                    }
                }
                case "remove" -> removeStaffName(args[2]);
                case "clear" -> clearList();
                case "list" -> outputList();
                default -> error();
            }
        } else {
            error();
        }
    }

    @Compile
    private String getFormattedPrefix(String prefix) {
        if (prefix.isEmpty()) return "";
        switch (prefix.toUpperCase()) {
            case "YT":
                return TextFormatting.RED + "Y" + TextFormatting.WHITE + "T" + TextFormatting.RESET;
            case "ML.MODER":
                return TextFormatting.BLUE + "ML.MODER" + TextFormatting.RESET;
            case "MODER":
                return TextFormatting.BLUE + "MODER" + TextFormatting.RESET;
            case "ST.MODER":
                return TextFormatting.BLUE + "ST.MODER" + TextFormatting.RESET;
            case "GL.MODER":
                return TextFormatting.DARK_BLUE + "GL.MODER" + TextFormatting.RESET;
            case "ADMIN":
                return TextFormatting.RED + "ADMIN" + TextFormatting.RESET;
            case "ML.ADMIN":
                return TextFormatting.AQUA + "ML.ADMIN" + TextFormatting.RESET;
            default:
                return null;
        }
    }

    @Compile
    private void addStaffName(String name, String prefix) {
        StaffManager manager = Manager.STAFF_MANAGER;

        if (manager.isStaff(name)) {
            sendMessage(TextFormatting.RED + "Этот игрок уже в Staff List!");
        } else {
            String rawPrefix = prefix.isEmpty() ? "" : prefix.replaceAll("\\u00A7[0-9a-fk-or]", "");
            manager.addStaff(name, rawPrefix);
            String message = prefix.isEmpty()
                    ? TextFormatting.GREEN + "Ник " + TextFormatting.WHITE + name + TextFormatting.GREEN + " добавлен в Staff List"
                    : TextFormatting.GREEN + "Ник " + TextFormatting.WHITE + name + TextFormatting.GREEN + " с префиксом " + prefix + TextFormatting.GREEN + " добавлен в Staff List";
            sendMessage(message);
        }
    }

    @Compile
    private void removeStaffName(String name) {
        StaffManager manager = Manager.STAFF_MANAGER;

        if (manager.isStaff(name)) {
            manager.removeStaff(name);
            sendMessage(TextFormatting.GREEN + "Ник " + TextFormatting.WHITE + name + TextFormatting.GREEN + " удален из Staff List");
        } else {
            sendMessage(TextFormatting.RED + "Этого игрока нет в Staff List!");
        }
    }

    @Compile
    private void clearList() {
        StaffManager manager = Manager.STAFF_MANAGER;

        if (manager.getStaffList().isEmpty()) {
            sendMessage(TextFormatting.RED + "Staff List пуст!");
        } else {
            manager.clearStaffs();
            sendMessage(TextFormatting.GREEN + "Staff List очищен");
        }
    }

    @Compile
    private void outputList() {
        StaffManager manager = Manager.STAFF_MANAGER;
        List<StaffManager.Staff> staffList = manager.getStaffList();

        if (staffList.size() > 10) {
            sendMessage(TextFormatting.RED + "Слишком большой стафф лист, посмотрите в папке с конфигом!");
        } else {
            sendMessage(TextFormatting.GRAY + "Список Staff:");
            for (StaffManager.Staff staff : staffList) {
                String formattedPrefix = getFormattedPrefix(staff.getPrefix());
                String display = formattedPrefix.isEmpty()
                        ? TextFormatting.WHITE + staff.getName()
                        : TextFormatting.WHITE + staff.getName() + TextFormatting.GRAY + " (" + formattedPrefix + ")";
                sendMessage(display);
            }
        }
    }

    @Compile
    @Override
    public void error() {
        sendMessage(TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        sendMessage(TextFormatting.WHITE + ".staff " + TextFormatting.GRAY + "<"
                + TextFormatting.RED + "add <name> [YT|ML.MODER|MODER|ST.MODER|GL.MODER|ADMIN|ML.ADMIN]; remove <name>; clear; list" + TextFormatting.GRAY + ">");
    }
}