package de.uni_jena.fpp.chatroom;

import de.uni_jena.fpp.chatroom.frames.ServerFrame;

import javax.swing.*;

public class GuiServerMain {

    public static void main(String[] args) throws Exception {

        int port = Config.getServerPort();

        UserRepository repo = new FileUserRepository(
                Config.getUsersFile(),
                Config.getPbkdf2Iterations()
        );

        ServerLogger logger = new ServerLogger(Config.getServerLogFile());
        ChatServer server = new ChatServer(port, repo, logger);

        // optional: sauber schließen beim Beenden
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.stop(); } catch (Exception ignore) {}
            try { logger.close(); } catch (Exception ignore) {}
        }));

        SwingUtilities.invokeLater(() -> {
            ServerFrame frame = new ServerFrame(server);
            frame.initialize(frame);
            frame.setVisible(true);
        });
    }
}
