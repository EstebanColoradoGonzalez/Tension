package com.estebancoloradogonzalez.tension.domain.model

data class BackupMetadata(
    val appVersion: String,
    val schemaVersion: Int,
    val exportDate: String,
    val recordCount: Int,
)
