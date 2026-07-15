package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AyhaViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AyhaViewModel"
    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val noteDao = db.noteDao()
    private val reminderDao = db.reminderDao()
    private val chatDao = db.chatDao()
    private val providerDao = db.providerDao()

    // --- Modular Voice Engine ---
    private val voiceManager = VoiceManager.getInstance(application)
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady = _isTtsReady.asStateFlow()

    // --- State Variables ---
    val allNotes = noteDao.getAllNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReminders = reminderDao.getAllReminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSessions = chatDao.getAllSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allProviders = providerDao.getAllProvidersFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Session
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessage>> = _activeSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                chatDao.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active AI Provider
    private val _activeProviderId = MutableStateFlow("gemini")
    val activeProviderId = _activeProviderId.asStateFlow()

    val activeProvider: StateFlow<AiProvider?> = combine(allProviders, _activeProviderId) { providers, activeId ->
        providers.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // User Profile Preferences / System Settings
    private val _userName = MutableStateFlow("Mr.Rahul")
    val userName = _userName.asStateFlow()

    private val _selectedVoice = MutableStateFlow("AYHA Female") // None, AYHA Female, ElevenLabs Cute, Device Default
    val selectedVoice = _selectedVoice.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage = _selectedLanguage.asStateFlow()

    // Voice assistant state: idle, listening, thinking, speaking
    private val _voiceState = MutableStateFlow("idle")
    val voiceState = _voiceState.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText = _streamingText.asStateFlow()

    private val _voiceSuggestion = MutableStateFlow("How can I help you today, Mr.Rahul?")
    val voiceSuggestion = _voiceSuggestion.asStateFlow()

    // Notes management
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredNotes = combine(allNotes, _searchQuery) { notes, query ->
        if (query.isBlank()) notes else {
            notes.filter { it.title.contains(query, true) || it.content.contains(query, true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect states from VoiceManager
        viewModelScope.launch {
            voiceManager.voiceState.collect { state ->
                _voiceState.value = state
                
                // Keep background assistant service informed about speaking status
                try {
                    val serviceActive = AyhaVoiceService.isServiceRunning.value
                    if (serviceActive) {
                        // Dynamically look up bound service or use static triggers to adjust recognizer activity
                        // during TTS synthesis to avoid self-listening!
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed communicating status to voice service", e)
                }
            }
        }

        viewModelScope.launch {
            voiceManager.isTtsReady.collect { ready ->
                _isTtsReady.value = ready
            }
        }

        // Bridge voice completion back to hands-free loop
        voiceManager.setOnSpeechCompletedListener {
            Log.d(TAG, "Speech completion callback invoked")
        }

        // Connect Service wake-active queries directly to our ViewModel generator
        AyhaVoiceService.setViewModelCallback { textQuery ->
            processVoiceQueryFromService(textQuery)
        }
    }

    fun speak(text: String) {
        if (_selectedVoice.value != "None") {
            voiceManager.speak(text, viewModelScope)
        }
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
        _voiceState.value = "idle"
    }

    // --- Provider Crud (No Rebuild Required Settings Panel) ---
    fun updateProvider(provider: AiProvider) {
        viewModelScope.launch(Dispatchers.IO) {
            providerDao.insertProvider(provider)
        }
    }

    fun setActiveProviderId(providerId: String) {
        _activeProviderId.value = providerId
    }

    fun selectProvider(providerId: String) {
        setActiveProviderId(providerId)
    }

    fun selectOrCreateSession(sessionId: String?) {
        if (sessionId == null) {
            startNewSession(null)
        } else {
            selectSession(sessionId)
        }
    }

    fun sendMessage(text: String) {
        submitChatMessage(text)
    }

    fun setUserName(name: String) {
        _userName.value = name
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedVoice(voice: String) {
        _selectedVoice.value = voice
    }

    fun updateVoiceSetting(voice: String) {
        setSelectedVoice(voice)
    }

    fun setSelectedLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun updateLanguageSetting(lang: String) {
        setSelectedLanguage(lang)
    }

    // --- Note CRUD ---
    fun addNote(title: String, content: String, folder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.insertNote(Note(title = title, content = content, folder = folder))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.deleteNote(note)
        }
    }

    fun togglePinNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    // --- Reminder CRUD ---
    fun addReminder(title: String, dateText: String, timeText: String, repeatType: String = "None") {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.insertReminder(Reminder(title = title, dateText = dateText, timeText = timeText, repeatType = repeatType))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.deleteReminder(reminder)
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.updateReminder(reminder.copy(isEnabled = !reminder.isEnabled))
        }
    }

    // --- Active Chat & Streaming Logic ---
    fun selectSession(sessionId: String?) {
        _activeSessionId.value = sessionId
    }

    fun startNewSession(initialText: String? = null) {
        val newId = UUID.randomUUID().toString()
        _activeSessionId.value = newId
        _streamingText.value = ""

        if (initialText != null) {
            submitChatMessage(initialText)
        }
    }

    fun submitChatMessage(text: String) {
        if (text.isBlank()) return

        var currSessionId = _activeSessionId.value
        if (currSessionId == null) {
            currSessionId = UUID.randomUUID().toString()
            _activeSessionId.value = currSessionId
        }

        viewModelScope.launch {
            // 1. Save User's input message
            val userMsg = ChatMessage(sessionId = currSessionId, role = "user", content = text)
            chatDao.insertMessage(userMsg)
            chatDao.insertSession(ChatSession(id = currSessionId, title = text.take(24), lastMessage = text))

            // 2. Clear streaming text and transition state
            _streamingText.value = ""
            _voiceState.value = "thinking"

            // Get provider details
            val prov = activeProvider.value
            val history = activeMessages.value

            // 3. Initiate API Stream
            try {
                var fullModelResponse = ""
                GeminiApiClient.generateTextStream(
                    prompt = text,
                    provider = prov,
                    history = history
                ).collect { chunk ->
                    _voiceState.value = "speaking"
                    _streamingText.value += chunk
                    fullModelResponse += chunk
                }

                // Save model's complete reply
                val modelMsg = ChatMessage(sessionId = currSessionId, role = "model", content = fullModelResponse)
                chatDao.insertMessage(modelMsg)
                chatDao.insertSession(ChatSession(id = currSessionId, title = text.take(24), lastMessage = fullModelResponse))

                // Speak back the response if vocal feedback is requested
                if (_selectedVoice.value != "None") {
                    speak(fullModelResponse)
                } else {
                    _voiceState.value = "idle"
                }

            } catch (e: Exception) {
                val errText = "Mr.Rahul, I experienced an error: ${e.localizedMessage}"
                val modelMsg = ChatMessage(sessionId = currSessionId, role = "model", content = errText)
                chatDao.insertMessage(modelMsg)
                _voiceState.value = "idle"
            }
        }
    }

    fun startVoiceListening() {
        stopSpeaking()
        _voiceState.value = "listening"
        _voiceSuggestion.value = "I'm listening Mr.Rahul, speak now..."
    }

    fun stopVoiceAndSubmit(voicePromptText: String) {
        if (voicePromptText.isBlank()) {
            _voiceState.value = "idle"
            _voiceSuggestion.value = "How can I help you today, Mr.Rahul?"
            return
        }

        viewModelScope.launch {
            _voiceState.value = "thinking"
            _voiceSuggestion.value = "Processing, Mr.Rahul..."

            // Get provider and history
            val prov = activeProvider.value

            try {
                // Generate reply
                val response = GeminiApiClient.generateText(
                    prompt = voicePromptText,
                    provider = prov
                )

                _voiceSuggestion.value = response
                speak(response)

            } catch (e: Exception) {
                val err = "Mr.Rahul, I could not complete the speech command: ${e.localizedMessage}"
                _voiceSuggestion.value = err
                speak(err)
            }
        }
    }

    /**
     * Executes queries requested hands-free via background Service
     */
    private fun processVoiceQueryFromService(userQuery: String) {
        if (userQuery.isBlank()) return

        var currSessionId = _activeSessionId.value
        if (currSessionId == null) {
            currSessionId = UUID.randomUUID().toString()
            _activeSessionId.value = currSessionId
        }

        viewModelScope.launch {
            _voiceState.value = "thinking"
            _voiceSuggestion.value = "Processing command, Mr.Rahul..."

            // 1. Save User Voice message to DB
            val userMsg = ChatMessage(sessionId = currSessionId, role = "user", content = userQuery)
            chatDao.insertMessage(userMsg)
            chatDao.insertSession(ChatSession(id = currSessionId, title = userQuery.take(24), lastMessage = userQuery))

            _streamingText.value = ""

            // Get provider and history
            val prov = activeProvider.value
            val history = activeMessages.value

            try {
                var fullModelResponse = ""
                GeminiApiClient.generateTextStream(
                    prompt = userQuery,
                    provider = prov,
                    history = history
                ).collect { chunk ->
                    _voiceState.value = "speaking"
                    _streamingText.value += chunk
                    fullModelResponse += chunk
                }

                // 2. Save complete response to DB
                val modelMsg = ChatMessage(sessionId = currSessionId, role = "model", content = fullModelResponse)
                chatDao.insertMessage(modelMsg)
                chatDao.insertSession(ChatSession(id = currSessionId, title = userQuery.take(24), lastMessage = fullModelResponse))

                _voiceSuggestion.value = fullModelResponse
                speak(fullModelResponse)

            } catch (e: Exception) {
                val err = "Mr.Rahul, I experienced an error: ${e.localizedMessage}"
                val modelMsg = ChatMessage(sessionId = currSessionId, role = "model", content = err)
                chatDao.insertMessage(modelMsg)
                _voiceSuggestion.value = err
                speak(err)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteSessionById(sessionId)
            chatDao.deleteMessagesForSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
            }
        }
    }

    fun clearAllChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.clearAllHistory()
            _activeSessionId.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
