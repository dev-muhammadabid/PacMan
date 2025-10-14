import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

/**
 * SoundManager class: Manages loading and playing specific sound effects in the game.
 * Uses the Java Sound API (javax.sound.sampled) for non-blocking sound playback.
 */
public class SoundManager {

    // Define sound keys, mapping them to the file names in the 'sfx' folder
    public static final String WAKA = "pacman_eating";
    public static final String EAT_GHOST = "ghost_eating";
    public static final String DEATH = "pacman_death";
    public static final String START_GAME = "pacman_start";

    private final String basePath = "sfx/"; // Resource folder path where WAV files are expected

    /**
     * Plays a sound effect once in a new thread.
     * This prevents the game from freezing while the sound is being loaded and played.
     * @param soundName The key name of the sound file (e.g., "pacman_eating") without the .wav extension.
     */
    public void playSound(String soundName) {
        // Start a new thread for sound playback to keep the main game loop responsive
        new Thread(() -> {
            try {
                // Construct the full resource path for the sound file
                URL soundURL = getClass().getResource(basePath + soundName + ".wav");
                if (soundURL == null) {
                    System.err.println("Sound file not found: " + basePath + soundName + ".wav");
                    return;
                }

                // Get an AudioInputStream from the URL
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);

                // Get a Clip, which is used for loading all audio data before playing
                Clip clip = AudioSystem.getClip();

                // Load the audio data into the clip
                clip.open(audioStream);

                // Add a listener to automatically close the clip when playback stops
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close(); // Release system resources
                    }
                });

                clip.start(); // Start playback

            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.err.println("Error playing sound '" + soundName + "': " + e.getMessage());
                // e.printStackTrace(); // Uncomment for detailed debugging
            }
        }).start();
    }
}