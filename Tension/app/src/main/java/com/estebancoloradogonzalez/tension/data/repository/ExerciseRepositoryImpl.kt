package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.EquipmentTypeDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleDao
import com.estebancoloradogonzalez.tension.data.local.dao.MuscleZoneDao
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseMuscleZoneEntity
import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.Module
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val moduleDao: ModuleDao,
    private val equipmentTypeDao: EquipmentTypeDao,
    private val muscleZoneDao: MuscleZoneDao,
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAll().map { list ->
            list.map { it.toDomainModel() }
        }

    override fun getExerciseById(id: Long): Flow<Exercise?> =
        exerciseDao.getById(id).map { it?.toDomainModel() }

    override fun getAllModules(): Flow<List<Module>> =
        moduleDao.getAll().map { list ->
            list.map { entity ->
                Module(
                    code = entity.code,
                    name = entity.name,
                    groupDescription = entity.groupDescription,
                    loadIncrementKg = entity.loadIncrementKg,
                )
            }
        }

    override fun getAllEquipmentTypes(): Flow<List<EquipmentType>> =
        equipmentTypeDao.getAll().map { list ->
            list.map { entity ->
                EquipmentType(
                    id = entity.id,
                    name = entity.name,
                )
            }
        }

    override fun getAllMuscleZones(): Flow<List<MuscleZone>> =
        muscleZoneDao.getAll().map { list ->
            list.map { entity ->
                MuscleZone(
                    id = entity.id,
                    name = entity.name,
                    muscleGroup = entity.muscleGroup,
                )
            }
        }

    override suspend fun createExercise(
        name: String,
        moduleCode: String,
        equipmentTypeId: Long,
        muscleZoneIds: List<Long>,
        isBodyweight: Boolean,
        isIsometric: Boolean,
        isToTechnicalFailure: Boolean,
        mediaResource: String?,
    ): Long {
        val entity = ExerciseEntity(
            name = name,
            moduleCode = moduleCode,
            equipmentTypeId = equipmentTypeId,
            isBodyweight = if (isBodyweight) 1 else 0,
            isIsometric = if (isIsometric) 1 else 0,
            isToTechnicalFailure = if (isToTechnicalFailure) 1 else 0,
            isCustom = 1,
            mediaResource = mediaResource,
        )
        return exerciseDao.insertExerciseWithMuscleZones(
            entity,
            muscleZoneIds.map { zoneId ->
                ExerciseMuscleZoneEntity(exerciseId = 0, muscleZoneId = zoneId)
            },
        )
    }

    override suspend fun updateExerciseImage(exerciseId: Long, mediaResource: String?) {
        exerciseDao.updateMediaResource(exerciseId, mediaResource)
    }

    override suspend fun exerciseExistsByNameAndEquipment(
        name: String,
        equipmentTypeId: Long,
    ): Boolean = exerciseDao.countByNameAndEquipment(name, equipmentTypeId) > 0

    override fun getEligibleSubstitutes(
        moduleCode: String,
        excludedExerciseIds: List<Long>,
    ): Flow<List<Exercise>> =
        exerciseDao.getByModuleCodeNotInIds(moduleCode, excludedExerciseIds).map { list ->
            list.map { it.toDomainModel() }
        }

    private fun com.estebancoloradogonzalez.tension.data.local.dao.ExerciseWithDetails.toDomainModel() =
        Exercise(
            id = id,
            name = name,
            moduleCode = moduleCode,
            moduleName = moduleName,
            equipmentTypeName = equipmentTypeName,
            muscleZones = muscleZones?.split(", ")?.filter { it.isNotBlank() } ?: emptyList(),
            isBodyweight = isBodyweight == 1,
            isIsometric = isIsometric == 1,
            isToTechnicalFailure = isToTechnicalFailure == 1,
            isCustom = isCustom == 1,
            mediaResource = mediaResource,
        )
}
