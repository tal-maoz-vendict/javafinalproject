import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntConsumer;

public class startscreen extends JFrame {
    public startscreen() {

        setTitle("Color blaster");
        setSize(350, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(10));
        mainPanel.setLayout(new BorderLayout());

        Font titles = new Font("Arial", Font.BOLD, 40);
        JLabel title = new JLabel("Color blaster", SwingConstants.CENTER);
        title.setFont(titles);
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(4, 0, 10, 20));

        JButton startgame = new JButton("Single Player");
        styleButton(startgame, new Color(100, 100, 100));

        startgame.addActionListener(e -> {
            new GameFrame();
            dispose();
        });

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        centerPanel.setBackground(new Color(10));
        centerPanel.add(startgame);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(title, BorderLayout.NORTH);
        add(mainPanel);
        setVisible(true);
    }

    private void styleButton(JButton button, Color color) {
        button.setMaximumSize(new Dimension(70, 50));
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(startscreen::new);
    }

    private static class GameFrame extends JFrame {
        private GameFrame() {
            setTitle("Color blaster");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setResizable(false);
            setContentPane(new GamePanel(score -> {
                dispose();
                new LoseFrame(score);
            }));
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }
    }

    private static class LoseFrame extends JFrame {
        private LoseFrame(int score) {
            setTitle("Color blaster - Game over");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(430, 260);
            setResizable(false);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(new Color(10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel loseLabel = new JLabel("YOU LOSE", SwingConstants.CENTER);
            loseLabel.setForeground(Color.RED);
            loseLabel.setFont(new Font("Arial", Font.BOLD, 46));

            JLabel scoreLabel = new JLabel("Final score: " + score, SwingConstants.CENTER);
            scoreLabel.setForeground(Color.WHITE);
            scoreLabel.setFont(new Font("Arial", Font.BOLD, 24));

            JButton restartButton = new JButton("Restart");
            restartButton.setFont(new Font("Arial", Font.BOLD, 22));
            restartButton.setFocusPainted(false);
            restartButton.addActionListener(e -> {
                dispose();
                new GameFrame();
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(new Color(10));
            buttonPanel.add(restartButton);

            JPanel center = new JPanel(new GridLayout(2, 1, 0, 10));
            center.setBackground(new Color(10));
            center.add(loseLabel);
            center.add(scoreLabel);

            panel.add(center, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            add(panel);
            setVisible(true);
        }
    }

    private static class GamePanel extends JPanel implements ActionListener, KeyListener {
        private static final int PANEL_WIDTH = 900;
        private static final int PANEL_HEIGHT = 500;
        private static final int HUD_HEIGHT = 70;
        private static final int LANE_COUNT = 4;
        private static final int SHIP_X = 110;
        private static final int ENTITY_SIZE = 84;
        private static final int BLOCK_START_X = PANEL_WIDTH - 170;
        private static final double BASE_SPEED = 4.0;
        private static final double SPEED_STEP = 0.35;

        private final IntConsumer onGameOver;
        private final Random random = new Random();
        private final List<String> colors = Arrays.asList("blue", "green", "red", "yellow");
        private final List<String> laneBlockColors = new ArrayList<>();
        private final Map<String, Image> shipImages = new HashMap<>();
        private final Map<String, Image> blockImages = new HashMap<>();
        private final Map<String, Color> fallbackColors = new HashMap<>();
        private final Timer timer;

        private double blockX = BLOCK_START_X;
        private double blockSpeed = BASE_SPEED;
        private int shipLane = 1;
        private int score = 0;
        private int lives = 3;
        private String shipColor = "blue";

        private GamePanel(IntConsumer onGameOver) {
            this.onGameOver = onGameOver;
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(new Color(12, 12, 12));
            setFocusable(true);
            addKeyListener(this);
            initializeFallbackColors();
            loadAssets();
            resetEncounter(true);
            timer = new Timer(16, this);
            timer.start();
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            requestFocusInWindow();
        }

        private void initializeFallbackColors() {
            fallbackColors.put("blue", new Color(66, 133, 244));
            fallbackColors.put("green", new Color(46, 204, 113));
            fallbackColors.put("red", new Color(231, 76, 60));
            fallbackColors.put("yellow", new Color(241, 196, 15));
        }

        private void loadAssets() {
            for (String color : colors) {
                shipImages.put(color, loadImage("ship-" + color + ".png"));
                blockImages.put(color, loadImage("block-" + color + ".png"));
            }
        }

        private Image loadImage(String filename) {
            String[] candidates = {"assets/" + filename, filename};
            for (String candidate : candidates) {
                File file = new File(candidate);
                if (file.exists()) {
                    try {
                        BufferedImage image = ImageIO.read(file);
                        if (image != null) {
                            return image;
                        }
                    } catch (IOException ignored) {
                        // Fall back to color blocks if image loading fails.
                    }
                }
            }
            return null;
        }

        private void resetEncounter(boolean initialRound) {
            laneBlockColors.clear();
            laneBlockColors.addAll(colors);
            Collections.shuffle(laneBlockColors, random);
            shipColor = colors.get(random.nextInt(colors.size()));
            blockX = BLOCK_START_X;
            if (!initialRound) {
                blockSpeed += SPEED_STEP;
            }
        }

        private int laneTop(int lane) {
            int laneHeight = (PANEL_HEIGHT - HUD_HEIGHT) / LANE_COUNT;
            return HUD_HEIGHT + lane * laneHeight + (laneHeight - ENTITY_SIZE) / 2;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(25, 25, 25));
            g2.fillRect(0, 0, PANEL_WIDTH, HUD_HEIGHT);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            g2.drawString("Score: " + score, 18, 45);

            g2.setFont(new Font("SansSerif", Font.BOLD, 30));
            int heartX = PANEL_WIDTH - 135;
            for (int i = 0; i < 3; i++) {
                g2.setColor(i < lives ? Color.RED : new Color(90, 90, 90));
                g2.drawString("\u2665", heartX + i * 38, 44);
            }

            int laneHeight = (PANEL_HEIGHT - HUD_HEIGHT) / LANE_COUNT;
            g2.setColor(new Color(40, 40, 40));
            for (int lane = 1; lane < LANE_COUNT; lane++) {
                int y = HUD_HEIGHT + lane * laneHeight;
                g2.drawLine(0, y, PANEL_WIDTH, y);
            }

            int shipY = laneTop(shipLane);
            drawEntity(g2, shipImages.get(shipColor), shipColor, SHIP_X, shipY, true);

            for (int lane = 0; lane < LANE_COUNT; lane++) {
                String blockColor = laneBlockColors.get(lane);
                drawEntity(g2, blockImages.get(blockColor), blockColor, (int) blockX, laneTop(lane), false);
            }

            g2.dispose();
        }

        private void drawEntity(Graphics2D g2, Image image, String colorName, int x, int y, boolean isShip) {
            if (image != null) {
                g2.drawImage(image, x, y, ENTITY_SIZE, ENTITY_SIZE, null);
                return;
            }

            g2.setColor(fallbackColors.getOrDefault(colorName, Color.LIGHT_GRAY));
            if (isShip) {
                int[] xPoints = {x, x, x + ENTITY_SIZE};
                int[] yPoints = {y, y + ENTITY_SIZE, y + ENTITY_SIZE / 2};
                g2.fillPolygon(xPoints, yPoints, 3);
            } else {
                g2.fillRoundRect(x, y, ENTITY_SIZE, ENTITY_SIZE, 20, 20);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            blockX -= blockSpeed;
            if (blockX <= SHIP_X + ENTITY_SIZE - 20) {
                resolveEncounter();
            }
            repaint();
        }

        private void resolveEncounter() {
            String blockColorAtShipLane = laneBlockColors.get(shipLane);
            boolean scorePoint = shipColor.equals(blockColorAtShipLane);

            if (scorePoint) {
                score++;
            } else {
                lives--;
                if (lives <= 0) {
                    timer.stop();
                    onGameOver.accept(score);
                    return;
                }
            }
            resetEncounter(false);
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) {
                shipLane = Math.max(0, shipLane - 1);
            } else if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) {
                shipLane = Math.min(LANE_COUNT - 1, shipLane + 1);
            }
            repaint();
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}
