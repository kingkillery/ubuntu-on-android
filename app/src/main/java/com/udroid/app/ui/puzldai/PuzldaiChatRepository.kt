package com.udroid.app.ui.puzldai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.udroid.app.ui.tui.MessageRole
import com.udroid.app.ui.tui.TuiMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.puzldaiChatDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "puzldai_chat"
)

/**
 * Repository for persisting pk-puzldai chat history.
 * Stores messages per Ubuntu session so conversations survive app restarts.
 */
@Singleton
class PuzldaiChatRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private val CHAT_IDS_KEY = stringPreferencesKey("chat_session_ids")
        private const val CHAT_PREFIX = "chat_"
        private const val MAX_MESSAGES_PER_SESSION = 500
    }

    /**
     * Save or update chat history for a session.
     */
    suspend fun saveChatHistory(sessionId: String, messages: List<TuiMessage>) {
        val chatData = ChatHistoryData(
            sessionId = sessionId,
            messages = messages.takeLast(MAX_MESSAGES_PER_SESSION).map { it.toData() },
            lastUpdated = System.currentTimeMillis()
        )

        context.puzldaiChatDataStore.edit { preferences ->
            val chatIds = getChatIdsFromPrefs(preferences).toMutableSet()
            chatIds.add(sessionId)

            preferences[CHAT_IDS_KEY] = chatIds.joinToString(",")
            preferences[stringPreferencesKey("$CHAT_PREFIX$sessionId")] =
                json.encodeToString(chatData)
        }
        Timber.d("Saved chat history for session: $sessionId (${messages.size} messages)")
    }

    /**
     * Load chat history for a session.
     */
    suspend fun loadChatHistory(sessionId: String): List<TuiMessage> {
        val chatJson = context.puzldaiChatDataStore.data.map { preferences ->
            preferences[stringPreferencesKey("$CHAT_PREFIX$sessionId")]
        }.firstOrNull() ?: return emptyList()

        return try {
            val chatData = json.decodeFromString<ChatHistoryData>(chatJson)
            chatData.messages.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode chat history for session: $sessionId")
            emptyList()
        }
    }

    /**
     * Observe chat history changes for a session.
     */
    fun observeChatHistory(sessionId: String): Flow<List<TuiMessage>> {
        return context.puzldaiChatDataStore.data.map { preferences ->
            val chatJson = preferences[stringPreferencesKey("$CHAT_PREFIX$sessionId")]
            if (chatJson != null) {
                try {
                    val chatData = json.decodeFromString<ChatHistoryData>(chatJson)
                    chatData.messages.map { it.toDomain() }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode chat history for session: $sessionId")
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    /**
     * Delete chat history for a session.
     */
    suspend fun deleteChatHistory(sessionId: String) {
        context.puzldaiChatDataStore.edit { preferences ->
            val chatIds = getChatIdsFromPrefs(preferences).toMutableSet()
            chatIds.remove(sessionId)

            if (chatIds.isEmpty()) {
                preferences.remove(CHAT_IDS_KEY)
            } else {
                preferences[CHAT_IDS_KEY] = chatIds.joinToString(",")
            }
            preferences.remove(stringPreferencesKey("$CHAT_PREFIX$sessionId"))
        }
        Timber.d("Deleted chat history for session: $sessionId")
    }

    /**
     * Clear all chat histories.
     */
    suspend fun clearAllChatHistories() {
        context.puzldaiChatDataStore.edit { preferences ->
            val chatIds = getChatIdsFromPrefs(preferences)
            chatIds.forEach { id ->
                preferences.remove(stringPreferencesKey("$CHAT_PREFIX$id"))
            }
            preferences.remove(CHAT_IDS_KEY)
        }
        Timber.d("Cleared all chat histories")
    }

    /**
     * Get metadata for all chat sessions.
     */
    fun observeAllChatMetadata(): Flow<List<ChatMetadata>> {
        return context.puzldaiChatDataStore.data.map { preferences ->
            val chatIds = getChatIdsFromPrefs(preferences)
            chatIds.mapNotNull { id ->
                val chatJson = preferences[stringPreferencesKey("$CHAT_PREFIX$id")]
                try {
                    chatJson?.let {
                        val chatData = json.decodeFromString<ChatHistoryData>(it)
                        ChatMetadata(
                            sessionId = chatData.sessionId,
                            messageCount = chatData.messages.size,
                            lastUpdated = chatData.lastUpdated
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode chat metadata for session: $id")
                    null
                }
            }.sortedByDescending { it.lastUpdated }
        }
    }

    private fun getChatIdsFromPrefs(preferences: Preferences): Set<String> {
        return preferences[CHAT_IDS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toSet()
            ?: emptySet()
    }
}

/**
 * Metadata about a chat session (for listing without loading all messages).
 */
data class ChatMetadata(
    val sessionId: String,
    val messageCount: Int,
    val lastUpdated: Long
)

/**
 * Serializable data class for chat history storage.
 */
@Serializable
data class ChatHistoryData(
    val sessionId: String,
    val messages: List<TuiMessageData>,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Serializable version of TuiMessage for storage.
 */
@Serializable
data class TuiMessageData(
    val id: String,
    val role: String,
    val content: String,
    val agent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tokens: Int? = null,
    val duration: Long? = null
)

/**
 * Convert TuiMessage to serializable data.
 */
fun TuiMessage.toData(): TuiMessageData = TuiMessageData(
    id = id,
    role = role.name,
    content = content,
    agent = agent,
    timestamp = timestamp,
    tokens = tokens,
    duration = duration
)

/**
 * Convert serializable data back to TuiMessage.
 */
fun TuiMessageData.toDomain(): TuiMessage = TuiMessage(
    id = id,
    role = MessageRole.valueOf(role),
    content = content,
    agent = agent,
    timestamp = timestamp,
    tokens = tokens,
    duration = duration
)
