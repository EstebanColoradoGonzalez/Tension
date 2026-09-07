package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.AGGREGATE_SEPARATOR
import com.estebancoloradogonzalez.tension.data.local.dao.EquipmentTypeDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSetDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseWithDetails
import com.estebancoloradogonzalez.tension.data.local.dao.MuscleZoneDao
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseEquipmentEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseMuscleZoneEntity
import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val equipmentTypeDao: EquipmentTypeDao,
    private val muscleZoneDao: MuscleZoneDao,
    private val exerciseSetDao: ExerciseSetDao,
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAll().map { list ->
            list.map { it.toDomainModel() }
        }

    override fun getExerciseById(id: Long): Flow<Exercise?> =
        exerciseDao.getById(id).map { it?.toDomainModel() }

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

    override fun getEquipmentTypesWithExercises(): Flow<List<EquipmentType>> =
        equipmentTypeDao.getWithExercises().map { list ->
            list.map { entity ->
                EquipmentType(
                    id = entity.id,
                    name = entity.name,
                )
            }
        }

    override fun getMuscleZonesWithExercises(): Flow<List<MuscleZone>> =
        muscleZoneDao.getWithExercises().map { list ->
            list.map { entity ->
                MuscleZone(
                    id = entity.id,
                    name = entity.name,
                    muscleGroup = entity.muscleGroup,
                )
            }
        }

    override fun getEquipmentIdsOfExercise(exerciseId: Long): Flow<List<Long>> =
        exerciseDao.getEquipmentIdsByExercise(exerciseId)

    override suspend fun createExercise(
        name: String,
        equipmentTypeIds: List<Long>,
        muscleZoneIds: List<Long>,
        isBodyweight: Boolean,
        isIsometric: Boolean,
        isToTechnicalFailure: Boolean,
        mediaResource: String?,
        progressionDifficulty: ProgressionDifficulty,
    ): Long {
        val entity = ExerciseEntity(
            name = name,
            isBodyweight = if (isBodyweight) 1 else 0,
            isIsometric = if (isIsometric) 1 else 0,
            isToTechnicalFailure = if (isToTechnicalFailure) 1 else 0,
            isCustom = 1,
            mediaResource = mediaResource,
            progressionDifficulty = progressionDifficulty.name,
        )
        return exerciseDao.insertExerciseWithRelations(
            exercise = entity,
            muscleZones = muscleZoneIds.map { zoneId ->
                ExerciseMuscleZoneEntity(exerciseId = 0, muscleZoneId = zoneId)
            },
            equipment = equipmentTypeIds.map { equipmentId ->
                ExerciseEquipmentEntity(exerciseId = 0, equipmentTypeId = equipmentId)
            },
        )
    }

    override suspend fun updateExerciseImage(exerciseId: Long, mediaResource: String?) {
        exerciseDao.updateMediaResource(exerciseId, mediaResource)
    }

    override suspend fun updateProgressionDifficulty(
        exerciseId: Long,
        difficulty: ProgressionDifficulty,
    ) {
        exerciseDao.updateProgressionDifficulty(exerciseId, difficulty.name)
    }

    override suspend fun exerciseExistsByName(name: String): Boolean =
        exerciseDao.countByName(name) > 0

    override suspend fun addEquipmentToExercise(exerciseId: Long, equipmentTypeId: Long) {
        exerciseDao.insertAllEquipment(
            listOf(
                ExerciseEquipmentEntity(
                    exerciseId = exerciseId,
                    equipmentTypeId = equipmentTypeId,
                ),
            ),
        )
    }

    override suspend fun removeEquipmentFromExercise(exerciseId: Long, equipmentTypeId: Long) {
        exerciseDao.deleteEquipment(exerciseId, equipmentTypeId)
    }

    override suspend fun countEquipmentOfExercise(exerciseId: Long): Int =
        exerciseDao.countEquipmentByExercise(exerciseId)

    override suspend fun countSetsWithEquipment(exerciseId: Long, equipmentTypeId: Long): Int =
        exerciseSetDao.countSetsByExerciseAndEquipment(exerciseId, equipmentTypeId)

    private fun ExerciseWithDetails.toDomainModel() =
        Exercise(
            id = id,
            name = name,
            equipmentTypes = equipmentTypes.toAggregatedList(),
            muscleZones = muscleZones.toAggregatedList(),
            muscleGroup = muscleGroup,
            isBodyweight = isBodyweight == 1,
            isIsometric = isIsometric == 1,
            isToTechnicalFailure = isToTechnicalFailure == 1,
            isCustom = isCustom == 1,
            mediaResource = mediaResource,
            progressionDifficulty = ProgressionDifficulty.fromCode(progressionDifficulty),
        )
}

/**
 * Deshace la agregación que hacen las consultas de catálogo y de plan.
 *
 * Ver [AGGREGATE_SEPARATOR]: el separador es la barra vertical y no `", "`, que era lo
 * que se partía antes contra un `GROUP_CONCAT` que emitía `","` — y por eso el filtro por
 * zona muscular no encontraba los ejercicios de dos zonas.
 */
internal fun String?.toAggregatedList(): List<String> =
    this?.split(AGGREGATE_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
