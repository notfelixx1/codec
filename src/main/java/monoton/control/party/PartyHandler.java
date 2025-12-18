package monoton.control.party;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static monoton.utils.IMinecraft.mc;

public class PartyHandler {
    private static final String API_URL = "http://185.100.157.243:6000/";
    private static final String API_KEY = "yaponecgeypoleteliyra";
    private static final Gson GSON = new GsonBuilder().create();
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
    private static final ConcurrentHashMap<String, double[]> PARTY_MEMBER_COORDINATES = new ConcurrentHashMap<>();
    private static final long MIN_REQUEST_INTERVAL_MS = 200;
    private static volatile boolean isPartyActive = false;
    private static volatile String currentPartyCode = null;
    private static volatile String lastPartyMessageTimestamp = "";
    private static volatile long lastRequestTimestamp = 0;
    private static volatile double[] lastSentCoordinates = null; // Tracks last sent coordinates [x, z]

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SCHEDULER.shutdown();
            try {
                if (!SCHEDULER.awaitTermination(1, TimeUnit.SECONDS)) {
                    SCHEDULER.shutdownNow();
                }
            } catch (InterruptedException e) {
                SCHEDULER.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    public static void init() {
        SCHEDULER.scheduleAtFixedRate(() -> {
            if (!isPartyActive || mc.player == null) {
                return;
            }
            updatePartyData(Profile.getUsername());
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static boolean isInParty() {
        return isPartyActive;
    }

    public static String getCurrentPartyCode() {
        return currentPartyCode;
    }

    public static ConcurrentHashMap<String, double[]> getPartyMemberCoordinates() {
        return PARTY_MEMBER_COORDINATES;
    }

    public static String createParty(String username) {
        String partyCode = generatePartyCode();
        if (sendPartyCreateToServer(username, partyCode)) {
            isPartyActive = true;
            currentPartyCode = partyCode;
            lastPartyMessageTimestamp = "";
            lastSentCoordinates = null; // Reset coordinates on party creation
            return partyCode;
        }
        return null;
    }

    public static boolean joinParty(String username, String partyCode) {
        if (sendJoinPartyToServer(username, partyCode)) {
            isPartyActive = true;
            currentPartyCode = partyCode;
            lastPartyMessageTimestamp = "";
            lastSentCoordinates = null; // Reset coordinates on joining party
            return true;
        }
        return false;
    }

    public static void leaveParty(String username) {
        if (currentPartyCode != null) {
            sendLeavePartyToServer(username, currentPartyCode);
        }
        isPartyActive = false;
        currentPartyCode = null;
        lastPartyMessageTimestamp = "";
        lastSentCoordinates = null; // Reset coordinates on leaving party
    }

    public static boolean disbandParty(String username, String partyCode) {
        JsonObject json = buildJsonObject(username, partyCode);
        JsonObject response = sendPostRequest("disband_party", json, "Error disbanding party");
        if (response != null && "success".equals(response.get("status").getAsString())) {
            isPartyActive = false;
            currentPartyCode = null;
            lastPartyMessageTimestamp = "";
            lastSentCoordinates = null; // Reset coordinates on disbanding party
            return true;
        }
        return false;
    }

    public static boolean dismissPartyLeader(String username, String newLeader, String partyCode) {
        JsonObject json = buildJsonObject(username, partyCode);
        json.addProperty("new_leader", newLeader);
        JsonObject response = sendPostRequest("dismiss_party_leader", json, "Error transferring party leadership");
        return response != null && "success".equals(response.get("status").getAsString());
    }

    public static boolean kickPartyMember(String username, String kickedUser, String partyCode) {
        JsonObject json = buildJsonObject(username, partyCode);
        json.addProperty("kicked_user", kickedUser);
        JsonObject response = sendPostRequest("kick_party_member", json, "Error kicking party member");
        return response != null && "success".equals(response.get("status").getAsString());
    }

    public static boolean sendPartyMessage(String username, String message) {
        JsonObject json = buildJsonObject(username, currentPartyCode);
        json.addProperty("message", message);
        JsonObject response = sendPostRequest("send_party_message", json, "Error sending party message");
        if (response != null && "success".equals(response.get("status").getAsString())) {
            String timestamp = response.get("timestamp").getAsString();
            if (timestamp.compareTo(lastPartyMessageTimestamp) > 0) {
                lastPartyMessageTimestamp = timestamp;
            }
            return true;
        }
        return false;
    }

    public static class PartyInfo {
        public final List<String> members;
        public final String leader;

        public PartyInfo(List<String> members, String leader) {
            this.members = members;
            this.leader = leader;
        }
    }

    public static PartyInfo getPartyInfo(String username, String partyCode) {
        try {
            String urlStr = API_URL + "get_party_info?api_key=" + API_KEY + "&username=" +
                    URLEncoder.encode(username, StandardCharsets.UTF_8.name()) +
                    "&party_code=" + URLEncoder.encode(partyCode, StandardCharsets.UTF_8.name());
            JsonObject response = sendGetRequest(urlStr, "Error retrieving party info");
            if (response != null && "success".equals(response.get("status").getAsString())) {
                JsonArray membersArray = response.getAsJsonArray("members");
                String leader = response.get("leader").getAsString();
                List<String> members = new ArrayList<>();
                membersArray.forEach(member -> members.add(member.getAsString()));
                return new PartyInfo(members, leader);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String generatePartyCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private static boolean sendPartyCreateToServer(String username, String partyCode) {
        JsonObject json = buildJsonObject(username, partyCode);
        JsonObject response = sendPostRequest("create_party", json, "Error creating party");
        return response != null && "success".equals(response.get("status").getAsString());
    }

    private static boolean sendJoinPartyToServer(String username, String partyCode) {
        JsonObject json = buildJsonObject(username, partyCode);
        JsonObject response = sendPostRequest("join_party", json, "Error joining party");
        return response != null && "success".equals(response.get("status").getAsString());
    }

    private static void sendLeavePartyToServer(String username, String partyCode) {
        JsonObject json = buildJsonObject(username, partyCode);
        sendPostRequest("leave_party", json, "Error leaving party");
    }

    private static JsonObject buildJsonObject(String username, String partyCode) {
        JsonObject json = new JsonObject();
        json.addProperty("api_key", API_KEY);
        json.addProperty("username", username);
        json.addProperty("party_code", partyCode);
        return json;
    }

    private static JsonObject sendPostRequest(String endpoint, JsonObject json, String errorMessage) {
        try {
            rateLimitRequest();
            HttpURLConnection conn = setupConnection(API_URL + endpoint, "POST");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
            }
            return handleResponse(conn, errorMessage);
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonObject sendGetRequest(String urlStr, String errorMessage) {
        try {
            rateLimitRequest();
            HttpURLConnection conn = setupConnection(urlStr, "GET");
            return handleResponse(conn, errorMessage);
        } catch (Exception e) {
            return null;
        }
    }

    private static HttpURLConnection setupConnection(String urlStr, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept-Charset", "UTF-8");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setDoOutput(method.equals("POST"));
        return conn;
    }

    private static JsonObject handleResponse(HttpURLConnection conn, String errorMessage) throws Exception {
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return GSON.fromJson(reader, JsonObject.class);
            }
        }
        return null;
    }

    private static void rateLimitRequest() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestTimestamp < MIN_REQUEST_INTERVAL_MS) {
            Thread.sleep(MIN_REQUEST_INTERVAL_MS - (currentTime - lastRequestTimestamp));
        }
        lastRequestTimestamp = currentTime;
    }

    private static void updatePartyData(String username) {
        if (mc.player == null) {
            return;
        }
        try {
            double currentX = mc.player.getPosX();
            double currentZ = mc.player.getPosZ();
            boolean shouldUpdateCoordinates = lastSentCoordinates == null ||
                    currentX != lastSentCoordinates[0] || currentZ != lastSentCoordinates[1];

            if (shouldUpdateCoordinates) {
                JsonObject coordJson = buildJsonObject(username, currentPartyCode);
                coordJson.addProperty("x", currentX);
                coordJson.addProperty("z", currentZ);
                JsonObject coordResponse = sendPostRequest("update_coordinates", coordJson, "Error updating coordinates");
                if (coordResponse == null || !"success".equals(coordResponse.get("status").getAsString())) {
                    return;
                }
                lastSentCoordinates = new double[]{currentX, currentZ};
            }

            String urlStr = API_URL + "get_updates?api_key=" + API_KEY + "&username=" +
                    URLEncoder.encode(username, StandardCharsets.UTF_8.name());
            if (!lastPartyMessageTimestamp.isEmpty()) {
                urlStr += "&last_timestamp=" + URLEncoder.encode(lastPartyMessageTimestamp, StandardCharsets.UTF_8.name());
            }
            JsonObject response = sendGetRequest(urlStr, "Error retrieving updates");
            if (response != null && "success".equals(response.get("status").getAsString())) {
                JsonArray partyMessages = response.getAsJsonArray("party_messages");
                partyMessages.forEach(msg -> {
                    JsonObject message = msg.getAsJsonObject();
                    String msgUsername = message.get("username").getAsString();
                    String text = message.get("message").getAsString();
                    String timestamp = message.get("timestamp").getAsString();
                    OtherUtil.sendMessageIRC(text);
                    if (timestamp.compareTo(lastPartyMessageTimestamp) > 0) {
                        lastPartyMessageTimestamp = timestamp;
                    }
                });

                JsonObject coordFetchResponse = sendGetRequest(
                        API_URL + "get_party_coordinates?api_key=" + API_KEY + "&party_code=" + currentPartyCode,
                        "Error retrieving party coordinates"
                );
                if (coordFetchResponse != null && "success".equals(coordFetchResponse.get("status").getAsString())) {
                    PARTY_MEMBER_COORDINATES.clear();
                    JsonArray members = coordFetchResponse.getAsJsonArray("members");
                    members.forEach(member -> {
                        JsonObject memberObj = member.getAsJsonObject();
                        String memberUsername = memberObj.get("username").getAsString();
                        if (!memberUsername.equals(username)) {
                            double x = memberObj.get("x").getAsDouble();
                            double z = memberObj.get("z").getAsDouble();
                            PARTY_MEMBER_COORDINATES.put(memberUsername, new double[]{x, z});
                        }
                    });
                }
            }
        } catch (Exception e) {
        }
    }
}