package monoton.cmd.impl;

import mods.baritone.api.BaritoneAPI;
import mods.baritone.api.IBaritone;
import mods.baritone.api.event.events.ChatEvent;
import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import monoton.control.Manager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.util.Session;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.network.NetworkManager;
import org.apache.commons.lang3.RandomStringUtils;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@CmdInfo(name = "bot", description = "Позволяет сменить ник и переподключиться к текущему серверу или отправить сообщение через все активные сессии по очереди")
public class BotCmd extends Cmd {
    // Store session, player, and connection triples for all active sessions
    private static final List<SessionConnection> activeSessions = new CopyOnWriteArrayList<>();
    // Executor service for scheduling message sending
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // Track the current scheduled task to allow cancellation
    private static ScheduledFuture<?> messageTask = null;

    // Helper class to store session, player, and its network connection
    private static class SessionConnection {
        Session session;
        ClientPlayerEntity player;
        NetworkManager networkManager;

        SessionConnection(Session session, ClientPlayerEntity player, NetworkManager networkManager) {
            this.session = session;
            this.player = player;
            this.networkManager = networkManager;
        }
    }

    @Override
    public void run(String[] args) throws Exception {
        Minecraft mc = Minecraft.getInstance();

        if (args.length >= 2 && args[1].equalsIgnoreCase("m")) {
            // Handle .bot m command
            if (args.length > 2) {
                sendMessage(TextFormatting.YELLOW + "Предупреждение: Аргументы после 'm' игнорируются. Используется стандартное сообщение.");
            }

            // Stop any existing message task
            if (messageTask != null) {
                messageTask.cancel(false);
                sendMessage(TextFormatting.YELLOW + "Предыдущая задача отправки сообщений остановлена.");
            }

            if (activeSessions.isEmpty()) {
                sendMessage(TextFormatting.RED + "Ошибка: Нет активных сессий для отправки сообщения.");
                return;
            }

            // Log current sessions for debugging
            sendMessage(TextFormatting.GRAY + "Текущие активные сессии: " + activeSessions.size());
            for (SessionConnection sc : activeSessions) {
                sendMessage(TextFormatting.GRAY + " - " + sc.session.getUsername() +
                        ", Player: " + (sc.player != null ? "exists" : "null") +
                        ", Connection: " + (sc.networkManager != null && sc.networkManager.isChannelOpen() ? "open" : "closed/null"));
            }

            // Create a counter to cycle through sessions
            AtomicInteger sessionIndex = new AtomicInteger(0);

            // Schedule the message-sending task
            messageTask = scheduler.scheduleAtFixedRate(() -> {
                // Run on the main Minecraft thread to avoid thread-safety issues
                mc.execute(() -> {
                    if (activeSessions.isEmpty()) {
                        sendMessage(TextFormatting.RED + "Ошибка: Нет активных сессий. Остановка задачи.");
                        if (messageTask != null) {
                            messageTask.cancel(false);
                        }
                        return;
                    }

                    // Get the current session
                    int index = sessionIndex.getAndUpdate(i -> (i + 1) % activeSessions.size());
                    SessionConnection sc = activeSessions.get(index);

                    if (sc.networkManager != null && sc.networkManager.isChannelOpen() && sc.player != null) {
                        String[] insults = {
                                "я некстик", "я даун некстик"
                        };

                        String[] targets = {
                                "и я наркоман и далбоёб", "и я ебал казу в 7 лет", "и меня насиловал отчим вчера", "и я читаю зарам перед едой", "и меня ебал осёл"
                        };

                        String[] extras = {
                                "", ""
                        };

                        Random rand = new Random();
                        String insult = insults[rand.nextInt(insults.length)];
                        String target = targets[rand.nextInt(targets.length)];
                        String extra = extras[rand.nextInt(extras.length)];

                        // Shuffle parts to create varied sentence structures
                        String[] parts = {insult, target, extra};
                        for (int i = parts.length - 1; i > 0; i--) {
                            int j = rand.nextInt(i + 1);
                            String temp = parts[i];
                            parts[i] = parts[j];
                            parts[j] = temp;
                        }

                        String message = "! " + String.join(" ", parts).trim();

                        // Debug Baritone and Command Manager
                        ChatEvent event = new ChatEvent(message);
                        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(sc.player);
                        boolean baritoneExists = baritone != null;
                        boolean eventCancelled = false;
                        boolean isCommand = false;

                        if (baritone != null) {
                            baritone.getGameEventHandler().onSendChatMessage(event);
                            eventCancelled = event.isCancelled();
                        } else {
                            sendMessage(TextFormatting.RED + "Ошибка: Baritone не найден для сессии " + sc.session.getUsername());
                        }

                        if (!eventCancelled) {
                            Manager.COMMAND_MANAGER.runCommands(message);
                            isCommand = Manager.COMMAND_MANAGER.isMessage;
                        }

                        if (!eventCancelled && !isCommand) {
                            sc.player.connection.sendPacket(new CChatMessagePacket(message));
                            sendMessage(TextFormatting.GREEN + "Сообщение отправлено через сессию (" +
                                    sc.session.getUsername() + "): " + TextFormatting.WHITE + message);
                        } else {
                            sendMessage(TextFormatting.RED + "Сообщение не отправлено для сессии " +
                                    sc.session.getUsername() + ": Baritone отменено=" + eventCancelled +
                                    ", COMMAND_MANAGER.isMessage=" + isCommand);
                        }

                    } else {
                        // Remove inactive session
                        activeSessions.remove(sc);
                        sendMessage(TextFormatting.YELLOW + "Сессия " + sc.session.getUsername() +
                                " неактивна или игрок отсутствует и была удалена из списка. " +
                                "Player: " + (sc.player != null ? "exists" : "null") +
                                ", Connection: " + (sc.networkManager != null && sc.networkManager.isChannelOpen() ? "open" : "closed/null"));
                    }
                });
            }, 0, 1, TimeUnit.SECONDS);

            sendMessage(TextFormatting.GREEN + "Запущена отправка сообщений через " + activeSessions.size() + " сессий с интервалом 1 секунду.");
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("stop")) {
            // Handle .bot stop command
            if (messageTask != null) {
                messageTask.cancel(false);
                messageTask = null;
                sendMessage(TextFormatting.GREEN + "Задача отправки сообщений остановлена.");
            } else {
                sendMessage(TextFormatting.YELLOW + "Нет активной задачи отправки сообщений.");
            }
            return;
        }

        // Existing session switch logic
        String username;
        if (args.length == 2) {
            if (args[1].equalsIgnoreCase("rand")) {
                username = RandomStringUtils.randomAlphabetic(10);
            } else if (args[1].length() < 20) {
                username = args[1];
            } else {
                error();
                return;
            }
        } else {
            error();
            return;
        }

        String uuid = UUID.randomUUID().toString();

        // Capture current server data
        ServerData currentServer = mc.getCurrentServerData();
        String serverIp = null;
        int serverPort = 25565; // Default Minecraft port
        if (currentServer != null) {
            serverIp = currentServer.serverIP;
            if (serverIp.contains(":")) {
                String[] parts = serverIp.split(":");
                serverIp = parts[0];
                try {
                    serverPort = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    // Fallback to default port
                }
            }
        }

        // Store current session, player, and connection before switching
        if (mc.session != null && mc.player != null && mc.getConnection() != null && mc.getConnection().getNetworkManager().isChannelOpen()) {
            activeSessions.add(new SessionConnection(mc.session, mc.player, mc.getConnection().getNetworkManager()));
            sendMessage(TextFormatting.YELLOW + "Сохранена сессия: " + mc.session.getUsername());
        }

        // Update session
        mc.session = new Session(username, uuid, "", "mojang");
        sendMessage(TextFormatting.GREEN + "Вы успешно вошли как " + TextFormatting.WHITE + mc.session.getUsername());

        // Reconnect to the current server if connected
        if (serverIp != null && mc.world != null && mc.getConnection() != null) {
            // Do not close the current connection to keep it active
            ServerData reconnectServer = new ServerData("ReconnectServer", serverIp + (serverPort != 25565 ? ":" + serverPort : ""), false);
            mc.displayGuiScreen(new net.minecraft.client.gui.screen.ConnectingScreen(
                    new net.minecraft.client.gui.screen.MainMenuScreen(),
                    mc,
                    reconnectServer
            ));
            sendMessage(TextFormatting.GREEN + "Переподключение к серверу: " + TextFormatting.WHITE + serverIp);
        } else {
            sendMessage(TextFormatting.YELLOW + "Вы не подключены к серверу. Сессия обновлена, но переподключение не требуется.");
        }
    }

    @Compile
    @Override
    public void error() {
        sendMessage(TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        sendMessage(TextFormatting.WHITE + ".bot " + TextFormatting.GRAY + "<" + "name" + TextFormatting.GRAY + ">");
        sendMessage(TextFormatting.WHITE + ".bot rand" + TextFormatting.GRAY + " (Генерирует случайное имя)");
        sendMessage(TextFormatting.WHITE + ".bot m " + TextFormatting.GRAY + "(Запускает циклическую отправку сообщений через все активные сессии)");
        sendMessage(TextFormatting.WHITE + ".bot stop " + TextFormatting.GRAY + "(Останавливает циклическую отправку сообщений)");
    }
}