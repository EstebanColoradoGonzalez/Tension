package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.SessionDao
import com.estebancoloradogonzalez.tension.data.local.dao.TreeStateDao
import com.estebancoloradogonzalez.tension.data.local.entity.TreeStateEntity
import com.estebancoloradogonzalez.tension.domain.model.TreeGrowthStage
import com.estebancoloradogonzalez.tension.domain.model.TreeState
import com.estebancoloradogonzalez.tension.domain.repository.TreeRepository
import com.estebancoloradogonzalez.tension.domain.rules.TreeGrowthStageRule
import com.estebancoloradogonzalez.tension.domain.rules.TreeHealthRule
import com.estebancoloradogonzalez.tension.domain.util.CurrentDateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deriva el árbol del historial de sesiones y lo persiste.
 *
 * Lee `SessionDao` directamente, como ya hacen `MetricsRepositoryImpl` y `AlertRepositoryImpl`.
 * La dependencia es de lectura y unidireccional: nada del sistema consulta `tree_state`.
 */
@Singleton
class TreeRepositoryImpl @Inject constructor(
    private val treeStateDao: TreeStateDao,
    private val sessionDao: SessionDao,
    private val currentDateProvider: CurrentDateProvider,
) : TreeRepository {

    /**
     * El estado del árbol, con los días transcurridos resueltos **al leer**.
     *
     * Persistir los días los volvería rancios en cuanto cambiara la fecha; derivarlos aquí
     * hace que la pantalla no pueda mostrar un conteo desactualizado ni siquiera si el
     * recálculo hubiera fallado.
     *
     * Sin fila persistida devuelve el estado de partida en lugar de fallar: es lo que ve un
     * ejecutante recién registrado, antes del primer recálculo.
     */
    override fun getTreeState(): Flow<TreeState> =
        treeStateDao.getTreeState().map { entity ->
            if (entity == null) {
                INITIAL_STATE
            } else {
                TreeState(
                    stage = TreeGrowthStage.fromCode(entity.growthStage),
                    healthScore = entity.healthScore,
                    daysSinceLastSession = daysSince(entity.lastSessionDate),
                )
            }
        }

    override suspend fun recalculate() {
        val today = currentDateProvider.today()
        val sessionCount = sessionDao.countClosedSessions()
        val lastSessionDate = sessionDao.getLastClosedSessionDate()

        val stage = TreeGrowthStageRule.resolve(sessionCount)
        val healthScore = TreeHealthRule.calculate(daysSince(lastSessionDate, today))

        treeStateDao.upsert(
            TreeStateEntity(
                healthScore = healthScore,
                growthStage = stage.code,
                lastSessionDate = lastSessionDate,
                calculatedAt = today.toString(),
            ),
        )
    }

    /**
     * Días naturales entre [isoDate] y [today]. Nulo sin fecha, y también con una fecha
     * ilegible: un dato corrupto deja el árbol en su estado de partida, no revienta la lectura.
     */
    private fun daysSince(isoDate: String?, today: LocalDate = currentDateProvider.today()): Int? {
        if (isoDate == null) return null
        return runCatching {
            ChronoUnit.DAYS.between(LocalDate.parse(isoDate), today).toInt()
        }.getOrNull()
    }

    private companion object {
        val INITIAL_STATE = TreeState(
            stage = TreeGrowthStage.SEED,
            healthScore = TreeHealthRule.calculate(null),
            daysSinceLastSession = null,
        )
    }
}
