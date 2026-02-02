package de.uni_jena.fpp.chatroom;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient {
    private volatile String username;
    public String getUsername(){
        return username;
    }

    private final String host;
    private final int port;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private volatile boolean running = true;
    private final Path downloadDir = Path.of("downloads");

    // Model + Listener
    private final ClientModel model = new ClientModel();
    private final CopyOnWriteArrayList<ChatClientListener> listeners = new CopyOnWriteArrayList<>();

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ClientModel getModel() {
        return model;
    }

    public void addListener(ChatClientListener l) {
        if (l != null) listeners.add(l);
    }

    public void removeListener(ChatClientListener l) {
        listeners.remove(l);
    }

    // Connection API für gui
    public void connect() throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        running = true;
        Thread listener = new Thread(this::listenToServer, "ServerListener");
        listener.start();

        fireInfo("Verbunden mit " + host + ":" + port);
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignore) {}
        fireConnectionClosed();
    }

    // Actions für UI
    public void register(String username, String password) throws IOException {
        send(Protocol.buildRegister(username, password));
    }

    public void login(String username, String password) throws IOException {
        this.username = username;
        send(Protocol.buildLogin(username, password));
    }

    public void createRoom(String name) throws IOException {
        send(Protocol.buildCreateRoom(name));
    }

    public void join(String room) throws IOException {
        send(Protocol.buildJoin(room));
    }

    public void leave() throws IOException {
        send(Protocol.buildLeave());
    }

    public void sendMessage(String text) throws IOException {
        send(Protocol.buildMsg(text));
    }

    public void logout() throws IOException {
        send(Protocol.buildLogout());
    }

    public void start() {
        try {
            connect();
            readConsoleInput();
        } catch (Exception e) {
            System.err.println("[CLIENT] Fehler: " + e.getMessage());
            fireError(e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void listenToServer() {
        try {
            while (running && socket != null && !socket.isClosed()) {
                String line;
                try {
                    line = in.readUTF();
                } catch (EOFException eof) {
                    fireInfo("Server hat Verbindung geschlossen.");
                    break;
                }

                if (line == null) break;
                handleServerMessage(line);
            }
        } catch (IOException e) {
            if (running) {
                fireError("Lesen fehlgeschlagen: " + e.getMessage());
            }
        } finally {
            running = false;
            fireConnectionClosed();
        }
    }

    private void handleServerMessage(String line) {
        // CHAT <room> <from> <text...> (4 tokens)
        if (line.startsWith(Protocol.RES_CHAT + " ")) {
            String[] t = line.split("\\s+", 4);
            if (t.length >= 4) {
                String room = t[1];
                String from = t[2];
                String text = t[3];

                model.addChatLine("[" + room + "][" + from + "] " + text);
                fireChat(room, from, text);
                return;
            }
        }

        if (line.startsWith(Protocol.RES_FILE + " ")) {
            String[] t = line.trim().split("\\s+", 3);
            if (t.length < 3) {
                model.addChatLine("[ERROR] Ungültiger FILE Header: " + line);
                fireError("Ungültiger FILE Header");
                return;
            }

            String filename = t[1];
            long size;
            try {
                size = Long.parseLong(t[2]);
            } catch (NumberFormatException e) {
                model.addChatLine("[ERROR] Ungültige Dateigröße: " + t[2]);
                fireError("Ungültige Dateigröße");
                return;
            }

            try {
                receiveFile(filename, size);
                String msg = "Download gespeichert: " + filename + " (" + size + " Bytes)";
                model.addChatLine("[INFO] " + msg);
                fireInfo(msg);
            } catch (IOException e) {
                model.addChatLine("[ERROR] Download fehlgeschlagen: " + e.getMessage());
                fireError("Download fehlgeschlagen: " + e.getMessage());
            }
            return;
        }

        String[] tokens = Protocol.splitTokens(line);
        if (tokens.length == 0) return;

        switch (tokens[0]) {

            case Protocol.RES_ROOM_LIST -> {
                List<String> rooms = (tokens.length >= 2)
                        ? Protocol.parsePipeList(tokens[1])
                        : List.of();
                model.setRooms(rooms);
                fireRoomsUpdated(model.getRooms());
            }

            case Protocol.RES_ROOM_USERS -> {
                if (tokens.length < 2) return;

                String room = tokens[1];
                List<String> users = (tokens.length >= 3)
                        ? Protocol.parsePipeList(tokens[2])
                        : List.of();

                String cur = model.getCurrentRoom();
                boolean roomChanged = (cur == null || cur.isBlank() || !cur.equals(room));

                if (roomChanged) {
                    model.setCurrentRoom(room); // cleared users/files

                }
                model.setUsersInCurrentRoom(users);
                fireUsersUpdated(room, users);
            }

            case Protocol.RES_WARN -> {
                String txt = payload(tokens);
                model.addChatLine("[WARN] " + txt);
                fireWarn(txt);
            }

            case Protocol.RES_BANNED -> {
                String reason = payload(tokens);
                model.addChatLine("[BANNED] " + reason);
                fireBanned(reason);
                disconnect();
            }

            case Protocol.RES_INFO -> {
                String txt = payload(tokens);

                if (txt != null && txt.startsWith("Joined ")) {
                    String room = txt.substring("Joined ".length()).trim();
                    if (!room.isBlank()) {
                        String cur = model.getCurrentRoom();
                        boolean roomChanged = (cur == null || cur.isBlank() || !cur.equals(room));
                        if (roomChanged) {
                            model.setCurrentRoom(room);
                        }
                    }
                }

                model.addChatLine("[INFO] " + txt);
                fireInfo(txt);
            }

            case Protocol.RES_ERROR -> {
                String txt = payload(tokens);
                model.addChatLine("[ERROR] " + txt);
                fireError(txt);
            }
            case Protocol.RES_FILE_LIST -> {
                if (tokens.length < 2) return;
                String room = tokens[1];
                List<String> files = (tokens.length >= 3)
                        ? Protocol.parsePipeList(tokens[2])
                        : List.of();
                String cur = model.getCurrentRoom();
                if (cur != null && cur.equals(room)) {
                    model.setFilesInCurrentRoom(files);
                }

                fireFileList(room, files);
            }

            case Protocol.RES_UPLOAD_OK -> {
                String fn = (tokens.length >= 2) ? tokens[1] : "?";
                model.addChatLine("[INFO] Upload OK: " + fn);
                fireInfo("Upload OK: " + fn);
            }

            case Protocol.RES_UPLOAD_FAILED -> {
                String reason = payload(tokens);
                model.addChatLine("[ERROR] Upload failed: " + reason);
                fireError("Upload failed: " + reason);
            }

            case Protocol.RES_DOWNLOAD_FAILED -> {
                String reason = payload(tokens);
                model.addChatLine("[ERROR] Download failed: " + reason);
                fireError("Download failed: " + reason);
            }

            default -> {
                model.addChatLine("[SERVER] " + line);
                fireInfo(line);
            }
        }
    }

    private void readConsoleInput() throws IOException {
        System.out.println("""
                Befehle:
                  /register <u> <p>
                  /login <u> <p>
                  /create <room>
                  /join <room>
                  /leave
                  /msg <text>
                  /logout
                  /quit
                  /files <room>
                  /download <room> <filename>
                  /upload <room> <path>
                
                """);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (running) {
            String line = br.readLine();
            if (line == null) break;

            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("/")) {
                if (!handleConsoleCommand(line)) break;
            } else {
                sendMessage(line);
            }
        }
    }
    private static String payload(String[] tokens) {
        if (tokens.length < 2) return "";
        if (tokens.length == 2) return tokens[1];
        return tokens[1] + " " + tokens[2];
    }

    private boolean handleConsoleCommand(String line) throws IOException {
        String[] parts = line.split("\\s+", 3);
        String cmd = parts[0];

        switch (cmd) {
            case "/register" -> {
                if (parts.length < 3) { System.out.println("Usage: /register <u> <p>"); return true; }
                register(parts[1], parts[2]);
            }
            case "/login" -> {
                if (parts.length < 3) { System.out.println("Usage: /login <u> <p>"); return true; }
                login(parts[1], parts[2]);
            }
            case "/create" -> {
                if (parts.length < 2) { System.out.println("Usage: /create <room>"); return true; }
                createRoom(parts[1]);
            }
            case "/join" -> {
                if (parts.length < 2) { System.out.println("Usage: /join <room>"); return true; }
                join(parts[1]);
            }
            case "/leave" -> leave();
            case "/msg" -> {
                if (parts.length < 2) { System.out.println("Usage: /msg <text>"); return true; }
                String text = (parts.length == 2) ? parts[1] : parts[1] + " " + parts[2];
                sendMessage(text);
            }
            case "/logout" -> logout();
            case "/quit" -> {
                try { logout(); } catch (IOException ignore) {}
                running = false;
                return false;
            }
            case "/files" -> {
                if (parts.length < 2) { System.out.println("Usage: /files <room>"); return true; }
                listFiles(parts[1]);
            }
            case "/download" -> {
                if (parts.length < 3) { System.out.println("Usage: /download <room> <filename>"); return true; }
                downloadFile(parts[1], parts[2]);
            }
            case "/upload" -> {
                if (parts.length < 3) { System.out.println("Usage: /upload <room> <path>"); return true; }
                uploadFile(parts[1], Path.of(parts[2]));
            }

            default -> System.out.println("Unbekannt: " + cmd);
        }
        return true;
    }

    private synchronized void send(String msg) throws IOException {
        if (out == null) throw new IOException("Not connected");
        out.writeUTF(msg);
        out.flush();
    }

    // Listener helper
    private void fireRoomsUpdated(List<String> rooms) {
        for (ChatClientListener l : listeners) l.onRoomsUpdated(rooms);
    }

    private void fireUsersUpdated(String room, List<String> users) {
        for (ChatClientListener l : listeners) l.onUsersUpdated(room, users);
    }

    private void fireChat(String room, String from, String text) {
        for (ChatClientListener l : listeners) l.onChatMessage(room, from, text);
    }

    private void fireInfo(String text) {
        for (ChatClientListener l : listeners) l.onInfo(text);
    }

    private void fireError(String text) {
        for (ChatClientListener l : listeners) l.onError(text);
    }

    private void fireWarn(String text) {
        for (ChatClientListener l : listeners) l.onWarn(text);
    }

    private void fireBanned(String reason) {
        for (ChatClientListener l : listeners) l.onBanned(reason);
    }

    private void fireConnectionClosed() {
        for (ChatClientListener l : listeners) l.onConnectionClosed();
    }

    private void receiveFile(String filename, long size) throws IOException {
        if (size < 0) throw new IOException("Negative Größe");

        Files.createDirectories(downloadDir);

        Path target = downloadDir.resolve(filename).normalize();
        if (!target.startsWith(downloadDir)) {
            throw new IOException("Ungültiger Dateiname (Path Traversal)");
        }

        // Falls Datei existiert: _1, _2, ...
        if (Files.exists(target)) {
            String base = filename;
            String ext = "";
            int dot = filename.lastIndexOf('.');
            if (dot > 0) {
                base = filename.substring(0, dot);
                ext = filename.substring(dot);
            }
            int i = 1;
            while (Files.exists(target)) {
                target = downloadDir.resolve(base + "_" + i + ext).normalize();
                i++;
            }
        }

        try (OutputStream fos = Files.newOutputStream(
                target,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        )) {
            byte[] buf = new byte[8192];
            long remaining = size;

            while (remaining > 0) {
                int toRead = (int) Math.min(buf.length, remaining);
                int r = in.read(buf, 0, toRead);
                if (r == -1) throw new IOException("EOF während Download");
                fos.write(buf, 0, r);
                remaining -= r;
            }
        }
    }

    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024; // 50 MB

    public void uploadFile(String room, Path file) throws IOException {
        if (file == null || !Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IOException("Datei existiert nicht");
        }
        if (room == null || room.isBlank()) {
            throw new IOException("Kein Raum angegeben");
        }

        String filename = file.getFileName().toString();
        if (!isAllowedUploadFilename(filename)) {
            throw new IOException("Nur PDF- und Bilddateien erlaubt (.pdf, .png, .jpg, .jpeg)");
        }

        long size = Files.size(file);

        if (size < 0 || size > MAX_FILE_BYTES) throw new IOException("Datei zu groß (max 50MB)");
        if (!isValidFilename(filename)) throw new IOException("Ungültiger Dateiname");

        // Wichtig: Header + Bytes dürfen nicht mit anderen send() Calls vermischt werden
        synchronized (this) {
            out.writeUTF(Protocol.buildUpload(room, filename, size));
            out.flush();

            try (InputStream fis = Files.newInputStream(file)) {
                byte[] buf = new byte[8192];
                long remaining = size;
                while (remaining > 0) {
                    int toRead = (int) Math.min(buf.length, remaining);
                    int r = fis.read(buf, 0, toRead);
                    if (r == -1) throw new IOException("EOF beim Lesen der Datei");
                    out.write(buf, 0, r);
                    remaining -= r;
                }
            }
            out.flush();
        }
    }

    public void listFiles(String room) throws IOException {
        send(Protocol.buildFiles(room));
    }

    public void downloadFile(String room, String filename) throws IOException {
        send(Protocol.buildDownload(room, filename));
    }

    private void fireFileList(String room, List<String> files) {
        for (ChatClientListener l : listeners) l.onFileList(room, files);
    }
    private void   fireFilesUpdated(String room, List<String> files) {
        for (ChatClientListener l : listeners) l.onFilesUpdated(room, files);
    }



    private boolean isValidFilename(String s) {
        if (s == null) return false;
        if (s.isBlank()) return false;
        if (s.contains("..")) return false;
        if (s.contains("/") || s.contains("\\")) return false;
        if (s.contains("|") || s.contains(";")) return false;
        if (s.contains(" ")) return false;
        return true;
    }

    private boolean isAllowedUploadFilename(String filename) {
        if (filename == null) return false;
        String f = filename.toLowerCase();
        return f.endsWith(".pdf") || f.endsWith(".png") || f.endsWith(".jpg")
                || f.endsWith(".jpeg");
    }


}


