package com.udroid.app.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.udroid.app.devbox.BindMode
import com.udroid.app.devbox.ServiceTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "dev_services")

@Singleton
class DevServiceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private val SERVICE_CONFIGS_KEY = stringPreferencesKey("service_configs")
        private val SHARE_STATES_KEY = stringPreferencesKey("share_states")
        private const val CONFIG_PREFIX = "config_"
        private const val SHARE_PREFIX = "share_"
    }

    suspend fun saveServiceConfig(config: ServiceConfigData) {
        context.serviceDataStore.edit { preferences ->
            val configIds = getConfigIdsFromPrefs(preferences).toMutableSet()
            configIds.add(config.id)

            preferences[SERVICE_CONFIGS_KEY] = configIds.joinToString(",")
            preferences[stringPreferencesKey("$CONFIG_PREFIX${config.id}")] =
                json.encodeToString(config)
        }
        Timber.d("Saved service config: ${config.id}")
    }

    suspend fun loadServiceConfig(configId: String): ServiceConfigData? {
        val configJson = context.serviceDataStore.data.map { preferences ->
            preferences[stringPreferencesKey("$CONFIG_PREFIX$configId")]
        }.firstOrNull() ?: return null

        return try {
            json.decodeFromString<ServiceConfigData>(configJson)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode service config: $configId")
            null
        }
    }

    fun observeServiceConfigs(): Flow<List<ServiceConfigData>> {
        return context.serviceDataStore.data.map { preferences ->
            val configIds = getConfigIdsFromPrefs(preferences)
            configIds.mapNotNull { id ->
                val configJson = preferences[stringPreferencesKey("$CONFIG_PREFIX$id")]
                try {
                    configJson?.let { this.json.decodeFromString<ServiceConfigData>(it) }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode service config: $id")
                    null
                }
            }
        }
    }

    fun observeServiceConfigsForSession(sessionId: String): Flow<List<ServiceConfigData>> {
        return observeServiceConfigs().map { configs ->
            configs.filter { it.sessionId == sessionId }
        }
    }

    suspend fun deleteServiceConfig(configId: String) {
        context.serviceDataStore.edit { preferences ->
            val configIds = getConfigIdsFromPrefs(preferences).toMutableSet()
            configIds.remove(configId)

            if (configIds.isEmpty()) {
                preferences.remove(SERVICE_CONFIGS_KEY)
            } else {
                preferences[SERVICE_CONFIGS_KEY] = configIds.joinToString(",")
            }
            preferences.remove(stringPreferencesKey("$CONFIG_PREFIX$configId"))
        }
        Timber.d("Deleted service config: $configId")
    }

    suspend fun deleteServiceConfigsForSession(sessionId: String) {
        val configs = observeServiceConfigs().firstOrNull() ?: return
        configs.filter { it.sessionId == sessionId }.forEach { config ->
            deleteServiceConfig(config.id)
        }
    }

    private fun getConfigIdsFromPrefs(preferences: Preferences): Set<String> {
        return preferences[SERVICE_CONFIGS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toSet()
            ?: emptySet()
    }

    // Share State persistence methods

    suspend fun saveShareState(shareState: ShareStateData) {
        context.serviceDataStore.edit { preferences ->
            val shareIds = getShareIdsFromPrefs(preferences).toMutableSet()
            shareIds.add(shareState.serviceId)

            preferences[SHARE_STATES_KEY] = shareIds.joinToString(",")
            preferences[stringPreferencesKey("$SHARE_PREFIX${shareState.serviceId}")] =
                json.encodeToString(shareState)
        }
        Timber.d("Saved share state for service: ${shareState.serviceId}")
    }

    suspend fun loadShareState(serviceId: String): ShareStateData? {
        val shareJson = context.serviceDataStore.data.map { preferences ->
            preferences[stringPreferencesKey("$SHARE_PREFIX$serviceId")]
        }.firstOrNull() ?: return null

        return try {
            json.decodeFromString<ShareStateData>(shareJson)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode share state: $serviceId")
            null
        }
    }

    fun observeShareStates(): Flow<List<ShareStateData>> {
        return context.serviceDataStore.data.map { preferences ->
            val shareIds = getShareIdsFromPrefs(preferences)
            shareIds.mapNotNull { id ->
                val shareJson = preferences[stringPreferencesKey("$SHARE_PREFIX$id")]
                try {
                    shareJson?.let { this.json.decodeFromString<ShareStateData>(it) }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode share state: $id")
                    null
                }
            }
        }
    }

    fun observeShareStatesForSession(sessionId: String): Flow<List<ShareStateData>> {
        return observeShareStates().map { states ->
            states.filter { it.sessionId == sessionId }
        }
    }

    suspend fun deleteShareState(serviceId: String) {
        context.serviceDataStore.edit { preferences ->
            val shareIds = getShareIdsFromPrefs(preferences).toMutableSet()
            shareIds.remove(serviceId)

            if (shareIds.isEmpty()) {
                preferences.remove(SHARE_STATES_KEY)
            } else {
                preferences[SHARE_STATES_KEY] = shareIds.joinToString(",")
            }
            preferences.remove(stringPreferencesKey("$SHARE_PREFIX$serviceId"))
        }
        Timber.d("Deleted share state for service: $serviceId")
    }

    suspend fun updateShareState(serviceId: String, isSharing: Boolean, shareUrl: String? = null) {
        val existingState = loadShareState(serviceId)
        if (existingState != null) {
            saveShareState(
                existingState.copy(
                    isSharing = isSharing,
                    shareUrl = shareUrl,
                    startedAt = if (isSharing) System.currentTimeMillis() else null
                )
            )
        }
    }

    private fun getShareIdsFromPrefs(preferences: Preferences): Set<String> {
        return preferences[SHARE_STATES_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toSet()
            ?: emptySet()
    }
}

@kotlinx.serialization.Serializable
data class ServiceConfigData(
    val id: String,
    val sessionId: String,
    val templateId: String,
    val port: Int,
    val bindMode: String = "LAN",
    val autoStart: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getTemplate(): ServiceTemplate? = ServiceTemplate.fromId(templateId)
    fun getBindMode(): BindMode = BindMode.entries.find { it.name == bindMode } ?: BindMode.LAN
}

/**
 * Persisted model for devbox share state.
 * Tracks whether a service is being shared and connection details.
 */
@kotlinx.serialization.Serializable
data class ShareStateData(
    val serviceId: String,
    val sessionId: String,
    val isSharing: Boolean = false,
    val shareUrl: String? = null,
    val sharePort: Int? = null,
    val shareProtocol: String = "HTTP",
    val startedAt: Long? = null,
    val expiresAt: Long? = null
)

// Conversion functions between domain and persistence models
fun ShareStateData.toDomain(): com.udroid.app.devbox.DevboxShareState {
    return com.udroid.app.devbox.DevboxShareState(
        serviceId = serviceId,
        sessionId = sessionId,
        isSharing = isSharing,
        shareUrl = shareUrl,
        sharePort = sharePort,
        shareProtocol = com.udroid.app.devbox.ShareProtocol.entries
            .find { it.name == shareProtocol } ?: com.udroid.app.devbox.ShareProtocol.HTTP,
        startedAt = startedAt,
        expiresAt = expiresAt
    )
}

fun com.udroid.app.devbox.DevboxShareState.toData(): ShareStateData {
    return ShareStateData(
        serviceId = serviceId,
        sessionId = sessionId,
        isSharing = isSharing,
        shareUrl = shareUrl,
        sharePort = sharePort,
        shareProtocol = shareProtocol.name,
        startedAt = startedAt,
        expiresAt = expiresAt
    )
}
