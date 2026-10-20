package app.studyscroll.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.studyscroll.domain.model.ConfidenceLabel
import app.studyscroll.domain.model.Lesson
import app.studyscroll.domain.model.LessonType
import app.studyscroll.domain.model.SourceReference

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId", "orderIndex"]),
        Index(value = ["bookmarked"]),
        Index(value = ["understood"])
    ]
)
data class LessonEntity(
    @PrimaryKey val id: String,
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
    val sources: List<SourceReference> = emptyList(),
    val confidence: ConfidenceLabel = ConfidenceLabel.HIGH,
    val bookmarked: Boolean = false,
    val understood: Boolean? = null,
    val generatedBy: String = "Offline Heuristic",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Lesson = Lesson(
        id = id,
        documentId = documentId,
        orderIndex = orderIndex,
        type = type,
        title = title,
        body = body,
        bullets = bullets,
        example = example,
        question = question,
        answer = answer,
        explanation = explanation,
        options = options,
        whyItMatters = whyItMatters,
        sources = sources,
        confidence = confidence,
        bookmarked = bookmarked,
        understood = understood,
        generatedBy = generatedBy
    )

    companion object {
        fun fromDomain(lesson: Lesson): LessonEntity = LessonEntity(
            id = lesson.id,
            documentId = lesson.documentId,
            orderIndex = lesson.orderIndex,
            type = lesson.type,
            title = lesson.title,
            body = lesson.body,
            bullets = lesson.bullets,
            example = lesson.example,
            question = lesson.question,
            answer = lesson.answer,
            explanation = lesson.explanation,
            options = lesson.options,
            whyItMatters = lesson.whyItMatters,
            sources = lesson.sources,
            confidence = lesson.confidence,
            bookmarked = lesson.bookmarked,
            understood = lesson.understood,
            generatedBy = lesson.generatedBy
        )
    }
}
