package app.studyscroll.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "page_texts",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId", "pageNumber"])
    ]
)
data class PageTextEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: String,
    val pageNumber: Int, // 1-based page number
    val blockOrder: Int,
    val blockType: String = "PARAGRAPH", // HEADING, PARAGRAPH, LIST, FORMULA
    val text: String
)
