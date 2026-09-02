package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.estebancoloradogonzalez.tension.data.local.entity.TreeStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: TreeStateEntity)

    @Query("SELECT * FROM tree_state WHERE id = 1")
    fun getTreeState(): Flow<TreeStateEntity?>

    @Query("SELECT * FROM tree_state WHERE id = 1")
    suspend fun getTreeStateOnce(): TreeStateEntity?
}
