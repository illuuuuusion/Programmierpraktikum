import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ChatFrame extends MainFrame {

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
        // TODO get updates from server 


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
                // TODO Send message
                System.out.println("sent message: " + tfMessage.getText());
            }
        });
        msgPanel.add(btnSend);


        
        chatPanel.add(msgPanel);


        chatLayout.putConstraint(SpringLayout.NORTH, chatBox, 0, SpringLayout.NORTH, chatPanel);   
        
        chatLayout.putConstraint(SpringLayout.SOUTH, chatBox, -30, SpringLayout.NORTH, msgPanel);
        chatLayout.putConstraint(SpringLayout.WEST, chatBox, 0, SpringLayout.WEST, chatPanel);
        chatLayout.putConstraint(SpringLayout.EAST, chatBox, -10, SpringLayout.EAST, chatPanel);

        chatLayout.putConstraint(SpringLayout.SOUTH, msgPanel, 0, SpringLayout.SOUTH, chatPanel);
        chatLayout.putConstraint(SpringLayout.WEST, msgPanel, 0, SpringLayout.WEST, chatPanel);
        chatLayout.putConstraint(SpringLayout.EAST, msgPanel, 0, SpringLayout.EAST, chatPanel);

        
        

        // --------------- Room Layer ---------------
        // Räume
        JPanel roomPanel1 = new JPanel();
        roomPanel1.setBorder(new TitledBorder("Rooms"));
        roomPanel1.setLayout(new GridLayout(2, 1));
        roomPanel1.setBackground(mainColor);
        roomPanel1.setOpaque(true);


        JPanel roomPanel2 = new JPanel();
        roomPanel2.setBorder(new TitledBorder("verwalten"));
        roomPanel2.setLayout(new GridLayout(1, 2));


        String[] rooms = {"klassenchat", "chat2"};
        JList roomList = new JList(rooms); // TODO get rooms array from server
        roomList.setFont(mainFont);
    

        JPanel roomPanel4 = new JPanel();
        roomPanel4.setLayout(new GridLayout(3, 1));

        JButton btnCreate = new JButton("Raum erstellen");
        btnCreate.setFont(mainFont);
        btnCreate.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CreateRoomFrame roomFrame = new CreateRoomFrame();
                roomFrame.initialize(roomFrame);
            }
            
        });
        JButton btnJoin = new JButton("Raum beitreten");
        btnJoin.setFont(mainFont);
        btnJoin.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO join room
                if (roomList.isSelectionEmpty()) {
                    System.out.println("Wähle einen Chat-Raum aus!");
                }
                else {
                    System.out.println("Raum " + roomList.getSelectedValue() + " beigetreten.");
                    chatPanel.setBorder(new TitledBorder((String) roomList.getSelectedValue()));
                }
            }
            
        });
        JButton btnLeave = new JButton("Raum verlassen");
        btnLeave.setFont(mainFont);
        btnLeave.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO send leave room request to server
                
                // TODO if last user in room get message from server "are you sure you want to close room"?
                CloseRoomFrame closeRoomFrame = new CloseRoomFrame();
                closeRoomFrame.initialize(closeRoomFrame);
            }
            
        });
        roomPanel4.add(btnCreate);
        roomPanel4.add(btnJoin);
        roomPanel4.add(btnLeave);


        roomPanel2.add(roomList);
        roomPanel2.add(roomPanel4);

        
        // online users + dateien
        JPanel roomPanel3 = new JPanel();
        roomPanel3.setLayout(new GridLayout(1, 2));

        JPanel users = new JPanel();
        users.setBorder(new TitledBorder("Nutzer im Raum"));
        users.setLayout(new GridLayout(1, 1));

        String[] userList = {"Anna", "Bernd", "Camilla"};
        JList lstUsers = new JList(userList); // TODO get list of active users from Server
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

        users.add(lstUsers);
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
    }

    public static void main(String[] args) {
        ChatFrame chatFrame = new ChatFrame();
        chatFrame.initialize(chatFrame);
    }
} 