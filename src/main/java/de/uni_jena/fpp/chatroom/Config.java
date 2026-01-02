package de.uni_jena.fpp.chatroom;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final String PROPERTIES_FILE = "chatroom.properties";

    private static String serverHost;
    private static int serverPort;

    private static String usersFile;
    private static int pbkdf2Iterations;
    private static String serverLogFile;

    static {
        load();
    }

    private static void load() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("[CONFIG] Konnte " + PROPERTIES_FILE + " nicht finden. Verwende Defaults.");
            }
        } catch (IOException e) {
            System.err.println("[CONFIG] Fehler beim Laden: " + e.getMessage());
        }

        serverHost = props.getProperty("server.host", "localhost");
        serverPort = parseInt(props.getProperty("server.port"), 5000);

        usersFile = props.getProperty("users.file", "data/users.db");
        pbkdf2Iterations = parseInt(
                props.getProperty("security.pbkdf2.iterations"),
                PasswordUtil.DEFAULT_ITERATIONS
        );
        serverLogFile = props.getProperty("server.log.file", "data/server.log");
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static String getServerHost() {
        return serverHost;
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static String getUsersFile() {
        return usersFile;
    }

    public static int getPbkdf2Iterations() {
        return pbkdf2Iterations;
    }

    public static String getServerLogFile() {
        return serverLogFile;
    }

}
