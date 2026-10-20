package app.studyscroll.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.studyscroll.data.local.entity.PageTextEntity

@Dao
interface PageTextDao {
    @Query("SELECT * FROM page_texts WHERE documentId = :documentId ORDER BY pageNumber ASC, blockOrder ASC")
    suspend fun getPageTextsForDocument(documentId: String): List<PageTextEntity>

    @Query("SELECT * FROM page_texts WHERE documentId = :documentId AND pageNumber BETWEEN :startPage AND :endPage ORDER BY pageNumber ASC, blockOrder ASC")
    suspend fun getPageTextsForRange(documentId: String, startPage: Int, endPage: Int): List<PageTextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageTexts(texts: List<PageTextEntity>)

    @Query("DELETE FROM page_texts WHERE documentId = :documentId")
    suspend fun deletePageTextsForDocument(documentId: String)
}
