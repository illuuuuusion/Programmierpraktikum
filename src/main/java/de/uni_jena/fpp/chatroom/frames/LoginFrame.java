package de.uni_jena.fpp.chatroom.frames;

import de.uni_jena.fpp.chatroom.ChatClient;
import de.uni_jena.fpp.chatroom.ChatClientListener;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

public class LoginFrame extends MainFrame {
    JTextField tfName;
    JPasswordField tfPassword;
    JLabel lbWelcome;

    private final ChatClient client;
    private boolean listenerBound = false;

    public LoginFrame() {
        this(null);
    }

    public LoginFrame(ChatClient client) {
        this.client = client;
    }

    public void initialize(JFrame frame) {

        // --------------- Form Panel -------------------------
        JLabel lbName = new JLabel("Benutzername:");
        lbName.setFont(mainFont);

        tfName = new JTextField();
        tfName.setBounds(10, 10, 106 , 21);
        tfName.setFont(mainFont);

        JLabel lbPassword = new JLabel("Passwort:");
        lbPassword.setFont(mainFont);

        tfPassword = new JPasswordField();
        tfPassword.setBounds(10, 10, 106 , 21);
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
        JButton btnLogin = new JButton("Anmeldung");
        btnLogin.setFont(mainFont);
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin(frame);
            }
        });

        JButton btnRegister = new JButton("Registrierung");
        btnRegister.setFont(mainFont);
        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doRegister();
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 2, 5, 5));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(btnLogin);
        buttonsPanel.add(btnRegister);

        // Enter im Passwortfeld = Login
        tfPassword.addActionListener(e -> doLogin(frame));

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
        frame.pack();

        // Listener nur 1x binden
        if (client != null && !listenerBound) {
            bindClientListener(frame);
            listenerBound = true;
        }
    }

    private void doLogin(JFrame frame) {
        if (client == null) {
            JOptionPane.showMessageDialog(this, "Kein Client verbunden (GUI nicht über GuiClientMain gestartet).",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String u = tfName.getText().trim();
        String p = new String(tfPassword.getPassword());

        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte Benutzername und Passwort eingeben.",
                    "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            client.login(u, p);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRegister() {
        if (client == null) {
            JOptionPane.showMessageDialog(this, "Kein Client verbunden (GUI nicht über GuiClientMain gestartet).",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String u = tfName.getText().trim();
        String p = new String(tfPassword.getPassword());

        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte Benutzername und Passwort eingeben.",
                    "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            client.register(u, p);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Registrierung Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bindClientListener(JFrame frame) {
        client.addListener(new ChatClientListener() {

            @Override
            public void onInfo(String text) {
                // Kommt aus Netzwerk-Thread -> UI Thread!
                SwingUtilities.invokeLater(() -> {
                    if (text == null) return;

                    // LOGIN OK -> Chat öffnen
                    if (text.equals("LOGIN_OK")) {
                        ChatFrame chatFrame = new ChatFrame(client);
                        chatFrame.initialize(chatFrame);
                        frame.dispose();
                        return;
                    }

                    // LOGIN FAILED
                    if (text.startsWith("LOGIN_FAILED")) {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Login fehlgeschlagen: " + text,
                                "Login", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // REGISTER OK
                    if (text.equals("REGISTER_OK")) {
                        MessageFrame msgFrame = new MessageFrame();
                        msgFrame.initialize(msgFrame, 1, 0);
                        return;
                    }

                    // REGISTER FAILED
                    if (text.startsWith("REGISTER_FAILED")) {
                        MessageFrame msgFrame = new MessageFrame();
                        if (text.contains("USERNAME_TAKEN")) {
                            msgFrame.initialize(msgFrame, 1, 2);
                        } else {
                            msgFrame.initialize(msgFrame, 1, 1);
                        }
                        return;
                    }

                    // Optional: Status oben anzeigen
                    // lbWelcome.setText(text);
                });
            }

            @Override
            public void onError(String text) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(LoginFrame.this, text, "Fehler", JOptionPane.ERROR_MESSAGE)
                );
            }

            @Override
            public void onConnectionClosed() {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Verbindung geschlossen.", "Info", JOptionPane.INFORMATION_MESSAGE)
                );
            }
        });
    }

    public static void main(String[] args) {
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.initialize(loginFrame);
    }
}
