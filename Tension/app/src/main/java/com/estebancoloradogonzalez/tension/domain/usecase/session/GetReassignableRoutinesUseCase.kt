package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.ReassignableRoutine
import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReassignableRoutinesUseCase @Inject constructor(
    private val weekDayRepository: WeekDayRepository,
) {

    operator fun invoke(): Flow<List<ReassignableRoutine>> {
        return weekDayRepository.getReassignableRoutines()
    }
}
