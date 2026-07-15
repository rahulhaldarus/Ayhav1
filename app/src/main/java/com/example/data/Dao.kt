package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query ORDER BY isPinned DESC, timestamp DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note): Int

    @Delete
    suspend fun deleteNote(note: Note): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int): Int
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY timestamp DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder): Int

    @Delete
    suspend fun deleteReminder(reminder: Reminder): Int

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int): Int
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String): Int

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String): Int

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int): Int

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllHistory(): Int
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM ai_providers")
    fun getAllProvidersFlow(): Flow<List<AiProvider>>

    @Query("SELECT * FROM ai_providers")
    suspend fun getAllProviders(): List<AiProvider>

    @Query("SELECT * FROM ai_providers WHERE id = :id LIMIT 1")
    suspend fun getProviderById(id: String): AiProvider?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: AiProvider): Long

    @Update
    suspend fun updateProvider(provider: AiProvider): Int
}
