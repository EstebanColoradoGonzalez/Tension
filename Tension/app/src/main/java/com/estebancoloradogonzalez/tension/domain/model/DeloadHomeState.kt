package com.estebancoloradogonzalez.tension.domain.model

sealed interface DeloadHomeState {
    data class Active(val progress: Int, val totalSessions: Int, val routineName: String) : DeloadHomeState
    data class Required(val routineName: String) : DeloadHomeState
}
