package de.uni_jena.fpp.chatroom;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient {

    private final String host;
    private final int port;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private volatile boolean running = true;

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

    // Connection API (für GUI)
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

    // Actions (für UI)
    public void register(String username, String password) throws IOException {
        send(Protocol.buildRegister(username, password));
    }

    public void login(String username, String password) throws IOException {
        send(Protocol.buildLogin(username, password));
    }

    public void createRoom(String name) throws IOException {
        send(Protocol.buildCreateRoom(name));
    }

    public void join(String room) throws IOException {
        // damit UI sofort currentRoom kennt
        model.setCurrentRoom(room);
        send(Protocol.buildJoin(room));
    }

    public void leave() throws IOException {
        model.setCurrentRoom(null);
        send(Protocol.buildLeave());
    }

    public void sendMessage(String text) throws IOException {
        send(Protocol.buildMsg(text));
    }

    public void logout() throws IOException {
        send(Protocol.buildLogout());
    }

    // Debug-Konsole bleibt (start())
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
        // CHAT <room> <from> <text...> (4 tokens) :contentReference[oaicite:2]{index=2}
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

                // Aktualisiere nur die Userliste des aktuellen Raums
                String cur = model.getCurrentRoom();
                if (cur != null && cur.equals(room)) {
                    model.setUsersInCurrentRoom(users);
                }
                fireUsersUpdated(room, users);
            }

            case Protocol.RES_WARN -> {
                String txt = (tokens.length >= 2) ? tokens[1] : "";
                model.addChatLine("[WARN] " + txt);
                fireWarn(txt);
            }

            case Protocol.RES_BANNED -> {
                String reason = (tokens.length >= 2) ? tokens[1] : "";
                model.addChatLine("[BANNED] " + reason);
                fireBanned(reason);
                disconnect();
            }

            case Protocol.RES_INFO -> {
                String txt = (tokens.length >= 2) ? tokens[1] : "";
                model.addChatLine("[INFO] " + txt);
                fireInfo(txt);
            }

            case Protocol.RES_ERROR -> {
                String txt = (tokens.length >= 2) ? tokens[1] : "";
                model.addChatLine("[ERROR] " + txt);
                fireError(txt);
            }

            default -> {
                // Für Debug: unbekannte Servermessage anzeigen
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
}
