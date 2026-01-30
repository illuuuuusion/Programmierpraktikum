package de.uni_jena.fpp.chatroom;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;



public class ClientHandler extends Thread {

    private final ChatServer server;
    private final Socket socket;
    private final int clientId;

    private DataInputStream in;
    private DataOutputStream out;

    private User user;
    private String displayName;
    private String currentRoom;

    private volatile boolean running = true;

    public ClientHandler(ChatServer server, Socket socket, int clientId) {
        super("ClientHandler-" + clientId);
        this.server = server;
        this.socket = socket;
        this.clientId = clientId;
        this.displayName = "client-" + clientId;
    }

    @Override
    public void run() {
        System.out.println("[SERVER] " + getName() + " gestartet: " + socket.getRemoteSocketAddress());

        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            while (running && !socket.isClosed()) {
                String line;
                try {
                    line = in.readUTF();
                } catch (EOFException eof) {
                    break;
                }

                if (line == null) break;
                handleCommand(line);
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("[SERVER] IO-Fehler " + getName() + ": " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private void handleCommand(String line) throws IOException {
        String[] tokens = Protocol.splitTokens(line);
        if (tokens.length == 0) {
            send(Protocol.RES_ERROR + " Leere Nachricht");
            return;
        }

        String cmd = tokens[0];

        switch (cmd) {
            case Protocol.CMD_REGISTER -> handleRegister(tokens);
            case Protocol.CMD_LOGIN -> handleLogin(tokens);

            case Protocol.CMD_CREATE_ROOM -> handleCreateRoom(tokens);
            case Protocol.CMD_JOIN -> handleJoin(tokens);
            case Protocol.CMD_LEAVE -> handleLeave();

            // MS3: Dateien
            case Protocol.CMD_UPLOAD -> handleUpload(line);
            case Protocol.CMD_FILES -> handleFiles(tokens);
            case Protocol.CMD_DOWNLOAD -> handleDownload(tokens);


            case Protocol.CMD_MSG -> handleMsg(line);
            case Protocol.CMD_LOGOUT -> handleLogout();

            default -> send(Protocol.RES_ERROR + " Unbekanntes Kommando: " + cmd);
        }
    }

    private void handleRegister(String[] tokens) throws IOException {
        if (tokens.length < 3) {
            send(Protocol.RES_ERROR + " Usage: REGISTER <username> <password>");
            return;
        }
        boolean ok = server.registerUser(tokens[1], tokens[2]);
        send(ok ? Protocol.RES_REGISTER_OK : (Protocol.RES_REGISTER_FAILED + " USERNAME_TAKEN"));
        server.logInfo("REGISTER user=" + tokens[1] + " ok=" + ok);

    }

    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024; // 50 MB

    private void handleUpload(String line) throws IOException {
        if (!requireLogin()) return;

        // UPLOAD <room> <filename> <size>
        String[] t = line.trim().split("\\s+", 4);
        if (t.length < 4) {
            send(Protocol.RES_ERROR + " Usage: UPLOAD <room> <filename> <size>");
            return;
        }

        String room = t[1];
        String filename = t[2];

        long size;
        try {
            size = Long.parseLong(t[3]);
        } catch (NumberFormatException e) {
            send(Protocol.buildUploadFailed("INVALID_SIZE"));
            closeNow(); // Protokoll kaputt -> lieber trennen
            return;
        }

        // harte Grenze
        if (size < 0 || size > MAX_FILE_BYTES) {
            send(Protocol.buildUploadFailed("SIZE_LIMIT"));
            closeNow(); // sonst müsste man ggf. endlos Bytes weglesen
            return;
        }

        if (currentRoom == null || !room.equals(currentRoom)) {
            send(Protocol.buildUploadFailed("NOT_IN_ROOM"));
            // Bytes weglesen (safe, weil size <= MAX_FILE_BYTES)
            discardBytes(size);
            return;
        }

        if (!server.roomExists(room)) {
            send(Protocol.buildUploadFailed("ROOM_NOT_FOUND"));
            discardBytes(size);
            return;
        }

        if (!isValidFilename(filename)) {
            send(Protocol.buildUploadFailed("INVALID_FILENAME"));
            discardBytes(size);
            return;
        }

        boolean ok = server.saveFileToRoom(room, filename, in, size);

        if (ok) {
            send(Protocol.buildUploadOk(filename));
            server.logInfo("UPLOAD user=" + displayName + " room=" + room + " file=" + filename + " size=" + size + " ok=true");
        } else {
            send(Protocol.buildUploadFailed("SAVE_FAILED"));
            server.logWarn("UPLOAD_FAIL user=" + displayName + " room=" + room + " file=" + filename + " size=" + size);
            closeNow();
        }
    }

    private void handleFiles(String[] tokens) throws IOException {
        if (!requireLogin()) return;

        // FILES <room>
        if (tokens.length < 2) {
            send(Protocol.RES_ERROR + " Usage: FILES <room>");
            return;
        }

        String room = tokens[1];

        if (currentRoom == null || !room.equals(currentRoom)) {
            send(Protocol.RES_ERROR + " Du bist nicht in diesem Raum.");
            return;
        }

        if (!server.roomExists(room)) {
            send(Protocol.RES_ERROR + " Raum existiert nicht.");
            return;
        }

        List<String> files = server.listFilesInRoom(room);
        send(Protocol.buildFileList(room, files));
    }

    private void handleDownload(String[] tokens) throws IOException {
        if (!requireLogin()) return;

        // DOWNLOAD <room> <filename>
        if (tokens.length < 3) {
            send(Protocol.RES_ERROR + " Usage: DOWNLOAD <room> <filename>");
            return;
        }

        String room = tokens[1];
        String filename = tokens[2];

        if (currentRoom == null || !room.equals(currentRoom)) {
            send(Protocol.buildDownloadFailed("NOT_IN_ROOM"));
            return;
        }

        if (!server.roomExists(room)) {
            send(Protocol.buildDownloadFailed("ROOM_NOT_FOUND"));
            return;
        }

        if (!isValidFilename(filename)) {
            send(Protocol.buildDownloadFailed("INVALID_FILENAME"));
            return;
        }

        Path file = server.getRoomFilePath(room, filename);
        if (file == null || !Files.exists(file) || !Files.isRegularFile(file)) {
            send(Protocol.buildDownloadFailed("NOT_FOUND"));
            return;
        }

        long size = Files.size(file);
        if (size < 0 || size > MAX_FILE_BYTES) {
            send(Protocol.buildDownloadFailed("SIZE_LIMIT"));
            return;
        }

        sendFileBytes(file, filename, size);
        server.logInfo("DOWNLOAD user=" + displayName + " room=" + room + " file=" + filename + " size=" + size);
    }

    private void sendFileBytes(Path file, String filename, long size) throws IOException {
        // Wichtig: Header + Bytes müssen ohne Interleaving rausgehen.
        synchronized (this) {
            out.writeUTF(Protocol.buildFileHeader(filename, size));
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

    private void discardBytes(long size) throws IOException {
        if (size <= 0) return;
        long remaining = size;
        byte[] buf = new byte[8192];

        while (remaining > 0) {
            int toRead = (int) Math.min(buf.length, remaining);
            int r = in.read(buf, 0, toRead);
            if (r == -1) throw new EOFException("EOF while discarding upload bytes");
            remaining -= r;
        }
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



    private void handleLogin(String[] tokens) throws IOException {
        if (tokens.length < 3) {
            send(Protocol.RES_ERROR + " Usage: LOGIN <username> <password>");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        if (this.user != null) {
            send(Protocol.RES_LOGIN_FAILED + " ALREADY_LOGGED_IN");
            server.logInfo("LOGIN_FAIL user=" + username);
            return;
        }

        if (server.isUserLoggedIn(username)) {
            send(Protocol.RES_LOGIN_FAILED + " ALREADY_LOGGED_IN");
            server.logInfo("LOGIN_FAIL user=" + username);
            return;
        }

        User u = server.authenticateUser(username, password);
        if (u == null) {
            send(Protocol.RES_LOGIN_FAILED + " INVALID_CREDENTIALS");
            server.logInfo("LOGIN_FAIL user=" + username);
            return;
        }

        if (u.isBanned()) {
            send(Protocol.buildBanned("Du bist dauerhaft gebannt."));
            server.logWarn("LOGIN_BANNED user=" + username);
            running = false;
            closeNow();
            return;
        }

        this.user = u;
        this.displayName = u.getUsername();
        server.addLoggedInClient(u, this);

        send(Protocol.RES_LOGIN_OK);
        server.logInfo("LOGIN_OK user=" + username);
        server.sendRoomListTo(this);

        boolean joined = server.joinRoom(ChatServer.DEFAULT_ROOM, this);
        if (joined) {
            send(Protocol.RES_INFO + " Joined " + ChatServer.DEFAULT_ROOM);
        } else {
            send(Protocol.RES_ERROR + " Konnte Lobby nicht betreten.");
        }
    }

    private void handleCreateRoom(String[] tokens) throws IOException {
        if (!requireLogin()) return;

        if (tokens.length < 2) {
            send(Protocol.RES_ERROR + " Usage: CREATE_ROOM <room>");
            return;
        }

        boolean ok = server.createRoom(tokens[1]);
        if (ok) {
            send(Protocol.RES_INFO + " Raum erstellt: " + tokens[1]);
            server.logInfo("CREATE_ROOM by=" + displayName + " room=" + tokens[1] + " ok=" + ok);
        }

        else send(Protocol.RES_ERROR + " Raum konnte nicht erstellt werden (Name ungültig oder existiert).");
    }

    private void handleJoin(String[] tokens) throws IOException {
        if (!requireLogin()) return;

        if (tokens.length < 2) {
            send(Protocol.RES_ERROR + " Usage: JOIN <room>");
            return;
        }

        String room = tokens[1];
        if (room.equals(currentRoom)) {
            send(Protocol.RES_INFO + " Du bist schon in diesem Raum.");
            return;
        }
        boolean ok = server.joinRoom(room, this);
        if (ok) {
            send(Protocol.RES_INFO + " Joined " + room);
            server.logInfo("JOIN user=" + displayName + " room=" + room + " ok=" + ok);
        }
        else send(Protocol.RES_ERROR + " Raum existiert nicht oder Name ungültig.");
    }

    private void handleLeave() throws IOException {
        if (!requireLogin()) return;

        // Immer zurück in Lobby
        boolean ok = server.joinRoom(ChatServer.DEFAULT_ROOM, this);
        if (ok) {
            send(Protocol.RES_INFO + " Joined " + ChatServer.DEFAULT_ROOM);
        } else {
            send(Protocol.RES_ERROR + " Konnte Lobby nicht betreten.");
        }
    }


    private void handleMsg(String line) throws IOException {
        if (!requireLogin()) return;

        if (currentRoom == null) {
            send(Protocol.RES_ERROR + " Du bist in keinem Raum. Erst JOIN <room> ausführen.");
            return;
        }

        String text = extractTextAfterCommand(line, Protocol.CMD_MSG);
        server.broadcastChatToRoom(currentRoom, displayName, text);
        server.logInfo("MSG room=" + currentRoom + " from=" + displayName + " len=" + text.length());
    }

    private void handleLogout() throws IOException {
        send(Protocol.RES_INFO + " Bye.");
        server.logInfo("LOGOUT user=" + displayName);
        running = false;
    }

    private boolean requireLogin() throws IOException {
        if (this.user == null) {
            send(Protocol.RES_ERROR + " Bitte zuerst LOGIN ausführen.");
            return false;
        }
        return true;
    }

    private String extractTextAfterCommand(String line, String cmd) {
        if (line.length() <= cmd.length()) return "";
        return line.substring(cmd.length()).trim();
    }

    public synchronized void send(String message) throws IOException {
        if (out == null) return;
        out.writeUTF(message);
        out.flush();
    }

    public void closeNow() {
        running = false;
        try { socket.close(); } catch (IOException ignore) {}
    }

    private void cleanup() {
        running = false;
        server.removeClient(this);
        try { socket.close(); } catch (IOException ignore) {}
        System.out.println("[SERVER] " + getName() + " beendet.");
    }

    public User getUser() { return user; }

    public String getDisplayName() { return displayName; }

    public String getCurrentRoom() { return currentRoom; }

    public void setCurrentRoom(String currentRoom) { this.currentRoom = currentRoom; }
}
