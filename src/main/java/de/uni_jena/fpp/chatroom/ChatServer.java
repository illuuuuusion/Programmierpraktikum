package de.uni_jena.fpp.chatroom;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.OutputStream;
import java.util.stream.Stream;
import java.io.DataInputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;



public class ChatServer {

    public static final String DEFAULT_ROOM = "Lobby";

    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024; // 50 MB
    private final Path roomsBaseDir = Path.of("data", "rooms");

    private final int port;
    private final UserRepository userRepo;

    private volatile boolean running;
    private ServerSocket serverSocket;

    private final List<ClientHandler> connections = new CopyOnWriteArrayList<>();
    private final Map<String, ClientHandler> loggedInClients = new ConcurrentHashMap<>();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    private int nextClientId = 1;
    private final ServerLogger logger;




    public ChatServer(int port, UserRepository userRepo, ServerLogger logger) {
        this.port = port;
        this.userRepo = userRepo;
        this.logger = logger;
        rooms.putIfAbsent(DEFAULT_ROOM, new Room(DEFAULT_ROOM, true));
        try {
            Files.createDirectories(roomsBaseDir);
        } catch (IOException e) {
            logger.error("Konnte roomsBaseDir nicht anlegen: " + e.getMessage());
        }

    }

    public void start() {
        logger.info("Server startet (Multi-Client) auf Port " + port);
        System.out.println("[SERVER] Starte ChatServer (Multi-Client) auf Port " + port);

        running = true;

        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;

            logger.info("Warte auf eingehende Clients ...");
            System.out.println("[SERVER] Warte auf eingehende Clients ...");

            while (running) {
                Socket clientSocket = ss.accept();
                int id = nextClientId++;

                logger.info("CONNECT id=" + id + " addr=" + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(this, clientSocket, id);
                connections.add(handler);
                handler.start();

                System.out.println("[SERVER] Verbindung angenommen (" + id + "): " + clientSocket.getRemoteSocketAddress());
            }

        } catch (IOException e) {
            if (running) {
                logger.error("ServerSocket Fehler: " + e.getMessage());
                System.err.println("[SERVER] ServerSocket Fehler: " + e.getMessage());
                e.printStackTrace();
            } else {
                // passiert häufig beim Stop(), wenn serverSocket.close() accept() unterbricht
                logger.info("ServerSocket geschlossen (stop).");
            }
        } finally {
            running = false;
            logger.info("Server beendet.");
            System.out.println("[SERVER] Server beendet.");

            // Logger schließen (Datei-Handle freigeben)

        }
    }
    public java.util.Map<String, String> getOnlineUserRooms() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (var e : loggedInClients.entrySet()) {
            String room = e.getValue().getCurrentRoom();
            map.put(e.getKey(), room == null ? "-" : room);
        }
        return map;
    }
    public boolean isRunning() {
        return running;
    }


    public void stop() {
        // Mehrfaches Stop ist ok
        if (!running) {
            logger.info("Stop requested (already stopped)");
        }

        running = false;
        logger.info("Stop requested");

        // 1) accept() beenden
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Fehler beim Schließen des ServerSocket: " + e.getMessage());
        }

        // 2) ALLE Clients trennen, damit nichts mehr "weiterläuft"
        for (ClientHandler ch : new ArrayList<>(connections)) {
            try {
                ch.send(Protocol.RES_INFO + " Server wird beendet.");
            } catch (Exception ignore) {}
            ch.closeNow();
        }

        // 3) Server-State zurücksetzen (optional, aber praktisch)
        connections.clear();
        loggedInClients.clear();

        rooms.clear();
        rooms.putIfAbsent(DEFAULT_ROOM, new Room(DEFAULT_ROOM, true));

        logger.info("Stop komplett (clients geschlossen)");
    }


    // ===== Schritt 2.3: Persistente Userverwaltung =====

    public boolean registerUser(String username, String password) {
        if (!isValidSimpleName(username) || password == null) return false;
        char[] pw = password.toCharArray();
        try {
            return userRepo.createUser(username, pw);
        } finally {
            java.util.Arrays.fill(pw, '\0');
        }
    }

    /**
     * @return User record on success, otherwise null.
     *         IMPORTANT: may return a banned user (user.isBanned()==true) if password matches.
     */
    public User authenticateUser(String username, String password) {
        if (username == null || password == null) return null;
        char[] pw = password.toCharArray();
        try {
            return userRepo.verifyLogin(username, pw);
        } finally {
            java.util.Arrays.fill(pw, '\0');
        }
    }

    public List<User> listAllUsers() {
        return userRepo.listUsers();
    }

    public boolean setUserBanned(String username, boolean banned) {
        return userRepo.setBanned(username, banned);
    }

    public boolean isUserLoggedIn(String username) {
        return loggedInClients.containsKey(username);
    }
