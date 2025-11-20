package de.uni_jena.fpp.chatroom;

public class ChatClientMain {

    public static void main(String[] args) {
        String host = Config.getServerHost();
        int port = Config.getServerPort();

        System.out.println("[CLIENT] Verbinde zu " + host + ":" + port);
        ChatClient client = new ChatClient(host, port);
        client.start();
    }
}
