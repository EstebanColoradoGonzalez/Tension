package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineVersionDao
import com.estebancoloradogonzalez.tension.data.local.entity.PlanAssignmentEntity
import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.PlanExercise
import com.estebancoloradogonzalez.tension.domain.model.PlanVersionDetail
import com.estebancoloradogonzalez.tension.domain.model.Routine
import com.estebancoloradogonzalez.tension.domain.model.RoutineWithVersions
import com.estebancoloradogonzalez.tension.domain.model.VersionSummary
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlanRepositoryImpl @Inject constructor(
    private val planAssignmentDao: PlanAssignmentDao,
    private val routineVersionDao: RoutineVersionDao,
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseDao,
) : PlanRepository {

    override fun getRoutinesWithVersionCounts(): Flow<List<RoutineWithVersions>> =
        combine(
            routineDao.getAll(),
            routineVersionDao.getAllWithExerciseCount(),
        ) { routines, versions ->
            val versionsByRoutine = versions.groupBy { it.routineId }
            routines.map { entity ->
                RoutineWithVersions(
                    routine = Routine(
                        id = entity.id,
                        name = entity.name,
                        sortOrder = entity.sortOrder,
                        createdAt = entity.createdAt,
                    ),
                    versions = versionsByRoutine[entity.id]?.map { rv ->
                        VersionSummary(
                            routineVersionId = rv.id,
                            versionNumber = rv.versionNumber,
                            exerciseCount = rv.exerciseCount,
                        )
                    } ?: emptyList(),
                )
            }
        }

    override suspend fun getAllRoutines(): List<Routine> =
        routineDao.getAllOnce().map { entity ->
            Routine(
                id = entity.id,
                name = entity.name,
                sortOrder = entity.sortOrder,
                createdAt = entity.createdAt,
            )
        }

    override fun getVersionDetail(routineVersionId: Long): Flow<PlanVersionDetail?> =
        combine(
            routineVersionDao.getById(routineVersionId),
            planAssignmentDao.getDetailsByRoutineVersionId(routineVersionId),
            routineDao.getAll(),
        ) { routineVersion, assignments, routines ->
            routineVersion?.let { rv ->
                val routine = routines.find { it.id == rv.routineId }
                PlanVersionDetail(
                    routineVersionId = rv.id,
                    routineName = routine?.name ?: "",
                    versionNumber = rv.versionNumber,
                    exercises = assignments.map { pa ->
                        PlanExercise(
                            exerciseId = pa.exerciseId,
                            name = pa.exerciseName,
                            equipmentTypeName = pa.equipmentTypeName,
                            muscleZones = pa.muscleZones?.split(", ")?.filter { it.isNotBlank() } ?: emptyList(),
                            sets = pa.sets,
                            reps = pa.reps,
                            isBodyweight = pa.isBodyweight == 1,
                            isIsometric = pa.isIsometric == 1,
                            isToTechnicalFailure = pa.isToTechnicalFailure == 1,
                            isCustom = pa.isCustom == 1,
                            slot = pa.slot,
                        )
                    },
                )
            }
        }

    override fun getAvailableExercisesForVersion(routineVersionId: Long): Flow<List<Exercise>> =
        exerciseDao.getNotInVersion(routineVersionId).map { list ->
            list.map { it.toDomainModel() }
        }

    override suspend fun assignExercise(
        routineVersionId: Long,
        exerciseId: Long,
        sets: Int,
        reps: String,
    ) {
        val nextSortOrder = (planAssignmentDao.getMaxSortOrder(routineVersionId) ?: 0) + 1
        val nextSlot = (planAssignmentDao.getMaxSlot(routineVersionId) ?: 0) + 1
        planAssignmentDao.insert(
            PlanAssignmentEntity(
                routineVersionId = routineVersionId,
                exerciseId = exerciseId,
                sets = sets,
                reps = reps,
                sortOrder = nextSortOrder,
                slot = nextSlot,
            ),
        )
    }

    override suspend fun addAlternativeToSlot(
        routineVersionId: Long,
        slot: Int,
        exerciseId: Long,
    ) {
        // Inherit sets/reps from first exercise in this slot
        val existing = planAssignmentDao.getAlternativesForSlot(routineVersionId, slot)
        val sets = existing.firstOrNull()?.sets ?: 4
        val reps = existing.firstOrNull()?.reps ?: "8-12"
        val nextSortOrder = (planAssignmentDao.getMaxSortOrder(routineVersionId) ?: 0) + 1
        planAssignmentDao.insert(
            PlanAssignmentEntity(
                routineVersionId = routineVersionId,
                exerciseId = exerciseId,
                sets = sets,
                reps = reps,
                sortOrder = nextSortOrder,
                slot = slot,
            ),
        )
    }

    override suspend fun getAlternativesForSlot(routineVersionId: Long, slot: Int): List<PlanExercise> {
        return planAssignmentDao.getAlternativesForSlot(routineVersionId, slot).map { pa ->
            PlanExercise(
                exerciseId = pa.exerciseId,
                name = pa.exerciseName,
                equipmentTypeName = pa.equipmentTypeName,
                muscleZones = pa.muscleZones?.split(", ")?.filter { it.isNotBlank() } ?: emptyList(),
                sets = pa.sets,
                reps = pa.reps,
                isBodyweight = pa.isBodyweight == 1,
                isIsometric = pa.isIsometric == 1,
                isToTechnicalFailure = pa.isToTechnicalFailure == 1,
                isCustom = pa.isCustom == 1,
                slot = pa.slot,
            )
        }
    }

    override suspend fun unassignExercise(routineVersionId: Long, exerciseId: Long) {
        val slot = planAssignmentDao.getSlotForExercise(routineVersionId, exerciseId)
        if (slot != null) {
            planAssignmentDao.deleteBySlot(routineVersionId, slot)
        } else {
            planAssignmentDao.delete(routineVersionId, exerciseId)
        }
    }

    override suspend fun updatePlanAssignment(
        routineVersionId: Long,
        exerciseId: Long,
        sets: Int,
        reps: String,
    ) {
        // Propagate sets/reps change to ALL exercises in the same slot,
        // so all alternatives remain consistent with each other.
        val slot = planAssignmentDao.getSlotForExercise(routineVersionId, exerciseId)
        if (slot != null) {
            planAssignmentDao.updateSetsAndRepsBySlot(routineVersionId, slot, sets, reps)
        } else {
            planAssignmentDao.updateSetsAndReps(routineVersionId, exerciseId, sets, reps)
        }
    }

    private fun com.estebancoloradogonzalez.tension.data.local.dao.ExerciseWithDetails.toDomainModel() =
        Exercise(
            id = id,
            name = name,
            equipmentTypeName = equipmentTypeName,
            muscleZones = muscleZones?.split(", ")?.filter { it.isNotBlank() } ?: emptyList(),
            muscleGroup = muscleGroup,
            isBodyweight = isBodyweight == 1,
            isIsometric = isIsometric == 1,
            isToTechnicalFailure = isToTechnicalFailure == 1,
            isCustom = isCustom == 1,
            mediaResource = mediaResource,
        )
}
