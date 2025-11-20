package de.uni_jena.fpp.chatroom;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final String PROPERTIES_FILE = "chatroom.properties";

    private static String serverHost;
    private static int serverPort;

    static {
        load();
    }

    private static void load() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                System.err.println("[WARN] Konfigurationsdatei " + PROPERTIES_FILE + " nicht gefunden. Verwende Defaults.");
                serverHost = "localhost";
                serverPort = 5000;
                return;
            }
            props.load(in);
            serverHost = props.getProperty("server.host", "localhost");
            String portStr = props.getProperty("server.port", "5000");
            serverPort = Integer.parseInt(portStr);
        } catch (IOException | NumberFormatException e) {
            System.err.println("[ERROR] Fehler beim Laden der Konfiguration: " + e.getMessage());
            System.err.println("[INFO] Verwende Default-Werte.");
            serverHost = "localhost";
            serverPort = 5000;
        }
    }

    public static String getServerHost() {
        return serverHost;
    }

    public static int getServerPort() {
        return serverPort;
    }
}
