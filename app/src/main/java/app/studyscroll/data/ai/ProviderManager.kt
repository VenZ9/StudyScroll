package app.studyscroll.data.ai

import app.studyscroll.core.Constants
import app.studyscroll.data.local.dao.ProviderDao
import app.studyscroll.data.local.entity.ProviderEntity
import app.studyscroll.data.repository.SettingsRepository
import app.studyscroll.data.security.SecretStorage
import app.studyscroll.domain.model.AuthMode
import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProviderManager(
    private val providerDao: ProviderDao,
    private val secretStorage: SecretStorage,
    private val settingsRepository: SettingsRepository,
    private val offlineEngine: OfflineRuleBasedEngine = OfflineRuleBasedEngine(),
    private val groqProvider: GroqProvider = GroqProvider(),
    private val geminiProvider: GeminiProvider = GeminiProvider(),
    private val nvidiaNimProvider: NvidiaNimProvider = NvidiaNimProvider(),
    private val kiloGatewayProvider: KiloGatewayProvider = KiloGatewayProvider(),
    private val customProvider: CustomCompatibleProvider = CustomCompatibleProvider()
) {

    private val providerAdapters: Map<ProviderType, LessonGenerationProvider> = mapOf(
        ProviderType.OFFLINE to offlineEngine,
        ProviderType.GROQ to groqProvider,
        ProviderType.GEMINI to geminiProvider,
        ProviderType.NVIDIA_NIM to nvidiaNimProvider,
        ProviderType.KILO_GATEWAY to kiloGatewayProvider,
        ProviderType.CUSTOM to customProvider
    )

    val allProviders: Flow<List<ProviderProfile>> = providerDao.getAllProviders().map { entities ->
        entities.map { entity ->
            val hasKey = secretStorage.hasApiKey(entity.id)
            entity.toDomain(hasApiKey = hasKey)
        }
    }

    suspend fun ensureDefaultProvidersSeeded() {
        val existing = providerDao.getPreferredProvider()
        if (existing == null) {
            val defaults = listOf(
                ProviderEntity(
                    id = "provider_offline",
                    type = ProviderType.OFFLINE.name,
                    displayName = "Offline (Template Engine)",
                    baseUrl = "",
                    modelId = "offline-rules-v1",
                    authMode = AuthMode.NONE.name,
                    isPreferred = true
                ),
                ProviderEntity(
                    id = "provider_kilo",
                    type = ProviderType.KILO_GATEWAY.name,
                    displayName = "Kilo AI Gateway",
                    baseUrl = KiloGatewayProvider.DEFAULT_BASE_URL,
                    modelId = KiloGatewayProvider.DEFAULT_MODEL,
                    authMode = AuthMode.ANONYMOUS_FREE.name,
                    isPreferred = false
                ),
                ProviderEntity(
                    id = "provider_groq",
                    type = ProviderType.GROQ.name,
                    displayName = "Groq",
                    baseUrl = GroqProvider.DEFAULT_BASE_URL,
                    modelId = GroqProvider.DEFAULT_MODEL,
                    authMode = AuthMode.API_KEY.name,
                    isPreferred = false
                ),
                ProviderEntity(
                    id = "provider_gemini",
                    type = ProviderType.GEMINI.name,
                    displayName = "Google Gemini",
                    baseUrl = GeminiProvider.DEFAULT_BASE_URL,
                    modelId = GeminiProvider.DEFAULT_MODEL,
                    authMode = AuthMode.API_KEY.name,
                    isPreferred = false
                ),
                ProviderEntity(
                    id = "provider_nvidia",
                    type = ProviderType.NVIDIA_NIM.name,
                    displayName = "NVIDIA NIM",
                    baseUrl = NvidiaNimProvider.DEFAULT_BASE_URL,
                    modelId = NvidiaNimProvider.DEFAULT_MODEL,
                    authMode = AuthMode.API_KEY.name,
                    isPreferred = false
                ),
                ProviderEntity(
                    id = "provider_custom",
                    type = ProviderType.CUSTOM.name,
                    displayName = "Custom Endpoint",
                    baseUrl = "https://api.openai.com/v1",
                    modelId = "gpt-4o-mini",
                    authMode = AuthMode.API_KEY.name,
                    isPreferred = false
                )
            )

            for (p in defaults) {
                providerDao.insertProvider(p)
            }
        }
    }

    suspend fun getProvider(providerId: String): ProviderProfile? {
        val entity = providerDao.getProviderById(providerId) ?: return null
        return entity.toDomain(hasApiKey = secretStorage.hasApiKey(entity.id))
    }

    suspend fun getPreferredProvider(): ProviderProfile {
        ensureDefaultProvidersSeeded()
        val entity = providerDao.getPreferredProvider()
        return entity?.toDomain(hasApiKey = secretStorage.hasApiKey(entity.id))
            ?: ProviderProfile(
                id = "provider_offline",
                type = ProviderType.OFFLINE,
                displayName = "Offline (Template Engine)",
                baseUrl = "",
                modelId = "offline-rules-v1",
                authMode = AuthMode.NONE,
                isPreferred = true
            )
    }

    suspend fun setPreferredProvider(providerId: String) {
        providerDao.setPreferred(providerId)
    }

    suspend fun saveProvider(profile: ProviderProfile, apiKey: String?) {
        val entity = ProviderEntity.fromDomain(profile)
        providerDao.insertProvider(entity)
        if (!apiKey.isNullOrBlank()) {
            secretStorage.saveApiKey(profile.id, apiKey.trim())
        }
    }

    suspend fun deleteProvider(providerId: String) {
        providerDao.deleteProviderById(providerId)
        secretStorage.removeApiKey(providerId)
    }

    suspend fun testConnection(providerId: String, apiKeyOverride: String? = null): Result<String> {
        val profile = getProvider(providerId)
            ?: return Result.failure(IllegalArgumentException("Provider not found: $providerId"))

        if (profile.type != ProviderType.OFFLINE) {
            val settings = settingsRepository.getSettings()
            if (settings.isLocalOnlyMode) {
                return Result.failure(IllegalStateException(Constants.ERROR_LOCAL_ONLY_ACTIVE))
            }
        }

        val adapter = providerAdapters[profile.type]
            ?: return Result.failure(IllegalArgumentException("Unsupported provider type: ${profile.type}"))

        val key = apiKeyOverride ?: secretStorage.getApiKey(providerId)
        val result = adapter.testConnection(profile, key)

        val success = result.isSuccess
        val message = if (success) result.getOrNull() ?: "Success" else result.exceptionOrNull()?.message ?: "Failed"
        providerDao.updateTestResult(providerId, success, message)

        return result
    }

    suspend fun generateLessons(
        providerId: String,
        request: GenerationRequest
    ): Result<List<RawLesson>> {
        val profile = getProvider(providerId)
            ?: return Result.failure(IllegalArgumentException("Provider not found: $providerId"))

        // Security enforcement: Local-only mode & Cloud consent
        if (profile.type != ProviderType.OFFLINE) {
            val settings = settingsRepository.getSettings()
            if (settings.isLocalOnlyMode) {
                return Result.failure(IllegalStateException(Constants.ERROR_LOCAL_ONLY_ACTIVE))
            }
            if (!settings.cloudProcessingConsented) {
                return Result.failure(IllegalStateException(Constants.ERROR_CLOUD_CONSENT_REQUIRED))
            }
        }

        val adapter = providerAdapters[profile.type]
            ?: return Result.failure(IllegalArgumentException("Unsupported provider type: ${profile.type}"))

        val apiKey = secretStorage.getApiKey(providerId)
        return adapter.generateLessons(profile, apiKey, request)
    }
}
