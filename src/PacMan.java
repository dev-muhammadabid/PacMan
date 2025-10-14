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
 * Features include tile-based movement, wall collision, and BFS ghost pathfinding.
 */
public class PacMan extends JPanel implements ActionListener, KeyListener {

    // Instantiate the SoundManager for use throughout the game
    private final SoundManager soundManager = new SoundManager();

    /**
     * Inner class representing a basic game object (Pac-Man, Ghost, Wall, Food).
     * Stores position, size, image, and movement properties.
     */
    class Block {
        int x, y, width, height; // Current position and size
        Image image;              // Visual asset
        int startX, startY;       // Initial spawn position for game resets
        char direction = 'U';     // Current movement direction
        int velocityX = 0, velocityY = 0; // Movement distance per game tick

        // for ghost AI scheduling (small random offset so ghosts don't all plan same tick)
        int planOffset = 0;

        /**
         * Constructor initializes a Block with its visual and physical properties.
         */
        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        /**
         * Updates the Block's direction, calculates new velocity, and checks for immediate wall collision.
         * If the intended move hits a wall, the direction is reverted and the move is canceled.
         * @param newDirection The desired new direction.
         */
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

        /**
         * Calculates the pixel distance (velocity) to move per tick based on the current direction.
         * Step size is proportional to tileSize.
         */
        void updateVelocity() {
            int step = Math.max(1, tileSize / 4); // Defines the speed of movement
            switch (this.direction) {
                case 'U' -> { velocityX = 0; velocityY = -step; }
                case 'D' -> { velocityX = 0; velocityY = step; }
                case 'L' -> { velocityX = -step; velocityY = 0; }
                case 'R' -> { velocityX = step; velocityY = 0; }
            }
        }

        /**
         * Resets the Block to its original position and clears its movement state.
         */
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
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X                 X",
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "OOOX X       X XOOO",
            "XXXX X XXrXX X XXXX",
            "O       bpo       O",
            "XXXX X XXXXX X XXXX",
            "OOOX X       X XOOO",
            "XXXX X XXXXX X XXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X  X     P     X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "X                 X",
            "XXXXXXXXXXXXXXXXXXX"
    };

    // Game objects storage lists
    private List<Block> walls, foods, enemies;
    private Block pacman;

    // Game loop & input
    private Timer gameLoop; // Controls the tick rate
    private final char[] directions = {'U','D','L','R'};
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

    // ----------------------- CONSTRUCTOR -----------------------
    PacMan() {
        // JPanel initialization
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load all image assets
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
    /**
     * Parses the tileMap string array to create and place all game objects (Blocks).
     */
    public void loadMap() {
        // Re-initialize lists for a new level/restart
        walls = new ArrayList<>();
        foods = new ArrayList<>();
        enemies = new ArrayList<>();
        pacman = null;

        for (int r = 0; r < rowCount; r++) {
            String row = tileMap[r];
            for (int c = 0; c < columnCount; c++) {
                char ch = row.charAt(c);
                int x = c * tileSize, y = r * tileSize;
                switch (ch) {
                    case 'X' -> walls.add(new Block(wallImage, x, y, tileSize, tileSize));
                    // Ghost spawn points
                    case 'b' -> enemies.add(new Block(blueEnemyImage, x, y, tileSize, tileSize));
                    case 'o' -> enemies.add(new Block(orangeEnemyImage, x, y, tileSize, tileSize));
                    case 'p' -> enemies.add(new Block(pinkEnemyImage, x, y, tileSize, tileSize));
                    case 'r' -> enemies.add(new Block(redEnemyImage, x, y, tileSize, tileSize));
                    case 'P' -> pacman = new Block(pmRImage, x, y, tileSize, tileSize);
                    // Standard food dot (small, centered block)
                    case ' ' -> foods.add(new Block(null, x + 14, y + 14, 4, 4));
                }
            }
        }

        // Fallback for Pac-Man placement
        if (pacman == null) pacman = new Block(pmRImage, tileSize*9, tileSize*15, tileSize, tileSize);
    }

    // ----------------------- RENDERING -----------------------
    /**
     * The main drawing method. Calls the appropriate drawing screen based on game state.
     */
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
    /**
     * Called by the Timer every 50ms. Triggers the main game update (`move()`) and redraws the screen.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameStarted && !gameOver) move();
        repaint();
    }

    /**
     * Handles all core game logic: state updates, movement, collision detection, and AI planning.
     */
    public void move() {
        tickCounter++;
        // Decrement frightened timer if active
        if (isFrightened) frightenedTimer--;
        // End frightened mode if timer runs out
        if (frightenedTimer <= 0) isFrightened = false;

        // --- Pac-Man Movement ---
        // If buffered input is available and movement is not blocked, execute the turn
        if (nextDirection != ' ' && canMove(pacman, nextDirection)) {
            pacman.updateDirection(nextDirection);
            nextDirection = ' ';
        }
        // Apply current velocity for smooth movement
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
                    if (lives <= 0) { gameOver = true; return; }
                    resetPositions();
                    return; // Stop game loop update immediately upon death/reset
                }
            }

