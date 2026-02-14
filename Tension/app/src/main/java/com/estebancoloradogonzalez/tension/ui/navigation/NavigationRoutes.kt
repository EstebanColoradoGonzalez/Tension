package com.estebancoloradogonzalez.tension.ui.navigation

object NavigationRoutes {
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val WEIGHT_HISTORY = "weight-history"
    const val SETTINGS = "settings"
    const val EXERCISE_DICTIONARY = "exercise-dictionary"
    const val EXERCISE_DETAIL = "exercise-detail/{exerciseId}"
    const val CREATE_EXERCISE = "create-exercise"
    const val TRAINING_PLAN = "training-plan"
    const val PLAN_VERSION_DETAIL = "plan-version-detail/{moduleVersionId}"
    const val EXERCISE_HISTORY = "exercise-history/{exerciseId}"
    const val SESSION_HISTORY = "session-history"
    const val METRICS = "metrics"
    const val ACTIVE_SESSION = "active-session/{sessionId}"

    fun exerciseDetailRoute(exerciseId: Long) = "exercise-detail/$exerciseId"
    fun exerciseHistoryRoute(exerciseId: Long) = "exercise-history/$exerciseId"
    fun planVersionDetailRoute(moduleVersionId: Long) = "plan-version-detail/$moduleVersionId"
    fun activeSessionRoute(sessionId: Long) = "active-session/$sessionId"
}
