import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class CreateRoomFrame extends MainFrame {

    public void initialize(JFrame frame) {

        // --------------- Button -------------------------
        JButton btnCreate = new JButton("erstellen");
        btnCreate.setFont(MainFrame.mainFont);
        btnCreate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (true) { // TODO check if RoomName is valid, "RoomName.valid() == true"
                    if (false) { // TODO check if RoomName is taken, "RoomName.not_taken() == true"
                        MessageFrame msgFrame = new MessageFrame();
                        msgFrame.initialize(msgFrame, 2, 0);
                    }
                    else {
                        MessageFrame msgFrame = new MessageFrame();
                        msgFrame.initialize(msgFrame, 2, 2);
                    }
                }
                else {
                    MessageFrame msgFrame = new MessageFrame();
                    msgFrame.initialize(msgFrame, 2, 1);
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


        // --------------- Label + TextField -------------------------
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder("Raum erstellen"));
        JTextField tfRoomName = new JTextField("Name");
        panel.add(tfRoomName);



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
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        frame.pack();  
    }

    public static void main(String[] args) {
        CreateRoomFrame createRoomFrame = new CreateRoomFrame();
        createRoomFrame.initialize(createRoomFrame);
    }
}
