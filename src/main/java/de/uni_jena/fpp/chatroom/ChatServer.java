package de.uni_jena.fpp.chatroom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {

    public static final String DEFAULT_ROOM = "Lobby";

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
        if (DEFAULT_ROOM.equals(roomName)) return false; // Lobby nie löschen

        Room room = rooms.remove(roomName);
        if (room == null) return false;

        // Members kopieren
        var members = new java.util.ArrayList<>(room.getMembers());

        for (ClientHandler ch : members) {
            try { ch.send(Protocol.RES_INFO + " Raum wurde vom Server gelöscht: " + roomName); } catch (Exception ignore) {}

            // sauber aus altem room-member-set entfernen
            room.removeMember(ch);

            ch.setCurrentRoom(null);

            // in Lobby
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


}
