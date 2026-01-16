package de.uni_jena.fpp.chatroom.frames;

import de.uni_jena.fpp.chatroom.ChatClient;
import de.uni_jena.fpp.chatroom.ChatClientListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ChatFrame extends MainFrame {

    private final ChatClient client;

    public ChatFrame() {
        this(null);
    }

    public ChatFrame(ChatClient client) {
        this.client = client;
    }

    public void initialize(JFrame frame) {
        setSize(900, 500);
        setLocation(750, 320);
        setTitle("Chat Client");

        // --------------- TOP Layer ---------------
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(mainColor);
        mainPanel.setFont(mainFont);
        mainPanel.setOpaque(true);
        mainPanel.setBorder(new TitledBorder("Chat Client"));
        mainPanel.setLayout(new BorderLayout());

        JPanel split = new JPanel();
        split.setLayout(new GridLayout(1,2));

        // --------------- Chat Layer ---------------

        // Chat Panel
        JPanel chatPanel = new JPanel();
        chatPanel.setBorder(new TitledBorder("Chat"));
        chatPanel.setBackground(mainColor);
        chatPanel.setOpaque(true);
        SpringLayout chatLayout = new SpringLayout();
        chatPanel.setLayout(chatLayout);

        TextArea chatBox = new TextArea();
        chatPanel.add(chatBox);

        // Message Panel
        JPanel msgPanel = new JPanel();
        msgPanel.setBorder(new TitledBorder("Message"));
        msgPanel.setLayout(new GridLayout(1, 2));

        JTextField tfMessage = new JTextField();
        tfMessage.setFont(mainFont);
        msgPanel.add(tfMessage);

        JButton btnSend = new JButton("Send");
        btnSend.setFont(mainFont);
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Send message to server
                if (client == null) {
                    chatBox.append("[ERROR] Kein Client verbunden.\n");
                    return;
                }
                String text = tfMessage.getText().trim();
                if (text.isEmpty()) return;

                try {
                    client.sendMessage(text);
                    tfMessage.setText("");
                } catch (IOException ex) {
                    chatBox.append("[ERROR] " + ex.getMessage() + "\n");
                }
            }
        });
        msgPanel.add(btnSend);

        // Enter im Textfeld = senden
        tfMessage.addActionListener(e -> btnSend.doClick());

        chatPanel.add(msgPanel);

        chatLayout.putConstraint(SpringLayout.NORTH, chatBox, 0, SpringLayout.NORTH, chatPanel);
        chatLayout.putConstraint(SpringLayout.SOUTH, chatBox, -30, SpringLayout.NORTH, msgPanel);
        chatLayout.putConstraint(SpringLayout.WEST, chatBox, 0, SpringLayout.WEST, chatPanel);
        chatLayout.putConstraint(SpringLayout.EAST, chatBox, -10, SpringLayout.EAST, chatPanel);

        chatLayout.putConstraint(SpringLayout.SOUTH, msgPanel, 0, SpringLayout.SOUTH, chatPanel);
        chatLayout.putConstraint(SpringLayout.WEST, msgPanel, 0, SpringLayout.WEST, chatPanel);
        chatLayout.putConstraint(SpringLayout.EAST, msgPanel, 0, SpringLayout.EAST, chatPanel);

        // --------------- Room Layer ---------------
        JPanel roomPanel1 = new JPanel();
        roomPanel1.setBorder(new TitledBorder("Rooms"));
        roomPanel1.setLayout(new GridLayout(2, 1));
        roomPanel1.setBackground(mainColor);
        roomPanel1.setOpaque(true);

        JPanel roomPanel2 = new JPanel();
        roomPanel2.setBorder(new TitledBorder("verwalten"));
        roomPanel2.setLayout(new GridLayout(1, 2));

        // Rooms Model statt Dummy Array
        DefaultListModel<String> roomsModel = new DefaultListModel<>();
        JList<String> roomList = new JList<>(roomsModel);
        roomList.setFont(mainFont);

        JPanel roomPanel4 = new JPanel();
        roomPanel4.setLayout(new GridLayout(3, 1));

        JButton btnCreate = new JButton("Raum erstellen");
        btnCreate.setFont(mainFont);
        btnCreate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Minimal: Frame wie gehabt öffnen (noch nicht angebunden)
                CreateRoomFrame roomFrame = new CreateRoomFrame(client);
                roomFrame.initialize(roomFrame);

            }
        });

        JButton btnJoin = new JButton("Raum beitreten");
        btnJoin.setFont(mainFont);
        btnJoin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (client == null) {
                    chatBox.append("[ERROR] Kein Client verbunden.\n");
                    return;
                }

                if (roomList.isSelectionEmpty()) {
                    chatBox.append("[INFO] Wähle einen Chat-Raum aus!\n");
                } else {
                    String room = roomList.getSelectedValue();
                    try {
                        String cur = client.getModel().getCurrentRoom();
                        if (cur != null && cur.equals(room)) {
                            chatBox.append("[INFO] Du bist schon in diesem Raum.\n");
                            return;
                        }

                        client.join(room);
                        chatPanel.setBorder(new TitledBorder(room));
                        chatBox.append("[INFO] Raum " + room + " beigetreten.\n");
                    } catch (IOException ex) {
                        chatBox.append("[ERROR] " + ex.getMessage() + "\n");
                    }
                }
            }
        });

        JButton btnLeave = new JButton("Raum verlassen");
        btnLeave.setFont(mainFont);
        btnLeave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (client == null) {
                    chatBox.append("[ERROR] Kein Client verbunden.\n");
                    return;
                }

                // nur Dialog öffnen, NICHT sofort leave senden
                CloseRoomFrame closeRoomFrame = new CloseRoomFrame(client);
                closeRoomFrame.initialize(closeRoomFrame);
            }
        });


        roomPanel4.add(btnCreate);
        roomPanel4.add(btnJoin);
        roomPanel4.add(btnLeave);

        roomPanel2.add(new JScrollPane(roomList));
        roomPanel2.add(roomPanel4);

        // online users + dateien
        JPanel roomPanel3 = new JPanel();
        roomPanel3.setLayout(new GridLayout(1, 2));

        JPanel users = new JPanel();
        users.setBorder(new TitledBorder("Nutzer im Raum"));
        users.setLayout(new GridLayout(1, 1));

        // Users Model statt Dummy Array
        DefaultListModel<String> usersModel = new DefaultListModel<>();
        JList<String> lstUsers = new JList<>(usersModel);
        lstUsers.setEnabled(false);
        lstUsers.setFont(mainFont);

        JPanel roomPanel5 = new JPanel();
        roomPanel5.setBorder(new TitledBorder("dateien"));
        roomPanel5.setLayout(new GridLayout(3, 1));

        JButton btnUpload = new JButton("Datei hochladen");
        btnUpload.setFont(mainFont);
        btnUpload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageFrame messageFrame = new MessageFrame();
                messageFrame.initialize(messageFrame, 0, 0);
            }
        });

        JButton btnShow = new JButton("Datei anzeigen");
        btnShow.setFont(mainFont);
        btnShow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageFrame messageFrame = new MessageFrame();
                messageFrame.initialize(messageFrame, 0, 0);
            }
        });

        JButton btnDownload = new JButton("Datei herunterladen");
        btnDownload.setFont(mainFont);
        btnDownload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageFrame messageFrame = new MessageFrame();
                messageFrame.initialize(messageFrame, 0, 0);
            }
        });

        roomPanel5.add(btnUpload);
        roomPanel5.add(btnShow);
        roomPanel5.add(btnDownload);

        users.add(new JScrollPane(lstUsers));
        roomPanel3.add(users);
        roomPanel3.add(roomPanel5);

        roomPanel1.add(roomPanel2);
        roomPanel1.add(roomPanel3);

        // FINAL ADDING
        split.add(chatPanel);
        split.add(roomPanel1);
        mainPanel.add(split, BorderLayout.CENTER);
        add(mainPanel);

        setVisible(true);

        // Listener anbinden (Server -> UI Updates)
        if (client != null) {
            client.addListener(new ChatClientListener() {
                @Override
                public void onRoomsUpdated(List<String> rooms) {
                    SwingUtilities.invokeLater(() -> {
                        roomsModel.clear();
                        for (String r : rooms) roomsModel.addElement(r);
                    });
                }

                @Override
                public void onUsersUpdated(String room, List<String> users) {
                    SwingUtilities.invokeLater(() -> {
                        usersModel.clear();
                        for (String u : users) usersModel.addElement(u);
                    });
                }

                @Override
                public void onChatMessage(String room, String from, String text) {
                    SwingUtilities.invokeLater(() -> chatBox.append("[" + room + "][" + from + "] " + text + "\n"));
                }

                @Override
                public void onInfo(String text) {
                    SwingUtilities.invokeLater(() -> {
                        // Nicht jedes Protokoll-Info muss im Chat stehen, aber fürs Debug ok
                        chatBox.append("[INFO] " + text + "\n");
                    });
                }

                @Override
                public void onWarn(String text) {
                    SwingUtilities.invokeLater(() -> chatBox.append("[WARN] " + text + "\n"));
                }

                @Override
                public void onError(String text) {
                    SwingUtilities.invokeLater(() -> chatBox.append("[ERROR] " + text + "\n"));
                }

                @Override
                public void onBanned(String reason) {
                    SwingUtilities.invokeLater(() -> {
                        chatBox.append("[BANNED] " + reason + "\n");
                        JOptionPane.showMessageDialog(ChatFrame.this,
                                "Du wurdest gebannt: " + reason,
                                "BANNED", JOptionPane.ERROR_MESSAGE);
                        frame.dispose();
                    });
                }

                @Override
                public void onConnectionClosed() {
                    SwingUtilities.invokeLater(() -> chatBox.append("[INFO] Verbindung geschlossen.\n"));
                }
            });

            // initialer State (falls schon Daten da sind)
            roomsModel.clear();
            for (String r : client.getModel().getRooms()) roomsModel.addElement(r);

            usersModel.clear();
            for (String u : client.getModel().getUsersInCurrentRoom()) usersModel.addElement(u);
        } else {
            chatBox.append("[WARN] ChatFrame ohne Client gestartet (nur UI Vorschau).\n");
        }
    }

    public static void main(String[] args) {
        ChatFrame chatFrame = new ChatFrame();
        chatFrame.initialize(chatFrame);
    }
}
