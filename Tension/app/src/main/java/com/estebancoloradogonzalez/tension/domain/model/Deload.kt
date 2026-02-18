package com.estebancoloradogonzalez.tension.domain.model

data class Deload(
    val id: Long,
    val status: String,
    val activationDate: String,
    val completionDate: String?,
    val frozenVersionModuleA: Int,
    val frozenVersionModuleB: Int,
    val frozenVersionModuleC: Int,
)
