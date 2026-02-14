package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.entity.PlanAssignmentEntity
import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.Module
import com.estebancoloradogonzalez.tension.domain.model.ModuleWithVersions
import com.estebancoloradogonzalez.tension.domain.model.PlanExercise
import com.estebancoloradogonzalez.tension.domain.model.PlanVersionDetail
import com.estebancoloradogonzalez.tension.domain.model.VersionSummary
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlanRepositoryImpl @Inject constructor(
    private val planAssignmentDao: PlanAssignmentDao,
    private val moduleVersionDao: ModuleVersionDao,
    private val moduleDao: ModuleDao,
    private val exerciseDao: ExerciseDao,
) : PlanRepository {

    override fun getModulesWithVersionCounts(): Flow<List<ModuleWithVersions>> =
        combine(
            moduleDao.getAll(),
            moduleVersionDao.getAllWithExerciseCount(),
        ) { modules, versions ->
            val versionsByModule = versions.groupBy { it.moduleCode }
            modules.map { entity ->
                ModuleWithVersions(
                    module = Module(
                        code = entity.code,
                        name = entity.name,
                        groupDescription = entity.groupDescription,
                        loadIncrementKg = entity.loadIncrementKg,
                    ),
                    versions = versionsByModule[entity.code]?.map { mv ->
                        VersionSummary(
                            moduleVersionId = mv.id,
                            versionNumber = mv.versionNumber,
                            exerciseCount = mv.exerciseCount,
                        )
                    } ?: emptyList(),
                )
            }
        }

    override fun getVersionDetail(moduleVersionId: Long): Flow<PlanVersionDetail?> =
        combine(
            moduleVersionDao.getById(moduleVersionId),
            planAssignmentDao.getDetailsByModuleVersionId(moduleVersionId),
            moduleDao.getAll(),
        ) { moduleVersion, assignments, modules ->
            moduleVersion?.let { mv ->
                val module = modules.find { it.code == mv.moduleCode }
                PlanVersionDetail(
                    moduleVersionId = mv.id,
                    moduleCode = mv.moduleCode,
                    moduleName = module?.name ?: mv.moduleCode,
                    versionNumber = mv.versionNumber,
                    exercises = assignments.map { pa ->
                        PlanExercise(
                            exerciseId = pa.exerciseId,
                            name = pa.exerciseName,
                            equipmentTypeName = pa.equipmentTypeName,
                            muscleZones = pa.muscleZones?.split(", ")?.filter { it.isNotBlank() }
                                ?: emptyList(),
                            sets = pa.sets,
                            reps = pa.reps,
                            isBodyweight = pa.isBodyweight == 1,
                            isIsometric = pa.isIsometric == 1,
                            isToTechnicalFailure = pa.isToTechnicalFailure == 1,
                            isCustom = pa.isCustom == 1,
                        )
                    },
                )
            }
        }

    override fun getAvailableExercisesForVersion(
        moduleCode: String,
        moduleVersionId: Long,
    ): Flow<List<Exercise>> =
        exerciseDao.getByModuleCodeNotInVersion(moduleCode, moduleVersionId).map { list ->
            list.map { it.toDomainModel() }
        }

    override suspend fun assignExercise(
        moduleVersionId: Long,
        exerciseId: Long,
        sets: Int,
        reps: String,
    ) {
        planAssignmentDao.insert(
            PlanAssignmentEntity(
                moduleVersionId = moduleVersionId,
                exerciseId = exerciseId,
                sets = sets,
                reps = reps,
            ),
        )
    }

    override suspend fun unassignExercise(moduleVersionId: Long, exerciseId: Long) {
        planAssignmentDao.delete(moduleVersionId, exerciseId)
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
