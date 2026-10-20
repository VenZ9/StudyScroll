package app.studyscroll.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.studyscroll.domain.model.Document

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val uriString: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val lessonCount: Int = 0,
    val currentPosition: Int = 0,
    val extractionStatus: String = "SUCCESS",
    val generationStatus: String = "IDLE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Document = Document(
        id = id,
        title = title,
        uriString = uriString,
        fileSizeBytes = fileSizeBytes,
        pageCount = pageCount,
        lessonCount = lessonCount,
        currentPosition = currentPosition,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(doc: Document, extractionStatus: String = "SUCCESS", generationStatus: String = "IDLE"): DocumentEntity =
            DocumentEntity(
                id = doc.id,
                title = doc.title,
                uriString = doc.uriString,
                fileSizeBytes = doc.fileSizeBytes,
                pageCount = doc.pageCount,
                lessonCount = doc.lessonCount,
                currentPosition = doc.currentPosition,
                extractionStatus = extractionStatus,
                generationStatus = generationStatus,
                createdAt = doc.createdAt,
                updatedAt = doc.updatedAt
            )
    }
}
