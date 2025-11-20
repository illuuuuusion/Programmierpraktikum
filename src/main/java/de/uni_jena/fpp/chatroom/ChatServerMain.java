package de.uni_jena.fpp.chatroom;

public class ChatServerMain {

    public static void main(String[] args) {
        int port = Config.getServerPort();

        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("[SERVER] Ungültiger Port in Argumenten, verwende Config-Port.");
            }
        }

        ChatServer server = new ChatServer(port);

        System.out.println("[SERVER] Verwende Konfiguration: port=" + port);
        server.start();
    }
}
