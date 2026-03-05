package com.estebancoloradogonzalez.tension.domain.util

object RepsDisplayMapper {
    fun mapRepsToDisplay(reps: String): Pair<String, Boolean> = when {
        reps == "TO_TECHNICAL_FAILURE" -> "Al fallo técnico" to true
        reps.endsWith("_SEC") -> {
            val cleaned = reps.removeSuffix("_SEC").replace("-", "\u2013")
            "$cleaned seg" to true
        }
        else -> "$reps reps" to false
    }
}
