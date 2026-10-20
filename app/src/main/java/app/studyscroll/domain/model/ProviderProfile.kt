package app.studyscroll.domain.model

data class ProviderProfile(
    val id: String,
    val type: ProviderType,
    val displayName: String,
    val baseUrl: String,
    val modelId: String,
    val authMode: AuthMode,
    val isPreferred: Boolean = false,
    val lastTestSuccess: Boolean? = null,
    val lastTestMessage: String? = null,
    val hasApiKey: Boolean = false
) {
    val requiresApiKey: Boolean
        get() = authMode == AuthMode.API_KEY
}
