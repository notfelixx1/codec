package monoton.cmd.impl;

import net.minecraft.client.gui.overlay.PlayerTabOverlayGui;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.text.TextFormatting;
import monoton.cmd.Cmd;
import monoton.cmd.CmdInfo;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CmdInfo(name = "parse", description = "Парсит префиксы игроков с таба")
public class ParseCmd extends Cmd {

    private static final File parseDir = new File("C:\\Monoton\\game\\Monoton\\parser");

    @Compile
    @Override
    public void run(String[] args) throws Exception {
        if (mc.isSingleplayer()) {
            sendMessage(TextFormatting.GRAY + "Эта команда не работает в одиночной игре!");
            return;
        }

        Map<String, List<String>> players = new LinkedHashMap<>();

        for (NetworkPlayerInfo playerInfo :
                PlayerTabOverlayGui.ENTRY_ORDERING.sortedCopy(mc.player.connection.getPlayerInfoMap())) {

            if (playerInfo.getPlayerTeam() != null) {
                String prefix = playerInfo.getPlayerTeam().getPrefix().getUnformattedComponentText();
                if (prefix != null && !prefix.isEmpty()) {
                    players.computeIfAbsent(prefix, k -> new ArrayList<>())
                            .add(playerInfo.getGameProfile().getName());
                }
            }
        }

        if (players.isEmpty()) {
            sendMessage(TextFormatting.GRAY + "Префиксов не обнаружено!");
            return;
        }

        try {
            if (!parseDir.exists()) parseDir.mkdirs();

            String serverIp = mc.getCurrentServerData().serverIP;
            int n = 1;
            File file = new File(parseDir, serverIp + "#" + n + ".txt");
            while (file.exists()) {
                file = new File(parseDir, serverIp + "#" + ++n + ".txt");
            }
            file.createNewFile();

            StringBuilder stringBuilder = new StringBuilder();
            players.keySet().forEach(prefix -> {
                stringBuilder.append(prefix.trim()).append(":\n");
                players.get(prefix).forEach(nick -> stringBuilder.append(nick).append("\n"));
                stringBuilder.append("\n");
            });

            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write(stringBuilder.toString());
            writer.close();

            sendMessage(TextFormatting.GRAY + "Готово!");
            try {
                java.awt.Desktop.getDesktop().open(file);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(TextFormatting.RED + "Какая-то ошибка при парсе! Подробности в консоли.");
        }
    }

    @Compile
    @Override
    public void error() {
        sendMessage(TextFormatting.GRAY + "Использование" + TextFormatting.WHITE + ":");
        sendMessage(TextFormatting.WHITE + ".parse");
    }
}