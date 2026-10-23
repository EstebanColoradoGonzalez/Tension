package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.WeightRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightRecordDao {
    @Insert
    suspend fun insert(record: WeightRecordEntity)

    /**
     * The current weight is the last entry. Several entries can share a date —each save
     * creates one, from the profile or from Metrics— so `id` breaks the tie: the one saved
     * last wins (HU-48, Rule 3).
     */
    @Query("SELECT * FROM weight_record ORDER BY date DESC, id DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightRecordEntity?>

    @Query("SELECT * FROM weight_record ORDER BY date DESC, id DESC")
    fun getAllDescByDate(): Flow<List<WeightRecordEntity>>
}
