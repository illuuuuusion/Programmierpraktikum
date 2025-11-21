package de.uni_jena.fpp.chatroom;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

public class ChatClient {

    private final String host;
    private final int port;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private volatile boolean running = true;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Startet den Client:
     * - baut Verbindung zum Server auf
     * - startet Listener-Thread für Server-Nachrichten
     * - liest Konsoleneingaben und sendet Kommandos an den Server
     */
    public void start() {
        try {
            socket = new Socket(host, port);
            System.out.println("[CLIENT] Verbunden mit " + host + ":" + port);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            // Thread: hört auf Nachrichten vom Server
            Thread listenerThread = new Thread(this::listenToServer, "ServerListener");
            listenerThread.start();

            // Hauptthread: verarbeitet Konsoleneingaben
            readConsoleInput();

            // Wenn wir aus der Konsolenschleife raus sind:
            running = false;
            try {
                listenerThread.join();
            } catch (InterruptedException ignore) {
            }

        } catch (IOException e) {
            System.err.println("[CLIENT] Fehler beim Verbinden: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Liest Nachrichten vom Server und gibt sies formatiert auf der Konsole aus.
     */
    private void listenToServer() {
        System.out.println("[CLIENT] ServerListener gestartet.");
        try {
            while (running && !socket.isClosed()) {
                String line;
                try {
                    line = in.readUTF(); // blockiert, bis etwas kommt
                } catch (EOFException eof) {
                    System.out.println("[CLIENT] Verbindung vom Server geschlossen (EOF).");
                    break;
                }

                if (line == null) {
                    System.out.println("[CLIENT] null vom Server gelesen – beende Listener.");
                    break;
                }

                handleServerMessage(line);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[CLIENT] Fehler beim Lesen vom Server: " + e.getMessage());
            }
        } finally {
            running = false;
            System.out.println("[CLIENT] ServerListener beendet.");
        }
    }

    /**
     * Verarbeitet eine einzelne Nachricht vom Server anhand des Protokolls.
     */
    private void handleServerMessage(String line) {
        String[] tokens = Protocol.splitTokens(line);
        if (tokens.length == 0) {
            System.out.println("[SERVER] (leer)");
            return;
        }

        String cmd = tokens[0];

        switch (cmd) {
            case Protocol.RES_CHAT -> {
                // CHAT <from> <text>
                if (tokens.length < 3) {
                    System.out.println("[SERVER] CHAT (ungültig): " + line);
                    return;
                }
                String from = tokens[1];
                String text = tokens[2];
                System.out.println("[" + from + "] " + text);
            }

            case Protocol.RES_USER_LIST -> {
                // USER_LIST user1,user2,...
                if (tokens.length < 2) {
                    System.out.println("[SERVER] USER_LIST (leer)");
                    return;
                }
                List<String> users = Protocol.parseUserList(tokens[1]);
                System.out.println("[INFO] Angemeldete Benutzer: " + String.join(", ", users));
            }

            case Protocol.RES_USER_JOINED -> {
                // USER_JOINED <username>
                if (tokens.length < 2) {
                    System.out.println("[SERVER] USER_JOINED (ungültig): " + line);
                    return;
                }
                String username = tokens[1];
                System.out.println("[INFO] " + username + " hat den Chat betreten.");
            }

            case Protocol.RES_USER_LEFT -> {
                // USER_LEFT <username>
                if (tokens.length < 2) {
                    System.out.println("[SERVER] USER_LEFT (ungültig): " + line);
                    return;
                }
                String username = tokens[1];
                System.out.println("[INFO] " + username + " hat den Chat verlassen.");
            }

            case Protocol.RES_LOGIN_OK -> {
                System.out.println("[INFO] Login erfolgreich.");
            }

            case Protocol.RES_LOGIN_FAILED -> {
                String reason = (tokens.length >= 2) ? tokens[1] : "Unbekannter Grund";
                System.out.println("[WARN] Login fehlgeschlagen: " + reason);
            }

            case Protocol.RES_REGISTER_OK -> {
                System.out.println("[INFO] Registrierung erfolgreich. Du kannst dich jetzt einloggen.");
            }

            case Protocol.RES_REGISTER_FAILED -> {
                String reason = (tokens.length >= 2) ? tokens[1] : "Unbekannter Grund";
                System.out.println("[WARN] Registrierung fehlgeschlagen: " + reason);
            }

            case Protocol.RES_INFO -> {
                String msg = (tokens.length >= 2) ? tokens[1] : "";
                System.out.println("[INFO] " + msg);
            }

            case Protocol.RES_ERROR -> {
                String msg = (tokens.length >= 2) ? tokens[1] : "";
                System.out.println("[ERROR] " + msg);
            }

            default -> {
                System.out.println("[SERVER] " + line);
            }
        }
    }

    /**
     * Liest Eingaben von der Konsole und schickt Kommandos an den Server.
     */
    private void readConsoleInput() {
        System.out.println("""
                [CLIENT] Eingabe bereit.
                Befehle:
                  /register <username> <password>
                  /login <username> <password>
                  /who
                  /msg <text>
                  /logout
                  /quit   (Client beenden)
                Normale Eingabe ohne '/' wird als Chat-Nachricht gesendet.
                """);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running) {
                String line = reader.readLine();
                if (line == null) {
                    break; // EOF auf System.in
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Client-internes Kommando?
                if (line.startsWith("/")) {
                    boolean stayRunning = handleConsoleCommand(line);
                    if (!stayRunning) {
                        break;
                    }
                } else {
                    // Normale Nachricht -> MSG
                    send(Protocol.buildMsg(line));
                }
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Fehler beim Lesen von der Konsole: " + e.getMessage());
        }

        System.out.println("[CLIENT] Konsole beendet.");
    }

    /**
     * Verarbeitet Befehle, die mit '/' beginnen.
     * @return false, wenn der Client beendet werden soll (/quit)
     */
    private boolean handleConsoleCommand(String line) throws IOException {
        String[] parts = line.split("\\s+", 3);
        String cmd = parts[0];

        switch (cmd) {
            case "/register" -> {
                if (parts.length < 3) {
                    System.out.println("Usage: /register <username> <password>");
                    return true;
                }
                String username = parts[1];
                String password = parts[2];
                send(Protocol.buildRegister(username, password));
            }

            case "/login" -> {
                if (parts.length < 3) {
                    System.out.println("Usage: /login <username> <password>");
                    return true;
                }
                String username = parts[1];
                String password = parts[2];
                send(Protocol.buildLogin(username, password));
            }

            case "/who" -> {
                send(Protocol.buildWho());
            }

            case "/msg" -> {
                if (parts.length < 2) {
                    System.out.println("Usage: /msg <text>");
                    return true;
                }
                String text = (parts.length == 2) ? parts[1] : parts[1] + " " + parts[2];
                send(Protocol.buildMsg(text));
            }

            case "/logout" -> {
                send(Protocol.buildLogout());
            }

            case "/quit" -> {
                // Versuchen, einen Logout zu schicken, dann beendet sich der Client
                try {
                    send(Protocol.buildLogout());
                } catch (IOException ignore) {
                }
                running = false;
                return false;
            }

            default -> {
                System.out.println("Unbekannter Befehl: " + cmd);
            }
        }

        return true;
    }

    /**
     * Schickt eine Protokollnachricht an den Server.
     */
    private synchronized void send(String message) throws IOException {
        if (out == null) {
            System.err.println("[CLIENT] Noch keine Verbindung zum Server.");
            return;
        }
        out.writeUTF(message);
        out.flush();
    }

    private void cleanup() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
        System.out.println("[CLIENT] Verbindung geschlossen.");
    }
}
