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
    private lateinit var scoreTextView: TextView
    private lateinit var holdTextView: TextView
    private lateinit var gameOverTextView: TextView
    private lateinit var maxScoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var athleteNameEditText: EditText
    private lateinit var congratulationTextView: TextView

    private var score = 0
    private var hold = 0
    private var isGameOver = false
    private var isMaxScoreReached = false

    private var handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var isTimerRunning = false

    // MediaPlayer for background music and sound effects
    private lateinit var backgroundMediaPlayer: MediaPlayer
    private lateinit var climbSoundPlayer: MediaPlayer
    private lateinit var fallSoundPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        scoreTextView = findViewById(R.id.scoreTextView)
        holdTextView = findViewById(R.id.holdTextView)
        gameOverTextView = findViewById(R.id.gameOverTextView)
        maxScoreTextView = findViewById(R.id.maxScoreTextView)
        timerTextView = findViewById(R.id.timerTextView)
        athleteNameEditText = findViewById(R.id.athleteNameEditText)
        congratulationTextView = findViewById(R.id.congratulationTextView)

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

        climbButton.setOnClickListener {
            if (!isTimerRunning && !isGameOver) {
                startTimer()
            }
            if (!isGameOver && !isMaxScoreReached) {
                // Play climb sound effect
                climbSoundPlayer.start()

                hold++
                when (hold) {
                    in 1..3 -> score++
                    in 4..6 -> score += 2
                    in 7..9 -> score += 3
                }
                updateUI()
            }
        }

        fallButton.setOnClickListener {
            if (!isGameOver && !isMaxScoreReached && hold > 0) {
                // Play fall sound effect
                fallSoundPlayer.start()

                gameOverTextView.visibility = TextView.VISIBLE
                // Display congratulation message
                val athleteName = athleteNameEditText.text.toString()
                congratulationTextView.text = getString(R.string.congratulations, athleteName, score, timerTextView.text)
                congratulationTextView.visibility = TextView.VISIBLE
                stopTimer()
                isGameOver = true
            }
        }

        resetButton.setOnClickListener {
            resetGame()
        }
    }

    private fun updateUI() {
        val scoreLabel = getString(R.string.score_label)
        val holdLabel = getString(R.string.hold_label)

        scoreTextView.text = "$scoreLabel $score"
        holdTextView.text = "$holdLabel $hold"
        when (hold) {
            in 1..3 -> holdTextView.setTextColor(resources.getColor(R.color.blue))
            in 4..6 -> holdTextView.setTextColor(resources.getColor(R.color.green))
            in 7..9 -> holdTextView.setTextColor(resources.getColor(R.color.red))
        }

        if (hold == 9) {
            maxScoreReached()
            isGameOver = true
        }
    }

    private fun maxScoreReached() {
        maxScoreTextView.visibility = TextView.VISIBLE
        stopTimer()
        isMaxScoreReached = true

        // Display congratulation message
        val athleteName = athleteNameEditText.text.toString()
        congratulationTextView.text = getString(R.string.congratulations, athleteName, score, timerTextView.text)
        congratulationTextView.visibility = TextView.VISIBLE
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        isTimerRunning = true
        handler.post(timerRunnable)
    }

    private fun stopTimer() {
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
    }

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

    override fun onDestroy() {
        super.onDestroy()
        // Release the MediaPlayer when the activity is destroyed
        backgroundMediaPlayer.release()
        climbSoundPlayer.release()
        fallSoundPlayer.release()
    }
}
