package app.studyscroll.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: String,
    val documentId: String,
    val orderIndex: Int,
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
    val confidence: ConfidenceLabel = ConfidenceLabel.HIGH,
    val bookmarked: Boolean = false,
    val understood: Boolean? = null,
    val generatedBy: String = "Offline Heuristic"
) {
    val formattedSourceLine: String
        get() {
            if (sources.isEmpty()) return "Source: Unknown"
            val distinctPages = sources.map { it.pageNumber }.distinct().sorted()
            return if (distinctPages.size == 1) {
                "Source: p. ${distinctPages.first()}"
            } else {
                "Sources: pp. ${distinctPages.joinToString(", ")}"
            }
        }
}
