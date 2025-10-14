import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

/**
 * PacMan class: Main game logic and rendering for a simple Pac-Man clone.
 * Added: BFS ghost pathfinding, minor cleanup, and adjustable AI planning.
 */
public class PacMan extends JPanel implements ActionListener, KeyListener {

    class Block {
        int x, y, width, height;
        Image image;
        int startX, startY;
        char direction = 'U';
        int velocityX = 0, velocityY = 0;

        // for ghost AI scheduling (small random offset so ghosts don't all plan same tick)
        int planOffset = 0;

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
            updateVelocity();

            // Update Pac-Man image when direction changes
            if (this == pacman && this.direction != prevDirection) {
                switch (this.direction) {
                    case 'U' -> this.image = pmUImage;
                    case 'D' -> this.image = pmDImage;
                    case 'L' -> this.image = pmLImage;
                    case 'R' -> this.image = pmRImage;
                }
            }

            // Move by velocity (small step)
            this.x += this.velocityX;
            this.y += this.velocityY;

            // Prevent moving into walls by reversing the step and restoring direction
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                    break;
                }
            }
        }

        void updateVelocity() {
            int step = Math.max(1, tileSize / 4);
            switch (this.direction) {
                case 'U' -> { velocityX = 0; velocityY = -step; }
                case 'D' -> { velocityX = 0; velocityY = step; }
                case 'L' -> { velocityX = -step; velocityY = 0; }
                case 'R' -> { velocityX = step; velocityY = 0; }
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

    // Images
    private Image wallImage, blueEnemyImage, orangeEnemyImage, pinkEnemyImage, redEnemyImage;
    private Image pmUImage, pmDImage, pmLImage, pmRImage;

    // Map (string rows)
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

    // Game objects
    private List<Block> walls, foods, enemies;
    private Block pacman;

    // Game loop & input
    private Timer gameLoop;
    private final char[] directions = {'U','D','L','R'};
    private final Random random = new Random();
    private int score = 0, lives = 3;

    private char nextDirection = ' ';
    private boolean gameStarted = false, gameOver = false;

    // AI tuning
    private final int aiPlanInterval = 6; // lower -> ghosts plan more often (harder)
    private int tickCounter = 0;

    // ----------------------- CONSTRUCTOR -----------------------
    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load images (ensure these resources exist at runtime)
        wallImage = new ImageIcon(getClass().getResource("images/wall.png")).getImage();
        blueEnemyImage = new ImageIcon(getClass().getResource("images/blueEnemy.png")).getImage();
        orangeEnemyImage = new ImageIcon(getClass().getResource("images/orangeEnemy.png")).getImage();
        pinkEnemyImage = new ImageIcon(getClass().getResource("images/pinkEnemy.png")).getImage();
        redEnemyImage = new ImageIcon(getClass().getResource("images/redEnemy.png")).getImage();

        pmUImage = new ImageIcon(getClass().getResource("images/pm-Up.png")).getImage();
        pmDImage = new ImageIcon(getClass().getResource("images/pm-Down.png")).getImage();
        pmLImage = new ImageIcon(getClass().getResource("images/pm-Left.png")).getImage();
        pmRImage = new ImageIcon(getClass().getResource("images/pm-Right.png")).getImage();

        loadMap(); // populate walls, foods, enemies, pacman

        // randomize initial enemy directions and give them planning offsets
        for (Block enemy : enemies) {
            enemy.updateDirection(directions[random.nextInt(4)]);
            enemy.planOffset = random.nextInt(aiPlanInterval);
        }

        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    // ----------------------- MAP LOADING -----------------------
    public void loadMap() {
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
                    case 'b' -> enemies.add(new Block(blueEnemyImage, x, y, tileSize, tileSize));
                    case 'o' -> enemies.add(new Block(orangeEnemyImage, x, y, tileSize, tileSize));
                    case 'p' -> enemies.add(new Block(pinkEnemyImage, x, y, tileSize, tileSize));
                    case 'r' -> enemies.add(new Block(redEnemyImage, x, y, tileSize, tileSize));
                    case 'P' -> pacman = new Block(pmRImage, x, y, tileSize, tileSize);
                    case ' ' -> foods.add(new Block(null, x + 14, y + 14, 4, 4));
                }
            }
        }

        // just in case pacman spot was missing, ensure it exists (fallback)
        if (pacman == null) pacman = new Block(pmRImage, tileSize*9, tileSize*15, tileSize, tileSize);
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
        // Draw map objects
        for (Block wall : walls) g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        for (Block food : foods) {
            g.setColor(Color.WHITE);
            g.fillRect(food.x, food.y, food.width, food.height);
        }

        // Draw enemies then pacman (so pacman appears above sometimes)
        for (Block enemy : enemies) g.drawImage(enemy.image, enemy.x, enemy.y, enemy.width, enemy.height, null);
        if (pacman != null) g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        // HUD
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

        // Pac-Man input & movement
        if (nextDirection != ' ' && canMove(pacman, nextDirection)) {
            pacman.updateDirection(nextDirection);
            nextDirection = ' ';
        }
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // Prevent Pac-Man entering walls
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        // Enemy logic
        Iterator<Block> enemyIter = enemies.iterator();
        while (enemyIter.hasNext()) {
            Block enemy = enemyIter.next();

            // Collision with Pac-Man
            if (collision(enemy, pacman)) {
                lives--;
                if (lives <= 0) { gameOver = true; return; }
                resetPositions();
                return; // bail until next tick
            }

            // If enemy is aligned to tile grid, consider planning a new direction
            int enemyRow = enemy.y / tileSize;
            int enemyCol = enemy.x / tileSize;

            if ((tickCounter + enemy.planOffset) % aiPlanInterval == 0) {
                char planned = findNextDirectionTowardsPacman(enemy);
                if (canMove(enemy, planned)) {
                    enemy.updateDirection(planned);
                } else {
                    // fallback: random direction that is allowed
                    List<Character> allowed = new ArrayList<>();
                    for (char d : directions) if (canMove(enemy, d)) allowed.add(d);
                    if (!allowed.isEmpty()) enemy.updateDirection(allowed.get(random.nextInt(allowed.size())));
                }
            }

            // Move enemy
            enemy.x += enemy.velocityX;
            enemy.y += enemy.velocityY;

            // If enemy hits wall or boundaries, step back and pick new random direction
            boolean bumped = false;
            if (enemy.x <= 0 || enemy.x + enemy.width >= boardWidth || enemy.y <= 0 || enemy.y + enemy.height >= boardHeight)
                bumped = true;
            else {
                for (Block wall : walls) if (collision(enemy, wall)) { bumped = true; break; }
            }

            if (bumped) {
                enemy.x -= enemy.velocityX;
                enemy.y -= enemy.velocityY;
                // pick a valid random direction
                List<Character> allowed = new ArrayList<>();
                for (char d : directions) if (canMove(enemy, d)) allowed.add(d);
                if (!allowed.isEmpty()) enemy.updateDirection(allowed.get(random.nextInt(allowed.size())));
            }
        }

        // Food collection
        Block eaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) { eaten = food; score += 10; break; }
        }
        if (eaten != null) foods.remove(eaten);

        // Reset level when no food left
        if (foods.isEmpty()) {
            loadMap();
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

    /**
     * BFS to find the immediate next direction from an enemy towards Pac-Man.
     * Returns one of 'U','D','L','R'. Falls back to a random direction if path not found.
     */
    private char findNextDirectionTowardsPacman(Block enemy) {
        int startR = Math.max(0, Math.min(rowCount - 1, enemy.y / tileSize));
        int startC = Math.max(0, Math.min(columnCount - 1, enemy.x / tileSize));
        int targetR = Math.max(0, Math.min(rowCount - 1, pacman.y / tileSize));
        int targetC = Math.max(0, Math.min(columnCount - 1, pacman.x / tileSize));

        if (startR == targetR && startC == targetC) return enemy.direction; // already same tile

        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        char[] dirChars = {'U','D','L','R'};

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
            if (r == targetR && c == targetC) { found = true; break; }
            for (int i = 0; i < 4; i++) {
                int nr = r + dirs[i][0], nc = c + dirs[i][1];
                if (nr >= 0 && nr < rowCount && nc >= 0 && nc < columnCount && !visited[nr][nc] && isWalkable(nr, nc)) {
                    visited[nr][nc] = true;
                    prev[nr][nc] = i; // store how we arrived to (nr,nc) from previous
                    q.add(new int[]{nr, nc});
                }
            }
        }

        if (!found) {
            // fallback: choose a valid random direction
            List<Character> allowed = new ArrayList<>();
            for (char d : directions) if (canMove(enemy, d)) allowed.add(d);
            if (!allowed.isEmpty()) return allowed.get(random.nextInt(allowed.size()));
            return directions[random.nextInt(4)];
        }

        // trace back from target to start to get the first step
        int r = targetR, c = targetC;
        while (!(r == startR && c == startC)) {
            int dir = prev[r][c];
            if (dir == -1) break; // should not happen
            int pr = r - dirs[dir][0];
            int pc = c - dirs[dir][1];
            if (pr == startR && pc == startC) {
                return dirChars[dir];
            }
            r = pr; c = pc;
        }

        // last fallback
        return directions[random.nextInt(4)];
    }

    // ----------------------- INPUT -----------------------
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (!gameStarted && e.getKeyCode() == KeyEvent.VK_ENTER) {
            gameStarted = true;
            resetPositions();
            score = 0;
            lives = 3;
            gameOver = false;
            gameLoop.start();
            return;
        }

        if (gameOver && e.getKeyCode() == KeyEvent.VK_ENTER) {
            resetPositions();
            loadMap();
            score = 0;
            lives = 3;
            gameOver = false;
            return;
        }

        if (gameStarted && !gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_UP) nextDirection = 'U';
            else if (e.getKeyCode() == KeyEvent.VK_DOWN) nextDirection = 'D';
            else if (e.getKeyCode() == KeyEvent.VK_LEFT) nextDirection = 'L';
            else if (e.getKeyCode() == KeyEvent.VK_RIGHT) nextDirection = 'R';
        }
    }
}
