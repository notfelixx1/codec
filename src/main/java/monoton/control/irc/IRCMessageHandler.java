package monoton.control.irc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import monoton.control.Manager;
import monoton.utils.other.OtherUtil;
import net.minecraft.util.text.TextFormatting;
import ru.kotopushka.compiler.sdk.classes.Profile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class IRCMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(IRCMessageHandler.class.getName());
    private static final String API_URL = "http://185.100.157.243:6000/";
    private static final String API_KEY = "yaponecgeypoleteliyra";
    private static String lastTimestamp = "";
    private static String currentPrefix = "user";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final JsonParser parser = new JsonParser();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }));
    }

    public static void init() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkForUpdates();
            } catch (Exception e) {
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static void sendMessage(String message) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("api_key", API_KEY);
                json.addProperty("username", Profile.getUsername());
                json.addProperty("message", message);
                json.addProperty("prefix", "owner");

                HttpURLConnection conn = (HttpURLConnection) new URL(API_URL + "send_message").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        JsonObject response = parser.parse(reader).getAsJsonObject();
                        if (response.get("status").getAsString().equals("success")) {
                            String timestamp = response.get("timestamp").getAsString();
                            if (timestamp.compareTo(lastTimestamp) > 0) {
                                lastTimestamp = timestamp;
                            }
                        } else {
                            String errorMsg = response.get("message").getAsString();
                            OtherUtil.sendMessage("Failed to send message: " + errorMsg);
                        }
                    }
                } else {
                    String errorResponse = "";
                    try (BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        errorResponse = errorReader.lines().collect(Collectors.joining());
                    } catch (Exception ex) {
                    }
                    OtherUtil.sendMessage("HTTP error while sending message: " + responseCode + " - " + errorResponse);
                }
            } catch (Exception e) {
                OtherUtil.sendMessage("Error sending message: " + e.getMessage());
            }
        });
    }

    public static void setPrefix(String prefix) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("api_key", API_KEY);
                json.addProperty("username", Profile.getUsername());
                json.addProperty("prefix", prefix);

                HttpURLConnection conn = (HttpURLConnection) new URL(API_URL + "set_prefix").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        JsonObject response = parser.parse(reader).getAsJsonObject();
                        if (response.get("status").getAsString().equals("success")) {
                            currentPrefix = prefix;
                            OtherUtil.sendMessage("Prefix updated successfully: " + prefix);
                            LOGGER.info("Prefix updated successfully: " + prefix);
                        } else {
                            String errorMsg = response.get("message").getAsString();
                            OtherUtil.sendMessage("Failed to update prefix: " + errorMsg);
                        }
                    }
                } else {
                    OtherUtil.sendMessage("HTTP error while updating prefix: " + responseCode);
                    currentPrefix = prefix;
                    OtherUtil.sendMessage("Prefix set locally: " + prefix + " (not synced with server due to HTTP error)");
                }
            } catch (Exception e) {
                OtherUtil.sendMessage("Error updating prefix: " + e.getMessage());
                currentPrefix = prefix;
                OtherUtil.sendMessage("Prefix set locally: " + prefix + " (not synced due to error)");
            }
        });
    }

    public static String getCurrentPrefix() {
        return currentPrefix;
    }

    private static String formatPrefix(String prefix) {
        switch (prefix) {
            case "owner":
                return TextFormatting.DARK_RED + "" + TextFormatting.BOLD + "OWNER" + TextFormatting.RESET;
            case "admin":
                return TextFormatting.DARK_RED + "" + TextFormatting.BOLD + "ADMIN" + TextFormatting.RESET;
            case "media":
                return TextFormatting.RED + "" + TextFormatting.BOLD + "Y" + TextFormatting.WHITE + TextFormatting.BOLD + "T" + TextFormatting.RESET;
            case "user":
                return TextFormatting.AQUA + "" + TextFormatting.BOLD + "USER" + TextFormatting.RESET;
            default:
                return prefix;
        }
    }

    private static void checkForUpdates() {
        try {
            String urlStr = API_URL + "get_updates?api_key=" + API_KEY;
            if (!lastTimestamp.isEmpty()) {
                urlStr += "&last_timestamp=" + URLEncoder.encode(lastTimestamp, StandardCharsets.UTF_8.name());
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    JsonObject response = parser.parse(reader).getAsJsonObject();
                    if (response.get("status").getAsString().equals("success")) {
                        JsonArray messages = response.getAsJsonArray("messages");
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            JsonObject msg = messages.get(i).getAsJsonObject();
                            String username = msg.get("username").getAsString();
                            String message = msg.get("message").getAsString();
                            String timestamp = msg.get("timestamp").getAsString();
                            String prefix = msg.has("prefix") ? msg.get("prefix").getAsString() : "user";

                            if (!Manager.FUNCTION_MANAGER.irc.state) {
                                continue;
                            }
                            if (!username.equals(Profile.getUsername())) {
                                String formattedPrefix = formatPrefix(prefix);
                                OtherUtil.sendMessageIRC("[" + formattedPrefix + "] " + username + ": " + message);
                            }

                            if (timestamp.compareTo(lastTimestamp) > 0) {
                                lastTimestamp = timestamp;
                                LOGGER.info("Updated last timestamp: " + timestamp);
                            }
                        }
                    } else {
                        String errorMsg = response.get("message").getAsString();
                        OtherUtil.sendMessage("Failed to fetch updates: " + errorMsg);
                    }
                }
            } else {
            }
        } catch (Exception e) {
        }
    }
}