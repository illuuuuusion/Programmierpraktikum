package de.uni_jena.fpp.chatroom;

@FunctionalInterface
public interface ServerLogListener {
    void onLogLine(String line);
}
