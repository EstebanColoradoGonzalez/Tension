package com.estebancoloradogonzalez.tension.domain.model

data class NextSession(
    val moduleCode: String,
    val versionNumber: Int,
    val moduleVersionId: Long,
)
