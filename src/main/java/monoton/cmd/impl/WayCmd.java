package monoton.cmd.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.utils.font.Fonts;
import monoton.utils.font.styled.StyledFont;
import monoton.utils.other.OtherUtil;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.ProjectionUtils;
import monoton.utils.render.RenderUtilka;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.TextFormatting;
import net.optifine.util.WorldUtils;
import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.copyOfRange;
import static monoton.ui.clickgui.Panel.getColorByName;
import static monoton.utils.render.ColorUtils.rgba;

@CmdInfo(
        name = "way",
        description = "система меток с названиями и сохранением"
)
public class WayCmd extends Cmd {

    private static final File WAYPOINTS_FILE = new File("C:/Monoton/game/Monoton/waypoints.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<Waypoint> waypoints = new ArrayList<>();

    static {
        loadWaypoints();
    }

    public static class Waypoint {
        public float x, z;
        public String name;
        public String dimension; // "overworld" или "nether"

        public Waypoint(float x, float z, String name, String dimension) {
            this.x = x;
            this.z = z;
            this.name = name.length() > 16 ? name.substring(0, 16) : name;
            this.dimension = dimension;
        }
    }

    @Override
    public void run(String[] args) throws Exception {
        if (args.length == 1) {
            error();
            return;
        }

        String sub = args[1].toLowerCase();

        // === LIST ===
        if (sub.equals("list")) {
            if (waypoints.isEmpty()) {
                OtherUtil.sendMessage(TextFormatting.WHITE + "Список меток пуст");
                return;
            }

            OtherUtil.sendMessage(TextFormatting.RED + "══ Список меток ══");
            for (int i = 0; i < waypoints.size(); i++) {
                Waypoint w = waypoints.get(i);
                OtherUtil.sendMessage(String.format(
                        TextFormatting.WHITE + "%d. " + TextFormatting.WHITE + "%s " +
                                TextFormatting.GRAY + "→ " + TextFormatting.GRAY + "%d %d",
                        i + 1, w.name, (int)w.x, (int)w.z
                ));
            }
            return;
        }

        // === CLEAR ===
        if (sub.equals("clear")) {
            int count = waypoints.size();
            waypoints.clear();
            saveWaypoints();
            OtherUtil.sendMessage(TextFormatting.RED + "Удалено меток: " + count);
            return;
        }

        // === REMOVE / DEL / DELETE ===
        if (sub.equals("remove") || sub.equals("del") || sub.equals("delete")) {
            if (args.length < 3) {
                OtherUtil.sendMessage(TextFormatting.RED + "Укажи номер или название метки");
                return;
            }

            String target = args[2];
            boolean removed = false;
            String removedName = "";

            if (isNumeric(target)) {
                int id = Integer.parseInt(target) - 1;
                if (id >= 0 && id < waypoints.size()) {
                    Waypoint w = waypoints.remove(id);
                    removedName = w.name;
                    OtherUtil.sendMessage(TextFormatting.WHITE + "Метка удалена: " + TextFormatting.RED + "#" + (id + 1) + " " + TextFormatting.WHITE + removedName);
                    removed = true;
                }
            } else {
                for (int i = 0; i < waypoints.size(); i++) {
                    Waypoint w = waypoints.get(i);
                    if (w.name.equalsIgnoreCase(target)) {
                        waypoints.remove(i);
                        removedName = w.name;
                        OtherUtil.sendMessage(TextFormatting.WHITE + "Метка удалена: " + TextFormatting.RED + removedName);
                        removed = true;
                        break;
                    }
                }
            }

            if (!removed) {
                OtherUtil.sendMessage(TextFormatting.RED + "Метка не найдена");
            } else {
                saveWaypoints();
            }
            return;
        }

        // === ADD WAYPOINT ===
        if (args.length >= 3) {
            try {
                float x = Float.parseFloat(args[1]);
                float z = Float.parseFloat(args[2]);

                String name = "Метка" + (waypoints.size() + 1);
                if (args.length >= 4) {
                    name = String.join(" ", copyOfRange(args, 3, args.length));
                    if (name.length() > 16) name = name.substring(0, 16);
                }

                String dim = WorldUtils.isNether(mc.world) ? "nether" : "overworld";
                waypoints.add(new Waypoint(x, z, name, dim));
                saveWaypoints();

                OtherUtil.sendMessage(TextFormatting.WHITE + "Метка добавлена: " +
                        TextFormatting.WHITE + "\"" + name + "\" " +
                        TextFormatting.GRAY + (int)x + " " + (int)z);

            } catch (NumberFormatException e) {
                OtherUtil.sendMessage(TextFormatting.RED + "Неверный формат координат");
            }
            return;
        }

        error();
    }

    public static void drawTags(MatrixStack stack) {
        if (waypoints.isEmpty()) return;

        StyledFont small = Fonts.intl[10];
        float height = 15F;

        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int fonduoColor = ColorUtils.setAlpha(getColorByName("fonduoColor"), 224);
        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");

        boolean isNether = WorldUtils.isNether(mc.world);

        for (Waypoint wp : waypoints) {
            double x = wp.x;
            double z = wp.z;

            // Делим на 8 только если текущий мир — Незер, а метка из Верхнего мира
            if (isNether && wp.dimension.equals("overworld")) {
                x /= 8.0;
                z /= 8.0;
            }
            // Обратная совместимость: если в Верхнем мире, а метка из Незера — умножаем
            if (!isNether && wp.dimension.equals("nether")) {
                x *= 8.0;
                z *= 8.0;
            }

            Vector2f screen = ProjectionUtils.project2f(x, 100.0, z);
            if (screen == null) continue;

            int distance = (int) Math.sqrt(
                    Math.pow(x - mc.player.getPosX(), 2) +
                            Math.pow(z - mc.player.getPosZ(), 2)
            );

            String text = wp.name.isEmpty() ? "???" : wp.name;
            String distText = distance + "m";

            float fixedWidth = small.getWidth("HOME") + 3.5F;
            float dynamicWidth = small.getWidth(text) + 3.5F;
            float textWidth = Math.max(dynamicWidth, fixedWidth);

            float baseX = screen.x - 50.5f;
            float baseY = screen.y - 51.0f;

            // Верхняя часть (иконка + фон)
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(
                    baseX, baseY - 3, textWidth + 1.0F, height + 5.0F,
                    new Vector4f(4f, 0f, 4f, 0f), fonduoColor, 1);

            Fonts.icon[17].drawString(stack, "H",
                    baseX + (textWidth / 2) - 4,
                    baseY + 5.5f,
                    ColorUtils.setAlpha(iconColor, 245));

            // Нижняя часть
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(
                    baseX, baseY + height, textWidth + 1.0F, height + 2.0F,
                    new Vector4f(0f, 4f, 0f, 4f), fonColor, 1);

            // Маленький квадратик снизу
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(
                    baseX + (textWidth / 2 - 3f),
                    baseY + height + height - 1.5f,
                    7, 7, new Vector4f(0f, 5f, 0f, 5f), fonColor, 1);

            // Название
            small.drawString(stack, text,
                    baseX + (textWidth / 2 - small.getWidth(text) / 2 + 0.5f),
                    baseY + 18.5f,
                    ColorUtils.setAlpha(textColor, 245));

            // Расстояние
            small.drawString(stack, distText,
                    baseX + (textWidth / 2 - small.getWidth(distText) / 2 + 0.5f),
                    baseY + 26f,
                    ColorUtils.setAlpha(textColor, 184));
        }
    }

    private static void saveWaypoints() {
        try {
            Files.createDirectories(WAYPOINTS_FILE.getParentFile().toPath());
            Files.write(WAYPOINTS_FILE.toPath(), GSON.toJson(waypoints).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadWaypoints() {
        if (!WAYPOINTS_FILE.exists()) return;
        try {
            String json = new String(Files.readAllBytes(WAYPOINTS_FILE.toPath()));
            List<Waypoint> loaded = GSON.fromJson(json, new TypeToken<List<Waypoint>>() {
            }.getType());
            if (loaded != null) waypoints.addAll(loaded);
        } catch (Exception e) {
            OtherUtil.sendMessage(TextFormatting.RED + "Ошибка загрузки меток!");
        }
    }

    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void error() {
        OtherUtil.sendMessage(TextFormatting.GRAY + "Использование команды " + TextFormatting.WHITE + ".way" + TextFormatting.GRAY + ":");
        OtherUtil.sendMessage(TextFormatting.WHITE + ".way " + TextFormatting.AQUA + "<x> <z> " + TextFormatting.GRAY + "[название]" + TextFormatting.WHITE + " — добавить метку");
        OtherUtil.sendMessage(TextFormatting.WHITE + ".way remove " + TextFormatting.GRAY + "<" + TextFormatting.RED + "id/название" + TextFormatting.GRAY + ">" + TextFormatting.WHITE + " — удалить метку");
        OtherUtil.sendMessage(TextFormatting.WHITE + ".way clear" + TextFormatting.WHITE + " — удалить все метки");
        OtherUtil.sendMessage(TextFormatting.WHITE + ".way list" + TextFormatting.WHITE + " — показать список меток");
    }
}