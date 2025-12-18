package monoton.control.config;

import com.google.gson.*;
import monoton.utils.IMinecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {

    public static final File CONFIG_DIR = new File("C:\\Monoton\\game\\Monoton\\config");
    private final File autoCfgDir = new File(CONFIG_DIR, "default.json");
    private static final JsonParser jsonParser = new JsonParser();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void init() throws Exception {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        if (!autoCfgDir.exists()) {
            autoCfgDir.createNewFile();
        }
        if (autoCfgDir.exists()) {
            if (autoCfgDir.length() > 0) {
                loadConfiguration("default", true);
            } else {
                saveConfiguration("default");
            }
        }
    }

    public List<String> getAllConfigurations() {
        List<String> configurations = new ArrayList<>();
        File[] files = CONFIG_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    String configName = file.getName().substring(0, file.getName().lastIndexOf(".json"));
                    configurations.add(configName);
                }
            }
        }
        return configurations;
    }

    public void loadConfiguration(String configuration, boolean start) {
        Config config = findConfig(configuration);
        if (config == null) {
            return;
        }

        try {
            JsonElement element = readFileAsJson(config.getFile());
            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                config.load(object, configuration, start);
            } else {
                saveConfiguration(configuration);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveConfiguration(String configuration) {
        Config config = findConfig(configuration);
        if (config == null) {
            config = new Config(configuration);
        }

        writeJsonToFile(config.getFile(), config.save());
    }

    public Config findConfig(String configName) {
        if (configName == null) return null;
        File configFile = new File(CONFIG_DIR, configName + ".json");
        if (configFile.exists()) {
            return new Config(configName);
        }
        return null;
    }

    public void deleteConfig(String configName) {
        if (configName == null) return;
        Config config = findConfig(configName);
        if (config != null) {
            File file = config.getFile();
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    System.out.println("Не удалось удалить конфиг " + file.getAbsolutePath());
                }
            }
        }
    }


    private static JsonElement readFileAsJson(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return jsonParser.parse(content);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static void writeJsonToFile(File file, JsonElement jsonElement) {
        try {
            String content = gson.toJson(jsonElement);
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}