package com.estebancoloradogonzalez.tension.domain.model

object RotationResolver {

    fun resolveRoutineIndex(position: Int, routineCount: Int): Int {
        require(routineCount > 0) { "Must have at least 1 routine" }
        require(position > 0) { "Position must be >= 1" }
        return (position - 1) % routineCount
    }

    fun microcycleSize(routineCount: Int): Int = routineCount

    fun advanceRotation(current: RotationState, routineCount: Int): RotationState {
        require(routineCount > 0) { "Must have at least 1 routine" }
        val cycleSize = microcycleSize(routineCount)
        return if (current.microcyclePosition < cycleSize) {
            current.copy(microcyclePosition = current.microcyclePosition + 1)
        } else {
            current.copy(
                microcyclePosition = 1,
                microcycleCount = current.microcycleCount + 1,
            )
        }
    }
}
