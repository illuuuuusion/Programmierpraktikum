import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class CloseRoomFrame extends MainFrame {

    public void initialize(JFrame frame) {

        // --------------- Button -------------------------
        JButton btnConfirm = new JButton("sicher");
        btnConfirm.setFont(MainFrame.mainFont);
        btnConfirm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO tell server to close current room
                System.out.println("raum geschlossen");
                // TODO update the Chat border back to "Chat"
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
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        frame.pack();  
    }

    public static void main(String[] args) {
        CloseRoomFrame closeRoomFrame = new CloseRoomFrame();
        closeRoomFrame.initialize(closeRoomFrame);
    }
}
