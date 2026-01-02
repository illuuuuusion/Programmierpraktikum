package de.uni_jena.fpp.chatroom;

import java.util.List;

public class ChatClientMain {

    public static void main(String[] args) {
        String host = Config.getServerHost();
        int port = Config.getServerPort();

        System.out.println("[CLIENT] Verbinde zu " + host + ":" + port);
        ChatClient client = new ChatClient(host, port);

        client.addListener(new ChatClientListener() {
            @Override public void onInfo(String text) { System.out.println("[INFO] " + text); }
            @Override public void onError(String text) { System.out.println("[ERROR] " + text); }
            @Override public void onWarn(String text) { System.out.println("[WARN] " + text); }
            @Override public void onBanned(String reason) { System.out.println("[BANNED] " + reason); }

            @Override public void onRoomsUpdated(List<String> rooms) {
                System.out.println("[ROOMS] " + rooms);
            }

            @Override public void onUsersUpdated(String room, List<String> users) {
                System.out.println("[USERS@" + room + "] " + users);
            }

            @Override public void onChatMessage(String room, String from, String text) {
                System.out.println("[" + room + "][" + from + "] " + text);
            }

            @Override public void onConnectionClosed() {
                System.out.println("[CONN] geschlossen");
            }
        });

        client.start();
    }
}
