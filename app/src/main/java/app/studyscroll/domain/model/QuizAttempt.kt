package app.studyscroll.domain.model

data class QuizAttempt(
    val id: Long = 0,
    val lessonId: String,
    val userAnswer: String,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
