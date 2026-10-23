package com.estebancoloradogonzalez.tension.data.repository

import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseOneRmDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSetDao
import com.estebancoloradogonzalez.tension.data.local.dao.OneRmCandidateData
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseOneRmEntity
import com.estebancoloradogonzalez.tension.domain.model.OneRmCandidate
import com.estebancoloradogonzalez.tension.domain.rules.OneRmRule
import com.estebancoloradogonzalez.tension.domain.rules.RealLoadRule

/**
 * Writes the estimated 1RM records, always by **rebuilding** them from the reference sets
 * (HU-49).
 *
 * Every write of `exercise_one_rm` goes through here. The record of a pair is the maximum over
 * its qualifying sets, measured on their real load and dated by the session that reached it
 * ([OneRmRule.record]); rebuilding it whole is what keeps the persisted value exact whatever
 * changed — a new set, a corrected one (HU-47), the exercise's body-weight percentage
 * (CA-49.08) or the body weight in force on a date (CA-49.06). The reconstruction is exact
 * because the table only exists on fresh installs (ADR-019), no purge exists and every
 * qualifying set is still in the history.
 *
 * It is a plain class built by the repositories that own the transaction, not a Hilt binding:
 * it has no state of its own, and each caller runs it **inside** the transaction that changes
 * what the record depends on, so that both are persisted or neither is.
 */
class OneRmRecordWriter(
    private val exerciseSetDao: ExerciseSetDao,
    private val exerciseOneRmDao: ExerciseOneRmDao,
) {

    /**
     * Rebuilds the record of one pair. It may appear, go up, go down or disappear: a pair
     * without a qualifying set has no real value, and CA-42.07 says it is not shown — the
     * table is the list, so the row goes.
     */
    suspend fun rebuildPair(exerciseId: Long, equipmentTypeId: Long) {
        val candidates = exerciseSetDao.getOneRmCandidatesForPair(
            exerciseId = exerciseId,
            equipmentTypeId = equipmentTypeId,
            reps = OneRmRule.REFERENCE_REPS,
            rir = OneRmRule.REFERENCE_RIR,
        ).map { it.toCandidate() }

        val record = OneRmRule.record(candidates)
        if (record != null) {
            exerciseOneRmDao.upsert(
                ExerciseOneRmEntity(
                    exerciseId = exerciseId,
                    equipmentTypeId = equipmentTypeId,
                    oneRmKg = record.oneRmKg,
                    achievedOn = record.achievedOn,
                ),
            )
        } else {
            exerciseOneRmDao.deleteByPair(exerciseId, equipmentTypeId)
        }
    }

    /** Rebuilds every pair of the exercise that has a reference set (CA-49.08). */
    suspend fun rebuildExercise(exerciseId: Long) {
        exerciseSetDao.getReferenceSetEquipmentIds(
            exerciseId = exerciseId,
            reps = OneRmRule.REFERENCE_REPS,
            rir = OneRmRule.REFERENCE_RIR,
        ).forEach { equipmentTypeId -> rebuildPair(exerciseId, equipmentTypeId) }
    }

    /**
     * Rebuilds the pairs with a reference set dated on or after [date] (`YYYY-MM-DD`). A new
     * body-weight entry changes the body weight in force from its date on and never before
     * (CA-49.06), so nothing older is touched.
     */
    suspend fun rebuildFrom(date: String) {
        exerciseSetDao.getReferenceSetPairsSince(
            date = date,
            reps = OneRmRule.REFERENCE_REPS,
            rir = OneRmRule.REFERENCE_RIR,
        ).forEach { pair -> rebuildPair(pair.exerciseId, pair.equipmentTypeId) }
    }

    private fun OneRmCandidateData.toCandidate() = OneRmCandidate(
        reps = OneRmRule.REFERENCE_REPS,
        rir = OneRmRule.REFERENCE_RIR,
        loadKg = RealLoadRule.realLoadKg(
            externalLoadKg = weightKg,
            bodyWeightKg = bodyWeightKg,
            bodyWeightPercent = bodyWeightPercent,
            isIsometric = isIsometric == 1,
            equipmentTypeName = equipmentTypeName,
        ),
        hasCountableLoad = OneRmRule.hasCountableLoad(
            isBodyweight = isBodyweight == 1,
            isIsometric = isIsometric == 1,
            equipmentTypeName = equipmentTypeName,
            bodyWeightPercent = bodyWeightPercent,
        ),
        date = sessionDate,
    )
}
