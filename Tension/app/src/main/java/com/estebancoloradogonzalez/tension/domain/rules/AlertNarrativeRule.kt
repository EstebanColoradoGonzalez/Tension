package com.estebancoloradogonzalez.tension.domain.rules

import com.estebancoloradogonzalez.tension.domain.model.PlateauCause
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import com.estebancoloradogonzalez.tension.domain.model.SuggestedActionKind
import java.util.Locale

/**
 * Writes what an alert says, in the language of the executant rather than of the engine.
 *
 * The same sentence is needed at two different moments — when the alert is emitted, so
 * it can be persisted and listed, and when its detail is opened, where it is expanded
 * with context — so it lives in one place: duplicating it guarantees the two drift.
 *
 * Rules that hold for every text produced here:
 * - it says **what** was detected and **over which** element, naming it;
 * - it carries the figure that originated it;
 * - it never exposes internal identifiers, alert type codes, severity codes or the name
 *   of the rule that fired.
 *
 * The strings live in this layer rather than in resources because the message is
 * persisted, so it must exist before any UI can format it, and because neither Domain
 * nor Data may reach Android resources.
 */
object AlertNarrativeRule {

    // ---------------------------------------------------------------- headlines

    fun plateauHeadline(exerciseName: String, sessions: Int): String =
        "$exerciseName lleva $sessions ${sessionWord(sessions)} sin subir carga"

    fun progressionRateHeadline(exerciseName: String, rate: Int, weeks: Long): String =
        "$exerciseName solo avanzó en el $rate% de sus sesiones de las últimas $weeks semanas"

    fun rirHeadline(
        routineName: String,
        avgRir: Double,
        isLow: Boolean,
        sessions: Int,
    ): String {
        val what = if (isLow) {
            "entrenaste demasiado cerca del fallo"
        } else {
            "te sobró margen en cada serie"
        }
        return "En $routineName $what durante las últimas $sessions sesiones " +
            "(RIR promedio ${decimal(avgRir)})"
    }

    fun adherenceHeadline(percentage: Int, consecutiveWeeks: Int): String =
        "Llevas $consecutiveWeeks ${weekWord(consecutiveWeeks)} seguidas entrenando el " +
            "$percentage% de lo que planificaste"

    fun tonnageHeadline(muscleGroup: String, dropPercentage: Int, microcycles: Int): String =
        "Tu volumen de $muscleGroup bajó $dropPercentage% en los últimos " +
            "$microcycles microciclos"

    fun inactivityHeadline(routineName: String, days: Long): String =
        "No has entrenado $routineName desde hace $days días"

    fun deloadHeadline(routineName: String): String =
        "Casi toda la rutina $routineName dejó de avanzar a la vez"

    // ------------------------------------------------------------- explanations

    fun plateauExplanation(
        exerciseName: String,
        sessions: Int,
        difficulty: ProgressionDifficulty,
        cause: PlateauCause,
    ): String {
        val opening = "$exerciseName lleva $sessions ${sessionWord(sessions)} sin subir " +
            "carga ni repeticiones."
        return "$opening ${paceNote(difficulty, sessions)} ${causeNote(cause)}"
    }

    fun progressionRateExplanation(
        exerciseName: String,
        rate: Int,
        difficulty: ProgressionDifficulty,
        isCritical: Boolean,
    ): String {
        val opening = if (isCritical) {
            "$exerciseName avanzó solo en el $rate% de sus sesiones recientes: lleva " +
                "tanto tiempo parado que ya no estás ganando adaptaciones con él."
        } else {
            "$exerciseName avanzó solo en el $rate% de sus sesiones recientes."
        }
        return "$opening ${paceNoteForRate(difficulty)}"
    }

    fun rirExplanation(routineName: String, avgRir: Double, isLow: Boolean): String {
        return if (isLow) {
            "En $routineName tu RIR promedio fue ${decimal(avgRir)}: estás terminando las " +
                "series casi sin margen. Entrenar siempre al fallo acumula fatiga más " +
                "rápido de lo que te recuperas."
        } else {
            "En $routineName tu RIR promedio fue ${decimal(avgRir)}: estás terminando las " +
                "series con margen de sobra, y con ese margen el estímulo puede quedarse " +
                "corto para que el músculo se adapte."
        }
    }

    fun adherenceExplanation(percentage: Int, consecutiveWeeks: Int): String {
        val opening = "Entrenaste el $percentage% de lo que planificaste durante " +
            "$consecutiveWeeks ${weekWord(consecutiveWeeks)} seguidas."
        return if (consecutiveWeeks >= AlertThresholdRule.ADHERENCE_CRISIS_WEEKS) {
            "$opening Con tanto tiempo entre sesiones, comparar una con la anterior deja " +
                "de decirte nada útil."
        } else {
            "$opening Cuanto más se espacian las sesiones, más tarda el sistema en " +
                "notar si algo va bien o mal."
        }
    }

    fun tonnageExplanation(
        muscleGroup: String,
        dropPercentage: Int,
        isDeload: Boolean,
    ): String {
        return if (isDeload) {
            "Estás en una descarga planificada: que el volumen de $muscleGroup baje un " +
                "$dropPercentage% es justo lo que se espera y está bajo control."
        } else {
            "El volumen que moviste en $muscleGroup cayó un $dropPercentage% sin que lo " +
                "hayas planificado. Puede venir de menos series, menos repeticiones o " +
                "menos carga."
        }
    }

    fun inactivityExplanation(
        routineName: String,
        days: Long,
        muscleGroups: List<String>,
    ): String {
        val opening = "$routineName lleva $days días sin una sola sesión."
        if (muscleGroups.isEmpty()) return opening
        return "$opening Lo que trabajas ahí (${muscleGroups.joinToString(", ")}) empieza " +
            "a perder lo que ya habías ganado."
    }

