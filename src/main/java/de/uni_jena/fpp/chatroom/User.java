package de.uni_jena.fpp.chatroom;

public class User {

    private final String username;
    private final int iterations;
    private final byte[] salt;
    private final byte[] passwordHash;
    private final boolean banned;

    public User(String username, int iterations, byte[] salt, byte[] passwordHash, boolean banned) {
        this.username = username;
        this.iterations = iterations;
        this.salt = salt;
        this.passwordHash = passwordHash;
        this.banned = banned;
    }

    public String getUsername() {
        return username;
    }

    public int getIterations() {
        return iterations;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getPasswordHash() {
        return passwordHash;
    }

    public boolean isBanned() {
        return banned;
    }

    public User withBanned(boolean banned) {
        return new User(username, iterations, salt, passwordHash, banned);
    }

    @Override
    public String toString() {
        return "User{username='" + username + "', banned=" + banned + "}";
    }
}
