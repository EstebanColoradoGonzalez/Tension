package com.estebancoloradogonzalez.tension.domain.usecase.backup

import com.estebancoloradogonzalez.tension.domain.repository.BackupRepository
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(json: String) = backupRepository.importFromJson(json)
}
