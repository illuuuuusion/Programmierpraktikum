import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ServerFrame extends MainFrame {

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


        // Server Panel
        JPanel serverPanel = new JPanel();
        //serverPanel.setBorder(new TitledBorder("Server verwalten"));
        serverPanel.setLayout(new GridLayout(1, 2));

        JButton btnStart = new JButton("Server starten");
        btnStart.setFont(mainFont);
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO start server
                System.out.println("Server gestartet");
            }
        });
        serverPanel.add(btnStart);

        JButton btnStop = new JButton("Server beenden");
        btnStop.setFont(mainFont);
        btnStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Stop Server
                System.out.println("Server beendet");
            }
        });
        serverPanel.add(btnStop);


        
        chatPanel.add(serverPanel);


        chatLayout.putConstraint(SpringLayout.NORTH, chatBox, 0, SpringLayout.NORTH, chatPanel);   
        
        chatLayout.putConstraint(SpringLayout.SOUTH, chatBox, -10, SpringLayout.NORTH, serverPanel);
        chatLayout.putConstraint(SpringLayout.WEST, chatBox, 0, SpringLayout.WEST, chatPanel);
        chatLayout.putConstraint(SpringLayout.EAST, chatBox, -5, SpringLayout.EAST, chatPanel);

        chatLayout.putConstraint(SpringLayout.SOUTH, serverPanel, 0, SpringLayout.SOUTH, chatPanel);
        chatLayout.putConstraint(SpringLayout.WEST, serverPanel, 0, SpringLayout.WEST, chatPanel);
        chatLayout.putConstraint(SpringLayout.EAST, serverPanel, 0, SpringLayout.EAST, chatPanel);

        

        

        // Rechte Seite
       

        // Gesamtes Panel
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayout(2, 1));

        // Räume
        JPanel roomPanel = new JPanel();
        roomPanel.setBorder(new TitledBorder("Räume"));
        roomPanel.setLayout(new GridLayout(1, 1 ));
        String[] rooms = {"klassenchat", "chat2"};
        JList roomList = new JList(rooms); // TODO get rooms array from server
        roomList.setFont(mainFont);
        roomList.setEnabled(false);
        roomPanel.add(roomList);
        panel1.add(roomPanel);

        // Nutzer
        JPanel userPanel = new JPanel();
        SpringLayout userPanelLayout = new SpringLayout();
        userPanel.setLayout(userPanelLayout);
        userPanel.setBorder(new TitledBorder("Nutzer"));

        JPanel pan = new JPanel();
        pan.setLayout(new GridLayout(1, 1 ));
        String[] userList = {"Anna", "Bernd", "Camilla"};
        JList lstUsers = new JList(userList); // TODO get list of active users from Server
        lstUsers.setFont(mainFont);
        pan.add(lstUsers);

        JButton btnKick = new JButton("Nutzer entfernen");
        btnKick.setFont(mainFont);
        btnKick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO kick user
                System.out.println(lstUsers.getSelectedValue() + " wurde gekickt.");
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







        

        // --------------- Room Layer ---------------
        // Räume
        /*
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

        */

        // FINAL ADDING

        split.add(chatPanel);
        split.add(panel1);
        mainPanel.add(split, BorderLayout.CENTER);
        add(mainPanel);

        setVisible(true);
    }

    public static void main(String[] args) {
        ServerFrame serverFrame = new ServerFrame();
        serverFrame.initialize(serverFrame);
    }
} 