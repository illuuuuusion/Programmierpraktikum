package de.uni_jena.fpp.chatroom;

import java.util.List;

public interface UserRepository {
    boolean createUser(String username, char[] password);
    User verifyLogin(String username, char[] password);
    List<User> listUsers();
    boolean setBanned(String username, boolean banned);
}
