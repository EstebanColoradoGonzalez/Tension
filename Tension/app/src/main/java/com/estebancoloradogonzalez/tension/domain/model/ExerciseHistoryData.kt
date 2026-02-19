package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseHistoryData(
    val exerciseName: String,
    val progressionStatus: String,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val entries: List<ExerciseHistoryEntry>,
)
