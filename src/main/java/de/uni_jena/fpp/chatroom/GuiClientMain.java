package de.uni_jena.fpp.chatroom;

import de.uni_jena.fpp.chatroom.frames.LoginFrame;

import javax.swing.*;

public class GuiClientMain {
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient(Config.getServerHost(), Config.getServerPort());
        client.connect();

        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame(client);
            login.initialize(login);
        });
    }
}
