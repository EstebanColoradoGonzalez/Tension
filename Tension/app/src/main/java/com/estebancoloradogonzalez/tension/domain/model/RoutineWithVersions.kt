package com.estebancoloradogonzalez.tension.domain.model

data class RoutineWithVersions(
    val routine: Routine,
    val versions: List<VersionSummary>,
)
