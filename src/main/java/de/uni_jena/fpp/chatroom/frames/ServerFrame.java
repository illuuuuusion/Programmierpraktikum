package de.uni_jena.fpp.chatroom.frames;

import de.uni_jena.fpp.chatroom.ChatServer;
import de.uni_jena.fpp.chatroom.Room;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

public class ServerFrame extends MainFrame {

    private final ChatServer server;
    private Thread serverThread;

    public ServerFrame(ChatServer server) {
        this.server = server;
    }

    public void initialize(JFrame frame) {
        setSize(900, 500);
        setLocation(750, 320);
        setTitle("Chat Server");

        // --------------- TOP Layer ---------------
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(mainColor);
        mainPanel.setFont(mainFont);
        mainPanel.setOpaque(true);
        mainPanel.setBorder(new TitledBorder("Chat Server"));
        mainPanel.setLayout(new BorderLayout());

        JPanel split = new JPanel();
        split.setLayout(new GridLayout(1,2));

        // --------------- Chat Layer ---------------
        JPanel chatPanel = new JPanel();
        chatPanel.setBorder(new TitledBorder("Log"));
        chatPanel.setBackground(mainColor);
        chatPanel.setOpaque(true);
        SpringLayout chatLayout = new SpringLayout();
        chatPanel.setLayout(chatLayout);

        TextArea chatBox = new TextArea();
        chatPanel.add(chatBox);

        // Server Panel (Start/Stop)
        JPanel serverPanel = new JPanel();
        serverPanel.setLayout(new GridLayout(1, 2));

        JButton btnStart = new JButton("Server starten");
        btnStart.setFont(mainFont);

        JButton btnStop = new JButton("Server beenden");
        btnStop.setFont(mainFont);
        btnStop.setEnabled(false);

        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (server == null) {
                    chatBox.append("[ERROR] Kein Server-Objekt.\n");
                    return;
                }
                if (serverThread != null && serverThread.isAlive()) {
                    chatBox.append("[INFO] Server läuft bereits.\n");
                    return;
                }

                serverThread = new Thread(server::start, "ChatServerThread");
                serverThread.start();

                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                chatBox.append("[INFO] Server-Thread gestartet.\n");
            }
        });

        btnStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (server == null) return;
                server.stop();
                chatBox.append("[INFO] Stop gesendet.\n");

                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
            }
        });

        serverPanel.add(btnStart);
        serverPanel.add(btnStop);

        chatPanel.add(serverPanel);

        chatLayout.putConstraint(SpringLayout.NORTH, chatBox, 0, SpringLayout.NORTH, chatPanel);
        chatLayout.putConstraint(SpringLayout.SOUTH, chatBox, -10, SpringLayout.NORTH, serverPanel);
        chatLayout.putConstraint(SpringLayout.WEST, chatBox, 0, SpringLayout.WEST, chatPanel);
        chatLayout.putConstraint(SpringLayout.EAST, chatBox, -5, SpringLayout.EAST, chatPanel);

        chatLayout.putConstraint(SpringLayout.SOUTH, serverPanel, 0, SpringLayout.SOUTH, chatPanel);
        chatLayout.putConstraint(SpringLayout.WEST, serverPanel, 0, SpringLayout.WEST, chatPanel);
        chatLayout.putConstraint(SpringLayout.EAST, serverPanel, 0, SpringLayout.EAST, chatPanel);

        // --------------- Rechte Seite ---------------
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayout(2, 1));

        // Räume
        // Räume
        JPanel roomPanel = new JPanel();
        roomPanel.setBorder(new TitledBorder("Räume"));
        roomPanel.setLayout(new BorderLayout());

// Liste
        DefaultListModel<String> roomsModel = new DefaultListModel<>();
        JList<String> roomList = new JList<>(roomsModel);
        roomList.setFont(mainFont);
        roomList.setEnabled(true);

        roomPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);

