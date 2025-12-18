package monoton.ui.alt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import monoton.control.Manager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Account {
    public String accountName;
    public Date creationDate;
    public long dateAdded;
    public ResourceLocation skin;
    public float x, y;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2); // Thread pool for async tasks

    public Account(String accountName) {
        this(accountName, System.currentTimeMillis());
    }

    public Account(String accountName, long dateAdded) {
        this.accountName = accountName;
        this.dateAdded = dateAdded;
        this.creationDate = new Date();
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + accountName).getBytes(StandardCharsets.UTF_8)); // Fallback UUID
        this.skin = DefaultPlayerSkin.getDefaultSkin(uuid);
        resolveUUIDAsync(accountName, uuid); // Async UUID resolution
    }

    private void resolveUUIDAsync(String name, UUID fallbackUUID) {
        EXECUTOR.submit(() -> {
            try (InputStreamReader in = new InputStreamReader(
                    new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openStream(),
                    StandardCharsets.UTF_8)) {
                JsonObject json = new Gson().fromJson(in, JsonObject.class);
                String id = json.get("id").getAsString();
                UUID uuid = UUID.fromString(id.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                loadSkinAsync(uuid);
            } catch (IOException e) {
                loadSkinAsync(fallbackUUID); // Use fallback UUID if API call fails
            }
        });
    }

    private void loadSkinAsync(UUID uuid) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().getSkinManager().loadProfileTextures(
                    new GameProfile(uuid, accountName),
                    (type, loc, tex) -> {
                        if (type == MinecraftProfileTexture.Type.SKIN) {
                            skin = loc;
                        }
                    }, false); // Set to false to avoid immediate loading
        });
    }
}

