package com.estebancoloradogonzalez.tension.domain.usecase.metrics

import com.estebancoloradogonzalez.tension.domain.model.BodyWeightSummary
import com.estebancoloradogonzalez.tension.domain.model.SessionTonnage
import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import com.estebancoloradogonzalez.tension.domain.rules.BodyWeightRule
import com.estebancoloradogonzalez.tension.domain.util.CurrentDateProvider
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * The «Body weight» section of the Metrics panel (HU-48).
 *
 * The tonnage is the **session tonnage of the system**, read from the same source as the
 * session history and the post-session summary. This use case does not compute it, so when
 * that calculation changes (HU-49, real load) the chart follows without a change here.
 *
 * Read-only: nothing in the decision engine reads what this returns (Rule 9).
 */
class GetBodyWeightSummaryUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val currentDateProvider: CurrentDateProvider,
) {

    /** Success with `null` only while there is no profile or no weight entry yet. */
    suspend operator fun invoke(weeksBack: Int): Result<BodyWeightSummary?> = try {
        Result.success(summarize(weeksBack))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun summarize(weeksBack: Int): BodyWeightSummary? {
        val profile = profileRepository.getProfile().first() ?: return null
        val records = profileRepository.getAllWeightRecords().first()
        val current = BodyWeightRule.latest(records) ?: return null

        val today = currentDateProvider.today()
        val periodStart = BodyWeightRule.periodStart(today, weeksBack)
        val sessions = sessionRepository.getSessionHistory().map {
            SessionTonnage(date = LocalDate.parse(it.date), tonnageKg = it.totalTonnageKg)
        }
        val daysSince = BodyWeightRule.daysSince(current.date, today)

        return BodyWeightSummary(
            currentWeightKg = current.weightKg,
            recordedOn = current.date,
            daysSinceLastRecord = daysSince,
            isStale = BodyWeightRule.isStale(daysSince),
            variationKg = BodyWeightRule.variation(records, periodStart),
            bmi = BodyWeightRule.bmi(current.weightKg, profile.heightM),
            periodStart = periodStart,
            periodEnd = today,
            weightPoints = BodyWeightRule.weightPoints(records, periodStart, today),
            weeklyTonnage = BodyWeightRule.weeklyTonnage(sessions, periodStart, today),
        )
    }
}
