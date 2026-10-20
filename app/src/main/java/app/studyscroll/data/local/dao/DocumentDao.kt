package app.studyscroll.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.studyscroll.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeDocumentById(id: String): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET title = :newTitle, updatedAt = :timestamp WHERE id = :id")
    suspend fun renameDocument(id: String, newTitle: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET currentPosition = :position, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateCurrentPosition(id: String, position: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET lessonCount = :count, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateLessonCount(id: String, count: Int, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()
}
