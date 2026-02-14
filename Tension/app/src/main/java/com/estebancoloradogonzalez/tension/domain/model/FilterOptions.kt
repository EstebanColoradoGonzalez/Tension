package com.estebancoloradogonzalez.tension.domain.model

data class FilterOptions(
    val modules: List<Module>,
    val equipmentTypes: List<EquipmentType>,
    val muscleZones: List<MuscleZone>,
)
