package com.estebancoloradogonzalez.tension.domain.model

data class Deload(
    val id: Long,
    val status: String,
    val activationDate: String,
    val completionDate: String?,
    val frozenVersions: Map<Long, Int>,
)
