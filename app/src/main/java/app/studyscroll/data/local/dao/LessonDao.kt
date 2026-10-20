package app.studyscroll.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.studyscroll.data.local.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE documentId = :documentId ORDER BY orderIndex ASC")
    fun getLessonsForDocument(documentId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE documentId = :documentId ORDER BY orderIndex ASC")
    suspend fun getLessonsForDocumentSync(documentId: String): List<LessonEntity>

    @Query("SELECT * FROM lessons ORDER BY documentId ASC, orderIndex ASC")
    suspend fun getAllLessonsSync(): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE documentId = :documentId AND bookmarked = 1 ORDER BY orderIndex ASC")
    fun getBookmarkedLessons(documentId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE documentId = :documentId AND (understood = 0 OR understood IS NULL) ORDER BY orderIndex ASC")
    fun getReviewLessons(documentId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE documentId = :documentId AND type = 'QUIZ' ORDER BY orderIndex ASC")
    fun getQuizLessons(documentId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLessonById(id: String): LessonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Query("UPDATE lessons SET bookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean)

    @Query("UPDATE lessons SET understood = :understood WHERE id = :id")
    suspend fun updateUnderstood(id: String, understood: Boolean?)

    @Query("DELETE FROM lessons WHERE documentId = :documentId")
    suspend fun deleteLessonsForDocument(documentId: String)

    @Transaction
    suspend fun replaceLessonsForDocument(documentId: String, lessons: List<LessonEntity>) {
        deleteLessonsForDocument(documentId)
        insertLessons(lessons)
    }

    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
}
