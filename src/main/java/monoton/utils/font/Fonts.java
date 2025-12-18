package monoton.utils.font;

import lombok.SneakyThrows;
import monoton.utils.font.common.Lang;
import monoton.utils.font.styled.StyledFont;

public class Fonts {
    public static final String FONT_DIR = "/assets/minecraft/monoton/font/";
    private static final Lang DEFAULT_LANG = Lang.ENG_RU;
    private static final float DEFAULT_OFFSET_X = 0.0f;
    private static final float DEFAULT_OFFSET_Y = 0.0f;
    private static final float DEFAULT_OFFSET_Z = 0.0f;
    private static final boolean DEFAULT_ANTI_ALIAS = true;

    public static final StyledFont[] msSemiBold = new StyledFont[24];
    public static final StyledFont[] icon = new StyledFont[33];
    public static final StyledFont[] iconnew = new StyledFont[33];
    public static final StyledFont[] intl = new StyledFont[32];

    private static final FontConfig[] FONT_CONFIGS = {
            new FontConfig("suisseintl.ttf", intl, 8, 31),
            new FontConfig("icon.ttf", icon, 8, 32),
            new FontConfig("icon-new.ttf", iconnew, 8, 32),
            new FontConfig("semibold.ttf", msSemiBold, 8, 23)
    };

    private record FontConfig(String fileName, StyledFont[] fontArray, int startSize, int endSize) {}

    @SneakyThrows
    public static void init() {
        for (FontConfig config : FONT_CONFIGS) {
            for (int i = config.startSize; i <= config.endSize; i++) {
                config.fontArray[i] = new StyledFont(
                        config.fileName, i, DEFAULT_OFFSET_X, DEFAULT_OFFSET_Y, DEFAULT_OFFSET_Z,
                        DEFAULT_ANTI_ALIAS, DEFAULT_LANG
                );
            }
        }
    }
}