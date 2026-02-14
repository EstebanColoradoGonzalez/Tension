package com.estebancoloradogonzalez.tension.domain.model

data class RotationState(
    val microcyclePosition: Int,
    val currentVersionModuleA: Int,
    val currentVersionModuleB: Int,
    val currentVersionModuleC: Int,
    val microcycleCount: Int,
)
