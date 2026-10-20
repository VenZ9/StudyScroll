package app.studyscroll.data.ai

import app.studyscroll.core.Constants
import app.studyscroll.domain.model.ProviderType

class GroqProvider : OpenAiCompatibleProvider(
    providerType = ProviderType.GROQ,
    displayName = "Groq"
) {
    companion object {
        const val DEFAULT_BASE_URL = Constants.GROQ_DEFAULT_URL
        const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
        val POPULAR_MODELS = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "mixtral-8x7b-32768"
        )
    }
}
