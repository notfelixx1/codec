package monoton.utils.misc;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IPackNameDecorator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static monoton.utils.IMinecraft.mc;

public class KeepTheResourcePack {

    private static final File latestServerResourcePack = new File(mc.gameDir.toPath().toFile(), "latestServerResourcePack.json");
    public static File cacheResourcePackFile = null;

    public KeepTheResourcePack() {
        if (latestServerResourcePack.exists()) {
            try {
                JsonObject jsonObject = new JsonParser().parse(FileUtils.readFileToString(latestServerResourcePack, Charsets.UTF_8)).getAsJsonObject();
                File resourcePack = new File(jsonObject.get("file").getAsString());

                if (resourcePack.exists()) {
                    cacheResourcePackFile = resourcePack;
                    Minecraft.getInstance().getPackFinder().setServerPack(resourcePack, IPackNameDecorator.SERVER);
                    Minecraft.getInstance().scheduleResourcesRefresh();
                } else setLatestServerResourcePack(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setLatestServerResourcePack(File file) {
        if (file == null) latestServerResourcePack.delete();
        else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("file", file.getPath());

            cacheResourcePackFile = file;
            try {
                FileUtils.writeStringToFile(latestServerResourcePack, jsonObject.toString(), Charsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}