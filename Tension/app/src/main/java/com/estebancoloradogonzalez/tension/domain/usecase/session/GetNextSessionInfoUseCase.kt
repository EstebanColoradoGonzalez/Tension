package com.estebancoloradogonzalez.tension.domain.usecase.session

import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.RotationResolver
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
                val versionNumber = RotationResolver.resolveVersionNumber(
                    moduleCode,
                    rotationState.currentVersionModuleA,
                    rotationState.currentVersionModuleB,
                    rotationState.currentVersionModuleC,
                )
                sessionRepository.getNextModuleVersionId().map { moduleVersionId ->
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
