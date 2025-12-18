package monoton.utils.discord.rpc;

import lombok.Getter;
import monoton.utils.discord.rpc.utils.DiscordEventHandlers;
import monoton.utils.discord.rpc.utils.DiscordRPC;
import monoton.utils.discord.rpc.utils.DiscordRichPresence;
import monoton.utils.discord.rpc.utils.RPCButton;
import ru.kotopushka.compiler.sdk.classes.Profile;

@Getter
public class DiscordManager {

    private DiscordDaemonThread discordDaemonThread;
    private long APPLICATION_ID;

    private boolean running;

    private String image;
    private String telegram;
    private String site;

    private void cppInit() {
        discordDaemonThread = new DiscordDaemonThread();
        APPLICATION_ID = 1446998114488815669L;
        running = true;
        image = "https://media1.tenor.com/m/9RCIDZjkhBsAAAAd/hamster-meme.gif";
        telegram = "https://www.youtube.com/@bravoprod";
        site = "https://t.me/bravo317st";
    }

    public void init() {
        cppInit();
        DiscordRichPresence.Builder builder = new DiscordRichPresence.Builder();
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();
        DiscordRPC.INSTANCE.Discord_Initialize(String.valueOf(APPLICATION_ID), handlers, true, "");
        builder.setStartTimestamp((System.currentTimeMillis() / 1000));
        String username = Profile.getUsername();
        int uid = Profile.getUid();
        String role = Profile.getRole();
        String prefix = "pasted by kofe1337";

        builder.setDetails("UID » " + uid);
        builder.setState("Role » " + prefix);
        builder.setLargeImage(image, username);
        builder.setButtons(RPCButton.create("Купить", site), RPCButton.create("Телеграм", telegram));
        DiscordRPC.INSTANCE.Discord_UpdatePresence(builder.build());
        discordDaemonThread.start();
    }

    public DiscordManager start() {
        init();
        return this;
    }

    public void stopRPC() {
        DiscordRPC.INSTANCE.Discord_Shutdown();
        discordDaemonThread.interrupt();
        this.running = false;
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");

            try {
                while (running) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    Thread.sleep(15 * 1000);
                }
            } catch (Exception exception) {
                stopRPC();
            }

            super.run();
        }
    }
}