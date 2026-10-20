package app.studyscroll.ai

import app.studyscroll.core.Constants
import app.studyscroll.data.ai.GenerationRequest
import app.studyscroll.data.ai.ProviderManager
import app.studyscroll.data.local.dao.ProviderDao
import app.studyscroll.data.local.entity.ProviderEntity
import app.studyscroll.data.pdf.BlockType
import app.studyscroll.data.pdf.TextBlock
import app.studyscroll.data.repository.AppSettings
import app.studyscroll.data.repository.SettingsRepository
import app.studyscroll.data.repository.ThemeMode
import app.studyscroll.data.security.MemoryEncryptedSecretStorage
import app.studyscroll.domain.model.AuthMode
import app.studyscroll.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalOnlyModeTest {

    private val fakeDao = object : ProviderDao {
        private val list = mutableListOf(
            ProviderEntity(
                id = "provider_offline",
                type = ProviderType.OFFLINE.name,
                displayName = "Offline Engine",
                baseUrl = "",
                modelId = "offline-v1",
                authMode = AuthMode.NONE.name,
                isPreferred = true
            ),
            ProviderEntity(
                id = "provider_groq",
                type = ProviderType.GROQ.name,
                displayName = "Groq",
                baseUrl = "https://api.groq.com/openai/v1",
                modelId = "llama-3.3-70b-versatile",
                authMode = AuthMode.API_KEY.name,
                isPreferred = false
            )
        )

        override fun getAllProviders(): Flow<List<ProviderEntity>> = flowOf(list)
        override suspend fun getProviderById(id: String): ProviderEntity? = list.find { it.id == id }
        override suspend fun getPreferredProvider(): ProviderEntity? = list.find { it.isPreferred }
        override suspend fun insertProvider(provider: ProviderEntity) { list.add(provider) }
        override suspend fun updateProvider(provider: ProviderEntity) {}
        override suspend fun deleteProvider(provider: ProviderEntity) { list.remove(provider) }
        override suspend fun deleteProviderById(id: String) { list.removeAll { it.id == id } }
        override suspend fun clearPreferred() {}
        override suspend fun setPreferredById(id: String) {}
        override suspend fun updateTestResult(id: String, success: Boolean, message: String, timestamp: Long) {}
    }

    private val secretStorage = MemoryEncryptedSecretStorage()

    class FakeSettingsRepository(
        private val isLocalOnly: Boolean,
        private val consentGranted: Boolean
    ) : SettingsRepository {
        override val settingsFlow: Flow<AppSettings> = flowOf(
            AppSettings(isLocalOnlyMode = isLocalOnly, cloudProcessingConsented = consentGranted)
        )
        override suspend fun getSettings(): AppSettings = AppSettings(
            isLocalOnlyMode = isLocalOnly,
            cloudProcessingConsented = consentGranted
        )
        override suspend fun setThemeMode(mode: ThemeMode) {}
        override suspend fun setLocalOnlyMode(enabled: Boolean) {}
        override suspend fun setCloudConsent(granted: Boolean) {}
        override suspend fun setDefaultGenerationMode(mode: String) {}
        override suspend fun setNarrationSpeechRate(rate: Float) {}
        override suspend fun setAllowHttpDev(allow: Boolean) {}
        override suspend fun clearAllPreferences() {}
    }

    @Test
    fun `local-only mode hard blocks cloud provider requests`() = runBlocking {
        val fakeSettingsRepo = FakeSettingsRepository(isLocalOnly = true, consentGranted = true)

        val providerManager = ProviderManager(
            providerDao = fakeDao,
            secretStorage = secretStorage,
            settingsRepository = fakeSettingsRepo
        )

        val request = GenerationRequest(
            documentId = "doc_1",
            documentTitle = "Test Document",
            textBlocks = listOf(
                TextBlock(
                    pageNumber = 1,
                    type = BlockType.PARAGRAPH,
                    title = "Title",
                    content = "Some content for lesson generation test.",
                    rawExcerpt = "Some content..."
                )
            )
        )

        // Calling cloud provider Groq must fail immediately with ERROR_LOCAL_ONLY_ACTIVE
        val cloudResult = providerManager.generateLessons("provider_groq", request)
        assertTrue(cloudResult.isFailure)
        assertEquals(Constants.ERROR_LOCAL_ONLY_ACTIVE, cloudResult.exceptionOrNull()?.message)

        // Calling offline provider must still succeed
        val offlineResult = providerManager.generateLessons("provider_offline", request)
        assertTrue(offlineResult.isSuccess)
    }

    @Test
    fun `cloud provider requires explicit cloud consent`() = runBlocking {
        val fakeSettingsRepo = FakeSettingsRepository(isLocalOnly = false, consentGranted = false)

        val providerManager = ProviderManager(
            providerDao = fakeDao,
            secretStorage = secretStorage,
            settingsRepository = fakeSettingsRepo
        )

        val request = GenerationRequest(
            documentId = "doc_1",
            documentTitle = "Test Document",
            textBlocks = listOf(
                TextBlock(
                    pageNumber = 1,
                    type = BlockType.PARAGRAPH,
                    title = "Title",
                    content = "Some content for lesson generation test.",
                    rawExcerpt = "Some content..."
                )
            )
        )

        val result = providerManager.generateLessons("provider_groq", request)
        assertTrue(result.isFailure)
        assertEquals(Constants.ERROR_CLOUD_CONSENT_REQUIRED, result.exceptionOrNull()?.message)
    }
}
