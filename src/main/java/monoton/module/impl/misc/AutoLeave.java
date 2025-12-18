package monoton.module.impl.misc;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.font.ReplaceUtil;
import monoton.utils.other.OtherUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Annotation(name = "AutoLeave", type = TypeList.Misc, desc = "Выходит с сервера или пишет команду при низком хп / рядом игрок / модератор")
public class AutoLeave extends Module {

    public MultiBoxSetting elements1 = new MultiBoxSetting("Ливать от кого", new BooleanOption[]{
            new BooleanOption("Игроков", true),
            new BooleanOption("Здоровье", true),
            new BooleanOption("Модераторы", false)
    });
    public SliderSetting range = new SliderSetting("Дистанция", 15, 5, 40, 1).setVisible(() -> elements1.get("Игроков"));
    public ModeSetting mode = new ModeSetting("Что делать?", "/spawn", "/spawn", "/hub", "/home", "kick");
    public SliderSetting healthSlider = new SliderSetting("Здоровье", 10, 5, 20, 1).setVisible(() -> elements1.get("Здоровье"));
    public BooleanOption onlyVanish = new BooleanOption("Ливать только от спека", true).setVisible(() -> elements1.get("Модераторы"));
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");

    public AutoLeave() {
        addSettings(elements1, mode, range, healthSlider, onlyVanish);
    }

    @Compile
    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (elements1.get("Здоровье") && mc.player.getHealth() <= healthSlider.getValue().floatValue()) {
                performLeaveAction(TextFormatting.RED + "AutoLeave" + TextFormatting.WHITE + " сработал из-за недостатка здоровья");
                return false;
            }

            if (elements1.get("Модераторы") && !OtherUtil.isPvP()) {
                List<StaffInfo> staffList = getStaffList();
                for (StaffInfo staff : staffList) {
                    if (staff.vanish || !onlyVanish.get()) {
                        performLeaveAction(TextFormatting.RED + "AutoLeave" + TextFormatting.WHITE + " ливнул от модератора: " + TextFormatting.RED + staff.staffName);
                        return false;
                    }
                }
            }

            if (elements1.get("Игроков")) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player == mc.player || player.isBot || Manager.FRIEND_MANAGER.isFriend(player.getGameProfile().getName())) {
                        continue;
                    }
                    if (mc.player.getDistance(player) <= range.getValue().floatValue()) {
                        performLeaveAction(TextFormatting.RED + "AutoLeave" + TextFormatting.WHITE + " сработал на игрока: " + TextFormatting.RED + player.getGameProfile().getName());
                        break;
                    }
                }
            }
        }
        return false;
    }

    @Compile
    private void performLeaveAction(String reason) {
        if (mode.is("kick")) {
            mc.player.connection.getNetworkManager().closeChannel(
                    OtherUtil.gradient("Вы вышли с сервера! \n" + reason,
                            new Color(121, 208, 255).getRGB(),
                            new Color(96, 133, 255).getRGB())
            );
            OtherUtil.sendMessageIRC(TextFormatting.WHITE + "Вы " + TextFormatting.RED + "вышли" + TextFormatting.WHITE + " с сервера: " + TextFormatting.RED + reason);
        } else {
            mc.player.connection.sendPacket(new CChatMessagePacket(mode.get()));
            OtherUtil.sendMessageIRC(reason);
        }
        setState(false);
    }

    @Compile
    private List<StaffInfo> getStaffList() {
        List<StaffInfo> staffList = new ArrayList<>();
        for (ScorePlayerTeam team : mc.world.getScoreboard().getTeams().stream()
                .sorted(Comparator.comparing(Team::getName)).toList()) {
            String name = team.getMembershipCollection().toString();
            name = name.substring(1, name.length() - 1);
            if (namePattern.matcher(name).matches()) {
                boolean vanish = true;
                boolean near = false;
                boolean active = false;

                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player.getName().getString().equals(name)) {
                        near = true;
                    }
                }

                for (net.minecraft.client.network.play.NetworkPlayerInfo info :
                        mc.getConnection().getPlayerInfoMap()) {
                    if (info.getGameProfile().getName().equals(name) &&
                            info.getGameType() != GameType.SPECTATOR) {
                        vanish = false;
                        if (!near) {
                            active = true;
                        }
                    }
                }

                String prefix = ReplaceUtil.replaceCustomFonts(team.getPrefix().getString());
                if ((prefix.toLowerCase().matches(".*(mod|der|adm|wne|мод|medi|хелп|помо|стаж|адм|владе|отри|таф|taf|yout|curat|курато|dev|раз|supp|сапп|yt|ютуб)(?<!D\\.HELPER).*") ||
                        Manager.STAFF_MANAGER.isStaff(name)) && !isBot(name)) {
                    if (vanish) {
                        staffList.add(new StaffInfo("Наблюдает", name, true));
                    } else if (active) {
                        staffList.add(new StaffInfo("Активен", name, false));
                    } else if (near) {
                        staffList.add(new StaffInfo("Рядом", name, false));
                    }
                }
            }
        }
        return staffList;
    }

    private boolean isBot(String name) {
        return mc.world.getPlayers().stream()
                .filter(player -> player.getName().getString().equals(name))
                .anyMatch(player -> player.isBot);
    }

    private static class StaffInfo {
        public final String status;
        public final String staffName;
        public final boolean vanish;

        public StaffInfo(String status, String staffName, boolean vanish) {
            this.status = status;
            this.staffName = staffName;
            this.vanish = vanish;
        }
    }
}