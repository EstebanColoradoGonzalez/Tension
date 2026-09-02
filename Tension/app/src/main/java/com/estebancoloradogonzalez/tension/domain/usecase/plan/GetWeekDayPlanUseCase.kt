package com.estebancoloradogonzalez.tension.domain.usecase.plan

import com.estebancoloradogonzalez.tension.domain.model.WeekDayRoutine
import com.estebancoloradogonzalez.tension.domain.repository.WeekDayRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWeekDayPlanUseCase @Inject constructor(
    private val weekDayRepository: WeekDayRepository,
) {
    operator fun invoke(): Flow<List<WeekDayRoutine>> = weekDayRepository.getWeekDayPlan()
}
