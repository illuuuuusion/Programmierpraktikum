package de.uni_jena.fpp.chatroom.frames;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MessageFrame extends MainFrame {
    String[][] message =    {{"Noch nicht implementiert."},
            {"Registrierung erfolgreich.", "Registrierung fehlgeschlagen. Invalide Nutzerdaten", "Registrierung fehlgeschlagen. Nutzerdaten bereits vergeben."},
            {"Raum erfolgreich erstellt.", "Invalider Raum Name.", "Raum Name existiert bereits."}};

    public void initialize(JFrame frame, int x, int y) {

        System.out.println(message[x][y]);

        // --------------- Button -------------------------
        JButton btnOK = new JButton("OK");
        btnOK.setFont(MainFrame.mainFont);
        btnOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(btnOK);

        // --------------- Error Message -------------------------
        JLabel lbMessage = new JLabel(message[x][y]);
        lbMessage.setFont(mainFont);

        // --------------- Main Panel -------------------------
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(mainColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        mainPanel.add(lbMessage, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH); //  nutzt buttonsPanel

        add(mainPanel);

        setTitle("Meldung");
        setMinimumSize(new Dimension(380, 150));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //  wichtig
        setVisible(true);
        frame.pack();
    }

    public static void main(String[] args) {
        MessageFrame messageFrame = new MessageFrame();
        messageFrame.initialize(messageFrame, 0, 0);
    }
}
