package app.studyscroll.data.ai

import app.studyscroll.core.Constants
import app.studyscroll.domain.model.AuthMode
import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType
import okhttp3.Request

class KiloGatewayProvider : OpenAiCompatibleProvider(
    providerType = ProviderType.KILO_GATEWAY,
    displayName = "Kilo AI Gateway"
) {
    override fun addHeaders(builder: Request.Builder, profile: ProviderProfile, apiKey: String?) {
        builder.addHeader("Content-Type", "application/json")
        if (profile.authMode == AuthMode.API_KEY && !apiKey.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $apiKey")
        }
        // If profile.authMode is ANONYMOUS_FREE, no Authorization header is added for eligible free models
    }

    companion object {
        const val DEFAULT_BASE_URL = Constants.KILO_GATEWAY_DEFAULT_URL
        const val DEFAULT_MODEL = "kilo/free-tier"
        val POPULAR_MODELS = listOf(
            "kilo/free-tier",
            "meta-llama/llama-3.3-70b-instruct",
            "mistralai/mistral-7b-instruct"
        )
    }
}
