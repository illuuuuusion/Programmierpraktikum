package de.uni_jena.fpp.chatroom;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

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
