package de.uni_jena.fpp.chatroom;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerLogger implements AutoCloseable {

    private final Path logFile;
    private final BufferedWriter writer;
    private final List<ServerLogListener> listeners = new CopyOnWriteArrayList<>();
    private final Deque<String> history = new ArrayDeque<>();
    private final int maxHistory;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ServerLogger(String filePath) throws IOException {
        this(filePath, 500);
    }

    public ServerLogger(String filePath, int maxHistory) throws IOException {
        this.logFile = Path.of(filePath);
        this.maxHistory = Math.max(50, maxHistory);

        if (logFile.getParent() != null) {
            Files.createDirectories(logFile.getParent());
        }

        this.writer = Files.newBufferedWriter(
                logFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        );
    }

    public void addListener(ServerLogListener l) {
        if (l != null) listeners.add(l);
    }
    public void info(String msg) { log("INFO", msg); }
    public void warn(String msg) { log("WARN", msg); }
    public void error(String msg) { log("ERROR", msg); }

    public void log(String level, String msg) {
        String line = fmt.format(LocalDateTime.now())
                + " [" + level + "] "
                + (msg == null ? "" : msg);

        // Datei + History
        synchronized (this) {
            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("[LOGGER] Schreiben fehlgeschlagen: " + e.getMessage());
            }

            history.addLast(line);
            while (history.size() > maxHistory) history.removeFirst();
        }

        // UI-Callback
        for (ServerLogListener l : listeners) {
            try {
                l.onLogLine(line);
            } catch (Exception ignore) {}
        }
    }

    @Override
    public synchronized void close() {
        try {
            writer.close();
        } catch (IOException ignore) {}
    }
}
