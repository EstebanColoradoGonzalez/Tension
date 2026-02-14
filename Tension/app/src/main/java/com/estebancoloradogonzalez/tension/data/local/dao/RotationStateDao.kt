package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.RotationStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RotationStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: RotationStateEntity)

    @Query("SELECT * FROM rotation_state WHERE id = 1")
    fun getRotationState(): Flow<RotationStateEntity?>
}
