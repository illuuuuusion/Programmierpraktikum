package de.uni_jena.fpp.chatroom;

import java.util.List;

public interface ChatClientListener {

    default void onRoomsUpdated(List<String> rooms) {}
    default void onUsersUpdated(String room, List<String> users) {}
    default void onChatMessage(String room, String from, String text) {}

    default void onInfo(String text) {}
    default void onError(String text) {}
    default void onWarn(String text) {}
    default void onBanned(String reason) {}

    default void onConnectionClosed() {}
}

