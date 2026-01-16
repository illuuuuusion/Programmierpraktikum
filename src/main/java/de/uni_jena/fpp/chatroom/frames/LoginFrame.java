import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class LoginFrame extends MainFrame {
    JTextField tfName;
    JPasswordField tfPassword;
    JLabel lbWelcome;
  

    public void initialize(JFrame frame) {

        // --------------- Form Panel -------------------------
        JLabel lbName = new JLabel("Benutzername:");
        lbName.setFont(mainFont);

        tfName = new JTextField();
        tfName.setBounds(10, 10, 106 , 21); // TODO get the textfields to fixed height
        tfName.setFont(mainFont);



        JLabel lbPassword = new JLabel("Passwort:");
        lbPassword.setFont(mainFont);

        tfPassword = new JPasswordField();
        tfPassword.setBounds(10, 10, 106 , 21); // TODO get the textfields to fixed height
        tfPassword.setFont(mainFont);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(2, 2, 5, 5));
        formPanel.setOpaque(false);
        formPanel.add(lbName);
        formPanel.add(tfName);
        formPanel.add(lbPassword);
        formPanel.add(tfPassword);

        // --------------- Anmeldung Label -------------------------
        lbWelcome = new JLabel();
        lbWelcome.setText("Chat Anmeldung");
        lbWelcome.setFont(mainFont);


        // --------------- Buttons Panel -------------------------
        // Login 
        JButton btnLogin = new JButton("Anmeldung");
        btnLogin.setFont(mainFont);
        btnLogin.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // login
                // TODO send login request to server
                if (true) {
                    // TODO open Chat window
                    ChatFrame chatFrame = new ChatFrame();
                    chatFrame.main(null);
                    frame.dispose();
                }
            }
        });

        // Register
        JButton btnRegister = new JButton("Registrierung");
        btnRegister.setFont(mainFont);
        btnRegister.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // register
                // TODO send register request to server
                MessageFrame msgFrame = new MessageFrame();
                msgFrame.initialize(msgFrame, 1, 0);
            }
            
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 2, 5, 5));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(btnLogin);
        buttonsPanel.add(btnRegister);


        // --------------- Main Panel -------------------------
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(mainColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 30, 50, 30));

  
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(lbWelcome, BorderLayout.NORTH);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(mainPanel);

        setTitle("Chat-Client");
        setMinimumSize(new Dimension(380, 280));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        frame.pack(); // bringt das fenster auf minimale größe sodass alle components rein passen, nutz setSize() für ein spezifsche größe

    }


    public static void main(String[] args) {
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.initialize(loginFrame);
    }
}