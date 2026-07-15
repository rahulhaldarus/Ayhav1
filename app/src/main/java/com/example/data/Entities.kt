package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val folder: String = "General",
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateText: String,
    val timeText: String,
    val isEnabled: Boolean = true,
    val repeatType: String = "None", // None, Daily, Weekly, Monthly
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_providers")
data class AiProvider(
    @PrimaryKey val id: String, // gemini, openai, claude, deepseek, grok, openrouter, mistral, custom, perplexity
    val name: String,
    val isEnabled: Boolean = true,
    val apiKey: String = "",
    val baseUrl: String = "",
    val selectedModel: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxTokens: Int = 2048,
    val isStreaming: Boolean = true,
    val timeout: Int = 30, // Timeout in seconds
    val retryCount: Int = 3
)
