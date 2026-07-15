package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [Note::class, Reminder::class, ChatSession::class, ChatMessage::class, AiProvider::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun chatDao(): ChatDao
    abstract fun providerDao(): ProviderDao

    companion object {
        private const val TAG = "AppDatabase"

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
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d(TAG, "Creating database and populating default data...")
            db.beginTransaction()
            try {
                // 1. Populate Default AI Providers
                val providers = listOf(
                    mapOf(
                        "id" to "gemini",
                        "name" to "Gemini AI",
                        "isEnabled" to 1,
                        "apiKey" to "",
                        "baseUrl" to "https://generativelanguage.googleapis.com/",
                        "selectedModel" to "gemini-3.5-flash",
                        "temperature" to 0.7f,
                        "topP" to 0.95f,
                        "topK" to 40,
                        "maxTokens" to 2048,
                        "isStreaming" to 1,
                        "timeout" to 30
                    ),
                    mapOf(
                        "id" to "openai",
                        "name" to "OpenAI GPT",
                        "isEnabled" to 0,
                        "apiKey" to "",
                        "baseUrl" to "https://api.openai.com/v1/",
                        "selectedModel" to "gpt-4o",
                        "temperature" to 0.7f,
                        "topP" to 0.9f,
                        "topK" to 40,
                        "maxTokens" to 2048,
                        "isStreaming" to 1,
                        "timeout" to 30
                    ),
                    mapOf(
                        "id" to "claude",
                        "name" to "Anthropic Claude",
                        "isEnabled" to 0,
                        "apiKey" to "",
                        "baseUrl" to "https://api.anthropic.com/v1/",
                        "selectedModel" to "claude-3-5-sonnet",
                        "temperature" to 0.5f,
                        "topP" to 0.9f,
                        "topK" to 40,
                        "maxTokens" to 4096,
                        "isStreaming" to 1,
                        "timeout" to 30
                    ),
                    mapOf(
                        "id" to "deepseek",
                        "name" to "DeepSeek AI",
                        "isEnabled" to 0,
                        "apiKey" to "",
                        "baseUrl" to "https://api.deepseek.com/v1/",
                        "selectedModel" to "deepseek-chat",
                        "temperature" to 0.7f,
                        "topP" to 0.9f,
                        "topK" to 40,
                        "maxTokens" to 2048,
                        "isStreaming" to 1,
                        "timeout" to 30
                    ),
                    mapOf(
                        "id" to "grok",
                        "name" to "xAI Grok",
                        "isEnabled" to 0,
                        "apiKey" to "",
                        "baseUrl" to "https://api.x.ai/v1/",
                        "selectedModel" to "grok-2-1212",
                        "temperature" to 0.7f,
                        "topP" to 0.9f,
                        "topK" to 40,
                        "maxTokens" to 2048,
                        "isStreaming" to 1,
                        "timeout" to 30
                    ),
                    mapOf(
                        "id" to "openrouter",
                        "name" to "OpenRouter",
                        "isEnabled" to 0,
                        "apiKey" to "",
                        "baseUrl" to "https://openrouter.ai/api/v1/",
                        "selectedModel" to "google/gemini-2.5-flash",
                        "temperature" to 0.7f,
                        "topP" to 0.9f,
                        "topK" to 40,
                        "maxTokens" to 2048,
                        "isStreaming" to 1,
                        "timeout" to 30
                    ),
                    mapOf(
                        "id" to "mistral",
                        "name" to "Mistral AI",
                        "isEnabled" to 0,
                        "apiKey" to "",
                        "baseUrl" to "https://api.mistral.ai/v1/",
                        "selectedModel" to "mistral-large-latest",
                        "temperature" to 0.7f,
                        "topP" to 0.9f,
                        "topK" to 40,
                        "maxTokens" to 2048,
                        "isStreaming" to 1,
                        "timeout" to 30
                    ),
                    mapOf(
                        "id" to "custom",
                        "name" to "Custom Endpoint",
                        "isEnabled" to 0,
                        "apiKey" to "",
                        "baseUrl" to "",
                        "selectedModel" to "",
                        "temperature" to 0.7f,
                        "topP" to 0.9f,
                        "topK" to 40,
                        "maxTokens" to 2048,
                        "isStreaming" to 1,
                        "timeout" to 30
                    ),
                    mapOf(
                        "id" to "perplexity",
                        "name" to "Perplexity AI",
                        "isEnabled" to 0,
                        "apiKey" to "",
                        "baseUrl" to "https://api.perplexity.ai/",
                        "selectedModel" to "sonar-reasoning",
                        "temperature" to 0.7f,
                        "topP" to 0.9f,
                        "topK" to 40,
                        "maxTokens" to 2048,
                        "isStreaming" to 1,
                        "timeout" to 30
                    )
                )
                for (p in providers) {
                    val values = ContentValues().apply {
                        put("id", p["id"] as String)
                        put("name", p["name"] as String)
                        put("isEnabled", p["isEnabled"] as Int)
                        put("apiKey", p["apiKey"] as String)
                        put("baseUrl", p["baseUrl"] as String)
                        put("selectedModel", p["selectedModel"] as String)
                        put("temperature", p["temperature"] as Float)
                        put("topP", p["topP"] as Float)
                        put("topK", p["topK"] as Int)
                        put("maxTokens", p["maxTokens"] as Int)
                        put("isStreaming", p["isStreaming"] as Int)
                        put("timeout", p["timeout"] as Int)
                        put("retryCount", (p["retryCount"] as? Int) ?: 3)
                    }
                    db.insert("ai_providers", SQLiteDatabase.CONFLICT_IGNORE, values)
                }

                // 2. Populate Default Notes
                val notes = listOf(
                    mapOf(
                        "title" to "Meeting Notes",
                        "content" to "Discussed the project milestones for AYHA. The client requested a futuristic purple neon theme and modular voice settings. Next up: implement streaming response.",
                        "folder" to "Work",
                        "isPinned" to 1
                    ),
                    mapOf(
                        "title" to "Project Ideas",
                        "content" to "1. AI Voice visualizer using circular breathing canvas.\n2. Modular TTS interface with ElevenLabs, OpenAI, and Device TTS support.\n3. Dynamic weather forecasting card.",
                        "folder" to "Ideas",
                        "isPinned" to 0
                    ),
                    mapOf(
                        "title" to "Daily Thoughts",
                        "content" to "AYHA is looking absolutely incredible. The dark futuristic slate styling is extremely eye-friendly. I should work on speech recognition tomorrow.",
                        "folder" to "Journal",
                        "isPinned" to 0
                    ),
                    mapOf(
                        "title" to "AYHA Roadmap",
                        "content" to "- Q3: Voice Engine Refactor\n- Q4: Local LLM Integration via MediaPipe\n- Q1: Wear OS Sync Support",
                        "folder" to "Work",
                        "isPinned" to 1
                    )
                )
                for (n in notes) {
                    val values = ContentValues().apply {
                        put("title", n["title"] as String)
                        put("content", n["content"] as String)
                        put("folder", n["folder"] as String)
                        put("isPinned", n["isPinned"] as Int)
                        put("timestamp", System.currentTimeMillis())
                    }
                    db.insert("notes", SQLiteDatabase.CONFLICT_IGNORE, values)
                }

                // 3. Populate Default Reminders
                val reminders = listOf(
                    mapOf(
                        "title" to "Meeting with Team",
                        "dateText" to "Today",
                        "timeText" to "10:00 AM",
                        "isEnabled" to 1,
                        "repeatType" to "Daily"
                    ),
                    mapOf(
                        "title" to "Buy Groceries",
                        "dateText" to "Today",
                        "timeText" to "06:00 PM",
                        "isEnabled" to 1,
                        "repeatType" to "None"
                    ),
                    mapOf(
                        "title" to "Workout",
                        "dateText" to "Tomorrow",
                        "timeText" to "07:00 AM",
                        "isEnabled" to 0,
                        "repeatType" to "Weekly"
                    ),
                    mapOf(
                        "title" to "Project Deadline",
                        "dateText" to "20 May 2024",
                        "timeText" to "11:59 PM",
                        "isEnabled" to 1,
                        "repeatType" to "None"
                    )
                )
                for (r in reminders) {
                    val values = ContentValues().apply {
                        put("title", r["title"] as String)
                        put("dateText", r["dateText"] as String)
                        put("timeText", r["timeText"] as String)
                        put("isEnabled", r["isEnabled"] as Int)
                        put("repeatType", r["repeatType"] as String)
                        put("timestamp", System.currentTimeMillis())
                    }
                    db.insert("reminders", SQLiteDatabase.CONFLICT_IGNORE, values)
                }

                db.setTransactionSuccessful()
                Log.d(TAG, "Database populated successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error populating database", e)
            } finally {
                db.endTransaction()
            }
        }
    }
}
