import java.awt.*;
import javax.swing.*;


public class MainFrame extends JFrame {
    final static Font  mainFont  = new Font("Dialog", Font.BOLD, 15);
    final static Color mainColor = new Color(128, 170, 180);
    

    public static void main(String[] args) {
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.initialize(loginFrame);
    }
}