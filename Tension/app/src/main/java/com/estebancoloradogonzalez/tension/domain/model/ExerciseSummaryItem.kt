package com.estebancoloradogonzalez.tension.domain.model

data class ExerciseSummaryItem(
    val exerciseId: Long,
    val name: String,
    /**
     * Consolidated classification of the exercise in this session (CA-40.04): it progressed
     * if any of its implements progressed.
     */
    val classification: ProgressionClassification?,
    val signal: ActionSignal,
    val weightKg: Double,
    val isBodyweight: Boolean,
    val isIsometric: Boolean,
    val isMastered: Boolean,
    val isDeload: Boolean,
    val completedSets: Int,
    val prescribedSets: Int,
    /**
     * One entry per implement actually used in the session (HU-40).
     *
     * With a single implement the presentation collapses it into the exercise's own line,
     * so for whoever does not alternate implements the summary is identical to what it was
     * before this story (CA-40.08).
     */
    val pairs: List<ExercisePairSummary> = emptyList(),
)

/**
 * What one implement did in the session: its own classification, its own signal and the
 * load its own double-threshold evaluation prescribed.
 */
data class ExercisePairSummary(
    val equipmentTypeId: Long,
    val equipmentTypeName: String,
    val classification: ProgressionClassification?,
    val signal: ActionSignal,
    val weightKg: Double,
    val completedSets: Int,
)
