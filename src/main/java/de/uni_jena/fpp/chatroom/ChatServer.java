package de.uni_jena.fpp.chatroom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {

    private final int port;
    private volatile boolean running;

    private ServerSocket serverSocket;

    // Thread-sichere Liste aller verbundenen ClientHandler (Verbindungen)
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // Benutzerverwaltung
    // registrierte User (überleben nur solange der Server läuft)
    private final Map<String, User> registeredUsers = new ConcurrentHashMap<>();
    // aktuell eingeloggte User -> zugehöriger ClientHandler
    private final Map<String, ClientHandler> loggedInClients = new ConcurrentHashMap<>();

    private int nextClientId = 1;

    public ChatServer(int port) {
        this.port = port;
    }

    /**
     * Multi-Client-Server:
     * - Öffnet einen ServerSocket
     * - Nimmt in einer Schleife neue Verbindungen an
     * - Startet für jeden Client einen ClientHandler-Thread
     */
    public void start() {
        System.out.println("[SERVER] Starte ChatServer (Multi-Client) auf Port " + port);
        running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket;
            System.out.println("[SERVER] Warte auf eingehende Clients ...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = nextClientId++;
                    System.out.println("[SERVER] Neuer Client (" + clientId + ") von " + clientSocket.getRemoteSocketAddress());

                    ClientHandler handler = new ClientHandler(this, clientSocket, clientId);
                    addClient(handler);
                    handler.start();

                } catch (IOException e) {
                    if (running) {
                        System.err.println("[SERVER] Fehler beim Accept: " + e.getMessage());
                    } else {
                        System.out.println("[SERVER] Accept unterbrochen, Server wird beendet.");
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Konnte ServerSocket nicht öffnen: " + e.getMessage());
            e.printStackTrace();
        } finally {
            running = false;
            System.out.println("[SERVER] Server beendet.");
        }
    }

    public void stop() {
        running = false;
        System.out.println("[SERVER] Stop-Signal gesetzt.");

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // unterbricht accept()
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Fehler beim Schließen des ServerSocket: " + e.getMessage());
        }

        for (ClientHandler ch : clients) {
            ch.interrupt();
        }
    }

    public void addClient(ClientHandler handler) {
        clients.add(handler);
        System.out.println("[SERVER] Client hinzugefügt. Aktive Verbindungen: " + clients.size());
    }

    /**
     * Wird von ClientHandler aufgerufen, wenn eine Verbindung endet.
     * Entfernt den Client aus der Verbindungs- und Login-Liste
     * und informiert ggf. andere Clients.
     */
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);

        User user = handler.getUser();
        if (user != null) {
            String username = user.getUsername();
            loggedInClients.remove(username);
            broadcastUserLeft(username);
        }

        System.out.println("[SERVER] Client entfernt. Aktive Verbindungen: " + clients.size());
    }

    /* ==================== Benutzerverwaltung ==================== */

    /**
     * Versucht, einen neuen User zu registrieren.
     * @return true, wenn der Username neu war und registriert wurde.
     */
    public boolean registerUser(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return false;
        }
        User user = new User(username, password);
        User existing = registeredUsers.putIfAbsent(username, user);
        return existing == null;
    }

    /**
     * Prüft Benutzername+Passwort.
     * @return User-Objekt bei Erfolg, sonst null.
     */
    public User authenticateUser(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        User user = registeredUsers.get(username);
        if (user == null) {
            return null;
        }
        if (!user.getPassword().equals(password)) {
            return null;
        }
        return user;
    }

    public boolean isUserLoggedIn(String username) {
        return loggedInClients.containsKey(username);
    }

    public void addLoggedInClient(User user, ClientHandler handler) {
        loggedInClients.put(user.getUsername(), handler);
    }

    public List<String> getLoggedInUsernames() {
        return new ArrayList<>(loggedInClients.keySet());
    }

    /* ==================== Broadcast-Funktionen ==================== */

    /**
     * Broadcastet eine Chat-Nachricht an alle eingeloggten Clients.
     */
    public void broadcastChat(String from, String text) {
        String message = Protocol.buildChat(from, text);
        System.out.println("[SERVER] Broadcast CHAT von " + from + ": " + text);

        for (ClientHandler ch : loggedInClients.values()) {
            try {
                ch.send(message);
            } catch (IOException e) {
                System.err.println("[SERVER] Fehler beim Senden an " + ch.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Info: Ein Benutzer ist neu beigetreten.
     * except: dieser Handler bekommt die JOIN-Nachricht nicht (z.B. der frisch Eingeloggte selbst).
     */
    public void broadcastUserJoined(String username, ClientHandler except) {
        String msg = Protocol.RES_USER_JOINED + " " + username;
        System.out.println("[SERVER] USER_JOINED: " + username);

        for (ClientHandler ch : loggedInClients.values()) {
            if (ch == except) {
                continue;
            }
            try {
                ch.send(msg);
            } catch (IOException e) {
                System.err.println("[SERVER] Fehler beim Senden USER_JOINED an " + ch.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Info: Ein Benutzer hat den Chat verlassen.
     */
    public void broadcastUserLeft(String username) {
        String msg = Protocol.RES_USER_LEFT + " " + username;
        System.out.println("[SERVER] USER_LEFT: " + username);

        for (ClientHandler ch : loggedInClients.values()) {
            try {
                ch.send(msg);
            } catch (IOException e) {
                System.err.println("[SERVER] Fehler beim Senden USER_LEFT an " + ch.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Broadcastet eine allgemeine INFO-Nachricht an alle eingeloggten Clients.
     */
    public void broadcastInfo(String text) {
        String msg = Protocol.RES_INFO + " " + text;
        System.out.println("[SERVER] INFO-Broadcast: " + text);

        for (ClientHandler ch : loggedInClients.values()) {
            try {
                ch.send(msg);
            } catch (IOException e) {
                System.err.println("[SERVER] Fehler beim Senden INFO an " + ch.getName() + ": " + e.getMessage());
            }
        }
    }
}
