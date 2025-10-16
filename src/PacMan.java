import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import java.io.IOException;

/**
 * PacMan class: Main game logic and rendering for a simple Pac-Man clone.
 * Extends JPanel for drawing and implements interfaces for game loop (ActionListener) and input (KeyListener).
 * Includes personality-based ghost AI and Monte Carlo predictive pathfinding.
 */
public class PacMan extends JPanel implements ActionListener, KeyListener {

    // Instantiate the SoundManager for use throughout the game
    private final SoundManager soundManager = new SoundManager();

    // ----------------------- NEW: Personalities -----------------------
    private enum Personality { AGGRESSIVE, STRATEGIC, SNEAKY, FICKLE }

    // ----------------------- BLOCK (game object) -----------------------
    class Block {
        int x, y, width, height; // Current position and size
        Image image; // Visual asset
        int startX, startY; // Initial spawn position for game resets
        char direction = 'U'; // Current movement direction
        int velocityX = 0, velocityY = 0; // Movement distance per game tick

        // for ghost AI scheduling (small random offset so ghosts don't all plan same tick)
        int planOffset = 0;

        // For named ghosts
        String name = "";
        Personality personality = null;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char newDirection) {
            char prevDirection = this.direction;
            this.direction = newDirection;
            updateVelocity(); // Set new velocity based on new direction

            // Logic specifically for Pac-Man to update the sprite based on direction
            if (this == pacman && this.direction != prevDirection) {
                switch (this.direction) {
                    case 'U' -> this.image = pmUImage;
                    case 'D' -> this.image = pmDImage;
                    case 'L' -> this.image = pmLImage;
                    case 'R' -> this.image = pmRImage;
                }
            }

            // Predictive move: apply velocity (small step) to test collision
            this.x += this.velocityX;
            this.y += this.velocityY;

            // Check if the predictive move caused a collision with any wall
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    // Collision detected: undo the predictive move
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    // Restore the previous direction since the new one was blocked
                    this.direction = prevDirection;
                    updateVelocity(); // Re-calculate velocity for the restored direction
                    break;
                }
            }
        }

        void updateVelocity() {
            int step = Math.max(1, tileSize / 4); // Defines the speed of movement
            switch (this.direction) {
                case 'U' -> {
                    velocityX = 0;
                    velocityY = -step;
                }
                case 'D' -> {
                    velocityX = 0;
                    velocityY = step;
                }
                case 'L' -> {
                    velocityX = -step;
                    velocityY = 0;
                }
                case 'R' -> {
                    velocityX = step;
                    velocityY = 0;
                }
            }
        }

        void reset() {
            x = startX;
            y = startY;
            velocityX = 0;
            velocityY = 0;
            direction = 'U';
        }
    }

    // ----------------------- CONFIG -----------------------
    private final int rowCount = 21, columnCount = 19, tileSize = 32;
    private final int boardWidth = columnCount * tileSize, boardHeight = rowCount * tileSize;

    // Image references
    private Image wallImage, blueEnemyImage, orangeEnemyImage, pinkEnemyImage, redEnemyImage;
    private Image pmUImage, pmDImage, pmLImage, pmRImage;

    // Map definition: X=Wall, space=Food, b/o/p/r=Ghosts, P=Pac-Man
    private final String[] tileMap = {
            "XXXXXXXXXXXXXXXXXXX",
            "X                 X",
            "X XX XXX X XXX XX X",
            "X       X         X",
            "X XX X XXXXX X XX X",
            "X     X   X       X",
            "XXXX XXXX XXXX XXXX",
            "OOOX   X   X   XOOO",
            "XXXX X XXrXX X XXXX",
            "O bo    p   O    OX",
            "XXXX X XXXXX X XXXX",
            "OOOX   X   X   OOOX",
            "XXXX X XXX X X XXXX",
            "X       X  P      X",
            "X XX XXX   XXX XX X",
            "X   X   X         X",
            "XX   X XXXXX X X XX",
            "X   X       X     X",
            "X XXXXXX X XXXXXX X",
            "X                 X",
            "XXXXXXXXXXXXXXXXXXX"
    };

    // Game objects storage lists
    private List<Block> walls, foods, enemies;
    private Block pacman;

    // Game loop & input
    private Timer gameLoop; // Controls the tick rate
    private final char[] directions = {'U', 'D', 'L', 'R'};
    private final Random random = new Random();
    private int score = 0, lives = 3;

    private char nextDirection = ' '; // Buffered direction input from player
    private boolean gameStarted = false, gameOver = false;

    // Game state variables for sound and ghost logic (frightened mode)
    private boolean isFrightened = false;
    private int frightenedTimer = 0; // Timer to track duration of frightened mode

    // AI tuning
    private final int aiPlanInterval = 6; // How often ghosts re-calculate their path (in ticks)
    private int tickCounter = 0; // Global tick counter for AI scheduling

    // Monte Carlo tuning (can change for performance/quality)
    private final int MC_SIMULATIONS = 120; // number of playouts per candidate direction
    private final int MC_DEPTH = 8; // steps per playout

    // ----------------------- CONSTRUCTOR -----------------------
    PacMan() {
        // JPanel initialization
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load all image assets (assumes images exist in resources)
        wallImage = new ImageIcon(getClass().getResource("images/wall.png")).getImage();
        blueEnemyImage = new ImageIcon(getClass().getResource("images/blueEnemy.png")).getImage();
        orangeEnemyImage = new ImageIcon(getClass().getResource("images/orangeEnemy.png")).getImage();
        pinkEnemyImage = new ImageIcon(getClass().getResource("images/pinkEnemy.png")).getImage();
        redEnemyImage = new ImageIcon(getClass().getResource("images/redEnemy.png")).getImage();

        pmUImage = new ImageIcon(getClass().getResource("images/pm-Up.png")).getImage();
        pmDImage = new ImageIcon(getClass().getResource("images/pm-Down.png")).getImage();
        pmLImage = new ImageIcon(getClass().getResource("images/pm-Left.png")).getImage();
        pmRImage = new ImageIcon(getClass().getResource("images/pm-Right.png")).getImage();

        loadMap(); // Initialize all game blocks from the tile map

        // Setup initial ghost state
        for (Block enemy : enemies) {
            enemy.updateDirection(directions[random.nextInt(4)]);
            // Assign a random offset to prevent all ghosts from planning simultaneously
            enemy.planOffset = random.nextInt(aiPlanInterval);
        }

        // Initialize the game loop timer (50ms interval = 20 FPS logic updates)
        gameLoop = new Timer(50, this);
        // Game starts paused until the user presses ENTER
    }

    // ----------------------- MAP LOADING -----------------------
    public void loadMap() {
        // Re-initialize lists for a new level/restart
        walls = new ArrayList<>();
        foods = new ArrayList<>();
        enemies = new ArrayList<>();
        pacman = null;

        for (int r = 0; r < rowCount; r++) {
            String row = tileMap[r];
            // ensure row has at least columnCount characters (pad with spaces if shorter)
            if (row.length() < columnCount) {
                row = String.format("%-" + columnCount + "s", row);
            }
            for (int c = 0; c < columnCount; c++) {
                char ch = row.charAt(c);
                int x = c * tileSize, y = r * tileSize;
                switch (ch) {
                    case 'X' -> walls.add(new Block(wallImage, x, y, tileSize, tileSize));
                    case 'b' -> {
                        Block eb = new Block(blueEnemyImage, x, y, tileSize, tileSize);
                        eb.name = "Inky";
                        eb.personality = Personality.SNEAKY;
                        enemies.add(eb);
                    }
                    case 'o' -> {
                        Block eo = new Block(orangeEnemyImage, x, y, tileSize, tileSize);
                        eo.name = "Clyde";
                        eo.personality = Personality.FICKLE;
                        enemies.add(eo);
                    }
                    case 'p' -> {
                        Block ep = new Block(pinkEnemyImage, x, y, tileSize, tileSize);
                        ep.name = "Pinky";
                        ep.personality = Personality.STRATEGIC;
                        enemies.add(ep);
                    }
                    case 'r' -> {
                        Block er = new Block(redEnemyImage, x, y, tileSize, tileSize);
                        er.name = "Blinky";
                        er.personality = Personality.AGGRESSIVE;
                        enemies.add(er);
                    }
                    case 'P' -> pacman = new Block(pmRImage, x, y, tileSize, tileSize);
                    case ' ' -> foods.add(new Block(null, x + 14, y + 14, 4, 4));
                    default -> { /* treat any other char as empty space */ }
                }
            }
        }

        // Fallback for Pac-Man placement
        if (pacman == null) pacman = new Block(pmRImage, tileSize * 9, tileSize * 15, tileSize, tileSize);
    }

    // ----------------------- RENDERING -----------------------
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!gameStarted) drawStartScreen(g);
        else if (gameOver) drawGameOverScreen(g);
        else drawGame(g);
    }

    private void drawStartScreen(Graphics g) {
        // Draw initial start screen elements
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 64));
        g.drawString("PAC-MAN", boardWidth / 2 - 150, boardHeight / 2 - 50);

        g.setColor(Color.YELLOW);
        g.fillArc(boardWidth / 2 - 30, boardHeight / 2, 60, 60, 30, 300);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Press ENTER to Start", boardWidth / 2 - 120, boardHeight / 2 + 100);
    }

    private void drawGameOverScreen(Graphics g) {
        // Draw game over screen elements
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 64));
        g.drawString("GAME OVER", boardWidth / 2 - 200, boardHeight / 2 - 50);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 28));
        g.drawString("Score: " + score, boardWidth / 2 - 70, boardHeight / 2 + 20);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Press ENTER to Restart", boardWidth / 2 - 140, boardHeight / 2 + 80);
    }

    private void drawGame(Graphics g) {
        // Draw walls and food
        for (Block wall : walls) g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        for (Block food : foods) {
            g.setColor(Color.WHITE);
            g.fillRect(food.x, food.y, food.width, food.height);
        }

        // Draw enemies and Pac-Man
        for (Block enemy : enemies) g.drawImage(enemy.image, enemy.x, enemy.y, enemy.width, enemy.height, null);
        if (pacman != null) g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        // Draw HUD (Lives and Score)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("x" + lives + " Score: " + score, tileSize / 2, tileSize / 2);
    }

    // ----------------------- GAME LOOP -----------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameStarted && !gameOver) move();
        repaint();
    }

    public void move() {
        tickCounter++;
        // Decrement frightened timer if active
        if (isFrightened) frightenedTimer--;
        // End frightened mode if timer runs out
        if (frightenedTimer <= 0) isFrightened = false;

        // --- Pac-Man Movement ---
        if (nextDirection != ' ' && canMove(pacman, nextDirection)) {
            pacman.updateDirection(nextDirection);
            nextDirection = ' ';
        }
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // Check for continuous collision with walls and stop movement if necessary
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        // --- Enemy Logic ---
        Iterator<Block> enemyIter = enemies.iterator();
        while (enemyIter.hasNext()) {
            Block enemy = enemyIter.next();

            // Collision Check with Pac-Man
            if (collision(enemy, pacman)) {
                if (isFrightened) {
                    // Ghost eaten (Frightened mode active)
                    // *** SOUND: EAT GHOST ***
                    soundManager.playSound(SoundManager.EAT_GHOST);
                    enemy.reset(); // Teleport ghost back to start
                    score += 200;
                    continue; // Skip the rest of the ghost's movement logic for this tick
                } else {
                    // Pac-Man caught (Frightened mode inactive)
                    // *** SOUND: DEATH ***
                    soundManager.playSound(SoundManager.DEATH);
                    lives--;
                    if (lives <= 0) {
                        gameOver = true;
                        return;
                    }
                    resetPositions();
                    return; // Stop game loop update immediately upon death/reset
                }
            }

            // AI Planning: Check if it's time for the ghost to re-plan its direction
            if ((tickCounter + enemy.planOffset) % aiPlanInterval == 0) {
                // Use new Monte Carlo predictive planner with personality adjustments
                char planned = planNextDirectionMonteCarlo(enemy);
                if (canMove(enemy, planned)) {
                    enemy.updateDirection(planned);
                } else {
                    // If planned move is blocked, fallback to a random valid direction
                    List<Character> allowed = new ArrayList<>();
                    for (char d : directions) if (canMove(enemy, d)) allowed.add(d);
                    if (!allowed.isEmpty())
                        enemy.updateDirection(allowed.get(random.nextInt(allowed.size())));
                }
            }

            // Apply ghost movement velocity
            enemy.x += enemy.velocityX;
            enemy.y += enemy.velocityY;

            // Check for wall or boundary bumps after movement
            boolean bumped = false;
            if (enemy.x <= 0 || enemy.x + enemy.width >= boardWidth || enemy.y <= 0 || enemy.y + enemy.height >= boardHeight)
                bumped = true;
            else {
                for (Block wall : walls) if (collision(enemy, wall)) {
                    bumped = true;
                    break;
                }
            }

            if (bumped) {
                // Undo movement and force a random, valid new direction
                enemy.x -= enemy.velocityX;
                enemy.y -= enemy.velocityY;
                List<Character> allowed = new ArrayList<>();
                for (char d : directions) if (canMove(enemy, d)) allowed.add(d);
                if (!allowed.isEmpty())
                    enemy.updateDirection(allowed.get(random.nextInt(allowed.size())));
            }
        }

        // --- Food Collection ---
        Block eaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                eaten = food;
                score += 10;
                // *** SOUND: WAKA WAKA ***
                soundManager.playSound(SoundManager.WAKA);
                break;
            }
        }
        if (eaten != null) foods.remove(eaten);

        // --- Level Completion ---
        if (foods.isEmpty()) {
            loadMap(); // Reload map to reset food
            resetPositions();
        }
    }

    // ----------------------- HELPERS -----------------------
    private boolean canMove(Block b, char dir) {
        int tempX = b.x, tempY = b.y;
        int step = Math.max(1, tileSize / 4);
        switch (dir) {
            case 'U' -> tempY -= step;
            case 'D' -> tempY += step;
            case 'L' -> tempX -= step;
            case 'R' -> tempX += step;
        }
        Block test = new Block(null, tempX, tempY, b.width, b.height);
        for (Block wall : walls) if (collision(test, wall)) return false;
        return true;
    }

    public boolean collision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
    }

    public void resetPositions() {
        if (pacman != null) pacman.reset();
        nextDirection = ' ';
        isFrightened = false;
        frightenedTimer = 0;
        for (Block enemy : enemies) {
            enemy.reset();
            enemy.updateDirection(directions[random.nextInt(4)]);
            enemy.planOffset = random.nextInt(aiPlanInterval);
        }
    }

    private boolean isWalkable(int row, int col) {
        if (row < 0 || row >= rowCount || col < 0 || col >= columnCount) return false;
        return tileMap[row].charAt(col) != 'X';
    }

    // ----------------------- PATHFINDING (BFS fallback) -----------------------
    private char findNextDirectionTowardsPacman(Block enemy) {
        int startR = Math.max(0, Math.min(rowCount - 1, enemy.y / tileSize));
        int startC = Math.max(0, Math.min(columnCount - 1, enemy.x / tileSize));
        int targetR = Math.max(0, Math.min(rowCount - 1, pacman.y / tileSize));
        int targetC = Math.max(0, Math.min(columnCount - 1, pacman.x / tileSize));

        if (startR == targetR && startC == targetC) return enemy.direction;

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // U, D, L, R
        char[] dirChars = {'U', 'D', 'L', 'R'};

        boolean[][] visited = new boolean[rowCount][columnCount];
        int[][] prev = new int[rowCount][columnCount];
        for (int i = 0; i < rowCount; i++) Arrays.fill(prev[i], -1);

        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{startR, startC});
        visited[startR][startC] = true;

        boolean found = false;
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1];
            if (r == targetR && c == targetC) {
                found = true;
                break;
            }
            for (int i = 0; i < 4; i++) {
                int nr = r + dirs[i][0], nc = c + dirs[i][1];
                if (nr >= 0 && nr < rowCount && nc >= 0 && nc < columnCount && !visited[nr][nc] && isWalkable(nr, nc)) {
                    visited[nr][nc] = true;
                    prev[nr][nc] = i;
                    q.add(new int[]{nr, nc});
                }
            }
        }

        if (!found) {
            List<Character> allowed = new ArrayList<>();
            for (char d : directions) if (canMove(enemy, d)) allowed.add(d);
            if (!allowed.isEmpty()) return allowed.get(random.nextInt(allowed.size()));
            return directions[random.nextInt(4)];
        }

        int r = targetR, c = targetC;
        char nextDir = directions[random.nextInt(4)];
        while (!(r == startR && c == startC)) {
            int dir = prev[r][c];
            if (dir == -1) break;
            int pr = r - dirs[dir][0];
            int pc = c - dirs[dir][1];
            if (pr == startR && pc == startC) {
                return dirChars[dir];
            }
            r = pr;
            c = pc;
        }

        return nextDir;
    }

    // ----------------------- MONTE CARLO PREDICTIVE PLANNING -----------------------
    /**
     * Main planner: runs Monte Carlo simulations for each candidate initial direction and
     * chooses the best based on the ghost's personality.
     */
    private char planNextDirectionMonteCarlo(Block enemy) {
        // collect candidate directions
        List<Character> candidates = new ArrayList<>();
        for (char d : directions) if (canMove(enemy, d)) candidates.add(d);

        if (candidates.isEmpty()) return enemy.direction; // no move possible

        // personality-specific modifiers
        Personality p = enemy.personality == null ? Personality.AGGRESSIVE : enemy.personality;

        double bestScore = Double.NEGATIVE_INFINITY;
        char bestDir = candidates.get(0);

        for (char cand : candidates) {
            double totalScore = 0.0;
            int sims = MC_SIMULATIONS;
            // Clyde (fickle) uses fewer sims and more randomness for speed and temperament
            if (p == Personality.FICKLE) sims = Math.max(30, MC_SIMULATIONS / 3);
            for (int s = 0; s < sims; s++) {
                // Simulate a playout and compute reward
                double score = simulatePlayout(enemy, cand, p, MC_DEPTH);
                totalScore += score;
            }
            double avg = totalScore / sims;

            // Personality-specific bias: some personalities prefer different trade-offs
            switch (p) {
                case AGGRESSIVE -> { /* already capture-focused, no extra bias */ }
                case STRATEGIC -> avg *= 1.05; // small bias to encourage interception choices
                case SNEAKY -> avg *= 1.02; // small bias to favor minimized final distance
                case FICKLE -> avg += (random.nextDouble() - 0.5) * 0.2; // inject randomness
            }

            if (avg > bestScore) {
                bestScore = avg;
                bestDir = cand;
            }
        }

        // If something went wrong or scores equal, fallback to BFS targetting
        if (bestScore == Double.NEGATIVE_INFINITY) {
            return findNextDirectionTowardsPacman(enemy);
        }
        return bestDir;
    }

    /**
     * Simulates a short playout starting with enemy taking initialDir and returns a score:
     * higher scores indicate better outcomes for that ghost's goal (capture, intercept).
     */
    private double simulatePlayout(Block enemyOrig, char initialDir, Personality p, int depth) {
        // Lightweight clones of positions (no images)
        SimState s = new SimState();
        s.ghostX = enemyOrig.x;
        s.ghostY = enemyOrig.y;
        s.ghostDir = initialDir;

        s.pacX = pacman.x;
        s.pacY = pacman.y;
        s.pacDir = pacman.direction;

        // initial velocities based on directions
        int step = Math.max(1, tileSize / 4);
        s.updateVelocities(step);

        double score = 0.0;
        for (int t = 0; t < depth; t++) {
            // Move Pac-Man (simulate likely player behavior)
            simulatePacmanStep(s);

            // Move ghost
            simulateGhostStep(s);

            // Check capture
            if (simCollision(s)) {
                // Earlier captures are valued higher
                score += 100.0 + (depth - t) * 10.0;
                break;
            } else {
                // No capture: compute heuristic closeness and future intercept likelihood
                double dist = distance(s.ghostX, s.ghostY, s.pacX, s.pacY);
                // personalities value different heuristics
                switch (p) {
                    case AGGRESSIVE -> score += (1.0 / (1 + dist)); // closer is better
                    case STRATEGIC -> {
                        // reward being closer to where pacman is heading (predict by projecting pacman's position forward)
                        int projSteps = 3;
                        int projX = s.pacX + projSteps * s.pacVelX;
                        int projY = s.pacY + projSteps * s.pacVelY;
                        double dproj = distance(s.ghostX, s.ghostY, projX, projY);
                        score += 1.2 * (1.0 / (1 + dproj));
                    }
                    case SNEAKY -> {
                        // sneaky prefers to minimize maximum distance over simulation (ambush)
                        score += (1.0 / (1 + dist)) * 0.9 + 0.1 * (Math.abs(s.ghostX - s.pacX) + Math.abs(s.ghostY - s.pacY));
                    }
                    case FICKLE -> score += (random.nextDouble() - 0.5) * 0.1 + (1.0 / (1 + dist)) * 0.6;
                }
            }
        }
        // small random jitter to break ties
        score += (random.nextDouble() - 0.5) * 0.01;
        return score;
    }

    // A tiny helper class used only during simulation to keep things fast and isolated
    private class SimState {
        int ghostX, ghostY;
        char ghostDir;
        int ghostVelX = 0, ghostVelY = 0;

        int pacX, pacY;
        char pacDir;
        int pacVelX = 0, pacVelY = 0;

        void updateVelocities(int step) {
            switch (ghostDir) {
                case 'U' -> { ghostVelX = 0; ghostVelY = -step; }
                case 'D' -> { ghostVelX = 0; ghostVelY = step; }
                case 'L' -> { ghostVelX = -step; ghostVelY = 0; }
                case 'R' -> { ghostVelX = step; ghostVelY = 0; }
            }
            switch (pacDir) {
                case 'U' -> { pacVelX = 0; pacVelY = -step; }
                case 'D' -> { pacVelX = 0; pacVelY = step; }
                case 'L' -> { pacVelX = -step; pacVelY = 0; }
                case 'R' -> { pacVelX = step; pacVelY = 0; }
            }
        }
    }

    // Simulates a Pac-Man step inside a Monte Carlo playout
    private void simulatePacmanStep(SimState s) {
        int step = Math.max(1, tileSize / 4);

        // Pac-Man behavior model for simulation:
        // With high probability, continue in the same direction if possible;
        // otherwise pick a random allowed direction with some bias towards forward movement.
        if (!simCanMove(s.pacX, s.pacY, s.pacDir)) {
            // choose a random allowed direction
            List<Character> allowed = new ArrayList<>();
            for (char d : directions) if (simCanMove(s.pacX, s.pacY, d)) allowed.add(d);
            if (!allowed.isEmpty()) {
                s.pacDir = allowed.get(random.nextInt(allowed.size()));
            }
        } else {
            // occasionally (15%) choose a different allowed direction to model player variability
            if (random.nextDouble() < 0.15) {
                List<Character> allowed = new ArrayList<>();
                for (char d : directions) if (simCanMove(s.pacX, s.pacY, d)) allowed.add(d);
                if (!allowed.isEmpty()) {
                    s.pacDir = allowed.get(random.nextInt(allowed.size()));
                }
            }
        }

        // update pacVel according to pacDir
        switch (s.pacDir) {
            case 'U' -> { s.pacVelX = 0; s.pacVelY = -step; }
            case 'D' -> { s.pacVelX = 0; s.pacVelY = step; }
            case 'L' -> { s.pacVelX = -step; s.pacVelY = 0; }
            case 'R' -> { s.pacVelX = step; s.pacVelY = 0; }
        }

        s.pacX += s.pacVelX;
        s.pacY += s.pacVelY;

        // if hits wall, undo
        if (simHitsWall(s.pacX, s.pacY)) {
            s.pacX -= s.pacVelX;
            s.pacY -= s.pacVelY;
            // pick random allowed direction
            List<Character> allowed = new ArrayList<>();
            for (char d : directions) if (simCanMove(s.pacX, s.pacY, d)) allowed.add(d);
            if (!allowed.isEmpty()) s.pacDir = allowed.get(random.nextInt(allowed.size()));
            // update velocity
            switch (s.pacDir) {
                case 'U' -> { s.pacVelX = 0; s.pacVelY = -step; }
                case 'D' -> { s.pacVelX = 0; s.pacVelY = step; }
                case 'L' -> { s.pacVelX = -step; s.pacVelY = 0; }
                case 'R' -> { s.pacVelX = step; s.pacVelY = 0; }
            }
        }
    }

    // Simulates a ghost step inside a Monte Carlo playout (simple behavior: continue direction, random turn if blocked)
    private void simulateGhostStep(SimState s) {
        int step = Math.max(1, tileSize / 4);
        // check if ghost can continue
        if (!simCanMove(s.ghostX, s.ghostY, s.ghostDir)) {
            // pick random allowed
            List<Character> allowed = new ArrayList<>();
            for (char d : directions) if (simCanMove(s.ghostX, s.ghostY, d)) allowed.add(d);
            if (!allowed.isEmpty()) s.ghostDir = allowed.get(random.nextInt(allowed.size()));
        }
        // update velocity
        switch (s.ghostDir) {
            case 'U' -> { s.ghostVelX = 0; s.ghostVelY = -step; }
            case 'D' -> { s.ghostVelX = 0; s.ghostVelY = step; }
            case 'L' -> { s.ghostVelX = -step; s.ghostVelY = 0; }
            case 'R' -> { s.ghostVelX = step; s.ghostVelY = 0; }
        }
        s.ghostX += s.ghostVelX;
        s.ghostY += s.ghostVelY;
        if (simHitsWall(s.ghostX, s.ghostY)) {
            s.ghostX -= s.ghostVelX;
            s.ghostY -= s.ghostVelY;
            List<Character> allowed = new ArrayList<>();
            for (char d : directions) if (simCanMove(s.ghostX, s.ghostY, d)) allowed.add(d);
            if (!allowed.isEmpty()) s.ghostDir = allowed.get(random.nextInt(allowed.size()));
        }
    }

    private boolean simHitsWall(int x, int y) {
        Block b = new Block(null, x, y, tileSize, tileSize);
        for (Block wall : walls) if (collision(b, wall)) return true;
        return false;
    }

    // Determines if a simulated position can move in a direction (grid-based)
    private boolean simCanMove(int x, int y, char dir) {
        int step = Math.max(1, tileSize / 4);
        int tempX = x, tempY = y;
        switch (dir) {
            case 'U' -> tempY -= step;
            case 'D' -> tempY += step;
            case 'L' -> tempX -= step;
            case 'R' -> tempX += step;
        }
        Block test = new Block(null, tempX, tempY, tileSize, tileSize);
        for (Block wall : walls) if (collision(test, wall)) return false;
        // also ensure within bounds
        if (tempX < 0 || tempX + tileSize > boardWidth || tempY < 0 || tempY + tileSize > boardHeight) return false;
        return true;
    }

    private boolean simCollision(SimState s) {
        // simple AABB with tile-size boxes
        Block a = new Block(null, s.ghostX, s.ghostY, tileSize, tileSize);
        Block b = new Block(null, s.pacX, s.pacY, tileSize, tileSize);
        return collision(a, b);
    }

    private double distance(int x1, int y1, int x2, int y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ----------------------- INPUT -----------------------
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Start Game logic
        if (!gameStarted && e.getKeyCode() == KeyEvent.VK_ENTER) {
            // *** SOUND: START GAME ***
            soundManager.playSound(SoundManager.START_GAME);

            gameStarted = true;
            resetPositions();
            score = 0;
            lives = 3;
            gameOver = false;
            gameLoop.start();
            return;
        }

        // Restart Game logic (from Game Over screen)
        if (gameOver && e.getKeyCode() == KeyEvent.VK_ENTER) {
            resetPositions();
            loadMap();
            score = 0;
            lives = 3;
            gameOver = false;
            return;
        }

        // Buffer Pac-Man direction input
        if (gameStarted && !gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_UP) nextDirection = 'U';
            else if (e.getKeyCode() == KeyEvent.VK_DOWN) nextDirection = 'D';
            else if (e.getKeyCode() == KeyEvent.VK_LEFT) nextDirection = 'L';
            else if (e.getKeyCode() == KeyEvent.VK_RIGHT) nextDirection = 'R';
        }
    }
}
