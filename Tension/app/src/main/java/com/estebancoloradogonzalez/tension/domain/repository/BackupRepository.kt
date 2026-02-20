package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.BackupValidationResult

interface BackupRepository {
    suspend fun exportToJson(): String
    fun validateBackup(json: String): BackupValidationResult
    suspend fun importFromJson(json: String)
}
