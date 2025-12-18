package monoton.ui.alt;

import monoton.control.Manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class AltConfig {
    public static final Path FILE_PATH = Path.of("C:\\Monoton\\game\\Monoton\\altmanager.json");
    private static final ConcurrentLinkedQueue<Runnable> pendingUpdates = new ConcurrentLinkedQueue<>();
    private static boolean isUpdating = false;

    public void init() throws IOException {
        if (!Files.exists(FILE_PATH.getParent())) {
            Files.createDirectories(FILE_PATH.getParent()); // Ensure directory exists
        }
        if (!Files.exists(FILE_PATH)) {
            Files.createFile(FILE_PATH);
        } else {
            readAlts();
        }
    }

    public static void updateFile() {
        pendingUpdates.offer(() -> {
            try {
                String content = Manager.ALT.accounts.stream()
                        .map(alt -> alt.accountName + ":" + alt.dateAdded)
                        .collect(Collectors.joining("\n"));
                Files.write(FILE_PATH, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        processPendingUpdates();
    }

    private static synchronized void processPendingUpdates() {
        if (isUpdating) return;
        isUpdating = true;
        Thread updateThread = new Thread(() -> {
            while (!pendingUpdates.isEmpty()) {
                Runnable update = pendingUpdates.poll();
                if (update != null) {
                    update.run();
                }
            }
            isUpdating = false;
        });
        updateThread.start();
    }

    private void readAlts() {
        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String username = parts[0];
                    long dateAdded = Long.parseLong(parts[1]);
                    Manager.ALT.accounts.add(new Account(username, dateAdded));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
