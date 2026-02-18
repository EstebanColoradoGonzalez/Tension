package com.estebancoloradogonzalez.tension.domain.model

data class TonnageSnapshot(
    val microcycleNumber: Int,
    val tonnageByGroup: Map<String, Double>,
)
