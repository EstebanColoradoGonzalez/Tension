package com.estebancoloradogonzalez.tension.domain.util

data class RepsRange(val min: Int, val max: Int, val isSeconds: Boolean)

object RepsRangeParser {
    fun parse(reps: String): RepsRange {
        val isSeconds = reps.endsWith("_SEC")
        val cleaned = reps.removeSuffix("_SEC")
        val parts = cleaned.split("-")
        return RepsRange(
            min = parts[0].toIntOrNull() ?: 30,
            max = parts.getOrNull(1)?.toIntOrNull() ?: 60,
            isSeconds = isSeconds,
        )
    }
}
