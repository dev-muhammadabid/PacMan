import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;

/**
 * PacMan class: Main game logic and rendering for a simple Pac-Man clone.
 * Extends JPanel for custom drawing and implements ActionListener and KeyListener
 * for the game loop and keyboard input handling.
 */
public class PacMan extends JPanel implements ActionListener, KeyListener {

    /**
     * Block class represents a single game object: walls, Pac-Man, enemies, or food.
     * Stores position, size, image, direction, and velocity.
     */
    class Block {
        int x, y, width, height;   // Current position and size
        Image image;               // Image to draw
        int startX, startY;        // Initial position for reset
        char direction = 'U';      // Current moving direction ('U','D','L','R')
        int velocityX = 0, velocityY = 0; // Current velocity in pixels

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
         * Updates the block's direction and velocity.
         * Also handles Pac-Man image updates when changing direction.
         * @param newDirection New direction to move.
         */
        void updateDirection(char newDirection) {
            char prevDirection = this.direction;
            this.direction = newDirection;
            updateVelocity();

            // Update Pac-Man image if direction changes
            if (this == pacman && this.direction != prevDirection) {
                switch (this.direction) {
                    case 'U' -> this.image = pmUImage;
                    case 'D' -> this.image = pmDImage;
                    case 'L' -> this.image = pmLImage;
                    case 'R' -> this.image = pmRImage;
                }
            }

            // Move block by its velocity
            this.x += this.velocityX;
            this.y += this.velocityY;

            // Prevent moving into walls
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        /**
         * Updates velocity based on current direction.
         */
        void updateVelocity() {
            switch (this.direction) {
                case 'U' -> { velocityX = 0; velocityY = -tileSize / 4; }
                case 'D' -> { velocityX = 0; velocityY = tileSize / 4; }
                case 'L' -> { velocityX = -tileSize / 4; velocityY = 0; }
                case 'R' -> { velocityX = tileSize / 4; velocityY = 0; }
            }
        }

        /**
         * Resets block position and velocity to starting values.
         */
        void reset() {
            x = startX;
            y = startY;
            velocityX = 0;
            velocityY = 0;
        }
    }

    // ----------------------- VARIABLES -----------------------
    private int rowCount = 21, columnCount = 19, tileSize = 32; // Board configuration
    private int boardWidth = columnCount * tileSize, boardHeight = rowCount * tileSize;

    // Game images
    private Image wallImage, blueEnemyImage, orangeEnemyImage, pinkEnemyImage, redEnemyImage;
    private Image pmUImage, pmDImage, pmLImage, pmRImage;

    // Map representation as strings
    private String[] tileMap = {
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

    HashSet<Block> walls, foods, enemies;  // Stores walls, food pellets, and enemies
    Block pacman;                           // Player character

    Timer gameLoop;                         // Main game loop timer
    char[] directions = {'U','D','L','R'};  // Possible directions
    Random random = new Random();
    int score = 0, lives = 3;               // Player stats

    char nextDirection = ' ';               // Stores next direction input
    boolean gameStarted = false;            // Track if game has started
    boolean gameOver = false;               // Track if game is over

    // ----------------------- CONSTRUCTOR -----------------------
    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load images for walls, enemies, and Pac-Man
        wallImage = new ImageIcon(getClass().getResource("images/wall.png")).getImage();
        blueEnemyImage = new ImageIcon(getClass().getResource("images/blueEnemy.png")).getImage();
        orangeEnemyImage = new ImageIcon(getClass().getResource("images/orangeEnemy.png")).getImage();
        pinkEnemyImage = new ImageIcon(getClass().getResource("images/pinkEnemy.png")).getImage();
        redEnemyImage = new ImageIcon(getClass().getResource("images/redEnemy.png")).getImage();

        pmUImage = new ImageIcon(getClass().getResource("images/pm-Up.png")).getImage();
        pmDImage = new ImageIcon(getClass().getResource("images/pm-Down.png")).getImage();
        pmLImage = new ImageIcon(getClass().getResource("images/pm-Left.png")).getImage();
        pmRImage = new ImageIcon(getClass().getResource("images/pm-Right.png")).getImage();

        loadMap();  // Initialize map based on tileMap

        // Randomize initial enemy directions
        for (Block enemy : enemies) {
            char newDirection = directions[random.nextInt(4)];
            enemy.updateDirection(newDirection);
        }

        // Start game loop
        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    // ----------------------- LOAD MAP -----------------------
    /**
     * Converts tileMap strings into walls, enemies, Pac-Man, and food blocks.
     */
    public void loadMap() {
        walls = new HashSet<>();
        foods = new HashSet<>();
        enemies = new HashSet<>();

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char ch = row.charAt(c);
                int x = c * tileSize, y = r * tileSize;
                switch(ch) {
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
    }

    // ----------------------- PAINT -----------------------
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!gameStarted) {
            drawStartScreen(g);
        } else if (gameOver) {
            drawGameOverScreen(g);
        } else {
            drawGame(g);
        }
    }

