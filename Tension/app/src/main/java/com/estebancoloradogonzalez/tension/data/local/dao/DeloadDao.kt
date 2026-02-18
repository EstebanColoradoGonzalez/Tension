package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.DeloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeloadDao {

    @Insert
    suspend fun insert(deload: DeloadEntity): Long

    @Query("SELECT * FROM deload WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveDeload(): Flow<DeloadEntity?>

    @Query("SELECT * FROM deload WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveDeloadOnce(): DeloadEntity?

    @Query(
        """
        UPDATE deload
        SET status = 'COMPLETED', completion_date = :completionDate
        WHERE id = :deloadId
        """,
    )
    suspend fun complete(deloadId: Long, completionDate: String)

    @Query("SELECT * FROM deload WHERE id = :deloadId")
    suspend fun getById(deloadId: Long): DeloadEntity?

    @Query(
        """
        SELECT * FROM deload
        WHERE status = 'COMPLETED'
        ORDER BY completion_date DESC
        LIMIT 1
        """,
    )
    suspend fun getLastCompletedDeload(): DeloadEntity?
}
