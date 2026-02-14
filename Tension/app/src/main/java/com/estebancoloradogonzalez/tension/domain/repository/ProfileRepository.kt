package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel
import com.estebancoloradogonzalez.tension.domain.model.Profile
import com.estebancoloradogonzalez.tension.domain.model.WeightRecord
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun createProfile(
        weightKg: Double,
        heightM: Double,
        experienceLevel: ExperienceLevel,
    )

    fun getProfile(): Flow<Profile?>

    suspend fun updateProfile(
        heightM: Double,
        experienceLevel: ExperienceLevel,
    )

    suspend fun updateWeight(weightKg: Double)

    fun getLatestWeight(): Flow<Double?>

    fun getAllWeightRecords(): Flow<List<WeightRecord>>

    fun profileExists(): Flow<Boolean>
}
