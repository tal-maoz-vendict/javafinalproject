import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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

    private static class GamePanel extends JPanel implements ActionListener {
        private static final int PANEL_WIDTH = 900;
        private static final int PANEL_HEIGHT = 500;
        private static final int HUD_HEIGHT = 70;
        private static final int LANE_COUNT = 4;
        private static final int SHIP_X = 110;
        private static final int ENTITY_SIZE = 84;
        private static final int BLOCK_START_X = PANEL_WIDTH - 170;
        private static final double BASE_SPEED = 0.45;
        private static final double SPEED_STEP = 0.03;
        private static final int ENCOUNTER_COOLDOWN_TICKS = 24;
        private static final int MAX_ACTIVE_OBJECTS = 3;
        private static final int OBJECT_SIZE = 34;
        private static final int LASER_DURATION_TICKS = 40;
        private static final int MIN_OBJECT_SPAWN_TICKS = 90;
        private static final int MAX_OBJECT_SPAWN_TICKS = 150;
        private static final int EVENT_TEXT_TICKS = 120;

        private enum FallingObjectType {
            KEY,
            BOMB,
            STAR,
            MULTIPLIER
        }

        private static class FallingObject {
            private final FallingObjectType type;
            private final double x;
            private final int size;
            private final double speed;
            private double y;

            private FallingObject(FallingObjectType type, double x, double y, int size, double speed) {
                this.type = type;
                this.x = x;
                this.y = y;
                this.size = size;
                this.speed = speed;
            }

            private Rectangle bounds() {
                return new Rectangle((int) x, (int) y, size, size);
            }
        }

        private final IntConsumer onGameOver;
        private final Random random = new Random();
        private final List<String> colors = Arrays.asList("blue", "green", "red", "yellow");
        private final List<String> laneBlockColors = new ArrayList<>();
        private final List<FallingObject> fallingObjects = new ArrayList<>();
        private final List<FallingObjectType> spawnBag = new ArrayList<>();
        private final Map<String, Image> shipImages = new HashMap<>();
        private final Map<String, Image> blockImages = new HashMap<>();
        private final Map<String, Color> fallbackColors = new HashMap<>();
        private final Timer timer;

        private double blockX = BLOCK_START_X;
        private double blockSpeed = BASE_SPEED;
        private int encounterCooldownTicks = 0;
        private int objectSpawnCooldownTicks = 110;
        private int laserTicksRemaining = 0;
        private int shipLane = 1;
        private int score = 0;
        private int lives = 3;
        private int invincibleCharges = 0;
        private int nextScoreMultiplier = 1;
        private String shipColor = "blue";
        private String eventText = "";
        private int eventTextTicks = 0;
        private boolean blockLocked = false;
        private boolean laserActive = false;

        private GamePanel(IntConsumer onGameOver) {
            this.onGameOver = onGameOver;
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(new Color(12, 12, 12));
            initializeFallbackColors();
            loadAssets();
            bindControls();
            resetEncounter(true);
            timer = new Timer(16, this);
            timer.start();
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

        private void bindControls() {
            bindKey("moveUpW", KeyStroke.getKeyStroke('w'), this::moveUp);
            bindKey("moveDownS", KeyStroke.getKeyStroke('s'), this::moveDown);
            bindKey("moveUpArrow", KeyStroke.getKeyStroke("UP"), this::moveUp);
            bindKey("moveDownArrow", KeyStroke.getKeyStroke("DOWN"), this::moveDown);
            bindKey("shootLaserD", KeyStroke.getKeyStroke('d'), this::fireLaser);
            bindKey("shootLaserUpperD", KeyStroke.getKeyStroke('D'), this::fireLaser);
        }

        private void bindKey(String actionName, KeyStroke keyStroke, Runnable action) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
            getActionMap().put(actionName, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });
        }

        private void moveUp() {
            shipLane = Math.max(0, shipLane - 1);
            repaint();
        }

        private void moveDown() {
            shipLane = Math.min(LANE_COUNT - 1, shipLane + 1);
            repaint();
        }

        private void fireLaser() {
            laserActive = true;
            laserTicksRemaining = LASER_DURATION_TICKS;
            processLaserHits();
            repaint();
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

            int heartX = PANEL_WIDTH - 136;
            for (int i = 0; i < 3; i++) {
                drawHeart(g2, heartX + i * 42, 18, 28, i < lives);
            }

            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(blockLocked ? new Color(255, 120, 120) : new Color(132, 237, 132));
            g2.drawString(blockLocked ? "Lock: ON (shoot key)" : "Lock: OFF", 220, 27);
            g2.setColor(Color.WHITE);
            g2.drawString("Shield: " + invincibleCharges, 220, 48);
            g2.drawString(nextScoreMultiplier > 1 ? "x2: READY" : "x2: OFF", 330, 48);
            if (!eventText.isEmpty()) {
                g2.setColor(new Color(255, 245, 170));
                g2.drawString(eventText, 450, 48);
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

            for (FallingObject object : fallingObjects) {
                drawFallingObject(g2, object);
            }

            if (laserActive) {
                int shipYForLaser = laneTop(shipLane);
                int beamY = shipYForLaser + ENTITY_SIZE / 2;
                Stroke oldStroke = g2.getStroke();
                g2.setStroke(new BasicStroke(4f));
                g2.setColor(new Color(130, 255, 255));
                g2.drawLine(SHIP_X + ENTITY_SIZE, beamY, PANEL_WIDTH, beamY);
                g2.setStroke(oldStroke);
            }

            g2.dispose();
        }

        private void drawHeart(Graphics2D g2, int x, int y, int size, boolean active) {
            Color fillColor = active ? new Color(236, 67, 67) : new Color(65, 65, 65);
            int lobeSize = size / 2;
            int centerYOffset = size / 6;

            g2.setColor(fillColor);
            g2.fillOval(x, y, lobeSize, lobeSize);
            g2.fillOval(x + lobeSize, y, lobeSize, lobeSize);

            Polygon tip = new Polygon();
            tip.addPoint(x - 1, y + centerYOffset);
            tip.addPoint(x + size + 1, y + centerYOffset);
            tip.addPoint(x + size / 2, y + size);
            g2.fillPolygon(tip);

            g2.setColor(new Color(20, 20, 20));
            g2.drawOval(x, y, lobeSize, lobeSize);
            g2.drawOval(x + lobeSize, y, lobeSize, lobeSize);
            g2.drawPolygon(tip);
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

        private void drawFallingObject(Graphics2D g2, FallingObject object) {
            int x = (int) object.x;
            int y = (int) object.y;
            int size = object.size;
            switch (object.type) {
                case KEY:
                    g2.setColor(new Color(248, 212, 95));
                    g2.fillOval(x, y, size, size);
                    g2.setColor(new Color(95, 70, 20));
                    g2.drawOval(x, y, size, size);
                    g2.setFont(new Font("Arial", Font.BOLD, 16));
                    g2.drawString("K", x + 11, y + 22);
                    break;
                case BOMB:
                    g2.setColor(new Color(32, 32, 32));
                    g2.fillOval(x, y, size, size);
                    g2.setColor(new Color(220, 70, 70));
                    g2.drawOval(x, y, size, size);
                    g2.setFont(new Font("Arial", Font.BOLD, 15));
                    g2.drawString("B", x + 11, y + 22);
                    break;
                case STAR:
                    g2.setColor(new Color(255, 244, 133));
                    g2.fillPolygon(createStarPolygon(x + size / 2, y + size / 2, size / 2));
                    g2.setColor(new Color(135, 120, 20));
                    g2.drawPolygon(createStarPolygon(x + size / 2, y + size / 2, size / 2));
                    break;
                case MULTIPLIER:
                    g2.setColor(new Color(194, 139, 255));
                    g2.fillRoundRect(x, y, size, size, 9, 9);
                    g2.setColor(new Color(68, 38, 110));
                    g2.drawRoundRect(x, y, size, size, 9, 9);
                    g2.setFont(new Font("Arial", Font.BOLD, 13));
                    g2.drawString("x2", x + 8, y + 21);
                    break;
            }
        }

        private Polygon createStarPolygon(int centerX, int centerY, int radius) {
            Polygon star = new Polygon();
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                int currentRadius = (i % 2 == 0) ? radius : radius / 2;
                int pointX = centerX + (int) (Math.cos(angle) * currentRadius);
                int pointY = centerY + (int) (Math.sin(angle) * currentRadius);
                star.addPoint(pointX, pointY);
            }
            return star;
        }

        private Rectangle laserBounds() {
            int shipY = laneTop(shipLane);
            int beamY = shipY + ENTITY_SIZE / 2 - 2;
            return new Rectangle(SHIP_X + ENTITY_SIZE, beamY, PANEL_WIDTH - (SHIP_X + ENTITY_SIZE), 6);
        }

        private void processLaserHits() {
            if (!laserActive) {
                return;
            }
            Rectangle beam = laserBounds();
            Iterator<FallingObject> iterator = fallingObjects.iterator();
            while (iterator.hasNext()) {
                FallingObject object = iterator.next();
                if (!beam.intersects(object.bounds())) {
                    continue;
                }
                iterator.remove();
                applyObjectHitEffect(object.type);
                if (lives <= 0) {
                    return;
                }
            }
        }

        private void applyObjectHitEffect(FallingObjectType type) {
            switch (type) {
                case KEY:
                    blockLocked = false;
                    showEvent("Key hit: lock disabled");
                    break;
                case BOMB:
                    loseLifeDirect();
                    showEvent("Bomb hit: -1 life");
                    break;
                case STAR:
                    invincibleCharges++;
                    showEvent("Star hit: +1 shield");
                    break;
                case MULTIPLIER:
                    nextScoreMultiplier = 2;
                    showEvent("x2 armed for next score");
                    break;
            }
        }

        private void loseLifeDirect() {
            lives--;
            if (lives <= 0) {
                timer.stop();
                onGameOver.accept(score);
            }
        }

        private void applyFailure() {
            if (invincibleCharges > 0) {
                invincibleCharges--;
                showEvent("Shield used: no life lost");
                return;
            }
            loseLifeDirect();
        }

        private void showEvent(String text) {
            eventText = text;
            eventTextTicks = EVENT_TEXT_TICKS;
        }

        private FallingObjectType nextSpawnType() {
            if (spawnBag.isEmpty()) {
                spawnBag.addAll(Arrays.asList(FallingObjectType.values()));
                Collections.shuffle(spawnBag, random);
            }
            return spawnBag.remove(0);
        }

        private void maybeSpawnObject() {
            if (fallingObjects.size() >= MAX_ACTIVE_OBJECTS) {
                return;
            }
            if (objectSpawnCooldownTicks > 0) {
                objectSpawnCooldownTicks--;
                return;
            }

            FallingObjectType type = nextSpawnType();
            double x = 230 + random.nextInt(PANEL_WIDTH - 310);
            double speed = 0.9 + random.nextDouble() * 0.5 + score * 0.015;
            fallingObjects.add(new FallingObject(type, x, -OBJECT_SIZE, OBJECT_SIZE, speed));
            if (type == FallingObjectType.KEY) {
                blockLocked = true;
                showEvent("Lock engaged: shoot key");
            }
            objectSpawnCooldownTicks = MIN_OBJECT_SPAWN_TICKS
                    + random.nextInt(MAX_OBJECT_SPAWN_TICKS - MIN_OBJECT_SPAWN_TICKS + 1);
        }

        private void updateFallingObjects() {
            Iterator<FallingObject> iterator = fallingObjects.iterator();
            while (iterator.hasNext()) {
                FallingObject object = iterator.next();
                object.y += object.speed;
                if (object.y > PANEL_HEIGHT) {
                    iterator.remove();
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (encounterCooldownTicks > 0) {
                encounterCooldownTicks--;
            } else {
                blockX -= blockSpeed;
                if (blockX <= SHIP_X + ENTITY_SIZE - 20) {
                    resolveEncounter();
                    if (lives <= 0) {
                        return;
                    }
                }
            }

            updateFallingObjects();
            maybeSpawnObject();

            if (laserActive) {
                processLaserHits();
                laserTicksRemaining--;
                if (laserTicksRemaining <= 0) {
                    laserActive = false;
                }
            }

            if (eventTextTicks > 0) {
                eventTextTicks--;
                if (eventTextTicks == 0) {
                    eventText = "";
                }
            }

            repaint();
        }

        private void resolveEncounter() {
            String blockColorAtShipLane = laneBlockColors.get(shipLane);
            boolean scorePoint = shipColor.equals(blockColorAtShipLane);

            if (scorePoint && !blockLocked) {
                score += nextScoreMultiplier;
                if (nextScoreMultiplier > 1) {
                    showEvent("x2 applied: +" + nextScoreMultiplier + " score");
                } else {
                    showEvent("Matched color: +1 score");
                }
                nextScoreMultiplier = 1;
            } else {
                if (scorePoint && blockLocked) {
                    showEvent("Lock penalty: life lost");
                } else {
                    showEvent("Wrong color: life lost");
                }
                applyFailure();
                if (lives <= 0) {
                    return;
                }
            }
            encounterCooldownTicks = ENCOUNTER_COOLDOWN_TICKS;
            resetEncounter(false);
        }
    }
}