            // AI Planning: Check if it's time for the ghost to re-plan its direction
            if ((tickCounter + enemy.planOffset) % aiPlanInterval == 0) {
                // Find the next best direction using BFS pathfinding towards Pac-Man
                char planned = findNextDirectionTowardsPacman(enemy);
                if (canMove(enemy, planned)) {
                    enemy.updateDirection(planned);
                } else {
                    // If planned move is blocked, fallback to a random valid direction
                    List<Character> allowed = new ArrayList<>();
                    for (char d : directions) if (canMove(enemy, d)) allowed.add(d);
                    if (!allowed.isEmpty()) enemy.updateDirection(allowed.get(random.nextInt(allowed.size())));
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
                for (Block wall : walls) if (collision(enemy, wall)) { bumped = true; break; }
            }

            if (bumped) {
                // Undo movement and force a random, valid new direction
                enemy.x -= enemy.velocityX;
                enemy.y -= enemy.velocityY;
                List<Character> allowed = new ArrayList<>();
                for (char d : directions) if (canMove(enemy, d)) allowed.add(d);
                if (!allowed.isEmpty()) enemy.updateDirection(allowed.get(random.nextInt(allowed.size())));
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
    /**
     * Checks if a move in a specific direction is blocked by a wall.
     */
    private boolean canMove(Block b, char dir) {
        int tempX = b.x, tempY = b.y;
        int step = Math.max(1, tileSize / 4);
        // Calculate the next potential position
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

    /**
     * Standard Axis-Aligned Bounding Box (AABB) collision detection.
     */
    public boolean collision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
    }

    /**
     * Resets Pac-Man and all Ghosts to their starting positions and states.
     */
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

    /**
     * Determines if a tile coordinate is not a wall.
     */
    private boolean isWalkable(int row, int col) {
        if (row < 0 || row >= rowCount || col < 0 || col >= columnCount) return false;
        return tileMap[row].charAt(col) != 'X';
    }

    /**
     * BFS (Breadth-First Search) implementation for pathfinding.
     * Finds the fastest route on the tile grid from the ghost to Pac-Man and returns the first step.
     */
    private char findNextDirectionTowardsPacman(Block enemy) {
        // Convert pixel positions to grid coordinates
        int startR = Math.max(0, Math.min(rowCount - 1, enemy.y / tileSize));
        int startC = Math.max(0, Math.min(columnCount - 1, enemy.x / tileSize));
        int targetR = Math.max(0, Math.min(rowCount - 1, pacman.y / tileSize));
        int targetC = Math.max(0, Math.min(columnCount - 1, pacman.x / tileSize));

        if (startR == targetR && startC == targetC) return enemy.direction;

        // Direction vectors and corresponding character representations
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}}; // U, D, L, R
        char[] dirChars = {'U','D','L','R'};

        boolean[][] visited = new boolean[rowCount][columnCount];
        int[][] prev = new int[rowCount][columnCount]; // Stores the direction index used to arrive at this tile
        for (int i = 0; i < rowCount; i++) Arrays.fill(prev[i], -1);

        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{startR, startC});
        visited[startR][startC] = true;

        boolean found = false;
        // BFS loop
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1];
            if (r == targetR && c == targetC) { found = true; break; } // Target found

            for (int i = 0; i < 4; i++) {
                int nr = r + dirs[i][0], nc = c + dirs[i][1];
                // Check if the neighbor is valid, unvisited, and walkable
                if (nr >= 0 && nr < rowCount && nc >= 0 && nc < columnCount && !visited[nr][nc] && isWalkable(nr, nc)) {
                    visited[nr][nc] = true;
                    prev[nr][nc] = i; // Record the direction index (i) that led here
                    q.add(new int[]{nr, nc});
                }
            }
        }

        // Handle case where path is not found (should be rare)
        if (!found) {
            List<Character> allowed = new ArrayList<>();
            for (char d : directions) if (canMove(enemy, d)) allowed.add(d);
            if (!allowed.isEmpty()) return allowed.get(random.nextInt(allowed.size()));
            return directions[random.nextInt(4)];
        }

        // Trace back from the target to the start to find the FIRST step
        int r = targetR, c = targetC;
        char nextDir = directions[random.nextInt(4)]; // Fallback result
        while (!(r == startR && c == startC)) {
            int dir = prev[r][c];
            if (dir == -1) break;
            int pr = r - dirs[dir][0]; // Calculate previous tile position
            int pc = c - dirs[dir][1];

            // If the previous tile is the starting tile, 'dir' is our immediate next move
            if (pr == startR && pc == startC) {
                return dirChars[dir];
            }
            r = pr; c = pc; // Move to the previous tile and continue tracing
        }

        return nextDir; // Return random fallback if traceback fails
    }

    // ----------------------- INPUT -----------------------
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {}

    /**
     * Handles keyboard input for game control and direction buffering.
     */
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