package com.example.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AyhaVoiceService : Service() {

    private val binder = LocalBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isListening = false
    private var lastQueryTime = 0L

    companion object {
        private const val TAG = "AyhaVoiceService"
        private const val NOTIFICATION_ID = 2026
        private const val CHANNEL_ID = "ayha_assistant_channel"

        // Global states so Composable UI and ViewModel can observe service status directly
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        private val _assistantState = MutableStateFlow("idle") // idle, waiting_wake, listening_query, thinking, speaking
        val assistantState = _assistantState.asStateFlow()

        private val _wakeWordDetected = MutableStateFlow(false)
        val wakeWordDetected = _wakeWordDetected.asStateFlow()

        private val _lastSpokenText = MutableStateFlow("")
        val lastSpokenText = _lastSpokenText.asStateFlow()

        private var globalViewModelCallback: (suspend (String) -> Unit)? = null

        fun setViewModelCallback(callback: suspend (String) -> Unit) {
            globalViewModelCallback = callback
        }

        fun startService(context: Context) {
            try {
                val intent = Intent(context, AyhaVoiceService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting AyhaVoiceService foreground service: ${e.message}", e)
                Toast.makeText(context.applicationContext, "Unable to activate background assistant: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, AyhaVoiceService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed stopping AyhaVoiceService: ${e.message}", e)
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): AyhaVoiceService = this@AyhaVoiceService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AyhaVoiceService created")
        _isServiceRunning.value = true
        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification("Hands-Free Active: Waiting for 'Hey AYHA'"),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification("Hands-Free Active: Waiting for 'Hey AYHA'"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            _isServiceRunning.value = false
            stopSelf()
            return
        }
        initSpeechRecognizer()
        startContinuousListening()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AYHA Hands-Free Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps AYHA voice recognition active for wake-word detection."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, notificationIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AYHA AI Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                    speechRecognizer?.setRecognitionListener(AyhaSpeechListener())

                    recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }
                    Log.d(TAG, "SpeechRecognizer initialized successfully")
                } else {
                    Log.e(TAG, "Speech recognition is not available on this device")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SpeechRecognizer: ${e.message}", e)
            }
        }
    }

    private fun startContinuousListening() {
        mainHandler.post {
            if (speechRecognizer == null) {
                initSpeechRecognizer()
            }
            try {
                if (!isListening) {
                    val recognizer = speechRecognizer
                    val intent = recognizerIntent
                    if (recognizer != null && intent != null) {
                        isListening = true
                        if (_assistantState.value == "idle" || _assistantState.value == "speaking") {
                            _assistantState.value = "waiting_wake"
                        }
                        updateNotification("Hands-Free Active: Waiting for 'Hey AYHA'")
                        recognizer.startListening(intent)
                        Log.d(TAG, "Recognizer startListening called successfully")
                    } else {
                        Log.w(TAG, "SpeechRecognizer or Intent is null, cannot start listening yet")
                        isListening = false
                        restartListeningDelayed(2000)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech listening: ${e.message}", e)
                isListening = false
                restartListeningDelayed(1000)
            }
        }
    }

    private fun stopListeningInternal() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer", e)
            }
            isListening = false
        }
    }

    private fun restartListeningDelayed(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (_isServiceRunning.value && _assistantState.value != "speaking" && _assistantState.value != "thinking") {
                isListening = false
                startContinuousListening()
            }
        }, delayMs)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AyhaVoiceService onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AyhaVoiceService destroyed")
        _isServiceRunning.value = false
        _assistantState.value = "idle"
        serviceJob.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        stopListeningInternal()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // --- Speech Recognition Listener ---
    inner class AyhaSpeechListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            isListening = false
        }

        override fun onError(error: Int) {
            val errMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown speech recognizer error: $error"
            }
            Log.w(TAG, "SpeechRecognizer Error: $errMsg (code: $error)")

            isListening = false
            // Gracefully restart listening
            if (_assistantState.value != "speaking" && _assistantState.value != "thinking") {
                val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1500L else 1000L
                restartListeningDelayed(delay)
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches.isNullOrEmpty()) {
                restartListeningDelayed(1000)
                return
            }

            val spokenText = matches[0].trim()
            Log.d(TAG, "Speech recognition results: $spokenText (all matches: $matches)")
            _lastSpokenText.value = spokenText

            val currentState = _assistantState.value
            if (currentState == "waiting_wake") {
                val matchesWakeWord = spokenText.contains("ayha", ignoreCase = true) || 
                                     spokenText.contains("aha", ignoreCase = true) ||
                                     spokenText.contains("hey", ignoreCase = true)

                if (matchesWakeWord) {
                    Log.d(TAG, "Wake word matches!")
                    _wakeWordDetected.value = true
                    _assistantState.value = "listening_query"
                    updateNotification("Listening to your request, Mr.Rahul...")

                    // Auto-start recording query
                    mainHandler.postDelayed({
                        try {
                            val recognizer = speechRecognizer
                            val intent = recognizerIntent
                            if (recognizer != null && intent != null) {
                                isListening = true
                                recognizer.startListening(intent)
                            } else {
                                Log.w(TAG, "Cannot start listening for active query: recognizer or intent is null")
                                isListening = false
                                _assistantState.value = "waiting_wake"
                                restartListeningDelayed(1000)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start listening for active query", e)
                            isListening = false
                            _assistantState.value = "waiting_wake"
                            restartListeningDelayed(1000)
                        }
                    }, 300)
                } else {
                    // Try again
                    restartListeningDelayed(800)
                }
            } else if (currentState == "listening_query") {
                _assistantState.value = "thinking"
                updateNotification("AYHA is thinking...")

                serviceScope.launch {
                    try {
                        val callback = globalViewModelCallback
                        if (callback != null) {
                            Log.d(TAG, "Executing active query callback: $spokenText")
                            callback.invoke(spokenText)
                        } else {
                            Log.w(TAG, "ViewModel callback is null, can't process active voice query")
                            _assistantState.value = "waiting_wake"
                            startContinuousListening()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing active query callback", e)
                        _assistantState.value = "waiting_wake"
                        startContinuousListening()
                    }
                }
            } else {
                restartListeningDelayed(1000)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val partial = matches[0]
                Log.d(TAG, "onPartialResults: $partial")
                _lastSpokenText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Call this when AYHA starts speaking, to suspend continuous listening.
     */
    fun onSpeakingStarted() {
        Log.d(TAG, "onSpeakingStarted called, pause recognizer")
        _assistantState.value = "speaking"
        updateNotification("AYHA is speaking...")
        stopListeningInternal()
    }

    /**
     * Call this when AYHA stops speaking, to resume continuous listening.
     */
    fun onSpeakingFinished() {
        Log.d(TAG, "onSpeakingFinished called, resume continuous listening")
        _assistantState.value = "waiting_wake"
        _wakeWordDetected.value = false
        _lastSpokenText.value = ""
        startContinuousListening()
    }
}
