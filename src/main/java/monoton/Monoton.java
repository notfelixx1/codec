package monoton;

import mods.voicechat.ForgeVoicechatClientMod;
import mods.voicechat.ForgeVoicechatMod;
import mods.cape.WaveyCapesBase;
import lombok.Getter;
import monoton.control.Manager;
import monoton.control.drag.DragManager;
import monoton.control.drag.Dragging;
import monoton.control.handler.HandlerManager;
import monoton.control.irc.IRCMessageHandler;
import monoton.control.party.PartyHandler;
import monoton.module.api.ModuleManager;
import monoton.utils.discord.rpc.DiscordManager;
import monoton.utils.tps.TPSCalc;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import monoton.module.api.Module;
import org.lwjgl.glfw.GLFW;
import monoton.cmd.CmdManager;
import monoton.cmd.macro.MacroManager;
import monoton.control.config.ConfigManager;
import monoton.control.config.LastAccountConfig;
import monoton.control.events.client.EventManager;
import monoton.control.events.game.EventKey;
import monoton.control.friend.FriendManager;
import monoton.control.staff.StaffManager;
import monoton.control.notif.NotifManager;
import monoton.ui.alt.AltConfig;
import monoton.ui.alt.AltManager;
import monoton.ui.clickgui.Window;
import monoton.utils.render.ShaderUtils;
import proxy.MinecraftProxy;
import proxy.ProxyConnection;
import ru.kotopushka.compiler.sdk.classes.Profile;
import viamcp.florianmichael.viamcp.ViaMCP;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Getter
public class Monoton {
    private static final File FIRST_RUN_MARKER = new File("C:\\Monoton\\game\\Monoton\\altmanager.json");
    public static final File DIR = new File("C:\\Monoton\\game\\Monoton");
    public static boolean isServer;

    @Getter
    private static TPSCalc tpsCalc;

    @Getter
    public static ViaMCP viaMCP;

    private DiscordManager discordManager;

    private HandlerManager handlerManager;

    public static TPSCalc getServerTPS() {
        return tpsCalc;
    }

    public void init() {
        EventManager.register(this);

        ShaderUtils.init();
        tpsCalc = new TPSCalc();
        EventManager.register(tpsCalc);
        viaMCP = new ViaMCP();
        EventManager.register(viaMCP);

        Manager.FUNCTION_MANAGER = new ModuleManager();
        Manager.FUNCTION_MANAGER.initialize();
        Manager.NOTIFICATION_MANAGER = new NotifManager();
        Manager.WAVYCAPES_BASE = new WaveyCapesBase();

        ensureDirectoriesExist();
        ensureFirstRun();
        IRCMessageHandler.init();
        PartyHandler.init();

        String prefix = "pasted by kofe1337";

        handlerManager = new HandlerManager();
        handlerManager.init();

        IRCMessageHandler.setPrefix(prefix);

        Manager.ALT = new AltManager();

        Manager.ALT_CONFIG = new AltConfig();
        Manager.FRIEND_MANAGER = new FriendManager();
        Manager.COMMAND_MANAGER = new CmdManager();
        Manager.STAFF_MANAGER = new StaffManager();
        Manager.MACRO_MANAGER = new MacroManager();
        Manager.LAST_ACCOUNT_CONFIG = new LastAccountConfig();
        Manager.CONFIG_MANAGER = new ConfigManager();
        Manager.CLICK_GUI = new Window(new StringTextComponent("A"));

        discordManager = new DiscordManager();
        discordManager.start();
        EventManager.register(discordManager);

        try {
            Manager.ALT_CONFIG.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Manager.FRIEND_MANAGER.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Manager.COMMAND_MANAGER.init();
        try {
            Manager.STAFF_MANAGER.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            Manager.MACRO_MANAGER.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            Manager.LAST_ACCOUNT_CONFIG.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            Manager.CONFIG_MANAGER.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DragManager.load();
        Manager.PROXY_CONN = new ProxyConnection();
        MinecraftProxy.initialize();
        Manager.WAVYCAPES_BASE.init();
        new ForgeVoicechatMod();
        new ForgeVoicechatClientMod();
    }

    private void ensureDirectoriesExist() {
        if (!DIR.exists() && !DIR.mkdirs()) {
            System.err.println("Failed to create directory: " + DIR.getAbsolutePath());
        }
    }

    private void ensureFirstRun() {
        if (FIRST_RUN_MARKER.exists()) {
            return;
        }

        openLinks();
        try {
            if (FIRST_RUN_MARKER.createNewFile()) {
                try (FileOutputStream ignored = new FileOutputStream(FIRST_RUN_MARKER)) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openLinks() {
        try {
            Runtime.getRuntime().exec(new String[] {
                    "rundll32", "url.dll,FileProtocolHandler", "https://www.youtube.com/@bravoprod"
            });
            Runtime.getRuntime().exec(new String[] {
                    "rundll32", "url.dll,FileProtocolHandler", "https://t.me/bravo317st"
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void keyPress(int key) {
        EventManager.call(new EventKey(key));

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            Minecraft.getInstance().displayGuiScreen(Manager.CLICK_GUI);
        }

        if (Manager.MACRO_MANAGER != null) {
            Manager.MACRO_MANAGER.onKeyPressed(key);
        }

        List<Module> modules = Manager.FUNCTION_MANAGER.getFunctions();
        for (Module m : modules) {
            if (m.bind == key) {
                m.toggle();
            }
        }
    }

    public static Dragging createDrag(Module module, String name, float x, float y) {
        Dragging drag = new Dragging(module, name, x, y);
        DragManager.draggables.put(name, drag);
        return drag;
    }

    public static void shutDown() {
        Manager.CONFIG_MANAGER.saveConfiguration("default");
        Manager.LAST_ACCOUNT_CONFIG.updateFile();
        AltConfig.updateFile();
        DragManager.save();
        if (Monoton.getInstance().getDiscordManager() != null) {
            Monoton.getInstance().getDiscordManager().stopRPC();
        }
    }

    @Getter
    private static final Monoton instance = new Monoton();
}