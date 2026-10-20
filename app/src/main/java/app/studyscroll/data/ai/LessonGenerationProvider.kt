package app.studyscroll.data.ai

import app.studyscroll.data.pdf.TextBlock
import app.studyscroll.domain.model.ConfidenceLabel
import app.studyscroll.domain.model.LessonType
import app.studyscroll.domain.model.ProviderProfile
import app.studyscroll.domain.model.ProviderType
import app.studyscroll.domain.model.SourceReference
import kotlinx.serialization.Serializable

data class GenerationRequest(
    val documentId: String,
    val documentTitle: String,
    val textBlocks: List<TextBlock>,
    val targetCount: Int = 40
)

@Serializable
data class RawLesson(
    val type: LessonType,
    val title: String,
    val body: String,
    val bullets: List<String> = emptyList(),
    val example: String? = null,
    val question: String? = null,
    val answer: String? = null,
    val explanation: String? = null,
    val options: List<String> = emptyList(),
    val whyItMatters: String? = null,
    val sources: List<SourceReference>,
    val confidence: ConfidenceLabel = ConfidenceLabel.HIGH
)

interface LessonGenerationProvider {
    val providerType: ProviderType
    val displayName: String

    suspend fun testConnection(profile: ProviderProfile, apiKey: String?): Result<String>
    suspend fun generateLessons(
        profile: ProviderProfile,
        apiKey: String?,
        request: GenerationRequest
    ): Result<List<RawLesson>>
}
