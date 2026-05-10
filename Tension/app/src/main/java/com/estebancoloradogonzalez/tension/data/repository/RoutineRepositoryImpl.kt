package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.RoutineCurrentVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.RotationStateDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionDao
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.entity.RoutineCurrentVersionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.RoutineEntity
import com.estebancoloradogonzalez.tension.data.local.entity.RoutineVersionEntity
import com.estebancoloradogonzalez.tension.domain.model.Routine
import com.estebancoloradogonzalez.tension.domain.model.RoutineVersion
import com.estebancoloradogonzalez.tension.domain.repository.RoutineRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
    private val routineVersionDao: RoutineVersionDao,
    private val routineCurrentVersionDao: RoutineCurrentVersionDao,
    private val rotationStateDao: RotationStateDao,
    private val sessionDao: SessionDao,
    private val database: TensionDatabase,
) : RoutineRepository {

    override fun getRoutines(): Flow<List<Routine>> =
        routineDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createRoutine(name: String) {
        database.withTransaction {
            val maxSort = routineDao.getMaxSortOrder() ?: 0
            val routineId = routineDao.insert(
                RoutineEntity(
                    name = name.trim(),
                    sortOrder = maxSort + 1,
                    createdAt = LocalDate.now().toString(),
                ),
            )
            routineVersionDao.insert(
                RoutineVersionEntity(routineId = routineId, versionNumber = 1),
            )
            routineCurrentVersionDao.insert(
                RoutineCurrentVersionEntity(routineId = routineId, currentVersionNumber = 1),
            )
        }
    }

    override suspend fun updateRoutineName(id: Long, name: String) {
        val routine = routineDao.getById(id) ?: return
        routineDao.update(routine.copy(name = name.trim()))
    }

    override suspend fun deleteRoutine(id: Long) {
        database.withTransaction {
            val deletedRoutine = routineDao.getById(id)
            val deletedSortOrder = deletedRoutine?.sortOrder ?: Int.MAX_VALUE

            routineCurrentVersionDao.deleteByRoutineId(id)
            routineDao.delete(id)

            val remaining = routineDao.getAllOnce().sortedBy { it.sortOrder }
            remaining.forEachIndexed { index, routine ->
                routineDao.updateSortOrder(routine.id, index + 1)
            }

            val rotationState = rotationStateDao.getRotationStateOnce()
            if (rotationState != null && remaining.isNotEmpty()) {
                val newCount = remaining.size
                var newPosition = rotationState.microcyclePosition
                if (deletedSortOrder < newPosition) {
                    newPosition--
                }
                if (newPosition > newCount) {
                    newPosition = newCount
                }
                if (newPosition < 1) {
                    newPosition = 1
                }
                if (newPosition != rotationState.microcyclePosition) {
                    rotationStateDao.update(
                        rotationState.copy(microcyclePosition = newPosition),
                    )
                }
            }
        }
    }

    override suspend fun reorderRoutines(orderedIds: List<Long>) {
        database.withTransaction {
            orderedIds.forEachIndexed { index, routineId ->
                routineDao.updateSortOrder(routineId, index + 1)
            }

        }
    }

    override fun getVersionsByRoutine(routineId: Long): Flow<List<RoutineVersion>> =
        routineVersionDao.getByRoutineIdWithExerciseCount(routineId).map { entities ->
            entities.map { entity ->
                RoutineVersion(
                    id = entity.id,
                    routineId = entity.routineId,
                    versionNumber = entity.versionNumber,
                    exerciseCount = entity.exerciseCount,
                )
            }
        }

    override suspend fun createVersion(routineId: Long) {
        val maxVersion = routineVersionDao.getMaxVersionNumber(routineId) ?: 0
        routineVersionDao.insert(
            RoutineVersionEntity(
                routineId = routineId,
                versionNumber = maxVersion + 1,
            ),
        )
    }

    override suspend fun deleteVersion(versionId: Long, routineId: Long) {
        database.withTransaction {
            routineVersionDao.delete(versionId)
            val currentVersion = routineCurrentVersionDao.getByRoutineId(routineId)
            if (currentVersion != null) {
                val stillExists = routineVersionDao.getByRoutineIdAndVersion(
                    routineId,
                    currentVersion.currentVersionNumber,
                )
                if (stillExists == null) {
                    val minVersion = routineVersionDao.getMinVersionNumber(routineId) ?: 1
                    routineCurrentVersionDao.update(
                        currentVersion.copy(currentVersionNumber = minVersion),
                    )
                }
            }
        }
    }

    override suspend fun countRoutines(): Int = routineDao.countRoutines()

    override suspend fun hasActiveSessionForRoutine(routineId: Long): Boolean =
        sessionDao.hasActiveSessionForRoutine(routineId)

    override suspend fun hasSessionsForRoutine(routineId: Long): Boolean =
        sessionDao.hasSessionsForRoutine(routineId)

    override suspend fun hasSessionsForVersion(routineVersionId: Long): Boolean =
        sessionDao.hasSessionsForVersion(routineVersionId)

    override suspend fun countVersionsByRoutine(routineId: Long): Int =
        routineVersionDao.countByRoutineId(routineId)

    override suspend fun routineNameExists(name: String): Boolean =
        routineDao.existsByName(name.trim())

    override suspend fun routineNameExistsExcluding(name: String, excludeId: Long): Boolean =
        routineDao.existsByNameExcluding(name.trim(), excludeId)

    private fun RoutineEntity.toDomain() = Routine(
        id = id,
        name = name,
        sortOrder = sortOrder,
        createdAt = createdAt,
    )
}
