package com.estebancoloradogonzalez.tension.domain.model

sealed interface DeloadHomeState {
    data class Active(val progress: Int, val moduleCode: String) : DeloadHomeState
    data class Required(val moduleCode: String) : DeloadHomeState
}
