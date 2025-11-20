package de.uni_jena.fpp.chatroom;

import java.util.Arrays;
import java.util.List;

public final class Protocol {

    // Client -> Server
    public static final String CMD_REGISTER = "REGISTER";
    public static final String CMD_LOGIN    = "LOGIN";
    public static final String CMD_MSG      = "MSG";
    public static final String CMD_LOGOUT   = "LOGOUT";
    public static final String CMD_WHO      = "WHO";

    // Server -> Client
    public static final String RES_REGISTER_OK     = "REGISTER_OK";
    public static final String RES_REGISTER_FAILED = "REGISTER_FAILED";

    public static final String RES_LOGIN_OK        = "LOGIN_OK";
    public static final String RES_LOGIN_FAILED    = "LOGIN_FAILED";

    public static final String RES_USER_LIST       = "USER_LIST";
    public static final String RES_USER_JOINED     = "USER_JOINED";
    public static final String RES_USER_LEFT       = "USER_LEFT";

    public static final String RES_CHAT            = "CHAT";
    public static final String RES_INFO            = "INFO";
    public static final String RES_ERROR           = "ERROR";

    private Protocol() {
        // Utility-Klasse, kein public Konstruktor
    }

    /**
     * Splittet eine eingehende Protokollzeile in Tokens.
     * Achtung: Für MESSAGE-Commands (MSG, CHAT, INFO, ERROR) ist alles nach dem ersten/zweiten Token der Text.
     */
    public static String[] splitTokens(String line) {
        if (line == null) {
            return new String[0];
        }
        return line.trim().split("\\s+", 3);
        // max 3 Teile:
        // 0: COMMAND
        // 1: param1 (z.B. username)
        // 2: restlicher Text (z.B. Message)
    }

    public static String buildRegister(String username, String password) {
        return CMD_REGISTER + " " + username + " " + password;
    }

    public static String buildLogin(String username, String password) {
        return CMD_LOGIN + " " + username + " " + password;
    }

    public static String buildMsg(String text) {
        return CMD_MSG + " " + text;
    }

    public static String buildLogout() {
        return CMD_LOGOUT;
    }

    public static String buildWho() {
        return CMD_WHO;
    }

    public static String buildChat(String from, String text) {
        return RES_CHAT + " " + from + " " + text;
    }

    public static String buildUserList(List<String> users) {
        String joined = String.join(",", users);
        return RES_USER_LIST + " " + joined;
    }

    public static List<String> parseUserList(String userListPayload) {
        if (userListPayload == null || userListPayload.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(userListPayload.split(","));
    }
}
