import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) throws Exception {
        // Define the dimensions of the game board in terms of rows and columns
        int rowCount = 21;        // Number of rows on the game board
        int columnCount = 19;     // Number of columns on the game board
        int tileSize = 32;        // Size of each tile in pixels

        // Calculate the overall size of the game board in pixels
        int boardWidth = columnCount * tileSize;
        int boardHeight = rowCount * tileSize;

        // Create the main window (JFrame) for the game
        JFrame frame = new JFrame("Pac Man"); // Set the title of the window

        // Configure the JFrame
        frame.setSize(boardWidth, boardHeight);  // Set the window size based on board dimensions
        frame.setLocationRelativeTo(null);       // Center the window on the screen
        frame.setResizable(false);               // Prevent resizing to maintain game layout
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Exit the program when window is closed

        // Create the main game panel (PacMan class should extend JPanel)
        PacMan pacmanGame = new PacMan();

        // Add the game panel to the JFrame
        frame.add(pacmanGame);

        // Adjust the frame size to fit the game panel's preferred size
        frame.pack();

        // Request keyboard focus for the game panel so it can handle input
        pacmanGame.requestFocus();

        // Make the game window visible
        frame.setVisible(true);
    }
}
