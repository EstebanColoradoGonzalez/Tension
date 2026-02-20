package com.estebancoloradogonzalez.tension.domain.usecase.backup

import com.estebancoloradogonzalez.tension.domain.model.BackupValidationResult
import com.estebancoloradogonzalez.tension.domain.repository.BackupRepository
import javax.inject.Inject

class ValidateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    operator fun invoke(json: String): BackupValidationResult =
        backupRepository.validateBackup(json)
}
