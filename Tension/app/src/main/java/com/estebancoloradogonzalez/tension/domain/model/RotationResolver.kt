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

    fun advanceRotation(current: RotationState, isDeload: Boolean = false): RotationState {
        return if (current.microcyclePosition < 6) {
            current.copy(microcyclePosition = current.microcyclePosition + 1)
        } else {
            if (isDeload) {
                current.copy(
                    microcyclePosition = 1,
                    microcycleCount = current.microcycleCount + 1,
                )
            } else {
                current.copy(
                    microcyclePosition = 1,
                    currentVersionModuleA = (current.currentVersionModuleA % 3) + 1,
                    currentVersionModuleB = (current.currentVersionModuleB % 3) + 1,
                    currentVersionModuleC = (current.currentVersionModuleC % 3) + 1,
                    microcycleCount = current.microcycleCount + 1,
                )
            }
        }
    }
}
