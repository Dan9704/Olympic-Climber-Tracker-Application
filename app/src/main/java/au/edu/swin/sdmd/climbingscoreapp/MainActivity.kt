package au.edu.swin.sdmd.climbingscoreapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    // Declare UI elements
    private lateinit var scoreTextView: TextView
    private lateinit var holdTextView: TextView
    private lateinit var gameOverTextView: TextView
    private lateinit var maxScoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var athleteNameEditText: EditText
    private lateinit var congratulationTextView: TextView

    // Game state variables
    private var score = 0
    private var hold = 0
    private var isGameOver = false
    private var isMaxScoreReached = false

    // Handler for updating the timer
    private var handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var isTimerRunning = false

    // MediaPlayer for background music and sound effects
    private lateinit var backgroundMediaPlayer: MediaPlayer
    private lateinit var climbSoundPlayer: MediaPlayer
    private lateinit var fallSoundPlayer: MediaPlayer

    // Override the onCreate method to set up the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge UI display
        enableEdgeToEdge()
        // Set the layout for the activity
        setContentView(R.layout.activity_main)
        // Set up insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI elements
        scoreTextView = findViewById(R.id.scoreTextView)
        holdTextView = findViewById(R.id.holdTextView)
        gameOverTextView = findViewById(R.id.gameOverTextView)
        maxScoreTextView = findViewById(R.id.maxScoreTextView)
        timerTextView = findViewById(R.id.timerTextView)
        athleteNameEditText = findViewById(R.id.athleteNameEditText)
        congratulationTextView = findViewById(R.id.congratulationTextView)

        // Initialize buttons
        val climbButton: Button = findViewById(R.id.climbButton)
        val fallButton: Button = findViewById(R.id.fallButton)
        val resetButton: Button = findViewById(R.id.resetButton)

        // Initialize the MediaPlayer for background music
        backgroundMediaPlayer = MediaPlayer.create(this, R.raw.background_music)
        backgroundMediaPlayer.isLooping = true // Loop the background music
        backgroundMediaPlayer.start()

        // Initialize the MediaPlayer for sound effects
        climbSoundPlayer = MediaPlayer.create(this, R.raw.climb_sound)
        fallSoundPlayer = MediaPlayer.create(this, R.raw.fall_sound)

        // Set click listeners for buttons
        climbButton.setOnClickListener {
            if (!isTimerRunning && !isGameOver) {
                startTimer() // Start the timer when the game starts
            }
            if (!isGameOver && !isMaxScoreReached) {
                // Play climb sound effect
                climbSoundPlayer.start()

                hold++ // Increase hold value
                when (hold) {
                    in 1..3 -> score++
                    in 4..6 -> score += 2
                    in 7..9 -> score += 3
                }
                updateUI() // Update the UI with the new score and hold values
            }
        }

        // Set click listeners for Fall button
        fallButton.setOnClickListener {
            if (!isGameOver && !isMaxScoreReached && hold > 0) {
                // Play fall sound effect
                fallSoundPlayer.start()
                score -= 3
                if (score < 0) score = 0
                updateUI() // Update the UI after score change
                gameOverTextView.visibility = TextView.VISIBLE
                // Display congratulation message
                val athleteName = athleteNameEditText.text.toString()
                congratulationTextView.text = getString(R.string.congratulations, athleteName, score, timerTextView.text)
                congratulationTextView.visibility = TextView.VISIBLE
                stopTimer() // Stop the timer when the game ends
                isGameOver = true  // Set the game over flag to true
            }
        }

        // Set click listeners for Reset button
        resetButton.setOnClickListener {
            resetGame()
        }
    }

    // Save the current state of the game
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("score", score)
        outState.putInt("hold", hold)
        outState.putBoolean("isGameOver", isGameOver)
        outState.putBoolean("isMaxScoreReached", isMaxScoreReached)
        outState.putLong("startTime", startTime)
        outState.putBoolean("isTimerRunning", isTimerRunning)
    }

    // Restore the saved state of the game
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        score = savedInstanceState.getInt("score")
        hold = savedInstanceState.getInt("hold")
        isGameOver = savedInstanceState.getBoolean("isGameOver")
        isMaxScoreReached = savedInstanceState.getBoolean("isMaxScoreReached")
        startTime = savedInstanceState.getLong("startTime")
        isTimerRunning = savedInstanceState.getBoolean("isTimerRunning")

        // Update the UI with the restored state
        updateUI()

        // Restart the timer if it was running before rotation
        if (isTimerRunning) {
            handler.post(timerRunnable)
        }
    }

    // Update the UI with the current score and hold values
    private fun updateUI() {
        val scoreLabel = getString(R.string.score_label)
        val holdLabel = getString(R.string.hold_label)

        scoreTextView.text = "$scoreLabel $score"
        holdTextView.text = "$holdLabel $hold"

        // Set text color based on hold value
        when (hold) {
            in 1..3 -> holdTextView.setTextColor(resources.getColor(R.color.blue))
            in 4..6 -> holdTextView.setTextColor(resources.getColor(R.color.green))
            in 7..9 -> holdTextView.setTextColor(resources.getColor(R.color.red))
        }

        // Check if the maximum score is reached, and if so, end the game
        if (hold == 9) {
            maxScoreReached()
            isGameOver = true
        }
    }

    // Handle the case when the maximum score is reached
    private fun maxScoreReached() {
        maxScoreTextView.visibility = TextView.VISIBLE
        stopTimer()
        isMaxScoreReached = true

        // Display congratulation message
        val athleteName = athleteNameEditText.text.toString()
        congratulationTextView.text = getString(R.string.congratulations, athleteName, score, timerTextView.text)
        congratulationTextView.visibility = TextView.VISIBLE
    }

    // Start the timer for the game
    private fun startTimer() {
        startTime = System.currentTimeMillis()
        isTimerRunning = true
        handler.post(timerRunnable)
    }

    // Stop the timer for the game
    private fun stopTimer() {
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    // Runnable for updating the timer
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                val elapsedMillis = System.currentTimeMillis() - startTime
                val seconds = (elapsedMillis / 1000) % 60
                val minutes = (elapsedMillis / 1000) / 60
                val milliseconds = (elapsedMillis % 1000) / 10
                timerTextView.text = String.format("%02d:%02d:%02d", minutes, seconds, milliseconds)
                handler.postDelayed(this, 10)
            }
        }
    }

    // Reset the game state to its initial values
    private fun resetGame() {
        score = 0
        hold = 0
        isGameOver = false
        isMaxScoreReached = false
        athleteNameEditText.setText("")
        gameOverTextView.visibility = TextView.GONE
        maxScoreTextView.visibility = TextView.GONE
        congratulationTextView.visibility = TextView.GONE
        updateUI()
        stopTimer()
        timerTextView.text = getString(R.string.default_timer_text)
    }

    // Release the MediaPlayer resources when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        // Release the MediaPlayer when the activity is destroyed
        backgroundMediaPlayer.release()
        climbSoundPlayer.release()
        fallSoundPlayer.release()
    }
}
