package de.uni_jena.fpp.chatroom;

public class ChatServerMain {

    public static void main(String[] args) throws Exception {
        int port = Config.getServerPort();

        UserRepository repo = new FileUserRepository(
                Config.getUsersFile(),
                Config.getPbkdf2Iterations()
        );

        ServerLogger logger = new ServerLogger(Config.getServerLogFile());
        ChatServer server = new ChatServer(port, repo, logger);

        logger.info("Config: port=" + port + " users.file=" + Config.getUsersFile()
                + " log.file=" + Config.getServerLogFile());

        server.start();
    }

}
