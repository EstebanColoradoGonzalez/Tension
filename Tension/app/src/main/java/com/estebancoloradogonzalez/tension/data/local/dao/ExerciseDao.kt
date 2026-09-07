package com.estebancoloradogonzalez.tension.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseEquipmentEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseMuscleZoneEntity
import kotlinx.coroutines.flow.Flow

data class ExerciseWithDetails(
    val id: Long,
    val name: String,
    /** Implementos admitidos, separados por [AGGREGATE_SEPARATOR] y en orden de catálogo. */
    val equipmentTypes: String?,
    val isBodyweight: Int,
    val isIsometric: Int,
    val isToTechnicalFailure: Int,
    val isCustom: Int,
    val mediaResource: String?,
    val progressionDifficulty: String,
    /** Zonas musculares, separadas por [AGGREGATE_SEPARATOR]. */
    val muscleZones: String?,
    val muscleGroup: String?,
)

/**
 * Separador de las listas que las consultas agregan en una sola columna.
 *
 * No se usa `GROUP_CONCAT(DISTINCT x)`: SQLite no admite separador junto a `DISTINCT` y
 * emite `","`, que aparece dentro de los nombres de dominio y obliga a adivinar dónde
 * termina un elemento — el filtro por zona muscular no encontraba los cuatro ejercicios
 * de dos zonas justamente por eso. La barra vertical no aparece en ningún nombre de zona
 * ni de equipamiento del catálogo.
 *
 * Las listas se agregan con subconsulta escalar y no con `LEFT JOIN`: dos uniones N:M en
 * la misma consulta producirían el producto cartesiano zonas × equipamientos para
 * deshacerlo después con `DISTINCT`. El `SELECT` anidado además deja el orden fijado por
 * el `id` del catálogo en lugar de dejarlo al plan de ejecución.
 */
const val AGGREGATE_SEPARATOR = "|"

@Dao
interface ExerciseDao {

    @Query(
        """
        SELECT
            e.id,
            e.name,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT et.name AS name FROM exercise_equipment ee
                INNER JOIN equipment_type et ON ee.equipment_type_id = et.id
                WHERE ee.exercise_id = e.id
                ORDER BY et.id
            )) AS equipmentTypes,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            e.media_resource AS mediaResource,
            e.progression_difficulty AS progressionDifficulty,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT mz.name AS name FROM exercise_muscle_zone emz
                INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
                WHERE emz.exercise_id = e.id
                ORDER BY mz.id
            )) AS muscleZones,
            (SELECT mz2.muscle_group FROM exercise_muscle_zone emz2
             INNER JOIN muscle_zone mz2 ON emz2.muscle_zone_id = mz2.id
             WHERE emz2.exercise_id = e.id LIMIT 1) AS muscleGroup
        FROM exercise e
        ORDER BY e.name ASC
        """,
    )
    fun getAll(): Flow<List<ExerciseWithDetails>>

    @Query(
        """
        SELECT
            e.id,
            e.name,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT et.name AS name FROM exercise_equipment ee
                INNER JOIN equipment_type et ON ee.equipment_type_id = et.id
                WHERE ee.exercise_id = e.id
                ORDER BY et.id
            )) AS equipmentTypes,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            e.media_resource AS mediaResource,
            e.progression_difficulty AS progressionDifficulty,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT mz.name AS name FROM exercise_muscle_zone emz
                INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
                WHERE emz.exercise_id = e.id
                ORDER BY mz.id
            )) AS muscleZones,
            (SELECT mz2.muscle_group FROM exercise_muscle_zone emz2
             INNER JOIN muscle_zone mz2 ON emz2.muscle_zone_id = mz2.id
             WHERE emz2.exercise_id = e.id LIMIT 1) AS muscleGroup
        FROM exercise e
        WHERE e.id = :exerciseId
        """,
    )
    fun getById(exerciseId: Long): Flow<ExerciseWithDetails?>

    @Query("SELECT * FROM exercise WHERE id = :exerciseId")
    suspend fun getByIdOnce(exerciseId: Long): ExerciseEntity?

    @Query(
        """
        SELECT
            e.id,
            e.name,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT et.name AS name FROM exercise_equipment ee
                INNER JOIN equipment_type et ON ee.equipment_type_id = et.id
                WHERE ee.exercise_id = e.id
                ORDER BY et.id
            )) AS equipmentTypes,
            e.is_bodyweight AS isBodyweight,
            e.is_isometric AS isIsometric,
            e.is_to_technical_failure AS isToTechnicalFailure,
            e.is_custom AS isCustom,
            e.media_resource AS mediaResource,
            e.progression_difficulty AS progressionDifficulty,
            (SELECT GROUP_CONCAT(name, '|') FROM (
                SELECT mz.name AS name FROM exercise_muscle_zone emz
                INNER JOIN muscle_zone mz ON emz.muscle_zone_id = mz.id
                WHERE emz.exercise_id = e.id
                ORDER BY mz.id
            )) AS muscleZones,
            (SELECT mz2.muscle_group FROM exercise_muscle_zone emz2
             INNER JOIN muscle_zone mz2 ON emz2.muscle_zone_id = mz2.id
             WHERE emz2.exercise_id = e.id LIMIT 1) AS muscleGroup
        FROM exercise e
        WHERE e.id NOT IN (
              SELECT exercise_id FROM plan_assignment WHERE routine_version_id = :routineVersionId
          )
        ORDER BY e.name ASC
        """,
    )
    fun getNotInVersion(routineVersionId: Long): Flow<List<ExerciseWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMuscleZones(zones: List<ExerciseMuscleZoneEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEquipment(equipment: List<ExerciseEquipmentEntity>)

    @Query(
        """
        DELETE FROM exercise_equipment
        WHERE exercise_id = :exerciseId AND equipment_type_id = :equipmentTypeId
        """,
    )
    suspend fun deleteEquipment(exerciseId: Long, equipmentTypeId: Long)

    @Query("SELECT COUNT(*) FROM exercise_equipment WHERE exercise_id = :exerciseId")
    suspend fun countEquipmentByExercise(exerciseId: Long): Int

    @Query(
        """
        SELECT ee.equipment_type_id FROM exercise_equipment ee
        INNER JOIN equipment_type et ON ee.equipment_type_id = et.id
        WHERE ee.exercise_id = :exerciseId
        ORDER BY et.id ASC
        """,
    )
    fun getEquipmentIdsByExercise(exerciseId: Long): Flow<List<Long>>

    @Query("UPDATE exercise SET media_resource = :mediaResource WHERE id = :exerciseId")
    suspend fun updateMediaResource(exerciseId: Long, mediaResource: String?)

    @Query("UPDATE exercise SET progression_difficulty = :difficulty WHERE id = :exerciseId")
    suspend fun updateProgressionDifficulty(exerciseId: Long, difficulty: String)

    /** El nombre es único por sí solo desde HU-39: el implemento no es parte de la identidad. */
    @Query("SELECT COUNT(*) FROM exercise WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Transaction
    suspend fun insertExerciseWithRelations(
        exercise: ExerciseEntity,
        muscleZones: List<ExerciseMuscleZoneEntity>,
        equipment: List<ExerciseEquipmentEntity>,
    ): Long {
        val exerciseId = insert(exercise)
        insertAllMuscleZones(muscleZones.map { it.copy(exerciseId = exerciseId) })
        insertAllEquipment(equipment.map { it.copy(exerciseId = exerciseId) })
        return exerciseId
    }
}
