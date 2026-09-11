package com.estebancoloradogonzalez.tension.domain.model

/**
 * Zona muscular del catálogo.
 *
 * [sortOrder] es el orden declarado: los 14 grupos en orden anatómico y, dentro de cada
 * uno, las zonas de mayor a menor. La lista llega ya ordenada desde la capa de datos, así
 * que la interfaz **no ordena**: solo agrupa por [muscleGroup] conservando el orden de
 * llegada.
 */
data class MuscleZone(
    val id: Long,
    val name: String,
    val muscleGroup: String,
    val sortOrder: Int,
)
