package com.estebancoloradogonzalez.tension.domain.model

data class ModuleWithVersions(
    val module: Module,
    val versions: List<VersionSummary>,
)