    fun deloadExplanation(routineName: String, regressionPercentage: Int): String =
        "En $routineName, el $regressionPercentage% de los ejercicios se quedó parado o " +
            "retrocedió en las sesiones recientes. Cuando cae tanto a la vez, no es el " +
            "ejercicio: es la fatiga acumulada."

    // ---------------------------------------------------------- suggested action

    fun suggestedActionText(
        kind: SuggestedActionKind,
        exerciseName: String,
        routineName: String,
        incrementKg: Double,
    ): String = when (kind) {
        SuggestedActionKind.INCREASE_LOAD_SLIGHTLY ->
            "Prueba subir ${decimal(incrementKg)} kg en $exerciseName aunque bajes un par " +
                "de repeticiones. Si te quedas corto, vuelve a la carga anterior la " +
                "sesión siguiente."
        SuggestedActionKind.EXTEND_REPS_BEFORE_LOAD ->
            "Antes de subir peso en $exerciseName, añade una repetición por serie durante " +
                "un par de sesiones. Cuando las tengas todas limpias, sube " +
                "${decimal(incrementKg)} kg."
        SuggestedActionKind.SWITCH_TO_SLOT_ALTERNATIVE ->
            "Cambia $exerciseName por la alternativa que tienes configurada en ese puesto. " +
                "Un estímulo distinto suele desatascar lo que la carga sola no mueve."
        SuggestedActionKind.ROTATE_ROUTINE_VERSION ->
            "Rota a otra versión de $routineName. Cambiar el orden y la selección de " +
                "ejercicios rompe la adaptación que te tiene parado."
        SuggestedActionKind.START_DELOAD ->
            "Activa la descarga y entrena el próximo microciclo al 60% de tus cargas. " +
                "Bajar una semana ahora te devuelve varias de progreso después."
        SuggestedActionKind.REDUCE_VOLUME ->
            "Revisa cuántas series estás haciendo en $routineName. Si recortaste sin " +
                "querer, vuelve a las que tenías; si el recorte fue por fatiga, mantenlo " +
                "y sube la carga en su lugar."
        SuggestedActionKind.LEAVE_REPS_IN_RESERVE ->
            "Deja 2 repeticiones en reserva en las primeras series. Corta la serie cuando " +
                "notes que la técnica se rompe, no cuando ya no puedas más."
        SuggestedActionKind.INCREASE_LOAD_FOR_STIMULUS ->
            "Sube la carga en los ejercicios de $routineName hasta terminar las series con " +
                "una o dos repeticiones de margen, no más."
        SuggestedActionKind.RESUME_MODULE ->
            "Retoma $routineName en tu próxima sesión. No hace falta recuperar lo perdido " +
                "de golpe: empieza con la carga que tenías."
        SuggestedActionKind.INCREASE_WEEKLY_FREQUENCY ->
            "Recupera al menos una sesión más esta semana. Si la frecuencia que fijaste no " +
                "te cabe, bájala en tu perfil en lugar de arrastrar semanas incompletas."
        SuggestedActionKind.REVIEW_TECHNIQUE ->
            "Revisa tu técnica y tu descanso entre series antes de cambiar nada de la carga."
    }

    // ------------------------------------------------------------------ helpers

    private fun paceNote(difficulty: ProgressionDifficulty, sessions: Int): String =
        when (difficulty) {
            ProgressionDifficulty.HIGH ->
                "Es un ejercicio que progresa despacio por naturaleza, por eso el sistema " +
                    "esperó $sessions sesiones antes de avisarte."
            ProgressionDifficulty.MEDIUM ->
                "El sistema esperó $sessions sesiones antes de avisarte para no confundir " +
                    "una racha mala con un estancamiento."
            ProgressionDifficulty.LOW ->
                "Es un ejercicio en el que se suele avanzar rápido, así que $sessions " +
                    "sesiones parado ya dice algo."
        }

    private fun paceNoteForRate(difficulty: ProgressionDifficulty): String =
        when (difficulty) {
            ProgressionDifficulty.HIGH ->
                "Al ser un ejercicio que progresa despacio, se le pide bastante menos que " +
                    "al resto antes de avisarte."
            ProgressionDifficulty.MEDIUM ->
                "Se mide sobre las últimas semanas para que una sesión mala suelta no " +
                    "cuente como estancamiento."
            ProgressionDifficulty.LOW ->
                "Es un ejercicio en el que se suele avanzar con facilidad, así que se le " +
                    "pide más que al resto."
        }

    private fun causeNote(cause: PlateauCause): String = when (cause) {
        PlateauCause.LOW_RIR_LIMIT ->
            "Vienes entrenando muy cerca del fallo, así que puede que estés en el techo " +
                "de carga que aguantas ahora mismo."
        PlateauCause.HIGH_RIR_CONSERVATIVE ->
            "Estás terminando las series con margen de sobra: hay sitio para exigirte más."
        PlateauCause.GROUP_STAGNATION ->
            "El resto de ejercicios del mismo grupo también está parado, así que apunta " +
                "a fatiga y no al ejercicio."
        PlateauCause.MIXED ->
            "No hay una causa única a la vista: conviene revisar carga, descanso y técnica."
    }

    private fun sessionWord(count: Int): String = if (count == 1) "sesión" else "sesiones"

    private fun weekWord(count: Int): String = if (count == 1) "semana" else "semanas"

    private fun decimal(value: Double): String = String.format(Locale.US, "%.1f", value)
}
