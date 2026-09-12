package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.OneRmEntry
import kotlinx.coroutines.flow.Flow

/**
 * Estimated 1RM per (exercise, equipment) pair.
 *
 * **The dependency is strictly one-way:** the 1RM is derived from the set history and nothing
 * in the system reads it back. No component of the decision engine — load prescription,
 * Double Threshold, plateau, regression, fatigue, deload protocol, cyclic rotation —
 * consults this contract, the 1RM raises no alerts and alters no KPI.
 *
 * It is a **read-only** contract on purpose. The record is written from the transaction that
 * registers the set, because it cannot be reconstructed if that write is lost; the reading is
 * what has to stay isolated, and keeping this interface without a single write method is what
 * makes that legible in the contract itself rather than only in the documentation.
 */
interface OneRmRepository {

    /**
     * Every pair that holds a record, ordered alphabetically by exercise and then by the
     * declared position of the implement.
     *
     * A pair without a qualifying set simply is not here: there is no row to hide, no zero
     * and no dash (CA-42.07).
     */
    fun getAll(): Flow<List<OneRmEntry>>
}
