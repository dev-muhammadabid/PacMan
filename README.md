# 🟡 PacMan: (AI Edition)

**Relive the classic arcade experience with a modern, adaptive twist.**

PacMan: Neo-Retro is a faithful implementation of the iconic maze game, built using Java and Swing for a truly nostalgic feel. While the graphics remain true to the 8-bit era, the enemy AI has been completely overhauled with advanced **Artificial Intelligence** and **Machine Learning** techniques, transforming the ghosts from simple pursuers into coordinated, adaptive, and intelligent adversaries.



---

## ✨ Features

We didn't just rebuild the game—we rebuilt the enemy. This PacMan experience features a multi-layered AI system designed to keep players on their toes.

### 👻 Smart Ghosts: Adaptive Enemy AI

The core of the modernization lies in the ghosts' intelligence. They are no longer predictable; they **learn** and **adapt** to your playstyle.

* **Personality-based AI:** Each of the four ghosts (Blinky, Pinky, Inky, and Clyde) operates with a distinct, evolving personality:
    * **Aggressive** (Blinky)
    * **Strategic** (Pinky)
    * **Sneaky/Ambush** (Inky)
    * **Fickle/Random** (Clyde)
* **Dynamic Difficulty:** An **AI Game Director** monitors the player's performance in real-time. It adjusts parameters like **ghost aggression**, **power pellet frequency**, or **enemy movement speed** to maintain an exciting, yet balanced, difficulty curve.
* **Learning & Coordination:** Ghosts learn from your past behavior. If you repeatedly use the same escape route (e.g., the top-right corner), the AI will adjust the ghost's target vectors to preemptively cut you off.

### 🧠 Predictive Pathfinding & Coordination

The ghosts use advanced algorithms to not just chase, but to **predict** and **coordinate** their attacks.

* **Neural Predictor:** A lightweight neural model estimates the player’s most likely next move based on their current direction, velocity, and proximity to junctions.
* **Predictive Pathfinding (Monte Carlo Simulation):** Ghosts use **Monte Carlo simulations** to quickly run thousands of potential game scenarios, allowing them to simulate multiple possible player escape routes and select the optimal, coordinated chase pattern.
* **BFS Pathfinding:** Underneath the predictive layer, all ghost movement relies on **Breadth-First Search (BFS)** to guarantee the shortest, valid path through the maze.

### 🎮 Classic Game Structure & Mechanics

The foundation remains the beloved classic Pac-Man.

* **Proper Ghost Roles:** The ghosts adhere to the classic **Chase**, **Scatter**, and **Ambush** state cycle.
* **Nostalgic Graphics:** The original tile size and aesthetic are maintained for a true nostalgic feel.
* **Clean Screens:** Intuitive and classic **Start** and **Game Over** screens.
* **Food & Scoring:** Accurate food pellet tracking and scoring mechanics.
* **Sound Effects:** Authentic, recognizable sound effects for eating, death, and starting the game.

---

## 🛠️ Tech Stack

This project is built entirely using standard Java technologies, with a focus on implementing complex algorithms efficiently.

| Category | Technology/Language | Purpose |
| :--- | :--- | :--- |
| **Language** | Java | Core game logic and application framework. |
| **Frontend/Graphics** | Java Swing & AWT | Rendering the game board, handling input, and maintaining the nostalgic 8-bit look. |
| **Game Loop** | `javax.swing.Timer` | Controls the precise, fixed-time game update cycle (e.g., 20 FPS). |
| **Pathfinding** | **BFS (Breadth-First Search)** | Efficiently calculating shortest routes for ghosts on the grid map. |
| **AI/ML Core** | Custom Java Implementation | Neural predictor, Monte Carlo simulation, and behavior trees for the adaptive ghost AI. |
| **Audio** | `javax.sound.sampled` | Handling and playback of game sound effects in a non-blocking thread. |

---

## 🚀 Getting Started

### Prerequisites

* Java Development Kit (JDK) 17 or newer.

### Running the Game

1.  **Clone the Repository:**
    ```bash
    git clone [YOUR-REPO-LINK-HERE]
    cd PacMan
    ```
2.  **Compile the Code:**
    ```bash
    javac PacMan.java SoundManager.java
    ```
3.  **Run the Application:**
    Ensure your compiled classes (`.class` files) and the resource folders (`images/`, `sfx/`) are correctly structured.
    ```bash
    java PacMan
    ```

### Controls

| Key | Action |
| :--- | :--- |
| **↑** | Move Up / Buffer Up |
| **↓** | Move Down / Buffer Down |
| **←** | Move Left / Buffer Left |
| **→** | Move Right / Buffer Right |
| **ENTER** | Start Game / Restart Game |

---