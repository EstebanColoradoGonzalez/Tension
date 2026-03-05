package com.estebancoloradogonzalez.tension.domain.util

import com.estebancoloradogonzalez.tension.domain.rules.DeloadLoadRule

object LoadDisplayMapper {
    fun mapLoadDisplay(
        isDeload: Boolean,
        isIsometric: Boolean,
        isBodyweight: Boolean,
        prescribedLoadKg: Double?,
        loadIncrementKg: Double,
    ): String = when {
        isDeload && isIsometric -> "Isométrico (30s)"
        isDeload && isBodyweight -> "Peso corporal (8 reps objetivo)"
        isDeload && prescribedLoadKg != null -> {
            val deloadLoad = DeloadLoadRule.calculateDeloadLoad(
                prescribedLoadKg,
                loadIncrementKg,
            )
            "\uD83D\uDD04 %.1f Kg".format(deloadLoad)
        }
        isIsometric -> "Isométrico (30\u201345s)"
        isBodyweight -> "Peso corporal"
        prescribedLoadKg != null -> "%.1f Kg".format(prescribedLoadKg)
        else -> "Sin historial \u2014 establecer carga"
    }
}
