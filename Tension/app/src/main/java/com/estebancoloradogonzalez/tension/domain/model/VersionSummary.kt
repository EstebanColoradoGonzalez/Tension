package com.estebancoloradogonzalez.tension.domain.model

data class VersionSummary(
    val moduleVersionId: Long,
    val versionNumber: Int,
    val exerciseCount: Int,
)
