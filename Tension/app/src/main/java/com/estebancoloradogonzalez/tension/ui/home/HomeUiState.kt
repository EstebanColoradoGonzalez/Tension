package com.estebancoloradogonzalez.tension.ui.home

import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.DeloadHomeState
import com.estebancoloradogonzalez.tension.domain.model.NextSession

data class HomeUiState(
    val isLoading: Boolean = true,
    val nextSession: NextSession? = null,
    val activeSession: ActiveSession? = null,
    val microcycleCount: Int = 0,
    val alertCount: Int = 0,
    val deloadState: DeloadHomeState? = null,
) {
    val showNextSessionCard: Boolean get() = activeSession == null && nextSession != null
    val showResumeCard: Boolean get() = activeSession != null
}
