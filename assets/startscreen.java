import javax.swing.*;
import java.awt.*;
public class startscreen extends JFrame {
    public startscreen(){

        setTitle("Color blaster");
        setSize(350, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(10));
        mainPanel.setLayout(new BorderLayout());

        Font titles = new Font("Ariel",Font.BOLD,40);
        JLabel title = new JLabel("Color blaster",SwingConstants.CENTER);
        title.setFont(titles);
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(4, 0, 10, 20));
        
        JButton startgame = new JButton("Single Player");
        styleButton(startgame, new Color(100,100,100));

        startgame.addActionListener(e ->{
            dispose();
        });
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        centerPanel.setBackground(new Color(10));
        centerPanel.add(startgame);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(title,BorderLayout.NORTH);
        add(mainPanel);
        setVisible(true);
    }
    private void styleButton(JButton button, Color color){
        button.setMaximumSize(new Dimension(70 ,50));
        button.setFont(new Font("Arial",Font.BOLD,20));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }

    public static void main(String[] args) {
        new startscreen();
    }
}
