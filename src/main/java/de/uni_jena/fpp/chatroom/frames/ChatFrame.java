package de.uni_jena.fpp.chatroom.frames;

import de.uni_jena.fpp.chatroom.ChatClient;
import de.uni_jena.fpp.chatroom.ChatClientListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.border.EmptyBorder;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class ChatFrame extends MainFrame {

    private final ChatClient client;

    // FIX 1: filesModel deklarieren
    private final DefaultListModel<String> filesModel = new DefaultListModel<>();
    private volatile String shownRoom = null;


    public ChatFrame() {
        this(null);
    }

    public ChatFrame(ChatClient client) {
        this.client = client;
    }

    public void initialize(JFrame frame) {
        // FIX 3: frame verwenden
        frame.setSize(900, 500);
        frame.setLocation(750, 320);
        frame.setTitle("Chat Client - " + client.getUsername());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().removeAll();

        // --------------- TOP Layer ---------------
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(mainColor);
        mainPanel.setFont(mainFont);
        mainPanel.setOpaque(true);
        mainPanel.setBorder(new TitledBorder("Chat Client"));

        JPanel split = new JPanel(new GridLayout(1, 2));

        // --------------- Chat Layer ---------------
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(new TitledBorder("Chat"));
        chatPanel.setBackground(mainColor);
        chatPanel.setOpaque(true);

        // FIX 2: Swing JTextArea statt AWT TextArea
        JTextArea chatBox = new JTextArea();
        chatBox.setEditable(false);
        chatBox.setFont(mainFont);
        chatBox.setLineWrap(true);
        chatBox.setWrapStyleWord(true);

        JScrollPane chatScroll = new JScrollPane(chatBox);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel msgPanel = new JPanel(new GridLayout(1, 2));
        msgPanel.setBorder(new TitledBorder("Nachricht"));

        JTextField tfMessage = new JTextField();
        tfMessage.setFont(mainFont);
        msgPanel.add(tfMessage);

        JButton btnSend = new JButton("Senden");
        btnSend.setFont(mainFont);
        btnSend.addActionListener((ActionEvent e) -> {
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
        });
        msgPanel.add(btnSend);

        tfMessage.addActionListener(e -> btnSend.doClick());
        chatPanel.add(msgPanel, BorderLayout.SOUTH);

        // --------------- Room Layer ---------------
        JPanel roomPanel1 = new JPanel(new GridLayout(2, 1));
        roomPanel1.setBorder(new TitledBorder("Räume"));
        roomPanel1.setBackground(mainColor);
        roomPanel1.setOpaque(true);

        JPanel roomPanel2 = new JPanel(new GridLayout(1, 2));
        roomPanel2.setBorder(new TitledBorder("verwalten"));

        DefaultListModel<String> roomsModel = new DefaultListModel<>();
        JList<String> roomList = new JList<>(roomsModel);
        roomList.setFont(mainFont);

        JPanel roomPanel4 = new JPanel(new GridLayout(3, 1));

        JButton btnCreate = new JButton("Raum erstellen");
        btnCreate.setFont(mainFont);
        btnCreate.addActionListener(e -> {
            CreateRoomFrame roomFrame = new CreateRoomFrame(client);
            roomFrame.initialize(roomFrame);
        });

        JButton btnJoin = new JButton("Raum beitreten");
        btnJoin.setFont(mainFont);
        btnJoin.addActionListener(e -> {
            if (client == null) {
                chatBox.append("[ERROR] Kein Client verbunden.\n");
                return;
            }

            if (roomList.isSelectionEmpty()) {
                chatBox.append("[INFO] Wähle einen Chat-Raum aus!\n");
                return;
            }

            String room = roomList.getSelectedValue();
            try {
                String cur = client.getModel().getCurrentRoom();
                if (cur != null && cur.equals(room)) {
                    chatBox.append("[INFO] Du bist schon in diesem Raum.\n");
                    return;
                }

                client.join(room);
                chatPanel.setBorder(new TitledBorder(room));
                chatPanel.revalidate();
                chatPanel.repaint();

                chatBox.append("[INFO] Raum " + room + " beigetreten.\n");
                chatBox.setCaretPosition(chatBox.getDocument().getLength());
            } catch (IOException ex) {
                chatBox.append("[ERROR] " + ex.getMessage() + "\n");
            }
        });

        JButton btnLeave = new JButton("Raum verlassen");
        btnLeave.setFont(mainFont);
        btnLeave.addActionListener(e -> {
            if (client == null) {
                chatBox.append("[ERROR] Kein Client verbunden.\n");
                return;
            }
            CloseRoomFrame closeRoomFrame = new CloseRoomFrame(client);
            closeRoomFrame.initialize(closeRoomFrame);
        });

        roomPanel4.add(btnCreate);
        roomPanel4.add(btnJoin);
        roomPanel4.add(btnLeave);

        roomPanel2.add(new JScrollPane(roomList));
        roomPanel2.add(roomPanel4);

        // --------------- Users + Files ---------------
        JPanel roomPanel3 = new JPanel(new GridLayout(1, 2));

        JPanel users = new JPanel(new GridLayout(1, 1));
        users.setBorder(new TitledBorder("Nutzer im Raum"));

        DefaultListModel<String> usersModel = new DefaultListModel<>();
        JList<String> lstUsers = new JList<>(usersModel);
        lstUsers.setEnabled(false);
        lstUsers.setFont(mainFont);

        users.add(new JScrollPane(lstUsers));

        // Dateien (Liste + Buttons)
        JPanel roomPanel5 = new JPanel(new BorderLayout());
        roomPanel5.setBorder(new TitledBorder("Dateien"));

        JList<String> lstFiles = new JList<>(filesModel);
        lstFiles.setFont(mainFont);

        JScrollPane filesScroll = new JScrollPane(lstFiles);
        filesScroll.setVisible(false);         // Start: unsichtbar
        roomPanel5.add(filesScroll, BorderLayout.CENTER);

        JPanel fileButtons = new JPanel(new GridLayout(3, 1));
        JToggleButton btnShowFiles = new JToggleButton("Dateien anzeigen");
        btnShowFiles.setFont(mainFont);

        btnShowFiles.addActionListener(e -> {
            if (client == null) {
                chatBox.append("[ERROR] Kein Client verbunden.\n");
                return;
            }

            String room = client.getModel().getCurrentRoom();
            if (room == null || room.isBlank()) {
                chatBox.append("[ERROR] Kein aktueller Raum.\n");
                btnShowFiles.setSelected(false);
                return;
            }

            boolean show = btnShowFiles.isSelected();
            filesScroll.setVisible(show);

            if (show) {
                filesModel.clear();
                try {
                    client.listFiles(room);
                } catch (IOException ex) {
                    chatBox.append("[ERROR] FILES: " + ex.getMessage() + "\n");
                }
            } else {
                filesModel.clear();
            }

            roomPanel5.revalidate();
            roomPanel5.repaint();
        });

        JButton btnUpload = new JButton("Datei hochladen");
        btnUpload.setFont(mainFont);
        btnUpload.addActionListener(e -> {
            if (client == null) {
                chatBox.append("[ERROR] Kein Client verbunden.\n");
                return;
            }

            String room = client.getModel().getCurrentRoom();
            if (room == null || room.isBlank()) {
                chatBox.append("[ERROR] Kein aktueller Raum.\n");
                return;
            }

            JFileChooser fc = new JFileChooser();
            int res = fc.showOpenDialog(frame);
            if (res != JFileChooser.APPROVE_OPTION) return;

            File f = fc.getSelectedFile();
            if (f == null) return;

            btnUpload.setEnabled(false);

            new Thread(() -> {
                try {
                    client.uploadFile(room, f.toPath());
                    if (btnShowFiles.isSelected()) {
                        client.listFiles(room);
                    }
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() ->
                            chatBox.append("[ERROR] Upload: " + ex.getMessage() + "\n"));
                } finally {
                    SwingUtilities.invokeLater(() -> btnUpload.setEnabled(true));
                }
            }, "UploadThread").start();
        });

        JButton btnDownload = new JButton("Datei herunterladen");
        btnDownload.setFont(mainFont);
        btnDownload.addActionListener(e -> {
            if (client == null) {
                chatBox.append("[ERROR] Kein Client verbunden.\n");
                return;
            }

            String room = client.getModel().getCurrentRoom();
            if (room == null || room.isBlank()) {
                chatBox.append("[ERROR] Kein aktueller Raum.\n");
                return;
            }

            String selected = lstFiles.getSelectedValue();
            if (selected == null || selected.isBlank()) {
                String input = JOptionPane.showInputDialog(frame, "Dateiname:", "Download", JOptionPane.PLAIN_MESSAGE);
                if (input == null) return;
                selected = input.trim();
                if (selected.isEmpty()) return;
            }

            try {
                client.downloadFile(room, selected);
            } catch (IOException ex) {
                chatBox.append("[ERROR] DOWNLOAD: " + ex.getMessage() + "\n");
            }
        });

        fileButtons.add(btnUpload);
        fileButtons.add(btnDownload);
        fileButtons.add(btnShowFiles);
        roomPanel5.add(fileButtons, BorderLayout.SOUTH);

        roomPanel3.add(users);
        roomPanel3.add(roomPanel5);

        roomPanel1.add(roomPanel2);
        roomPanel1.add(roomPanel3);

        split.add(chatPanel);
        split.add(roomPanel1);

        mainPanel.add(split, BorderLayout.CENTER);

        frame.getContentPane().add(mainPanel);
        frame.setVisible(true);

        // Listener anbinden
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
                        String cur = client.getModel().getCurrentRoom();
                        if (cur == null || !cur.equals(room)) return;

                        // Wenn der aktuell angezeigte Raum wechselt -> Chatfenster komplett leeren
                        if (shownRoom == null || !shownRoom.equals(room)) {
                            shownRoom = room;

                            chatBox.setText("");
                            chatBox.append("[INFO] Raum: " + room + "\n");
                            chatBox.setCaretPosition(chatBox.getDocument().getLength());

                            btnShowFiles.setSelected(false);
                            filesScroll.setVisible(false);
                            filesModel.clear();
                            roomPanel5.revalidate();
                            roomPanel5.repaint();
                        }

                        usersModel.clear();
                        for (String u : users) usersModel.addElement(u);
                    });
                }


                @Override
                public void onFileList(String room, List<String> files) {
                    SwingUtilities.invokeLater(() -> {
                        String cur = client.getModel().getCurrentRoom();
                        if (cur == null || !cur.equals(room)) return;

                        filesModel.clear();
                        for (String f : files) filesModel.addElement(f);
                    });
                }

                @Override
                public void onChatMessage(String room, String from, String text) {
                    SwingUtilities.invokeLater(() -> {
                        String cur = client.getModel().getCurrentRoom();
                        if (cur == null || !cur.equals(room)) return;
                        chatBox.append("[" + from + "] " + text + "\n");
                        chatBox.setCaretPosition(chatBox.getDocument().getLength());
                    });
                }


                @Override
                public void onInfo(String text) {
                    SwingUtilities.invokeLater(() -> {
                        chatBox.append("[INFO] " + text + "\n");
                        chatBox.setCaretPosition(chatBox.getDocument().getLength());
                    });
                }

                @Override
                public void onWarn(String text) {
                    SwingUtilities.invokeLater(() -> {
                        chatBox.append("[WARN] " + text + "\n");
                        chatBox.setCaretPosition(chatBox.getDocument().getLength());

                        showWarnDialog(frame, text);
                    });
                }


                @Override
                public void onError(String text) {
                    SwingUtilities.invokeLater(() -> {
                        chatBox.append("[ERROR] " + text + "\n");
                        chatBox.setCaretPosition(chatBox.getDocument().getLength());
                    });
                }

                @Override
                public void onBanned(String reason) {
                    SwingUtilities.invokeLater(() -> {
                        chatBox.append("[BANNED] " + reason + "\n");
                        JOptionPane.showMessageDialog(frame,
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

            roomsModel.clear();
            for (String r : client.getModel().getRooms()) roomsModel.addElement(r);
            new Thread(() -> {
                try {
                    String room = client.getModel().getCurrentRoom();

                    if (room == null || room.isBlank()) {
                        room = client.getModel().getRooms().stream()
                                .filter(r -> r != null && r.equalsIgnoreCase("Lobby"))
                                .findFirst()
                                .orElseGet(() -> client.getModel().getRooms().isEmpty()
                                        ? null
                                        : client.getModel().getRooms().get(0));
                    }

                    if (room == null || room.isBlank()) {
                        SwingUtilities.invokeLater(() ->
                                chatBox.append("[ERROR] Kein initialer Raum gefunden.\n"));
                        return;
                    }
                    client.join(room);

                    final String finalRoom = room;
                    SwingUtilities.invokeLater(() -> {
                        chatPanel.setBorder(new TitledBorder(finalRoom));
                        SwingUtilities.invokeLater(() -> {
                            btnShowFiles.setSelected(false);
                            filesScroll.setVisible(false);
                            filesModel.clear();
                            roomPanel5.revalidate();
                            roomPanel5.repaint();
                        });
                        chatBox.append("[INFO] Initialer Raum: " + finalRoom + "\n");
                    });

                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() ->
                            chatBox.append("[ERROR] Init-Room: " + ex.getMessage() + "\n"));
                }
            }, "InitRoomThread").start();

            usersModel.clear();
            for (String u : client.getModel().getUsersInCurrentRoom()) usersModel.addElement(u);
        } else {
            chatBox.append("[WARN] ChatFrame ohne Client gestartet (nur UI Vorschau).\n");
        }
    }
    private void showWarnDialog(JFrame parent, String reason) {
        final JDialog dialog = new JDialog(parent, "Verwarnung", true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Toolkit.getDefaultToolkit().beep();
            }
        });

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Kopfzeile mit Icon
        JLabel header = new JLabel("Du wurdest vom Server verwarnt.");
        header.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 15f));

        JLabel lblReason = new JLabel("Verwarngrund:");
        lblReason.setFont(lblReason.getFont().deriveFont(Font.BOLD));

        JTextArea ta = new JTextArea(reason == null ? "" : reason);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(420, 140));

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dialog.dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(ok);

        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.add(lblReason, BorderLayout.NORTH);
        center.add(sp, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }



    public static void main(String[] args) {
        ChatFrame chatFrame = new ChatFrame();
        chatFrame.initialize(chatFrame);
    }
}
