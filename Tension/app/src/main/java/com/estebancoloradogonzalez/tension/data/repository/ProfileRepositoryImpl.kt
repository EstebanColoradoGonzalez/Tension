package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.ProfileDao
import com.estebancoloradogonzalez.tension.data.local.dao.RotationStateDao
import com.estebancoloradogonzalez.tension.data.local.dao.WeightRecordDao
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.entity.ProfileEntity
import com.estebancoloradogonzalez.tension.data.local.entity.RotationStateEntity
import com.estebancoloradogonzalez.tension.data.local.entity.WeightRecordEntity
import com.estebancoloradogonzalez.tension.domain.model.ExperienceLevel
import com.estebancoloradogonzalez.tension.domain.model.Profile
import com.estebancoloradogonzalez.tension.domain.model.WeightRecord
import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val weightRecordDao: WeightRecordDao,
    private val rotationStateDao: RotationStateDao,
    private val database: TensionDatabase,
) : ProfileRepository {

    override suspend fun createProfile(
        weightKg: Double,
        heightM: Double,
        experienceLevel: ExperienceLevel,
    ) {
        database.withTransaction {
            val now = LocalDate.now().toString()
            profileDao.insert(
                ProfileEntity(
                    id = 1,
                    heightM = heightM,
                    experienceLevel = experienceLevel.name,
                    weeklyFrequency = 6,
                    createdAt = now,
                ),
            )
            weightRecordDao.insert(
                WeightRecordEntity(
                    weightKg = weightKg,
                    date = now,
                ),
            )
            rotationStateDao.insert(RotationStateEntity())
        }
    }

    override fun getProfile(): Flow<Profile?> {
        return combine(
            profileDao.getProfile(),
            weightRecordDao.getLatestWeight(),
        ) { profileEntity, latestWeight ->
            if (profileEntity != null && latestWeight != null) {
                Profile(
                    currentWeightKg = latestWeight.weightKg,
                    heightM = profileEntity.heightM,
                    experienceLevel = ExperienceLevel.valueOf(profileEntity.experienceLevel),
                    weeklyFrequency = profileEntity.weeklyFrequency,
                    createdAt = LocalDate.parse(profileEntity.createdAt),
                )
            } else {
                null
            }
        }
    }

    override suspend fun updateProfile(
        heightM: Double,
        experienceLevel: ExperienceLevel,
    ) {
        val current = profileDao.getProfile().first() ?: return
        profileDao.update(
            current.copy(
                heightM = heightM,
                experienceLevel = experienceLevel.name,
            ),
        )
    }

    override suspend fun updateWeight(weightKg: Double) {
        weightRecordDao.insert(
            WeightRecordEntity(
                weightKg = weightKg,
                date = LocalDate.now().toString(),
            ),
        )
    }

    override fun getLatestWeight(): Flow<Double?> {
        return weightRecordDao.getLatestWeight().map { it?.weightKg }
    }

    override fun getAllWeightRecords(): Flow<List<WeightRecord>> {
        return weightRecordDao.getAllDescByDate().map { records ->
            records.map { entity ->
                WeightRecord(
                    id = entity.id,
                    weightKg = entity.weightKg,
                    date = LocalDate.parse(entity.date),
                )
            }
        }
    }

    override fun profileExists(): Flow<Boolean> {
        return profileDao.getProfile().map { it != null }
    }
}