// Buttons unten
        JPanel roomButtons = new JPanel();
        roomButtons.setLayout(new GridLayout(1, 2, 5, 5));

        JButton btnRoomCreate = new JButton("Raum erstellen");
        btnRoomCreate.setFont(mainFont);

        JButton btnRoomDelete = new JButton("Raum löschen");
        btnRoomDelete.setFont(mainFont);

        btnRoomCreate.addActionListener(e -> {
            if (server == null) return;

            String name = JOptionPane.showInputDialog(frame, "Raumname:", "Raum erstellen", JOptionPane.PLAIN_MESSAGE);
            if (name == null) return; // abgebrochen
            name = name.trim();
            if (name.isEmpty()) {
                chatBox.append("[ERROR] Raumname darf nicht leer sein.\n");
                return;
            }

            boolean ok = server.createRoomAsServer(name);
            chatBox.append(ok
                    ? "[INFO] Server-Raum erstellt: " + name + "\n"
                    : "[ERROR] Raum konnte nicht erstellt werden (existiert schon oder Name ungültig).\n");
        });

        btnRoomDelete.addActionListener(e -> {
            if (server == null) return;

            String selected = roomList.getSelectedValue();
            if (selected == null) {
                chatBox.append("[INFO] Bitte erst einen Raum auswählen.\n");
                return;
            }

            if (ChatServer.DEFAULT_ROOM.equals(selected)) {
                chatBox.append("[ERROR] Lobby darf nicht gelöscht werden.\n");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "Raum wirklich löschen?\n" + selected,
                    "Raum löschen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) return;

            boolean ok = server.deleteRoomAsServer(selected);
            chatBox.append(ok
                    ? "[INFO] Server-Raum gelöscht: " + selected + "\n"
                    : "[ERROR] Raum konnte nicht gelöscht werden.\n");
        });

        roomButtons.add(btnRoomCreate);
        roomButtons.add(btnRoomDelete);

        roomPanel.add(roomButtons, BorderLayout.SOUTH);
        panel1.add(roomPanel);


        // Nutzer
        JPanel userPanel = new JPanel();
        SpringLayout userPanelLayout = new SpringLayout();
        userPanel.setLayout(userPanelLayout);
        userPanel.setBorder(new TitledBorder("Nutzer"));

        JPanel pan = new JPanel();
        pan.setLayout(new GridLayout(1, 1 ));

        DefaultListModel<String> usersModel = new DefaultListModel<>();
        JList<String> lstUsers = new JList<>(usersModel);
        lstUsers.setFont(mainFont);
        lstUsers.setEnabled(true);

        pan.add(new JScrollPane(lstUsers));

        JButton btnKick = new JButton("Nutzer bearbeiten");
        btnKick.setFont(mainFont);
        btnKick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (server == null) return;

                if (lstUsers.isSelectionEmpty()) {
                    chatBox.append("[INFO] Bitte erst einen Nutzer auswählen.\n");
                    return;
                }

                // Anzeige ist z.B. "max | raum1" -> username extrahieren
                String selected = lstUsers.getSelectedValue();
                String username = selected.split("\\s*\\|\\s*", 2)[0];

                Object[] options = {"Warnen", "Bannen", "Abbrechen"};
                int choice = JOptionPane.showOptionDialog(

                        frame,
                        "Aktion für Nutzer: " + username,
                        "Admin-Aktion",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
                );
                if (choice < 0 || choice == 2) return; // Fenster geschlossen oder Abbrechen


                if (choice == 0) { // Warnen
                    String text = JOptionPane.showInputDialog(frame, "Warnungstext:", "Warnen", JOptionPane.PLAIN_MESSAGE);
                    if (text == null) return;
                    boolean ok = server.warnUser(username, text);
                    chatBox.append(ok ? "[INFO] WARN gesendet an " + username + "\n"
                            : "[ERROR] WARN nicht möglich (User offline?)\n");
                } else if (choice == 1) { // Bannen
                    String reason = JOptionPane.showInputDialog(frame, "Ban-Grund:", "Bannen", JOptionPane.PLAIN_MESSAGE);
                    if (reason == null) return;
                    boolean ok = server.banUser(username, reason);
                    chatBox.append(ok ? "[INFO] USER gebannt: " + username + "\n"
                            : "[ERROR] BAN fehlgeschlagen (User unbekannt?)\n");
                }
            }
        });


        userPanel.add(pan);
        userPanel.add(btnKick);

        userPanelLayout.putConstraint(SpringLayout.NORTH, pan, 0, SpringLayout.NORTH, userPanel);
        userPanelLayout.putConstraint(SpringLayout.SOUTH, pan, -10, SpringLayout.NORTH, btnKick);
        userPanelLayout.putConstraint(SpringLayout.WEST, pan, 0, SpringLayout.WEST, userPanel);
        userPanelLayout.putConstraint(SpringLayout.EAST, pan, -5, SpringLayout.EAST, userPanel);

        userPanelLayout.putConstraint(SpringLayout.SOUTH, btnKick, 0, SpringLayout.SOUTH, userPanel);
        userPanelLayout.putConstraint(SpringLayout.WEST, btnKick, 0, SpringLayout.WEST, userPanel);
        userPanelLayout.putConstraint(SpringLayout.EAST, btnKick, 0, SpringLayout.EAST, userPanel);

        panel1.add(userPanel);

        // FINAL ADDING
        split.add(chatPanel);
        split.add(panel1);
        mainPanel.add(split, BorderLayout.CENTER);
        add(mainPanel);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setVisible(true);

        // Log Listener: ServerLogger -> GUI
        if (server != null) {
            server.addLogListener(line -> SwingUtilities.invokeLater(() -> chatBox.append(line + "\n")));
        } else {
            chatBox.append("[WARN] ServerFrame ohne Server gestartet.\n");
        }

        // Periodischer Refresh von Rooms/Users (einfach + robust)
        Timer refreshTimer = new Timer(1000, evt -> {
            if (server == null) return;

            // Selection merken (Rooms)
            String selectedRoom = roomList.getSelectedValue();

            // Selection merken (Users) -> Username aus "name | room"
            String selectedUserLabel = lstUsers.getSelectedValue();
            String selectedUsername = null;
            if (selectedUserLabel != null) {
                selectedUsername = selectedUserLabel.split("\\s*\\|\\s*", 2)[0];
            }

            // ===== Rooms updaten =====
            List<String> rooms = server.getRoomNames();
            roomsModel.clear();
            for (String r : rooms) roomsModel.addElement(r);

            // Room selection restore
            if (selectedRoom != null && rooms.contains(selectedRoom)) {
                roomList.setSelectedValue(selectedRoom, true);
            }


            // ===== Users updaten (mit Raum) =====
            usersModel.clear();

            String room = roomList.getSelectedValue(); // ggf. nach restore
            if (room != null) {
                // Nutzer im selektierten Raum
                Map<String, de.uni_jena.fpp.chatroom.Room> map = server.getRoomsUnsafe();
                de.uni_jena.fpp.chatroom.Room rr = map.get(room);
                if (rr != null) {
                    for (String u : rr.getMemberNames()) {
                        usersModel.addElement(u + " | " + room);
                    }
                }
            } else {
                // Online-User + ihre Räume
                var online = server.getOnlineUserRooms(); // neuer Helfer in ChatServer
                var names = new java.util.ArrayList<>(online.keySet());
                names.sort(String::compareToIgnoreCase);
                for (String u : names) {
                    usersModel.addElement(u + " | " + online.get(u));
                }
            }

            // User selection restore
            if (selectedUsername != null) {
                for (int i = 0; i < usersModel.size(); i++) {
                    String label = usersModel.get(i);
                    if (label.startsWith(selectedUsername + " |")) {
                        lstUsers.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });
        refreshTimer.start();
    }

    public static void main(String[] args) {
        try {
            int port = de.uni_jena.fpp.chatroom.Config.getServerPort();

            de.uni_jena.fpp.chatroom.UserRepository repo =
                    new de.uni_jena.fpp.chatroom.FileUserRepository(
                            de.uni_jena.fpp.chatroom.Config.getUsersFile(),
                            de.uni_jena.fpp.chatroom.Config.getPbkdf2Iterations()
                    );

            de.uni_jena.fpp.chatroom.ServerLogger logger =
                    new de.uni_jena.fpp.chatroom.ServerLogger(de.uni_jena.fpp.chatroom.Config.getServerLogFile());

            de.uni_jena.fpp.chatroom.ChatServer server =
                    new de.uni_jena.fpp.chatroom.ChatServer(port, repo, logger);

            javax.swing.SwingUtilities.invokeLater(() -> {
                ServerFrame f = new ServerFrame(server);
                f.initialize(f);
                f.setVisible(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Server GUI konnte nicht gestartet werden:\n" + e.getMessage(),
                    "Fehler",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }


}
