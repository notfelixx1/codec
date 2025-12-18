package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.ModeSetting;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Annotation(name = "StreamerMode", type = TypeList.Player, desc = "Изменяет вашу статистику сервера")
public class StreamerMode extends Module {

    public static ModeSetting mode = new ModeSetting("Донат", "GL.MODER", "MEDIA", "YOUTUBE", "HELPER", "ML.MODER", "MODER", "MODER+", "ST.MODER", "GL.MODER", "ML.ADMIN", "ADMIN");

    private static final Map<Character, String> REPLACEMENT_MAP = new HashMap<>();
    private static final Map<String, Character> DONATE_TO_SYMBOL = new HashMap<>();

    private static final Pattern RANK_PATTERN = Pattern.compile("Ранг\\s*[:]\\s*([ꔀ-꛿])");

    static {
        REPLACEMENT_MAP.put('ꔉ', "HELPER");
        REPLACEMENT_MAP.put('ꔓ', "ML.MODER");
        REPLACEMENT_MAP.put('ꔗ', "MODER");
        REPLACEMENT_MAP.put('ꔡ', "MODER+");
        REPLACEMENT_MAP.put('ꔥ', "ST.MODER");
        REPLACEMENT_MAP.put('ꔩ', "GL.MODER");
        REPLACEMENT_MAP.put('ꔳ', "ML.ADMIN");
        REPLACEMENT_MAP.put('ꔷ', "ADMIN");
        REPLACEMENT_MAP.put('ꔁ', "MEDIA");

        REPLACEMENT_MAP.forEach((symbol, donate) -> DONATE_TO_SYMBOL.put(donate, symbol));
        DONATE_TO_SYMBOL.put("YOUTUBE", 'ꔅ');
    }

    public StreamerMode() {
        addSettings(mode);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }

    private char currentRankSymbol = 0;

    public String patch(String text) {
        if (!this.state || text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = RANK_PATTERN.matcher(text);
        if (matcher.find()) {
            currentRankSymbol = matcher.group(1).charAt(0);
        }

        if (currentRankSymbol == 0) {
            return text;
        }

        char targetSymbol = DONATE_TO_SYMBOL.getOrDefault(mode.get(), 'ꔷ');

        if (currentRankSymbol == targetSymbol) {
            return text;
        }

        return text.replace(currentRankSymbol, targetSymbol);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentRankSymbol = 0;
    }
}