package com.estebancoloradogonzalez.tension.domain.model

object RotationResolver {

    fun resolveModuleCode(position: Int): String = when (position) {
        1, 4 -> "A"
        2, 5 -> "B"
        3, 6 -> "C"
        else -> error("Invalid microcycle position: $position")
    }

    fun resolveVersionNumber(
        moduleCode: String,
        versionA: Int,
        versionB: Int,
        versionC: Int,
    ): Int = when (moduleCode) {
        "A" -> versionA
        "B" -> versionB
        "C" -> versionC
        else -> error("Invalid module code: $moduleCode")
    }
}
