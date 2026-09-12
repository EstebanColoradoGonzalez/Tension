package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseOneRmDao
import com.estebancoloradogonzalez.tension.domain.model.OneRmEntry
import com.estebancoloradogonzalez.tension.domain.repository.OneRmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the 1RM records and maps them to the domain. Nothing else.
 *
 * It does **not** order — SQL does, and the order is part of the query because alphabetical
 * by exercise and catalog order by implement are both properties of the data. It does **not**
 * group either: the cards are shaped by `GetOneRmListUseCase`, where the grouping can be
 * asserted on the JVM with a mocked repository, which is the strategy the project already
 * uses for everything else.
 *
 * It lives apart from `SessionRepositoryImpl`, which is what writes the record, so that the
 * direction of the dependency is legible in the contracts: the 1RM is derived from the
 * history and nothing in the system reads it back.
 */
@Singleton
class OneRmRepositoryImpl @Inject constructor(
    private val exerciseOneRmDao: ExerciseOneRmDao,
) : OneRmRepository {

    override fun getAll(): Flow<List<OneRmEntry>> =
        exerciseOneRmDao.getAll().map { rows ->
            rows.map { row ->
                OneRmEntry(
                    exerciseId = row.exerciseId,
                    exerciseName = row.exerciseName,
                    equipmentTypeId = row.equipmentTypeId,
                    equipmentTypeName = row.equipmentTypeName,
                    oneRmKg = row.oneRmKg,
                )
            }
        }
}
