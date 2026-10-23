package com.estebancoloradogonzalez.tension.domain.usecase.onerm

import com.estebancoloradogonzalez.tension.domain.model.ExerciseOneRm
import com.estebancoloradogonzalez.tension.domain.model.OneRmEquipment
import com.estebancoloradogonzalez.tension.domain.repository.OneRmRepository
import com.estebancoloradogonzalez.tension.domain.rules.OneRmRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The 1RM list as the screen composes it: one card per exercise, with the implements that
 * hold a record inside it.
 *
 * The grouping lives here and not in the repository so that it can be asserted on the JVM
 * with a mocked repository, which is how every other use case of the project is covered. The
 * order is **not** recomputed: the rows arrive sorted from SQL — alphabetically by exercise
 * (CA-42.02) and then by the declared position of the implement — and grouping preserves it.
 *
 * The relative strength is computed here, from the record and the body weight in force on the
 * date it was reached (HU-49, CA-49.03), so that the screen formats it and never divides.
 *
 * An empty list in is an empty list out. Whether that means «nothing has ever qualified» is
 * the screen's business, and it resolves it with the explanatory empty state (CA-42.07).
 */
class GetOneRmListUseCase @Inject constructor(
    private val oneRmRepository: OneRmRepository,
) {
    operator fun invoke(): Flow<List<ExerciseOneRm>> =
        oneRmRepository.getAll().map { entries ->
            entries
                .groupBy { it.exerciseId }
                .map { (exerciseId, rows) ->
                    ExerciseOneRm(
                        exerciseId = exerciseId,
                        exerciseName = rows.first().exerciseName,
                        equipment = rows.map { row ->
                            OneRmEquipment(
                                equipmentTypeId = row.equipmentTypeId,
                                equipmentTypeName = row.equipmentTypeName,
                                oneRmKg = row.oneRmKg,
                                achievedOn = row.achievedOn,
                                bodyWeightKg = row.bodyWeightKg,
                                relativeStrength = OneRmRule.relativeStrength(
                                    row.oneRmKg,
                                    row.bodyWeightKg,
                                ),
                            )
                        },
                    )
                }
        }
}
