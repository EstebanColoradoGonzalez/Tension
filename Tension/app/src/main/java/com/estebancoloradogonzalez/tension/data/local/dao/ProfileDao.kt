package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.estebancoloradogonzalez.tension.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Query("SELECT * FROM profile WHERE id = 1")
    fun getProfile(): Flow<ProfileEntity?>

    @Update
    suspend fun update(profile: ProfileEntity)
}