// ===== Schritt 2.4: Admin-Funktionen =====

    /**
     * Sends a warning to an ONLINE user.
     * @return true if user was online and WARN was sent (best effort).
     */
    public boolean warnUser(String username, String text) {
        if (username == null || username.isBlank()) return false;
        logger.warn("WARN user=" + username + " text=" + text);

        ClientHandler ch = loggedInClients.get(username);
        if (ch == null) return false;

        String msg = Protocol.buildWarn(text == null ? "" : text);

        try {
            ch.send(msg);
            return true;
        } catch (IOException e) {
            System.err.println("[SERVER] WARN an " + username + " fehlgeschlagen: " + e.getMessage());
            ch.closeNow(); // best effort cleanup
            return false;
        }
    }

    /**
     * Permanently bans a user (persistent) and kicks them if online.
     * @return true if the user existed in the repository and was persisted as banned.
     */
    public boolean banUser(String username, String reason) {
        if (username == null || username.isBlank()) return false;
        logger.warn("BAN user=" + username + " reason=" + reason);

        boolean persisted = userRepo.setBanned(username, true);

        ClientHandler ch = loggedInClients.get(username);
        if (ch != null) {
            try {
                ch.send(Protocol.buildBanned(reason == null ? "" : reason));
            } catch (IOException ignore) {
                // ignore, we kick anyway
            }
            // optional: sofort aus online-Liste nehmen (cleanup macht's nochmal)
            loggedInClients.remove(username);
            ch.closeNow();
        }

        return persisted;
    }

    // für UI: Entbannen
    public boolean unbanUser(String username) {
        if (username == null || username.isBlank()) return false;
        return userRepo.setBanned(username, false);
    }

    // für UI: Online-Usernamen
    public List<String> getOnlineUsernames() {
        List<String> list = new ArrayList<>(loggedInClients.keySet());
        list.sort(String::compareToIgnoreCase);
        return list;
    }

    public void addLoggedInClient(User user, ClientHandler handler) {
        loggedInClients.put(user.getUsername(), handler);
    }

    public void removeClient(ClientHandler handler) {
        connections.remove(handler);
        String uname = (handler.getUser() != null) ? handler.getUser().getUsername() : ("client-" + handler.getName());
        logger.info("DISCONNECT " + uname + " (active=" + connections.size() + ")");

        if (handler.getUser() != null) {
            forceLeaveRoom(handler);
            loggedInClients.remove(handler.getUser().getUsername());
        }

        System.out.println("[SERVER] Verbindung entfernt. Aktive Verbindungen: " + connections.size());
    }

    // Rooms

    public List<String> getRoomNames() {
        List<String> list = new ArrayList<>(rooms.keySet());
        list.sort(String::compareToIgnoreCase);
        return list;
    }

    public boolean createRoom(String roomName) {
        if (!isValidRoomName(roomName)) return false;


        Room existing = rooms.putIfAbsent(roomName, new Room(roomName));
        if (existing == null) {
            broadcastRoomListToAll();
            logger.info("ROOM_CREATE " + roomName);
            return true;

        }
        return false;
    }

    public boolean joinRoom(String roomName, ClientHandler handler) {
        if (!isValidRoomName(roomName)) return false;

        if (roomName.equals(handler.getCurrentRoom())) {
            return true;
        }

        leaveRoom(handler);

        Room target = rooms.get(roomName);
        if (target == null) return false;

        handler.setCurrentRoom(roomName);
        target.addMember(handler);

        broadcastRoomUsers(roomName);
        return true;
    }

    public void leaveRoom(ClientHandler handler) {
        String old = handler.getCurrentRoom();
        if (old == null) return;

        Room room = rooms.get(old);
        handler.setCurrentRoom(null);

        if (room != null) {
            room.removeMember(handler);

            broadcastRoomUsers(old);

            if (room.isEmpty() && !room.isPersistent() && !DEFAULT_ROOM.equals(old)) {
                rooms.remove(old);
                deleteRoomStorage(old);
                broadcastRoomListToAll();
                logger.info("ROOM_DELETE " + old);
            }

        }
    }

    private void forceLeaveRoom(ClientHandler handler) {
        leaveRoom(handler);
    }

    // Push Updates

    public void sendRoomListTo(ClientHandler ch) {
        try {
            ch.send(Protocol.buildRoomList(getRoomNames()));
        } catch (IOException e) {
            System.err.println("[SERVER] ROOM_LIST an " + ch.getName() + " fehlgeschlagen: " + e.getMessage());
        }
    }

    public void broadcastRoomListToAll() {
        String msg = Protocol.buildRoomList(getRoomNames());
        for (ClientHandler ch : loggedInClients.values()) {
            try {
                ch.send(msg);
            } catch (IOException e) {
                System.err.println("[SERVER] ROOM_LIST broadcast fehlgeschlagen: " + e.getMessage());
            }
        }
    }

    public void broadcastRoomUsers(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) return;

        String msg = Protocol.buildRoomUsers(roomName, room.getMemberNames());
        for (ClientHandler member : room.getMembers()) {
            try {
                member.send(msg);
            } catch (IOException e) {
                System.err.println("[SERVER] ROOM_USERS an " + member.getName() + " fehlgeschlagen: " + e.getMessage());
            }
        }
    }

    // Chat in Room

    public void broadcastChatToRoom(String roomName, String from, String text) {
        Room room = rooms.get(roomName);
        if (room == null) return;

        String msg = Protocol.buildChat(roomName, from, text);
        for (ClientHandler member : room.getMembers()) {
            try {
                member.send(msg);
            } catch (IOException e) {
                System.err.println("[SERVER] CHAT an " + member.getName() + " fehlgeschlagen: " + e.getMessage());
            }
        }
    }

    public boolean createRoomAsServer(String roomName) {
        if (!isValidRoomName(roomName)) return false;

        Room existing = rooms.putIfAbsent(roomName, new Room(roomName, true));
        if (existing == null) {
            broadcastRoomListToAll();
            logger.info("ROOM_CREATE_ADMIN " + roomName);
            return true;
        }
        return false;
    }

    public boolean deleteRoomAsServer(String roomName) {
        if (roomName == null || roomName.isBlank()) return false;
        if (DEFAULT_ROOM.equals(roomName)) return false;

        Room room = rooms.remove(roomName);
        if (room == null) return false;          // <- erst prüfen
        deleteRoomStorage(roomName);             // <- dann löschen

        var members = new java.util.ArrayList<>(room.getMembers());

        for (ClientHandler ch : members) {
            try { ch.send(Protocol.RES_INFO + " Raum wurde vom Server gelöscht: " + roomName); } catch (Exception ignore) {}
            room.removeMember(ch);
            ch.setCurrentRoom(null);
            joinRoom(DEFAULT_ROOM, ch);
        }

        broadcastRoomListToAll();
        broadcastRoomUsers(DEFAULT_ROOM);
        logger.info("ROOM_DELETE_ADMIN " + roomName);
        return true;
    }




    // Validation Helpers

    private boolean isValidRoomName(String s) {
        if (!isValidSimpleName(s)) return false;
        return !s.contains("|");
    }

    private boolean isValidSimpleName(String s) {
        if (s == null) return false;
        if (s.isBlank()) return false;
        return !s.contains(" ");
    }

    public Map<String, Room> getRoomsUnsafe() {
        return Collections.unmodifiableMap(rooms);
    }

    public void addLogListener(ServerLogListener l) { logger.addListener(l); }
    public List<String> getLogHistory() { return logger.getHistorySnapshot(); }
    public void logInfo(String msg) { logger.info(msg); }
    public void logWarn(String msg) { logger.warn(msg); }
    public void logError(String msg) { logger.error(msg); }

