package app.studyscroll.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.studyscroll.domain.model.AuthMode
import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val type: String, // OFFLINE, GROQ, GEMINI, NVIDIA_NIM, KILO_GATEWAY, CUSTOM
    val displayName: String,
    val baseUrl: String,
    val modelId: String,
    val authMode: String, // NONE, API_KEY, ANONYMOUS_FREE
    val isPreferred: Boolean = false,
    val lastTestSuccess: Boolean? = null,
    val lastTestMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(hasApiKey: Boolean = false): ProviderProfile = ProviderProfile(
        id = id,
        type = runCatching { ProviderType.valueOf(type) }.getOrDefault(ProviderType.OFFLINE),
        displayName = displayName,
        baseUrl = baseUrl,
        modelId = modelId,
        authMode = runCatching { AuthMode.valueOf(authMode) }.getOrDefault(AuthMode.NONE),
        isPreferred = isPreferred,
        lastTestSuccess = lastTestSuccess,
        lastTestMessage = lastTestMessage,
        hasApiKey = hasApiKey
    )

    companion object {
        fun fromDomain(profile: ProviderProfile): ProviderEntity = ProviderEntity(
            id = profile.id,
            type = profile.type.name,
            displayName = profile.displayName,
            baseUrl = profile.baseUrl,
            modelId = profile.modelId,
            authMode = profile.authMode.name,
            isPreferred = profile.isPreferred,
            lastTestSuccess = profile.lastTestSuccess,
            lastTestMessage = profile.lastTestMessage
        )
    }
}
