package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estado visual del árbol de entrenamiento.
 *
 * Tabla de fila única (`id = 1`), como `rotation_state` y `day_skip`: el árbol es uno solo.
 *
 * Guarda lo que los criterios exigen persistido y **nada que sea derivable en caliente**. No
 * lleva el conteo de sesiones —la etapa ya lo resume, y guardarlo crearía un segundo sitio
 * donde el mismo hecho puede desincronizarse— ni los días transcurridos, que dependen de la
 * fecha de hoy: un entero guardado ayer es rancio hoy, y mostrar valores rancios es justo lo
 * que el recálculo pretende evitar. Los días se derivan de [lastSessionDate] al leer.
 *
 * Sin claves foráneas. El árbol **lee** del historial y nada del sistema lee del árbol: una FK
 * a `session` convertiría esa dependencia de lectura en una de integridad, y borrar una sesión
 * arrastraría el árbol.
 */
@Entity(tableName = "tree_state")
data class TreeStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    /** Puntaje de salud 0–100. Expresa la recencia del último entrenamiento. */
    @ColumnInfo(name = "health_score")
    val healthScore: Int,

    /** Código de la etapa de crecimiento. Expresa el historial acumulado. */
    @ColumnInfo(name = "growth_stage")
    val growthStage: String,

    /** Fecha ISO `YYYY-MM-DD` de la última sesión registrada. Nulo si no hay historial. */
    @ColumnInfo(name = "last_session_date")
    val lastSessionDate: String?,

    /** Fecha ISO del último recálculo. Hace auditable el orden respecto al barrido diario. */
    @ColumnInfo(name = "calculated_at")
    val calculatedAt: String,
)
