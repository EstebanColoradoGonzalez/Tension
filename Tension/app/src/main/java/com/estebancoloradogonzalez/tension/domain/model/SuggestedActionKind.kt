package com.estebancoloradogonzalez.tension.domain.model

/**
 * Concrete next step an alert proposes. Every alert carries one: an alert that only
 * describes a problem leaves the executant to interpret the signal and decide the
 * action, which is the work the system exists to do.
 */
enum class SuggestedActionKind {
    INCREASE_LOAD_SLIGHTLY,
    EXTEND_REPS_BEFORE_LOAD,
    SWITCH_TO_SLOT_ALTERNATIVE,
    ROTATE_ROUTINE_VERSION,
    START_DELOAD,
    REDUCE_VOLUME,
    LEAVE_REPS_IN_RESERVE,
    INCREASE_LOAD_FOR_STIMULUS,
    RESUME_MODULE,
    INCREASE_WEEKLY_FREQUENCY,
    REVIEW_TECHNIQUE,
}
