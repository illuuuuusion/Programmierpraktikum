package de.uni_jena.fpp.chatroom;

import java.util.Arrays;
import java.util.List;

public final class Protocol {

    // ===== Client -> Server =====
    public static final String CMD_REGISTER     = "REGISTER";
    public static final String CMD_LOGIN        = "LOGIN";
    public static final String CMD_MSG          = "MSG";
    public static final String CMD_LOGOUT       = "LOGOUT";
    public static final String CMD_WHO          = "WHO"; // optional/debug

    // MS2: Räume
    public static final String CMD_CREATE_ROOM  = "CREATE_ROOM";
    public static final String CMD_JOIN         = "JOIN";
    public static final String CMD_LEAVE        = "LEAVE";

    // ===== Server -> Client =====
    public static final String RES_REGISTER_OK     = "REGISTER_OK";
    public static final String RES_REGISTER_FAILED = "REGISTER_FAILED";

    public static final String RES_LOGIN_OK        = "LOGIN_OK";
    public static final String RES_LOGIN_FAILED    = "LOGIN_FAILED";

    // MS2: dynamische Raum-/Useranzeige
    public static final String RES_ROOM_LIST       = "ROOM_LIST";
    public static final String RES_ROOM_USERS      = "ROOM_USERS";

    // MS2: Chat + Admin
    public static final String RES_CHAT            = "CHAT";     // CHAT <room> <from> <text...>
    public static final String RES_WARN            = "WARN";     // WARN <text...>
    public static final String RES_BANNED          = "BANNED";   // BANNED <reason...>

    public static final String RES_INFO            = "INFO";
    public static final String RES_ERROR           = "ERROR";

    private static final String LIST_DELIM = "|";

    private Protocol() {}

    /**
     * Splittet eine Zeile in max. 3 Teile:
     * 0: COMMAND
     * 1: param1
     * 2: rest
     */
    public static String[] splitTokens(String line) {
        if (line == null) return new String[0];
        return line.trim().split("\\s+", 3);
    }

    // Builder: Client -> Server
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

    public static String buildCreateRoom(String room) {
        return CMD_CREATE_ROOM + " " + room;
    }

    public static String buildJoin(String room) {
        return CMD_JOIN + " " + room;
    }

    public static String buildLeave() {
        return CMD_LEAVE;
    }

    // Builder: Server -> Client
    public static String buildChat(String room, String from, String text) {
        // CHAT <room> <from> <text...>
        return RES_CHAT + " " + room + " " + from + " " + text;
    }

    public static String buildRoomList(List<String> rooms) {
        // ROOM_LIST <room1>|<room2>|...
        String payload = String.join(LIST_DELIM, rooms);
        return payload.isEmpty() ? RES_ROOM_LIST : (RES_ROOM_LIST + " " + payload);
    }

    public static String buildRoomUsers(String room, List<String> users) {
        // ROOM_USERS <room> <user1>|<user2>|...
        String payload = String.join(LIST_DELIM, users);
        return payload.isEmpty()
                ? (RES_ROOM_USERS + " " + room)
                : (RES_ROOM_USERS + " " + room + " " + payload);
    }

    public static String buildWarn(String text) {
        return RES_WARN + " " + (text == null ? "" : text);
    }

    public static String buildBanned(String reason) {
        return RES_BANNED + " " + (reason == null ? "" : reason);
    }


    // Parser
    public static List<String> parsePipeList(String payload) {
        if (payload == null || payload.isBlank()) return List.of();
        return Arrays.asList(payload.split("\\Q" + LIST_DELIM + "\\E"));
    }
}
