package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.DeloadFrozenVersionEntity

@Dao
interface DeloadFrozenVersionDao {

    @Query("SELECT * FROM deload_frozen_version WHERE deload_id = :deloadId")
    suspend fun getByDeloadId(deloadId: Long): List<DeloadFrozenVersionEntity>

    @Insert
    suspend fun insertAll(entities: List<DeloadFrozenVersionEntity>)

    @Query("DELETE FROM deload_frozen_version WHERE deload_id = :deloadId")
    suspend fun deleteByDeloadId(deloadId: Long)
}
