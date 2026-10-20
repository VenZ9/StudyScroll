package app.studyscroll.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.studyscroll.data.local.entity.QuizAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_attempts WHERE lessonId = :lessonId ORDER BY timestamp DESC")
    fun getAttemptsForLesson(lessonId: String): Flow<List<QuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity): Long

    @Query("DELETE FROM quiz_attempts WHERE lessonId = :lessonId")
    suspend fun deleteAttemptsForLesson(lessonId: String)
}
