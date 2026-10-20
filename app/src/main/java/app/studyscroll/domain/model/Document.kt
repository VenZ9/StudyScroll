package app.studyscroll.domain.model

data class Document(
    val id: String,
    val title: String,
    val uriString: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val lessonCount: Int,
    val currentPosition: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
