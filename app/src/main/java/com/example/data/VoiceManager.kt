package com.example.data

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class VoiceManager private constructor(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("ayha_voice_settings", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private val _voiceState = MutableStateFlow("idle") // idle, thinking, speaking, listening
    val voiceState = _voiceState.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady = _isTtsReady.asStateFlow()

    // Listener for completion of speaking
    private var onSpeechCompletedListener: (() -> Unit)? = null

    init {
        initDeviceTts()
    }

    companion object {
        private const val TAG = "VoiceManager"
        @Volatile
        private var INSTANCE: VoiceManager? = null

        fun getInstance(context: Context): VoiceManager {
            return INSTANCE ?: synchronized(this) {
                val instance = VoiceManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private fun initDeviceTts() {
        _voiceState.value = "thinking"
        try {
            tts = TextToSpeech(context) { status ->
                try {
                    if (status == TextToSpeech.SUCCESS) {
                        val result = tts?.setLanguage(Locale.US)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e(TAG, "Device TTS English is not supported")
                            _isTtsReady.value = false
                        } else {
                            // Make speech cute, slightly high pitch and elegant
                            tts?.setPitch(1.25f)
                            tts?.setSpeechRate(0.95f)
                            _isTtsReady.value = true
                            Log.d(TAG, "Device TTS Fallback Initialized successfully!")
                        }
                    } else {
                        Log.e(TAG, "Device TTS Initialization failed")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during TextToSpeech initialization success callback: ${e.message}", e)
                    _isTtsReady.value = false
                } finally {
                    _voiceState.value = "idle"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech: ${e.message}", e)
            _isTtsReady.value = false
            _voiceState.value = "idle"
        }
    }

    fun setOnSpeechCompletedListener(listener: () -> Unit) {
        onSpeechCompletedListener = listener
    }

    // --- SharedPreferences Helpers ---
    fun getActiveEngine(): String = sharedPrefs.getString("active_engine", "device") ?: "device"
    fun setActiveEngine(engine: String) = sharedPrefs.edit().putString("active_engine", engine).apply()

    fun getElevenLabsApiKey(): String = sharedPrefs.getString("elevenlabs_api_key", "") ?: ""
    fun setElevenLabsApiKey(key: String) = sharedPrefs.edit().putString("elevenlabs_api_key", key).apply()
    fun getElevenLabsVoiceId(): String = sharedPrefs.getString("elevenlabs_voice_id", "21m00Tcm4TlvDq8ikWAM") ?: "21m00Tcm4TlvDq8ikWAM" // Rachel cute
    fun setElevenLabsVoiceId(id: String) = sharedPrefs.edit().putString("elevenlabs_voice_id", id).apply()

    fun getOpenAiApiKey(): String = sharedPrefs.getString("openai_api_key", "") ?: ""
    fun setOpenAiApiKey(key: String) = sharedPrefs.edit().putString("openai_api_key", key).apply()
    fun getOpenAiVoice(): String = sharedPrefs.getString("openai_voice", "nova") ?: "nova" // Cute/calm female
    fun setOpenAiVoice(voice: String) = sharedPrefs.edit().putString("openai_voice", voice).apply()

    fun getGoogleCloudApiKey(): String = sharedPrefs.getString("google_api_key", "") ?: ""
    fun setGoogleCloudApiKey(key: String) = sharedPrefs.edit().putString("google_api_key", key).apply()
    fun getGoogleCloudVoiceName(): String = sharedPrefs.getString("google_voice", "en-US-Neural2-F") ?: "en-US-Neural2-F"
    fun setGoogleCloudVoiceName(name: String) = sharedPrefs.edit().putString("google_voice", name).apply()

    fun getAzureApiKey(): String = sharedPrefs.getString("azure_api_key", "") ?: ""
    fun setAzureApiKey(key: String) = sharedPrefs.edit().putString("azure_api_key", key).apply()
    fun getAzureRegion(): String = sharedPrefs.getString("azure_region", "eastus") ?: "eastus"
    fun setAzureRegion(region: String) = sharedPrefs.edit().putString("azure_region", region).apply()
    fun getAzureVoiceName(): String = sharedPrefs.getString("azure_voice", "en-US-JennyNeural") ?: "en-US-JennyNeural"
    fun setAzureVoiceName(name: String) = sharedPrefs.edit().putString("azure_voice", name).apply()

    /**
     * Synthesizes and speaks text using active engine, falls back to Device TTS if there are failures.
     */
    fun speak(text: String, scope: CoroutineScope) {
        val cleanText = text.replace(Regex("[*#_~`]"), "").trim()
        if (cleanText.isBlank()) return

        stopSpeaking()
        _voiceState.value = "thinking"

        val engine = getActiveEngine()
        Log.d(TAG, "Request to speak with engine: $engine")

        scope.launch(Dispatchers.IO) {
            var success = false
            try {
                when (engine) {
                    "elevenlabs" -> success = synthesizeElevenLabs(cleanText)
                    "openai" -> success = synthesizeOpenAi(cleanText)
                    "google" -> success = synthesizeGoogleCloud(cleanText)
                    "azure" -> success = synthesizeAzure(cleanText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS network service failed: ${e.message}. Falling back to Device TTS.", e)
            }

            if (!success) {
                Log.w(TAG, "Falling back to Device TTS")
                speakDeviceTts(cleanText, scope)
            }
        }
    }

    private fun speakDeviceTts(text: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.Main) {
            _voiceState.value = "speaking"
            try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ayha_tts")
            } catch (e: Exception) {
                Log.e(TAG, "Failed calling tts.speak", e)
            }
            // Wait for speaking to complete or simulate completion
            scope.launch(Dispatchers.IO) {
                try {
                    while (tts?.isSpeaking == true) {
                        kotlinx.coroutines.delay(100)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking tts.isSpeaking status", e)
                }
                kotlinx.coroutines.delay(500)
                _voiceState.value = "idle"
                onSpeechCompletedListener?.invoke()
            }
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media playback", e)
        }
        _voiceState.value = "idle"
    }

    // --- ElevenLabs API ---
    private fun synthesizeElevenLabs(text: String): Boolean {
        val apiKey = getElevenLabsApiKey()
        if (apiKey.isBlank()) return false

        val voiceId = getElevenLabsVoiceId()
        val url = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId"

        val json = JSONObject().apply {
            put("text", text)
            put("model_id", "eleven_monolingual_v1")
            put("voice_settings", JSONObject().apply {
                put("stability", 0.5f)
                put("similarity_boost", 0.75f)
            })
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("xi-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return executeAudioRequest(request)
    }

    // --- OpenAI Speech API ---
    private fun synthesizeOpenAi(text: String): Boolean {
        val apiKey = getOpenAiApiKey()
        if (apiKey.isBlank()) return false

        val voice = getOpenAiVoice()
        val url = "https://api.openai.com/v1/audio/speech"

        val json = JSONObject().apply {
            put("model", "tts-1")
            put("input", text)
            put("voice", voice)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return executeAudioRequest(request)
    }

    // --- Google Cloud TTS API ---
    private fun synthesizeGoogleCloud(text: String): Boolean {
        val apiKey = getGoogleCloudApiKey()
        if (apiKey.isBlank()) return false

        val voiceName = getGoogleCloudVoiceName()
        val url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey"

        val json = JSONObject().apply {
            put("input", JSONObject().put("text", text))
            put("voice", JSONObject().apply {
                put("languageCode", "en-US")
                put("name", voiceName)
                put("ssmlGender", "FEMALE")
            })
            put("audioConfig", JSONObject().put("audioEncoding", "MP3"))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val respStr = response.body?.string() ?: return false
                val root = JSONObject(respStr)
                val audioContentBase64 = root.optString("audioContent", "")
                if (audioContentBase64.isBlank()) return false

                val audioBytes = Base64.decode(audioContentBase64, Base64.DEFAULT)
                val tempFile = File(context.cacheDir, "ayha_voice.mp3")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(audioBytes)
                }

                playLocalFile(tempFile)
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google TTS failed", e)
            return false
        }
    }

    // --- Azure Speech API ---
    private fun synthesizeAzure(text: String): Boolean {
        val apiKey = getAzureApiKey()
        if (apiKey.isBlank()) return false

        val region = getAzureRegion()
        val voiceName = getAzureVoiceName()
        val url = "https://$region.tts.speech.microsoft.com/cognitiveservices/v1"

        val ssml = """
            <speak version='1.0' xml:lang='en-US'>
                <voice xml:lang='en-US' name='$voiceName'>
                    <prosody pitch='+10%' rate='0.95'>
                        $text
                    </prosody>
                </voice>
            </speak>
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Ocp-Apim-Subscription-Key", apiKey)
            .addHeader("Content-Type", "application/ssml+xml")
            .addHeader("X-Microsoft-OutputFormat", "audio-16khz-128kbitrate-mono-mp3")
            .addHeader("User-Agent", "AYHA")
            .post(ssml.toRequestBody("application/ssml+xml".toMediaType()))
            .build()

        return executeAudioRequest(request)
    }

    private fun executeAudioRequest(request: Request): Boolean {
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "TTS request failed with code: ${response.code} / ${response.body?.string()}")
                    return false
                }

                val body = response.body ?: return false
                val tempFile = File(context.cacheDir, "ayha_voice.mp3")
                FileOutputStream(tempFile).use { fos ->
                    body.byteStream().copyTo(fos)
                }

                playLocalFile(tempFile)
                return true
            }
        } catch (e: IOException) {
            Log.e(TAG, "TTS network IOException", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "TTS execute error", e)
            return false
        }
    }

    private fun playLocalFile(file: File) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    _voiceState.value = "speaking"
                    start()
                    setOnCompletionListener {
                        _voiceState.value = "idle"
                        release()
                        mediaPlayer = null
                        onSpeechCompletedListener?.invoke()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing audio file", e)
                _voiceState.value = "idle"
                onSpeechCompletedListener?.invoke()
            }
        }
    }

    fun release() {
        try {
            tts?.shutdown()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up VoiceManager", e)
        }
    }
}
