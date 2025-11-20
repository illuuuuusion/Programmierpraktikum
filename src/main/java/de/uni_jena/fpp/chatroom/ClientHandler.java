package de.uni_jena.fpp.chatroom;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {

    private final ChatServer server;
    private final Socket socket;
    private final int clientId;

    private DataInputStream in;
    private DataOutputStream out;

    // Benutzer-Infos
    private User user;              // null, solange nicht eingeloggt
    private String displayName;     // standardmäßig "client-<id>", nach Login = username

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
        System.out.println("[SERVER] " + getName() + " gestartet für " + socket.getRemoteSocketAddress());
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            while (running && !socket.isClosed()) {
                String line;

                try {
                    line = in.readUTF(); // blockiert bis Nachricht da ist
                } catch (EOFException eof) {
                    System.out.println("[SERVER] " + getName() + " EOF – Client hat Verbindung beendet.");
                    break;
                }

                if (line == null) {
                    System.out.println("[SERVER] " + getName() + " null gelesen – beende.");
                    break;
                }

                System.out.println("[SERVER] (" + getName() + ") Empfangen: " + line);
                handleCommand(line);
            }

        } catch (IOException e) {
            System.err.println("[SERVER] IO-Fehler in " + getName() + ": " + e.getMessage());
        } finally {
            cleanup();
            System.out.println("[SERVER] " + getName() + " beendet.");
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

            case Protocol.CMD_WHO -> handleWho();

            case Protocol.CMD_MSG -> handleMsg(line, cmd);

            case Protocol.CMD_LOGOUT -> handleLogout();

            default -> send(Protocol.RES_ERROR + " Unbekanntes Kommando: " + cmd);
        }
    }

    /* ==================== Einzelne Command-Handler ==================== */

    private void handleRegister(String[] tokens) throws IOException {
        if (tokens.length < 3) {
            send(Protocol.RES_ERROR + " Usage: REGISTER <username> <password>");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        boolean ok = server.registerUser(username, password);
        if (ok) {
            send(Protocol.RES_REGISTER_OK);
            System.out.println("[SERVER] User registriert: " + username);
        } else {
            send(Protocol.RES_REGISTER_FAILED + " USERNAME_TAKEN");
            System.out.println("[SERVER] Registrierung fehlgeschlagen, Username vergeben: " + username);
        }
    }

    private void handleLogin(String[] tokens) throws IOException {
        if (tokens.length < 3) {
            send(Protocol.RES_ERROR + " Usage: LOGIN <username> <password>");
            return;
        }
        if (this.user != null) {
            send(Protocol.RES_LOGIN_FAILED + " ALREADY_LOGGED_IN");
            return;
        }

        String username = tokens[1];
        String password = tokens[2];

        if (server.isUserLoggedIn(username)) {
            send(Protocol.RES_LOGIN_FAILED + " ALREADY_LOGGED_IN");
            return;
        }

        User u = server.authenticateUser(username, password);
        if (u == null) {
            send(Protocol.RES_LOGIN_FAILED + " INVALID_CREDENTIALS");
            return;
        }

        // Erfolg: Benutzer im Handler und im Server registrieren
        this.user = u;
        this.displayName = u.getUsername();
        server.addLoggedInClient(u, this);

        send(Protocol.RES_LOGIN_OK);
        System.out.println("[SERVER] User eingeloggt: " + username + " (Client " + clientId + ")");

        // aktuelle Userliste an diesen Client schicken
        List<String> users = server.getLoggedInUsernames();
        send(Protocol.buildUserList(users));

        // Willkommensnachricht nur an diesen Client
        send(Protocol.RES_INFO + " Willkommen, " + username + "!");

        // andere Clients informieren
        server.broadcastUserJoined(username, this);
    }

    private void handleWho() throws IOException {
        List<String> users = server.getLoggedInUsernames();
        send(Protocol.buildUserList(users));
    }

    private void handleMsg(String line, String cmd) throws IOException {
        if (this.user == null) {
            // Benutzer nicht eingeloggt → Fehler
            send(Protocol.RES_ERROR + " Bitte zuerst LOGIN ausführen.");
            return;
        }
        String text = extractTextAfterCommand(line, cmd);
        System.out.println("[SERVER] MSG von " + displayName + ": " + text);

        // an alle eingeloggten User verteilen
        server.broadcastChat(displayName, text);
    }

    private void handleLogout() throws IOException {
        System.out.println("[SERVER] LOGOUT von " + displayName);
        send(Protocol.RES_INFO + " Bye.");
        running = false; // run()-Schleife endet -> cleanup() wird aufgerufen
    }

    /* ==================== Hilfsmethoden ==================== */

    private String extractTextAfterCommand(String line, String cmd) {
        if (line.length() <= cmd.length()) {
            return "";
        }
        return line.substring(cmd.length()).trim();
    }

    public void send(String message) throws IOException {
        if (out == null) {
            return;
        }
        out.writeUTF(message);
        out.flush();
    }

    private void cleanup() {
        running = false;
        server.removeClient(this);
        try {
            socket.close();
        } catch (IOException ignore) {
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public int getClientId() {
        return clientId;
    }

    public User getUser() {
        return user;
    }
}
