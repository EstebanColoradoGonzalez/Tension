package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import javax.inject.Inject

/** Deshace la reasignación temporal y devuelve el día a su relación permanente. */
class ClearTemporaryRoutineUseCase @Inject constructor(
    private val weekDayRepository: WeekDayRepository,
) {
    suspend operator fun invoke() {
        weekDayRepository.clearTodayOverride()
    }
}
