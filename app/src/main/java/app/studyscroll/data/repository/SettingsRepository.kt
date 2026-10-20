package app.studyscroll.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studyscroll_preferences")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLocalOnlyMode: Boolean = false,
    val cloudProcessingConsented: Boolean = false,
    val defaultGenerationMode: String = "OFFLINE",
    val narrationSpeechRate: Float = 1.0f,
    val allowHttpDev: Boolean = false
)

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLocalOnlyMode(enabled: Boolean)
    suspend fun setCloudConsent(granted: Boolean)
    suspend fun setDefaultGenerationMode(mode: String)
    suspend fun setNarrationSpeechRate(rate: Float)
    suspend fun setAllowHttpDev(allow: Boolean)
    suspend fun clearAllPreferences()
}

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LOCAL_ONLY = booleanPreferencesKey("local_only_mode")
        val CLOUD_CONSENT = booleanPreferencesKey("cloud_consent_granted")
        val DEFAULT_MODE = stringPreferencesKey("default_generation_mode")
        val SPEECH_RATE = floatPreferencesKey("narration_speech_rate")
        val ALLOW_HTTP_DEV = booleanPreferencesKey("allow_http_dev")
    }

    override val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val themeString = preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val theme = runCatching { ThemeMode.valueOf(themeString) }.getOrDefault(ThemeMode.SYSTEM)
        AppSettings(
            themeMode = theme,
            isLocalOnlyMode = preferences[Keys.LOCAL_ONLY] ?: false,
            cloudProcessingConsented = preferences[Keys.CLOUD_CONSENT] ?: false,
            defaultGenerationMode = preferences[Keys.DEFAULT_MODE] ?: "OFFLINE",
            narrationSpeechRate = preferences[Keys.SPEECH_RATE] ?: 1.0f,
            allowHttpDev = preferences[Keys.ALLOW_HTTP_DEV] ?: false
        )
    }

    override suspend fun getSettings(): AppSettings = settingsFlow.first()

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setLocalOnlyMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LOCAL_ONLY] = enabled
        }
    }

    override suspend fun setCloudConsent(granted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CLOUD_CONSENT] = granted
        }
    }

    override suspend fun setDefaultGenerationMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_MODE] = mode
        }
    }

    override suspend fun setNarrationSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SPEECH_RATE] = rate
        }
    }

    override suspend fun setAllowHttpDev(allow: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ALLOW_HTTP_DEV] = allow
        }
    }

    override suspend fun clearAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