// ===== MS3: Dateien =====

    public boolean roomExists(String room) {
        if (room == null) return false;
        return rooms.containsKey(room);
    }

    private Path roomDir(String room) {
        // data/rooms/<room>
        return roomsBaseDir.resolve(room).normalize();
    }

    public Path getRoomFilePath(String room, String filename) {
        if (room == null || filename == null) return null;

        Path dir = roomDir(room);
        Path file = dir.resolve(filename).normalize();

        // Safety: kein "../" aus dem base raus
        if (!file.startsWith(dir)) return null;

        return file;
    }

    public List<String> listFilesInRoom(String room) {
        if (!roomExists(room)) return List.of();

        Path dir = roomDir(room);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return List.of();

        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) {
                    String name = p.getFileName().toString();
                    // optional: tmp ausblenden
                    if (name.endsWith(".tmp")) continue;
                    out.add(name);
                }
            }
        } catch (IOException e) {
            logger.warn("FILES_LIST_FAIL room=" + room + " err=" + e.getMessage());
        }

        out.sort(String::compareToIgnoreCase);
        return out;
    }

    /**
     * Speichert genau <size> Bytes aus dem Client-Stream nach data/rooms/<room>/<filename>.
     * Liest EXAKT size Bytes (sonst wäre der Stream danach kaputt).
     */
    public boolean saveFileToRoom(String room, String filename, DataInputStream in, long size) {
        if (!roomExists(room)) return false;
        if (in == null) return false;
        if (size < 0 || size > MAX_FILE_BYTES) return false;
        if (!isValidFilename(filename)) return false;

        try {
            Files.createDirectories(roomsBaseDir);

            Path dir = roomDir(room);
            Files.createDirectories(dir);

            Path target = getRoomFilePath(room, filename);
            if (target == null) return false;

            // temp schreiben und dann atomar ersetzen
            Path tmp = dir.resolve(filename + ".tmp").normalize();
            if (!tmp.startsWith(dir)) return false;

            try (OutputStream fos = Files.newOutputStream(
                    tmp,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                byte[] buf = new byte[8192];
                long remaining = size;

                while (remaining > 0) {
                    int toRead = (int) Math.min(buf.length, remaining);
                    int r = in.read(buf, 0, toRead);
                    if (r == -1) throw new EOFException("EOF während Upload");
                    fos.write(buf, 0, r);
                    remaining -= r;
                }
            }

            Files.move(tmp, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            logger.info("FILE_SAVED room=" + room + " file=" + filename + " size=" + size);
            return true;

        } catch (IOException e) {
            logger.warn("FILE_SAVE_FAIL room=" + room + " file=" + filename + " err=" + e.getMessage());
            return false;
        }
    }


    private boolean isValidFilename(String s) {
        if (s == null) return false;
        if (s.isBlank()) return false;
        if (s.contains("..")) return false;
        if (s.contains("/") || s.contains("\\") ) return false;
        if (s.contains("|") || s.contains(";")) return false;
        if (s.contains(" ")) return false;
        return true;
    }

    // Optional: Room-Folder beim Room-Delete entfernen
    private void deleteRoomStorage(String roomName) {
        if (!isValidRoomName(roomName)) return;
        if (DEFAULT_ROOM.equals(roomName)) return;

        Path dir = roomsBaseDir.resolve(roomName).normalize();
        if (!dir.startsWith(roomsBaseDir)) return;
        if (!Files.exists(dir)) return;

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> Integer.compare(b.getNameCount(), a.getNameCount()))// erst Kinder, dann Eltern
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignore) {}
                    });
        } catch (IOException e) {
            logger.error("DELETE_ROOM_STORAGE_FAILED room=" + roomName + " err=" + e.getMessage());
        }
    }

}
