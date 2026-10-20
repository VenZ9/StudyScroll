package app.studyscroll.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.studyscroll.data.local.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers ORDER BY isPreferred DESC, displayName ASC")
    fun getAllProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getProviderById(id: String): ProviderEntity?

    @Query("SELECT * FROM providers WHERE isPreferred = 1 LIMIT 1")
    suspend fun getPreferredProvider(): ProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderEntity)

    @Update
    suspend fun updateProvider(provider: ProviderEntity)

    @Delete
    suspend fun deleteProvider(provider: ProviderEntity)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteProviderById(id: String)

    @Query("UPDATE providers SET isPreferred = 0")
    suspend fun clearPreferred()

    @Transaction
    suspend fun setPreferred(providerId: String) {
        clearPreferred()
        setPreferredById(providerId)
    }

    @Query("UPDATE providers SET isPreferred = 1 WHERE id = :id")
    suspend fun setPreferredById(id: String)

    @Query("UPDATE providers SET lastTestSuccess = :success, lastTestMessage = :message, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTestResult(id: String, success: Boolean, message: String, timestamp: Long = System.currentTimeMillis())
}
