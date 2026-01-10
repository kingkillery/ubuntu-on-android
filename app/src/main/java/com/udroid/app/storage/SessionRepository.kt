package com.udroid.app.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.udroid.app.model.DistroVariant
import com.udroid.app.model.SessionState
import com.udroid.app.session.SessionConfig
import com.udroid.app.session.UbuntuSession
import com.udroid.app.session.UbuntuSessionImpl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "sessions")

@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private val SESSION_IDS_KEY = stringPreferencesKey("session_ids")
        private const val SESSION_PREFIX = "session_"
    }

    suspend fun saveSession(session: SessionInfo) {
        context.sessionDataStore.edit { preferences ->
            val sessionIds = getSessionIdsFromPrefs(preferences).toMutableSet()
            sessionIds.add(session.id)
            
            preferences[SESSION_IDS_KEY] = sessionIds.joinToString(",")
            preferences[stringPreferencesKey("$SESSION_PREFIX${session.id}")] = 
                json.encodeToString(session)
        }
        Timber.d("Saved session: ${session.id}")
    }

    suspend fun loadSession(sessionId: String): SessionInfo? {
        val sessionJson = context.sessionDataStore.data.map { preferences ->
            preferences[stringPreferencesKey("$SESSION_PREFIX$sessionId")]
        }.firstOrNull() ?: return null

        return try {
            json.decodeFromString<SessionInfo>(sessionJson)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode session: $sessionId")
            null
        }
    }

    fun observeSessions(): Flow<List<SessionInfo>> {
        return context.sessionDataStore.data.map { preferences ->
            val sessionIds = getSessionIdsFromPrefs(preferences)
            sessionIds.mapNotNull { id ->
                val json = preferences[stringPreferencesKey("$SESSION_PREFIX$id")]
                try {
                    json?.let { this.json.decodeFromString<SessionInfo>(it) }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode session: $id")
                    null
                }
            }.sortedByDescending { it.createdAt }
        }
    }

    suspend fun deleteSession(sessionId: String) {
        context.sessionDataStore.edit { preferences ->
            val sessionIds = getSessionIdsFromPrefs(preferences).toMutableSet()
            sessionIds.remove(sessionId)
            
            if (sessionIds.isEmpty()) {
                preferences.remove(SESSION_IDS_KEY)
            } else {
                preferences[SESSION_IDS_KEY] = sessionIds.joinToString(",")
            }
            preferences.remove(stringPreferencesKey("$SESSION_PREFIX$sessionId"))
        }
        Timber.d("Deleted session: $sessionId")
    }

    suspend fun updateSessionState(sessionId: String, state: SessionStateData) {
        context.sessionDataStore.edit { preferences ->
            val sessionJson = preferences[stringPreferencesKey("$SESSION_PREFIX$sessionId")]
            sessionJson?.let {
                val session = json.decodeFromString<SessionInfo>(it)
                val updated = session.copy(state = state)
                preferences[stringPreferencesKey("$SESSION_PREFIX$sessionId")] =
                    json.encodeToString(updated)
            }
        }
    }

    private fun getSessionIdsFromPrefs(preferences: Preferences): Set<String> {
        return preferences[SESSION_IDS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toSet() 
            ?: emptySet()
    }
}

@kotlinx.serialization.Serializable
data class SessionInfo(
    val id: String,
    val name: String,
    val distroId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val state: SessionStateData = SessionStateData.Created
) {
    fun toConfig() = SessionConfig(
        name = name,
        distro = DistroVariant.fromId(distroId) ?: DistroVariant.JAMMY_XFCE4,
        desktopEnvironment = DistroVariant.fromId(distroId)?.desktop 
            ?: com.udroid.app.model.DesktopEnvironment.XFCE4
    )
}

@kotlinx.serialization.Serializable
sealed class SessionStateData {
    @kotlinx.serialization.Serializable
    data object Created : SessionStateData()

    @kotlinx.serialization.Serializable
    data object Starting : SessionStateData()

    @kotlinx.serialization.Serializable
    data class Running(val vncPort: Int) : SessionStateData()

    @kotlinx.serialization.Serializable
    data object Stopping : SessionStateData()

    @kotlinx.serialization.Serializable
    data object Stopped : SessionStateData()

    @kotlinx.serialization.Serializable
    data class Error(val message: String) : SessionStateData()
}

fun SessionStateData.toDomain(): SessionState = when (this) {
    is SessionStateData.Created -> SessionState.Created
    is SessionStateData.Starting -> SessionState.Starting
    is SessionStateData.Running -> SessionState.Running(vncPort)
    is SessionStateData.Stopping -> SessionState.Stopping
    is SessionStateData.Stopped -> SessionState.Stopped
    is SessionStateData.Error -> SessionState.Error(message)
}

fun SessionState.toData(): SessionStateData = when (this) {
    is SessionState.Created -> SessionStateData.Created
    is SessionState.Starting -> SessionStateData.Starting
    is SessionState.Running -> SessionStateData.Running(vncPort)
    is SessionState.Stopping -> SessionStateData.Stopping
    is SessionState.Stopped -> SessionStateData.Stopped
    is SessionState.Error -> SessionStateData.Error(message)
}
