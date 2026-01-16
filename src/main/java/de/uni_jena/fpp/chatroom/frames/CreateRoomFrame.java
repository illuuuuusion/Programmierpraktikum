package de.uni_jena.fpp.chatroom.frames;

import de.uni_jena.fpp.chatroom.ChatClient;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class CreateRoomFrame extends MainFrame {

    private final ChatClient client;

    public CreateRoomFrame() {
        this(null);
    }

    public CreateRoomFrame(ChatClient client) {
        this.client = client;
    }

    public void initialize(JFrame frame) {

        // --------------- Label + TextField -------------------------
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder("Raum erstellen"));
        JTextField tfRoomName = new JTextField("Name");
        panel.add(tfRoomName);

        // --------------- Button -------------------------
        JButton btnCreate = new JButton("erstellen");
        btnCreate.setFont(MainFrame.mainFont);
        btnCreate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String roomName = tfRoomName.getText().trim();

                // Minimal-Checks (Server prüft eh nochmal)
                if (roomName.isEmpty() || roomName.equalsIgnoreCase("Name")) {
                    MessageFrame msgFrame = new MessageFrame();
                    msgFrame.initialize(msgFrame, 2, 1); // "Invalider Raum Name."
                    return;
                }
                if (roomName.contains(" ") || roomName.contains("|")) {
                    MessageFrame msgFrame = new MessageFrame();
                    msgFrame.initialize(msgFrame, 2, 1);
                    return;
                }

                if (client == null) {
                    MessageFrame msgFrame = new MessageFrame();
                    msgFrame.initialize(msgFrame, 0, 0); // "Noch nicht implementiert."
                    return;
                }

                try {
                    client.createRoom(roomName);

                    // Wir können nicht 100% sicher wissen ob ok, weil Server INFO/ERROR async schickt.
                    // Für MVP: optimistisch -> Erfolgsmeldung + Fenster zu.
                    MessageFrame msgFrame = new MessageFrame();
                    msgFrame.initialize(msgFrame, 2, 0); // "Raum erfolgreich erstellt."
                    frame.dispose();

                } catch (IOException ex) {
                    // Bei Send-Problemen -> Fehler
                    MessageFrame msgFrame = new MessageFrame();
                    msgFrame.initialize(msgFrame, 2, 2); // "Raum Name existiert bereits." (oder allgemeiner fail)
                }
            }
        });

        JButton btnCancel = new JButton("abbrechen");
        btnCancel.setFont(MainFrame.mainFont);
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 2));
        buttonsPanel.add(btnCreate);
        buttonsPanel.add(btnCancel);

        // --------------- Main Panel -------------------------
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(mainColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(mainPanel);

        setTitle("Meldung");
        setMinimumSize(new Dimension(380, 150));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // ✅ wichtig
        setVisible(true);
        frame.pack();
    }

    public static void main(String[] args) {
        CreateRoomFrame createRoomFrame = new CreateRoomFrame();
        createRoomFrame.initialize(createRoomFrame);
    }
}
