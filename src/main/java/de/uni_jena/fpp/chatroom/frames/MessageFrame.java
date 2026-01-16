import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MessageFrame extends MainFrame {
    String[][] message =    {{"Noch nicht implementiert."},
                            {"Registrierung erfolgreich.", "Registrierung fehlgeschlagen. Invalide Nutzerdaten", "Registrierung fehlgeschlagen. Nutzerdaten bereits vergeben."},
                            {"Raum erfolgreich erstellt.", "Invalider Raum Name.", "Raum Name existiert bereits."}};

    public void initialize(JFrame frame, int x, int y) {

        /* Message Types
        00 - noch nicht implementiert

        10 - Registrierung erfolgreich 
        11 - Registrierung fehlgeschlagen, invalide Nutzerdaten
        12 - Registrierung fehlgeschlagen, Nutzerdaten bereits vergeben

        20 - Raum erfolgreich erstellt
        21 - Raum konnte nicht erstellt werden, invalider Name
        22 - Raum konnte nicht erstellt werden, existiert bereits
        
        */

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
        mainPanel.add(btnOK, BorderLayout.SOUTH);

        add(mainPanel);

        setTitle("Meldung");
        setMinimumSize(new Dimension(380, 150));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        frame.pack();  
    }

    public static void main(String[] args) {
        MessageFrame messageFrame = new MessageFrame();
        messageFrame.initialize(messageFrame, 0, 0);
    }
}
