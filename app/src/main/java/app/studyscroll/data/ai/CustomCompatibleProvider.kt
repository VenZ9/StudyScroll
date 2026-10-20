package app.studyscroll.data.ai

import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType

class CustomCompatibleProvider : OpenAiCompatibleProvider(
    providerType = ProviderType.CUSTOM,
    displayName = "Custom (OpenAI-compatible)"
) {
    override suspend fun testConnection(profile: ProviderProfile, apiKey: String?): Result<String> {
        val validationError = validateBaseUrl(profile.baseUrl)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }
        return super.testConnection(profile, apiKey)
    }

    override suspend fun generateLessons(
        profile: ProviderProfile,
        apiKey: String?,
        request: GenerationRequest
    ): Result<List<RawLesson>> {
        val validationError = validateBaseUrl(profile.baseUrl)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }
        return super.generateLessons(profile, apiKey, request)
    }

    companion object {
        fun validateBaseUrl(url: String, allowHttp: Boolean = false): String? {
            if (url.isBlank()) return "Base URL cannot be blank."
            val trimmed = url.trim()
            if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
                return "URL must begin with https:// (or http:// for local development)."
            }
            if (trimmed.startsWith("http://") && !allowHttp && !trimmed.contains("10.0.2.2") && !trimmed.contains("localhost")) {
                return "Cleartext HTTP is not allowed for security reasons. Please use HTTPS."
            }
            return null
        }
    }
}
