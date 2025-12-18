package monoton.control.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import monoton.module.impl.player.NameProtect;
import monoton.ui.clickgui.Panel;
import monoton.utils.IMinecraft;
import net.minecraft.util.text.TextFormatting;
import monoton.control.Manager;
import monoton.utils.other.OtherUtil;

import java.io.File;

public final class Config {

    private final File file;
    public String author;

    public Config(String name) {
        this.file = new File(ConfigManager.CONFIG_DIR, name + ".json");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public JsonObject save() {
        JsonObject jsonObject = new JsonObject();

        JsonObject modulesObject = new JsonObject();
        Manager.FUNCTION_MANAGER.getFunctions().forEach(module -> modulesObject.add(module.name, module.save()));
        jsonObject.add("Features", modulesObject);

        JsonObject stylesObject = new JsonObject();
        jsonObject.add("styles", stylesObject);

        JsonObject otherObject = new JsonObject();
        if (!otherObject.has("time"))
            otherObject.addProperty("time", System.currentTimeMillis());
        otherObject.addProperty("nameProtect", NameProtect.getNameProtect());
        jsonObject.add("Others", otherObject);

        JsonArray colorsArray = new JsonArray();
        for (Panel.ColorEntry entry : Panel.getColorEntries()) {
            JsonObject colorObject = new JsonObject();
            colorObject.addProperty("useName", entry.useName);
            colorObject.addProperty("color", entry.color);
            colorObject.addProperty("hue", entry.hue);
            colorObject.addProperty("saturation", entry.saturation);
            colorObject.addProperty("brightness", entry.brightness);
            colorObject.addProperty("alpha", entry.alpha);
            colorsArray.add(colorObject);
        }
        jsonObject.add("Colors", colorsArray);

        return jsonObject;
    }

    public void load(JsonObject object, String configuration, boolean start) {
        if (object.has("Features")) {
            JsonObject modulesObject = object.getAsJsonObject("Features");
            Manager.FUNCTION_MANAGER.getFunctions().forEach(module -> {
                if (!start && module.isState()) {
                    module.setState(false);
                }
                module.load(modulesObject.getAsJsonObject(module.name), start);
            });
        }

        try {
            if (object.has("Others")) {
                JsonObject otherObject = object.getAsJsonObject("Others");
                if (otherObject.has("nameProtect")) {
                    String nameProtect = otherObject.get("nameProtect").getAsString();
                    NameProtect.setNameProtect(nameProtect);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load theme colors
        if (object.has("Colors")) {
            JsonArray colorsArray = object.getAsJsonArray("Colors");
            for (Panel.ColorEntry entry : Panel.getColorEntries()) {
                for (int i = 0; i < colorsArray.size(); i++) {
                    JsonObject colorObject = colorsArray.get(i).getAsJsonObject();
                    if (colorObject.get("useName").getAsString().equals(entry.useName)) {
                        entry.color = colorObject.get("color").getAsInt();
                        entry.hue = colorObject.get("hue").getAsFloat();
                        entry.saturation = colorObject.get("saturation").getAsFloat();
                        entry.brightness = colorObject.get("brightness").getAsFloat();
                        entry.alpha = colorObject.get("alpha").getAsFloat();
                        if (entry.useName.equals("primaryColor")) {
                            Panel.setSelectedColor(
                                    entry.color,
                                    entry.hue,
                                    entry.saturation,
                                    entry.brightness,
                                    entry.alpha
                            );
                        }
                    }
                }
            }
        }

        OtherUtil.sendMessage("Конфигурация " + TextFormatting.RED + configuration + TextFormatting.RESET + " загружена");
    }

    public File getFile() {
        return file;
    }
}