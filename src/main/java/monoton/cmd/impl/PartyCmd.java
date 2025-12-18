package monoton.cmd.impl;

import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.utils.other.OtherUtil;
import net.minecraft.util.text.TextFormatting;
import monoton.control.party.PartyHandler;
import ru.kotopushka.compiler.sdk.classes.Profile;

@CmdInfo(name = "party", description = "Создаёт группу в которую можно добавить друзей")
public class PartyCmd extends Cmd {

    @Override
    public void run(String[] args) {
        if (args.length < 2) {
            error();
            return;
        }

        String subCommand = args[1].toLowerCase();
        String username = Profile.getUsername() != null ? Profile.getUsername() : "Unknown";

        if (subCommand.equals("create")) {
            if (PartyHandler.isInParty()) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Вы уже состоите в группе, для начала " + TextFormatting.RED + "покиньте текущую группу" + TextFormatting.WHITE + ", чтобы создать новую");
                return;
            }
            String partyCode = PartyHandler.createParty(username);
            if (partyCode != null) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Группа успешно создана, код группы: " + TextFormatting.RED + partyCode + TextFormatting.WHITE + " все кто знают этот код, могут присоединиться к группе");
            } else {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Не удалось создать группу, попробуйте " + TextFormatting.RED + "снова" + TextFormatting.WHITE + " позже");
            }
        } else if (subCommand.equals("join") && args.length == 3) {
            if (PartyHandler.isInParty()) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Вы уже состоите в группе, для начала " + TextFormatting.RED + "покиньте текущую группу" + TextFormatting.WHITE + ", чтобы присоединиться к другой");
                return;
            }
            String partyCode = args[2];
            if (PartyHandler.joinParty(username, partyCode)) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Вы успешно присоединились к группе с кодом: " + TextFormatting.RED + partyCode);
                PartyHandler.sendPartyMessage(username, TextFormatting.RED + username + TextFormatting.WHITE + " присоединился к группе!");
            } else {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Не удалось присоединиться к группе, код " + TextFormatting.RED + "неверный" + TextFormatting.WHITE + ", группа не существует или вы забанены");
            }
        } else if (subCommand.equals("leave")) {
            if (!PartyHandler.isInParty()) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "У вас нет активной группы, " + TextFormatting.RED + "присоединитесь" + TextFormatting.WHITE + " к группе, чтобы использовать эту команду");
                return;
            }
            PartyHandler.PartyInfo partyInfo = PartyHandler.getPartyInfo(username, PartyHandler.getCurrentPartyCode());
            if (partyInfo != null && partyInfo.leader.equals(username)) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Вы лидер группы. Используйте " + TextFormatting.RED + ".party disband" + TextFormatting.WHITE + ", чтобы распустить группу");
                return;
            }
            PartyHandler.sendPartyMessage(username, TextFormatting.RED + username + TextFormatting.WHITE + " покинул группу!");
            PartyHandler.leaveParty(username);
            OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Вы успешно " + TextFormatting.RED + "покинули" + TextFormatting.WHITE + " группу");
        } else if (subCommand.equals("disband")) {
            if (!PartyHandler.isInParty()) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "У вас нет активной группы, " + TextFormatting.RED + "присоединитесь" + TextFormatting.WHITE + " к группе, чтобы использовать эту команду");
                return;
            }
            PartyHandler.PartyInfo partyInfo = PartyHandler.getPartyInfo(username, PartyHandler.getCurrentPartyCode());
            if (partyInfo == null || !partyInfo.leader.equals(username)) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Только лидер может распустить группу. Используйте " + TextFormatting.RED + ".party leave" + TextFormatting.WHITE + ", чтобы выйти");
                return;
            }
            PartyHandler.sendPartyMessage(username, TextFormatting.RED + username + TextFormatting.WHITE + " распустил группу!");
            PartyHandler.disbandParty(username, PartyHandler.getCurrentPartyCode());
            OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Группа успешно " + TextFormatting.RED + "распущена");
        } else if (subCommand.equals("dismiss") && args.length == 3) {
            if (!PartyHandler.isInParty()) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "У вас нет активной группы, " + TextFormatting.RED + "присоединитесь" + TextFormatting.WHITE + " к группе, чтобы использовать эту команду");
                return;
            }
            PartyHandler.PartyInfo partyInfo = PartyHandler.getPartyInfo(username, PartyHandler.getCurrentPartyCode());
            if (partyInfo == null || !partyInfo.leader.equals(username)) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Только лидер может передать лидерство");
                return;
            }
            String newLeader = args[2];
            if (!partyInfo.members.contains(newLeader)) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Игрок " + TextFormatting.RED + newLeader + TextFormatting.WHITE + " не состоит в группе");
                return;
            }
            if (newLeader.equals(username)) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Вы не можете передать лидерство самому себе");
                return;
            }
            if (PartyHandler.dismissPartyLeader(username, newLeader, PartyHandler.getCurrentPartyCode())) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Лидерство успешно передано игроку " + TextFormatting.RED + newLeader);
                PartyHandler.sendPartyMessage(username, TextFormatting.RED + newLeader + TextFormatting.WHITE + " теперь лидер группы!");
            } else {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Не удалось передать лидерство, попробуйте " + TextFormatting.RED + "снова" + TextFormatting.WHITE + " позже");
            }
        } else if (subCommand.equals("kick") && args.length == 3) {
            if (!PartyHandler.isInParty()) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "У вас нет активной группы, " + TextFormatting.RED + "присоединитесь" + TextFormatting.WHITE + " к группе, чтобы использовать эту команду");
                return;
            }
            PartyHandler.PartyInfo partyInfo = PartyHandler.getPartyInfo(username, PartyHandler.getCurrentPartyCode());
            if (partyInfo == null || !partyInfo.leader.equals(username)) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Только лидер может исключать игроков");
                return;
            }
            String kickedUser = args[2];
            if (!partyInfo.members.contains(kickedUser)) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Игрок " + TextFormatting.RED + kickedUser + TextFormatting.WHITE + " не состоит в группе");
                return;
            }
            if (kickedUser.equals(username)) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Вы не можете исключить самого себя. Используйте " + TextFormatting.RED + ".party disband" + TextFormatting.WHITE + ", чтобы распустить группу");
                return;
            }
            if (PartyHandler.kickPartyMember(username, kickedUser, PartyHandler.getCurrentPartyCode())) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Игрок " + TextFormatting.RED + kickedUser + TextFormatting.WHITE + " исключен из группы");
                PartyHandler.sendPartyMessage(username, TextFormatting.RED + kickedUser + TextFormatting.WHITE + " был исключен из группы!");
            } else {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Не удалось исключить игрока, попробуйте " + TextFormatting.RED + "снова" + TextFormatting.WHITE + " позже");
            }
        } else if (subCommand.equals("info")) {
            if (!PartyHandler.isInParty()) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "У вас нет активной группы, " + TextFormatting.RED + "присоединитесь" + TextFormatting.WHITE + " к группе, чтобы просмотреть информацию");
                return;
            }
            PartyHandler.PartyInfo partyInfo = PartyHandler.getPartyInfo(username, PartyHandler.getCurrentPartyCode());
            if (partyInfo != null) {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Состав группы:");
                for (String member : partyInfo.members) {
                    if (member.equals(partyInfo.leader)) {
                        OtherUtil.sendMessageIRC(TextFormatting.WHITE + member + TextFormatting.RED + " (лидер)");
                    } else {
                        OtherUtil.sendMessageIRC(TextFormatting.WHITE + member);
                    }
                }
            } else {
                OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Не удалось получить информацию о группе, попробуйте " + TextFormatting.RED + "снова" + TextFormatting.WHITE + " позже");
            }
        } else {
            error();
        }
    }

    @Override
    public void error() {
        OtherUtil.sendMessageIRC(TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        OtherUtil.sendMessageIRC(TextFormatting.WHITE + "." + "party create");
        OtherUtil.sendMessageIRC(TextFormatting.WHITE + "." + "party join " + TextFormatting.GRAY + "<" + TextFormatting.RED + "code" + TextFormatting.GRAY + ">");
        OtherUtil.sendMessageIRC(TextFormatting.WHITE + "." + "party leave");
        OtherUtil.sendMessageIRC(TextFormatting.WHITE + "." + "party disband");
        OtherUtil.sendMessageIRC(TextFormatting.WHITE + "." + "party dismiss " + TextFormatting.GRAY + "<" + TextFormatting.RED + "username" + TextFormatting.GRAY + ">");
        OtherUtil.sendMessageIRC(TextFormatting.WHITE + "." + "party kick " + TextFormatting.GRAY + "<" + TextFormatting.RED + "username" + TextFormatting.GRAY + ">");
        OtherUtil.sendMessageIRC(TextFormatting.WHITE + "." + "party info");
    }
}