    // Draws the start screen before the game begins
    private void drawStartScreen(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0,0,getWidth(),getHeight());

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 64));
        g.drawString("PAC-MAN", boardWidth/2 - 150, boardHeight/2 - 50);

        g.setColor(Color.YELLOW);
        g.fillArc(boardWidth/2 - 30, boardHeight/2, 60, 60, 30, 300);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Press ENTER to Start", boardWidth/2 - 120, boardHeight/2 + 100);
    }

    // Draws the game over screen
    private void drawGameOverScreen(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0,0,getWidth(),getHeight());

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 64));
        g.drawString("GAME OVER", boardWidth/2 - 200, boardHeight/2 - 50);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 28));
        g.drawString("Score: " + score, boardWidth/2 - 70, boardHeight/2 + 20);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Press ENTER to Restart", boardWidth/2 - 140, boardHeight/2 + 80);
    }

    // Draws the gameplay screen
    private void drawGame(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);
        for (Block enemy : enemies) g.drawImage(enemy.image, enemy.x, enemy.y, enemy.width, enemy.height, null);
        for (Block wall : walls) g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);

        g.setColor(Color.WHITE);
        for (Block food : foods) g.fillRect(food.x, food.y, food.width, food.height);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("x" + lives + " Score: " + score, tileSize/2, tileSize/2);
    }

    // ----------------------- GAME LOOP -----------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameStarted && !gameOver) {
            move(); // Update game state
        }
        repaint(); // Refresh the screen
    }

    // Handles movement of Pac-Man and enemies, collision detection, and food collection
    public void move() {
        if (nextDirection != ' ' && canMove(pacman, nextDirection)) {
            pacman.updateDirection(nextDirection);
            nextDirection = ' ';
        }

        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // Wall collisions
        for (Block wall : walls) if (collision(pacman, wall)) {
            pacman.x -= pacman.velocityX; pacman.y -= pacman.velocityY; break;
        }

        // Enemy movement and collisions
        for (Block enemy : enemies) {
            if (collision(enemy, pacman)) {
                lives--;
                if (lives == 0) { gameOver = true; return; }
                resetPositions();
            }

            // Enemy movement logic
            if (enemy.y == tileSize*9 && enemy.direction != 'U' && enemy.direction != 'D') enemy.updateDirection('U');
            enemy.x += enemy.velocityX; enemy.y += enemy.velocityY;

            for (Block wall : walls)
                if (collision(enemy, wall) || enemy.x <= 0 || enemy.x + enemy.width >= boardWidth) {
                    enemy.x -= enemy.velocityX; enemy.y -= enemy.velocityY;
                    enemy.updateDirection(directions[random.nextInt(4)]);
                }
        }

        // Check for food collection
        Block foodEaten = null;
        for (Block food : foods) if (collision(pacman, food)) { foodEaten = food; score += 10; }
        foods.remove(foodEaten);

        // Reset map if all food collected
        if (foods.isEmpty()){ loadMap(); resetPositions(); }
    }

    /**
     * Checks if a block can move in the specified direction without hitting a wall.
     */
    private boolean canMove(Block b, char dir) {
        int tempX=b.x, tempY=b.y;
        switch(dir){
            case 'U'-> tempY-=tileSize/4;
            case 'D'-> tempY+=tileSize/4;
            case 'L'-> tempX-=tileSize/4;
            case 'R'-> tempX+=tileSize/4;
        }
        Block test=new Block(null,tempX,tempY,b.width,b.height);
        for(Block wall:walls) if(collision(test,wall)) return false;
        return true;
    }

    /**
     * Simple rectangle-based collision detection.
     */
    public boolean collision(Block a, Block b){
        return a.x<b.x+b.width && a.x+a.width>b.x && a.y<b.y+b.height && a.y+a.height>b.y;
    }

    /**
     * Resets Pac-Man and all enemy positions to their starting locations.
     */
    public void resetPositions() {
        pacman.reset(); nextDirection=' ';
        for(Block enemy:enemies){ enemy.reset(); enemy.updateDirection(directions[random.nextInt(4)]);}
    }

    // ----------------------- KEY LISTENERS -----------------------
    @Override
    public void keyTyped(KeyEvent e){}

    @Override
    public void keyPressed(KeyEvent e){}

    @Override
    public void keyReleased(KeyEvent e){
        // Start the game if ENTER pressed on start screen
        if (!gameStarted && e.getKeyCode()==KeyEvent.VK_ENTER){
            gameStarted = true;
            resetPositions(); score=0; lives=3; gameOver=false; gameLoop.start();
            return;
        }

        // Restart game if ENTER pressed after game over
        if (gameOver && e.getKeyCode()==KeyEvent.VK_ENTER){
            resetPositions(); loadMap(); score=0; lives=3; gameOver=false;
            return;
        }

        // Handle movement keys
        if (gameStarted && !gameOver) {
            if (e.getKeyCode()==KeyEvent.VK_UP) nextDirection='U';
            else if (e.getKeyCode()==KeyEvent.VK_DOWN) nextDirection='D';
            else if (e.getKeyCode()==KeyEvent.VK_LEFT) nextDirection='L';
            else if (e.getKeyCode()==KeyEvent.VK_RIGHT) nextDirection='R';
        }
    }
}
