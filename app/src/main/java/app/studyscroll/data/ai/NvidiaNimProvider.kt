package app.studyscroll.data.ai

import app.studyscroll.core.Constants
import app.studyscroll.domain.model.ProviderType

class NvidiaNimProvider : OpenAiCompatibleProvider(
    providerType = ProviderType.NVIDIA_NIM,
    displayName = "NVIDIA NIM"
) {
    companion object {
        const val DEFAULT_BASE_URL = Constants.NVIDIA_DEFAULT_URL
        const val DEFAULT_MODEL = "meta/llama-3.3-70b-instruct"
        val POPULAR_MODELS = listOf(
            "meta/llama-3.3-70b-instruct",
            "mistralai/mistral-large-2-instruct",
            "nvidia/nemotron-4-340b-instruct"
        )
    }
}
