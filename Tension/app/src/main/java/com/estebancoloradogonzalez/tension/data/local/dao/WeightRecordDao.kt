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

    @Query("SELECT * FROM weight_record ORDER BY date DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightRecordEntity?>

    @Query("SELECT * FROM weight_record ORDER BY date DESC")
    fun getAllDescByDate(): Flow<List<WeightRecordEntity>>
}
