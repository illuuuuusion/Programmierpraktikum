package de.uni_jena.fpp.chatroom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Room {

    private final String name;
    private final Set<ClientHandler> members = ConcurrentHashMap.newKeySet();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<ClientHandler> getMembers() {
        return members;
    }

    public void addMember(ClientHandler ch) {
        members.add(ch);
    }

    public void removeMember(ClientHandler ch) {
        members.remove(ch);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public List<String> getMemberNames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler ch : members) {
            String n = ch.getDisplayName();
            if (n != null && !n.isBlank()) names.add(n);
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }
}
