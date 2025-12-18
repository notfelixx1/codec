package monoton.cmd.impl;

import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.control.Manager;
import monoton.control.friend.Friend;
import monoton.control.friend.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;


@CmdInfo(name = "friend", description = "Позволяет добавить человек в список друзей")
public class FriendCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        if (args.length > 1) {
            switch (args[1]) {
                case "add" -> {
                    final String friendName = args[2];
                    addFriend(friendName);
                }
                case "remove" -> {
                    final String friendName = args[2];
                    removeFriend(friendName);
                }
                case "list" -> friendList();
                case "clear" -> clearFriendList();
            }
        } else {
            error();
        }
    }

    /**
     * Система добавления игрока в друзья
     *
     * @param friendName имя игрока
     */
    
    private void addFriend(final String friendName) {
        final FriendManager friendManager = Manager.FRIEND_MANAGER;

        if (friendName.equals(Minecraft.getInstance().player.getName().getString())) {
            sendMessage("Вы не можете добавить самого себя в друзья");
            return;
        }

        if (friendManager.getFriends().stream().map(Friend::getName).toList().contains(friendName)) {
            sendMessage(friendName + " уже есть в списке друзей");
            return;
        }

        sendMessage(friendName + " успешно добавлен в список друзей!");
        friendManager.addFriend(friendName);
    }

    /**
     * Система удаления игрока из списка друзей
     *
     * @param friendName имя игрока
     */
    private void removeFriend(final String friendName) {
        final FriendManager friendManager = Manager.FRIEND_MANAGER;

        if (friendManager.isFriend(friendName)) {
            friendManager.removeFriend(friendName);
            sendMessage(friendName + " был удален из списка друзей");
            return;
        }
        sendMessage(friendName + " нету в списке друзей");
    }

    /**
     * Выводит список ников всех друзей
     */
    
    private void friendList() {
        final FriendManager friendManager = Manager.FRIEND_MANAGER;

        if (friendManager.getFriends().isEmpty()) {
            sendMessage("Список друзей пуст");
            return;
        }

        sendMessage("Список друзей:");
        for (Friend friend : friendManager.getFriends()) {
            sendMessage(TextFormatting.GRAY + friend.getName());
        }
    }

    /**
     * Очистка списка друзей
     */
    
    private void clearFriendList() {
        final FriendManager friendManager = Manager.FRIEND_MANAGER;

        if (friendManager.getFriends().isEmpty()) {
            sendMessage("Список друзей пуст");
            return;
        }

        friendManager.clearFriend();
        sendMessage("Список друзей успешно очищен");
    }

    
    @Override
    public void error() {
        sendMessage(TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        sendMessage(TextFormatting.WHITE + "." + "friend add " + TextFormatting.GRAY + "<"
                + TextFormatting.RED + "name" + TextFormatting.GRAY + ">");
        sendMessage(TextFormatting.WHITE + "." + "friend remove " + TextFormatting.GRAY + "<"
                + TextFormatting.RED + "name" + TextFormatting.GRAY + ">");
        sendMessage(TextFormatting.WHITE + "." + "friend list" + TextFormatting.GRAY + (" - показывает список всех фриендов"));
        sendMessage(TextFormatting.WHITE + "." + "friend clear" + TextFormatting.GRAY + (" - очищает всех фриендов"));
    }
}
