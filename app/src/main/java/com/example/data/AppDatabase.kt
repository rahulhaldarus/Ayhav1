package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Note::class, Reminder::class, ChatSession::class, ChatMessage::class, AiProvider::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun chatDao(): ChatDao
    abstract fun providerDao(): ProviderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ayha_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            // 1. Populate Default AI Providers
            val providers = listOf(
                AiProvider(
                    id = "gemini",
                    name = "Gemini AI",
                    isEnabled = true,
                    apiKey = "",
                    baseUrl = "https://generativelanguage.googleapis.com/",
                    selectedModel = "gemini-3.5-flash",
                    temperature = 0.7f,
                    topP = 0.95f,
                    topK = 40,
                    maxTokens = 2048,
                    isStreaming = true,
                    timeout = 30
                ),
                AiProvider(
                    id = "openai",
                    name = "OpenAI GPT",
                    isEnabled = false,
                    apiKey = "",
                    baseUrl = "https://api.openai.com/v1/",
                    selectedModel = "gpt-4o",
                    temperature = 0.7f,
                    topP = 0.9f,
                    topK = 40,
                    maxTokens = 2048,
                    isStreaming = true,
                    timeout = 30
                ),
                AiProvider(
                    id = "claude",
                    name = "Anthropic Claude",
                    isEnabled = false,
                    apiKey = "",
                    baseUrl = "https://api.anthropic.com/v1/",
                    selectedModel = "claude-3-5-sonnet",
                    temperature = 0.5f,
                    topP = 0.9f,
                    topK = 40,
                    maxTokens = 4096,
                    isStreaming = true,
                    timeout = 30
                ),
                AiProvider(
                    id = "deepseek",
                    name = "DeepSeek AI",
                    isEnabled = false,
                    apiKey = "",
                    baseUrl = "https://api.deepseek.com/v1/",
                    selectedModel = "deepseek-chat",
                    temperature = 0.7f,
                    topP = 0.9f,
                    topK = 40,
                    maxTokens = 2048,
                    isStreaming = true,
                    timeout = 30
                ),
                AiProvider(
                    id = "grok",
                    name = "xAI Grok",
                    isEnabled = false,
                    apiKey = "",
                    baseUrl = "https://api.x.ai/v1/",
                    selectedModel = "grok-2-1212",
                    temperature = 0.7f,
                    topP = 0.9f,
                    topK = 40,
                    maxTokens = 2048,
                    isStreaming = true,
                    timeout = 30
                ),
                AiProvider(
                    id = "openrouter",
                    name = "OpenRouter",
                    isEnabled = false,
                    apiKey = "",
                    baseUrl = "https://openrouter.ai/api/v1/",
                    selectedModel = "google/gemini-2.5-flash",
                    temperature = 0.7f,
                    topP = 0.9f,
                    topK = 40,
                    maxTokens = 2048,
                    isStreaming = true,
                    timeout = 30
                ),
                AiProvider(
                    id = "mistral",
                    name = "Mistral AI",
                    isEnabled = false,
                    apiKey = "",
                    baseUrl = "https://api.mistral.ai/v1/",
                    selectedModel = "mistral-large-latest",
                    temperature = 0.7f,
                    topP = 0.9f,
                    topK = 40,
                    maxTokens = 2048,
                    isStreaming = true,
                    timeout = 30
                ),
                AiProvider(
                    id = "custom",
                    name = "Custom Endpoint",
                    isEnabled = false,
                    apiKey = "",
                    baseUrl = "",
                    selectedModel = "",
                    temperature = 0.7f,
                    topP = 0.9f,
                    topK = 40,
                    maxTokens = 2048,
                    isStreaming = true,
                    timeout = 30
                )
            )
            for (p in providers) {
                db.providerDao().insertProvider(p)
            }

            // 2. Populate Default Notes
            val notes = listOf(
                Note(
                    title = "Meeting Notes",
                    content = "Discussed the project milestones for AYHA. The client requested a futuristic purple neon theme and modular voice settings. Next up: implement streaming response.",
                    folder = "Work",
                    isPinned = true
                ),
                Note(
                    title = "Project Ideas",
                    content = "1. AI Voice visualizer using circular breathing canvas.\n2. Modular TTS interface with ElevenLabs, OpenAI, and Device TTS support.\n3. Dynamic weather forecasting card.",
                    folder = "Ideas",
                    isPinned = false
                ),
                Note(
                    title = "Daily Thoughts",
                    content = "AYHA is looking absolutely incredible. The dark futuristic slate styling is extremely eye-friendly. I should work on speech recognition tomorrow.",
                    folder = "Journal",
                    isPinned = false
                ),
                Note(
                    title = "AYHA Roadmap",
                    content = "- Q3: Voice Engine Refactor\n- Q4: Local LLM Integration via MediaPipe\n- Q1: Wear OS Sync Support",
                    folder = "Work",
                    isPinned = true
                )
            )
            for (n in notes) {
                db.noteDao().insertNote(n)
            }

            // 3. Populate Default Reminders
            val reminders = listOf(
                Reminder(
                    title = "Meeting with Team",
                    dateText = "Today",
                    timeText = "10:00 AM",
                    isEnabled = true,
                    repeatType = "Daily"
                ),
                Reminder(
                    title = "Buy Groceries",
                    dateText = "Today",
                    timeText = "06:00 PM",
                    isEnabled = true,
                    repeatType = "None"
                ),
                Reminder(
                    title = "Workout",
                    dateText = "Tomorrow",
                    timeText = "07:00 AM",
                    isEnabled = false,
                    repeatType = "Weekly"
                ),
                Reminder(
                    title = "Project Deadline",
                    dateText = "20 May 2024",
                    timeText = "11:59 PM",
                    isEnabled = true,
                    repeatType = "None"
                )
            )
            for (r in reminders) {
                db.reminderDao().insertReminder(r)
            }
        }
    }
}
