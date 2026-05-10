package com.estebancoloradogonzalez.tension.domain.rules

object LoadIncrementResolver {

    private const val UPPER_BODY_INCREMENT = 2.5
    private const val LOWER_BODY_INCREMENT = 5.0

    private val UPPER_BODY_GROUPS = setOf(
        "Pecho", "Espalda", "Hombro", "Bíceps", "Tríceps", "Abdomen",
    )

    private val LOWER_BODY_GROUPS = setOf(
        "Cuádriceps", "Isquiotibiales", "Glúteos", "Aductores", "Abductores", "Gemelos",
    )

    fun resolve(muscleGroup: String?): Double {
        return when {
            muscleGroup == null -> UPPER_BODY_INCREMENT
            muscleGroup in UPPER_BODY_GROUPS -> UPPER_BODY_INCREMENT
            muscleGroup in LOWER_BODY_GROUPS -> LOWER_BODY_INCREMENT
            else -> UPPER_BODY_INCREMENT
        }
    }
}
