package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone
import com.estebancoloradogonzalez.tension.domain.model.ProgressionDifficulty
import kotlinx.coroutines.flow.Flow

/** Zonas de un ejercicio, ya partidas por jerarquía. */
data class ExerciseMuscleZoneIds(
    val primary: List<Long>,
    val secondary: List<Long>,
)

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExerciseById(id: Long): Flow<Exercise?>
    fun getAllEquipmentTypes(): Flow<List<EquipmentType>>
    fun getAllMuscleZones(): Flow<List<MuscleZone>>
    fun getEquipmentTypesWithExercises(): Flow<List<EquipmentType>>
    fun getMuscleZonesWithExercises(): Flow<List<MuscleZone>>

    /** Ids de los implementos que el ejercicio admite, en orden de catálogo. */
    fun getEquipmentIdsOfExercise(exerciseId: Long): Flow<List<Long>>

    /** Zonas del ejercicio partidas por jerarquía: principales y secundarias. */
    fun getMuscleZoneIdsOfExercise(exerciseId: Long): Flow<ExerciseMuscleZoneIds>

    /**
     * Reemplaza por completo las zonas del ejercicio.
     *
     * El caso de uso valida antes: al menos una principal y ninguna zona en las dos listas
     * (CA-41.09).
     */
    suspend fun setMuscleZones(
        exerciseId: Long,
        primaryMuscleZoneIds: List<Long>,
        secondaryMuscleZoneIds: List<Long>,
    )

    suspend fun createExercise(
        name: String,
        equipmentTypeIds: List<Long>,
        primaryMuscleZoneIds: List<Long>,
        secondaryMuscleZoneIds: List<Long>,
        isBodyweight: Boolean,
        isIsometric: Boolean,
        isToTechnicalFailure: Boolean,
        mediaResource: String?,
        progressionDifficulty: ProgressionDifficulty,
    ): Long
    suspend fun updateExerciseImage(exerciseId: Long, mediaResource: String?)
    suspend fun updateProgressionDifficulty(exerciseId: Long, difficulty: ProgressionDifficulty)

    /** El nombre es único por sí solo: el implemento no forma parte de la identidad. */
    suspend fun exerciseExistsByName(name: String): Boolean

    suspend fun addEquipmentToExercise(exerciseId: Long, equipmentTypeId: Long)
    suspend fun removeEquipmentFromExercise(exerciseId: Long, equipmentTypeId: Long)

    /** Cuántos implementos admite el ejercicio. Nunca puede quedar en 0. */
    suspend fun countEquipmentOfExercise(exerciseId: Long): Int

    /** Series ya registradas del ejercicio con ese implemento. */
    suspend fun countSetsWithEquipment(exerciseId: Long, equipmentTypeId: Long): Int
}
