package app.studyscroll.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.studyscroll.domain.model.QuizAttempt

@Entity(
    tableName = "quiz_attempts",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["lessonId"])
    ]
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String,
    val userAnswer: String,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): QuizAttempt = QuizAttempt(
        id = id,
        lessonId = lessonId,
        userAnswer = userAnswer,
        isCorrect = isCorrect,
        timestamp = timestamp
    )
}
