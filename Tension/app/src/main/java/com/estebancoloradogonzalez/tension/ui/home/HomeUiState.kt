package com.estebancoloradogonzalez.tension.ui.home

import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.DeloadHomeState
import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.ReassignableRoutine
import com.estebancoloradogonzalez.tension.domain.model.TodaySession

data class HomeUiState(
    val isLoading: Boolean = true,
    val todaySession: TodaySession? = null,
    val activeSession: ActiveSession? = null,
    val microcycleCount: Int = 0,
    val alertCount: Int = 0,
    val deloadState: DeloadHomeState? = null,
    val reassignOptions: List<ReassignableRoutine> = emptyList(),
    val isReassignDialogOpen: Boolean = false,
    val errorMessage: String? = null,
) {
    val nextSession: NextSession? get() = todaySession?.session

    val showNextSessionCard: Boolean get() = activeSession == null && nextSession != null
    val showResumeCard: Boolean get() = activeSession != null

    /** El día no tiene rutina asignada y no hay reasignación vigente. */
    val showRestDayCard: Boolean
        get() = activeSession == null && todaySession?.showRestDayCard == true

    /**
     * La reasignación solo está disponible antes de iniciar la sesión: una vez iniciada, la
     * rutina queda fijada. La tarjeta que la aloja tampoco se compone con sesión activa, así
     * que la acción no existe en ese estado en lugar de existir deshabilitada.
     */
    val canReassign: Boolean get() = activeSession == null

    val isTemporaryOverride: Boolean get() = todaySession?.isTemporaryOverride == true
}
