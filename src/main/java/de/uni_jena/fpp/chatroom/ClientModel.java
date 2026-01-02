package de.uni_jena.fpp.chatroom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientModel {

    private final List<String> rooms = new ArrayList<>();
    private String currentRoom;
    private final List<String> usersInCurrentRoom = new ArrayList<>();
    private final List<String> chatLines = new ArrayList<>();

    // Begrenzung, damit UI nicht unendlich wächst
    private int maxChatLines = 500;

    public synchronized List<String> getRooms() {
        return new ArrayList<>(rooms);
    }

    public synchronized String getCurrentRoom() {
        return currentRoom;
    }

    public synchronized List<String> getUsersInCurrentRoom() {
        return new ArrayList<>(usersInCurrentRoom);
    }

    public synchronized List<String> getChatLines() {
        return new ArrayList<>(chatLines);
    }

    public synchronized void setRooms(List<String> newRooms) {
        rooms.clear();
        if (newRooms != null) rooms.addAll(newRooms);
        rooms.sort(String::compareToIgnoreCase);
    }

    public synchronized void setCurrentRoom(String room) {
        this.currentRoom = room;
        // wenn room gewechselt wird, kann UI users neu bekommen
        usersInCurrentRoom.clear();
    }

    public synchronized void setUsersInCurrentRoom(List<String> users) {
        usersInCurrentRoom.clear();
        if (users != null) usersInCurrentRoom.addAll(users);
        usersInCurrentRoom.sort(String::compareToIgnoreCase);
    }

    public synchronized void addChatLine(String line) {
        if (line == null) line = "";
        chatLines.add(line);
        while (chatLines.size() > maxChatLines) {
            chatLines.remove(0);
        }
    }

    public synchronized void setMaxChatLines(int max) {
        maxChatLines = Math.max(50, max);
        while (chatLines.size() > maxChatLines) chatLines.remove(0);
    }

    // Für Debug/Tests: verhindert External modification
    public synchronized List<String> getRoomsUnmodifiable() {
        return Collections.unmodifiableList(new ArrayList<>(rooms));
    }
}
