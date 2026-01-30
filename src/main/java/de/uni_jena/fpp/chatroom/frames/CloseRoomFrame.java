package de.uni_jena.fpp.chatroom.frames;

import de.uni_jena.fpp.chatroom.ChatClient;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

import de.uni_jena.fpp.chatroom.ChatClient;
import java.io.IOException;



public class CloseRoomFrame extends MainFrame {

    private final ChatClient client;
    public CloseRoomFrame() {
        this(null);
    }
    public CloseRoomFrame(ChatClient client) {
        this.client = client;
    }

    public void initialize(JFrame frame) {

        // --------------- Button -------------------------
        JButton btnConfirm = new JButton("sicher");
        btnConfirm.setFont(MainFrame.mainFont);
        btnConfirm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //  Raum verlassen (Server löscht ggf. Raum automatisch wenn leer)
                if (client != null) {
                    try { client.leave(); } catch (IOException ex) { /* optional MessageFrame */ }
                }
                frame.dispose();

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
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(btnConfirm);
        buttonsPanel.add(btnCancel);

        // --------------- Label -------------------------
        JLabel lbConfirm = new JLabel("Möchten sie den Raum schließen?");

        // --------------- Main Panel -------------------------
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(mainColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        mainPanel.add(lbConfirm, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(mainPanel);

        setTitle("Meldung");
        setMinimumSize(new Dimension(380, 150));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // ✅ wichtig
        setVisible(true);
        frame.pack();
    }

    public static void main(String[] args) {
        CloseRoomFrame closeRoomFrame = new CloseRoomFrame();
        closeRoomFrame.initialize(closeRoomFrame);
    }
}
