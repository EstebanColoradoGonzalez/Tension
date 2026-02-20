package com.estebancoloradogonzalez.tension.domain.model

data class BackupValidationResult(
    val isValid: Boolean,
    val metadata: BackupMetadata?,
    val sessionCount: Int,
    val errorMessage: String?,
)
