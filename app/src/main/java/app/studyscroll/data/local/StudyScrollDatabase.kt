package app.studyscroll.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.studyscroll.data.local.dao.DocumentDao
import app.studyscroll.data.local.dao.LessonDao
import app.studyscroll.data.local.dao.PageTextDao
import app.studyscroll.data.local.dao.ProviderDao
import app.studyscroll.data.local.dao.QuizDao
import app.studyscroll.data.local.entity.DocumentEntity
import app.studyscroll.data.local.entity.LessonEntity
import app.studyscroll.data.local.entity.PageTextEntity
import app.studyscroll.data.local.entity.ProviderEntity
import app.studyscroll.data.local.entity.QuizAttemptEntity

@Database(
    entities = [
        DocumentEntity::class,
        PageTextEntity::class,
        LessonEntity::class,
        QuizAttemptEntity::class,
        ProviderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyScrollDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun pageTextDao(): PageTextDao
    abstract fun lessonDao(): LessonDao
    abstract fun quizDao(): QuizDao
    abstract fun providerDao(): ProviderDao

    companion object {
        @Volatile
        private var instance: StudyScrollDatabase? = null

        fun getInstance(context: Context): StudyScrollDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyScrollDatabase::class.java,
                    "studyscroll.db"
                ).build().also { instance = it }
            }
        }
    }
}
