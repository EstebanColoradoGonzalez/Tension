package com.estebancoloradogonzalez.tension.ui.home

import com.estebancoloradogonzalez.tension.domain.model.ActiveSession
import com.estebancoloradogonzalez.tension.domain.model.DeloadHomeState
import com.estebancoloradogonzalez.tension.domain.model.DayOutcome
import com.estebancoloradogonzalez.tension.domain.model.NextSession
import com.estebancoloradogonzalez.tension.domain.model.ReassignableRoutine
import com.estebancoloradogonzalez.tension.domain.model.TodaySession
import com.estebancoloradogonzalez.tension.domain.model.TreeState
import com.estebancoloradogonzalez.tension.domain.model.UpcomingSession

data class HomeUiState(
    val isLoading: Boolean = true,
    val todaySession: TodaySession? = null,
    val activeSession: ActiveSession? = null,
    val microcycleCount: Int = 0,
    val alertCount: Int = 0,
    val deloadState: DeloadHomeState? = null,
    /** Estado del arbol. Nulo solo hasta la primera emision: la tarjeta se compone siempre. */
    val treeState: TreeState? = null,
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

    /** El día ya se resolvió: se entrenó o se declaró que no se entrena. */
    val showResolvedCard: Boolean
        get() = activeSession == null && todaySession?.showResolvedCard == true

    val dayOutcome: DayOutcome? get() = todaySession?.dayOutcome

    /** La sesión del siguiente día con rutina. Informativa: no puede iniciarse hoy. */
    val upcoming: UpcomingSession? get() = todaySession?.upcoming

    /**
     * La reasignación solo está disponible antes de iniciar la sesión: una vez iniciada, la
     * rutina queda fijada. La tarjeta que la aloja tampoco se compone con sesión activa, así
     * que la acción no existe en ese estado en lugar de existir deshabilitada.
     *
     * Con el día ya resuelto tampoco se ofrece: reasignar entonces sería la puerta trasera
     * para ejecutar una segunda sesión el mismo día.
     */
    val canReassign: Boolean
        get() = activeSession == null && todaySession?.isDayResolved != true

    /**
     * La cancelación del día se ofrece mientras el día siga abierto, **también con una sesión
     * en curso**: abrir la sesión y no entrenar nada es justo el caso que hay que poder
     * cancelar.
     */
    val showSkipToday: Boolean get() = todaySession?.isDayResolved != true

    /**
     * Con una sola serie registrada ya hubo entrenamiento: cancelar borraría trabajo real. La
     * acción se muestra deshabilitada en lugar de desaparecer, porque lo que hay que comunicar
     * no es que no exista, sino que la salida es cerrar la sesión como incompleta.
     */
    val canSkipToday: Boolean
        get() = showSkipToday && activeSession?.canBeCancelled != false

    val isTemporaryOverride: Boolean get() = todaySession?.isTemporaryOverride == true
}
