package com.estebancoloradogonzalez.tension.domain.repository

import com.estebancoloradogonzalez.tension.domain.model.EquipmentType
import com.estebancoloradogonzalez.tension.domain.model.Exercise
import com.estebancoloradogonzalez.tension.domain.model.Module
import com.estebancoloradogonzalez.tension.domain.model.MuscleZone
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExerciseById(id: Long): Flow<Exercise?>
    fun getAllModules(): Flow<List<Module>>
    fun getAllEquipmentTypes(): Flow<List<EquipmentType>>
    fun getAllMuscleZones(): Flow<List<MuscleZone>>
    suspend fun createExercise(
        name: String,
        moduleCode: String,
        equipmentTypeId: Long,
        muscleZoneIds: List<Long>,
        isBodyweight: Boolean,
        isIsometric: Boolean,
        isToTechnicalFailure: Boolean,
        mediaResource: String?,
    ): Long
    suspend fun updateExerciseImage(exerciseId: Long, mediaResource: String?)
    suspend fun exerciseExistsByNameAndEquipment(name: String, equipmentTypeId: Long): Boolean
    fun getEligibleSubstitutes(moduleCode: String, excludedExerciseIds: List<Long>): Flow<List<Exercise>>
}
