package monoton.control;

import com.google.common.eventbus.EventBus;
import mods.cape.WaveyCapesBase;
import monoton.cmd.CmdManager;
import monoton.cmd.macro.MacroManager;
import monoton.control.config.ConfigManager;
import monoton.control.config.LastAccountConfig;
import monoton.control.friend.FriendManager;
import monoton.control.staff.StaffManager;
import monoton.control.notif.NotifManager;
import monoton.module.api.ModuleManager;
import monoton.ui.alt.AltConfig;
import monoton.ui.alt.AltManager;
import monoton.ui.clickgui.Window;
import proxy.ProxyConnection;

public class Manager {

    public static ModuleManager FUNCTION_MANAGER;
    public static CmdManager COMMAND_MANAGER;
    public static FriendManager FRIEND_MANAGER;
    public static MacroManager MACRO_MANAGER;
    public static LastAccountConfig LAST_ACCOUNT_CONFIG;
    public static WaveyCapesBase WAVYCAPES_BASE;
    public static StaffManager STAFF_MANAGER;
    public static Window CLICK_GUI;
    public static ConfigManager CONFIG_MANAGER;
    public static NotifManager NOTIFICATION_MANAGER;
    public static AltManager ALT;
    public static AltConfig ALT_CONFIG;
    public static EventBus EVENT_BUS;
    public static ProxyConnection PROXY_CONN;

    static {
        EVENT_BUS = new EventBus();
    }
}
