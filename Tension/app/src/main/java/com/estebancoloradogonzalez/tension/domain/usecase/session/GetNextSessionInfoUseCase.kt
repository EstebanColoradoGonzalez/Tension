package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.RotationResolver
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetNextSessionInfoUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<NextSession?> {
        return sessionRepository.getRotationState().flatMapLatest { rotationState ->
            if (rotationState == null) {
                flowOf(null)
            } else {
                val moduleCode = RotationResolver.resolveModuleCode(
                    rotationState.microcyclePosition,
                )
                combine(
                    sessionRepository.getNextModuleVersionId(),
                    sessionRepository.getActiveDeload(),
                ) { moduleVersionId, deload ->
                    val versionNumber = if (deload != null) {
                        RotationResolver.resolveVersionNumber(
                            moduleCode,
                            deload.frozenVersionModuleA,
                            deload.frozenVersionModuleB,
                            deload.frozenVersionModuleC,
                        )
                    } else {
                        RotationResolver.resolveVersionNumber(
                            moduleCode,
                            rotationState.currentVersionModuleA,
                            rotationState.currentVersionModuleB,
                            rotationState.currentVersionModuleC,
                        )
                    }
                    NextSession(
                        moduleCode = moduleCode,
                        versionNumber = versionNumber,
                        moduleVersionId = moduleVersionId,
                    )
                }
            }
        }
    }
}
