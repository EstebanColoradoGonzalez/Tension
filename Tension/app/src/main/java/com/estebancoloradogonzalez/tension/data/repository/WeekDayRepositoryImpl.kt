package com.estebancoloradogonzalez.tension.data.repository

import androidx.room.withTransaction
import com.estebancoloradogonzalez.tension.data.local.dao.DailyRoutineOverrideDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineCurrentVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineDao
import com.estebancoloradogonzalez.tension.data.local.dao.RoutineVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.WeekDayDao
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.entity.DailyRoutineOverrideEntity
import com.estebancoloradogonzalez.tension.domain.model.DailyRoutineOverride
import com.estebancoloradogonzalez.tension.domain.model.ReassignableRoutine
import com.estebancoloradogonzalez.tension.domain.model.WeekDay
import com.estebancoloradogonzalez.tension.domain.model.WeekDayRoutine
import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import com.estebancoloradogonzalez.tension.domain.rules.WeekDayAssignmentRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class WeekDayRepositoryImpl @Inject constructor(
    private val weekDayDao: WeekDayDao,
    private val dailyRoutineOverrideDao: DailyRoutineOverrideDao,
    private val routineDao: RoutineDao,
    private val routineVersionDao: RoutineVersionDao,
    private val routineCurrentVersionDao: RoutineCurrentVersionDao,
    private val planAssignmentDao: PlanAssignmentDao,
    private val database: TensionDatabase,
) : WeekDayRepository {

    override fun getWeekDayPlan(): Flow<List<WeekDayRoutine>> {
        return combine(
            weekDayDao.getAll(),
            routineDao.getAll(),
        ) { weekDays, routines ->
            val routineNames = routines.associate { it.id to it.name }
            weekDays
                .mapNotNull { entity ->
                    val weekDay = WeekDay.fromCode(entity.code) ?: return@mapNotNull null
                    WeekDayRoutine(
                        weekDay = weekDay,
                        routineId = entity.routineId,
                        routineName = entity.routineId?.let { routineNames[it] },
                    )
                }
                .sortedBy { it.weekDay.isoNumber }
        }
    }

    /**
     * Se ofrece toda rutina con versión vigente no vacía, no solo las seis que tienen día.
     * El wireframe dibuja los días, y este listado los contiene: las rutinas del plan
     * predeterminado muestran su etiqueta de día. Lo que añade es que una rutina creada por
     * el ejecutante —que ningún día reclama— siga siendo ejecutable.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getReassignableRoutines(): Flow<List<ReassignableRoutine>> {
        return combine(
            routineDao.getAll(),
            routineCurrentVersionDao.getAll(),
            weekDayDao.getAll(),
        ) { routines, currentVersions, weekDays ->
            Triple(routines, currentVersions, weekDays)
        }.flatMapLatest { (routines, currentVersions, weekDays) ->
            val daysByRoutineId = weekDays
                .mapNotNull { entity ->
                    val routineId = entity.routineId ?: return@mapNotNull null
                    val weekDay = WeekDay.fromCode(entity.code) ?: return@mapNotNull null
                    routineId to weekDay
                }
                .groupBy({ it.first }, { it.second })
            // La fecha se lee al resolver, no al construir el flujo: el dia de hoy cambia
            // mientras la pantalla sigue abierta.
            val todayIso = LocalDate.now().dayOfWeek.value
            val todaysRoutineId = weekDays.firstOrNull { it.id == todayIso }?.routineId

            val options = routines.sortedBy { it.sortOrder }.mapNotNull { routine ->
                val versionNumber = currentVersions
                    .firstOrNull { it.routineId == routine.id }
                    ?.currentVersionNumber
                    ?: return@mapNotNull null
                val version = routineVersionDao.getByRoutineIdAndVersion(routine.id, versionNumber)
                    ?: return@mapNotNull null
                if (planAssignmentDao.countExercisesForRoutineVersion(version.id) == 0) {
                    return@mapNotNull null
                }
                ReassignableRoutine(
                    routineId = routine.id,
                    routineName = routine.name,
                    routineVersionId = version.id,
                    versionNumber = versionNumber,
                    weekDays = daysByRoutineId[routine.id].orEmpty(),
                    isTodaysRoutine = routine.id == todaysRoutineId,
                )
            }
            flowOf(options)
        }
    }

    /**
     * El día es el dueño de la relación: cada fila de `week_day` apunta a una rutina o a
     * ninguna. Por eso asignar un día que pertenecía a otra rutina lo **mueve** en lugar de
     * duplicarlo, y una rutina puede ocupar varios días sin que el modelo cambie de forma.
     */
    override suspend fun setRoutineWeekDays(routineId: Long, weekDays: Set<WeekDay>) {
        database.withTransaction {
            weekDayDao.getAllOnce().forEach { entity ->
                val weekDay = WeekDay.fromCode(entity.code) ?: return@forEach
                val newRoutineId = WeekDayAssignmentRule.resolveRoutineFor(
                    weekDay = weekDay,
                    currentRoutineId = entity.routineId,
                    editedRoutineId = routineId,
                    selectedWeekDays = weekDays,
                )
                if (newRoutineId != entity.routineId) {
                    weekDayDao.update(entity.copy(routineId = newRoutineId))
                }
            }
        }
    }

    override fun getTodayOverride(): Flow<DailyRoutineOverride?> {
        return dailyRoutineOverrideDao.getOverride().map { entity ->
            entity?.let { DailyRoutineOverride(date = it.date, routineId = it.routineId) }
        }
    }

    override suspend fun setTodayOverride(routineId: Long) {
        dailyRoutineOverrideDao.upsert(
            DailyRoutineOverrideEntity(
                date = LocalDate.now().toString(),
                routineId = routineId,
            ),
        )
    }

    override suspend fun clearTodayOverride() {
        dailyRoutineOverrideDao.clear()
    }
}
