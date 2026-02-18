package com.estebancoloradogonzalez.tension.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.estebancoloradogonzalez.tension.data.local.dao.AlertDao
import com.estebancoloradogonzalez.tension.data.local.dao.DeloadDao
import com.estebancoloradogonzalez.tension.data.local.dao.EquipmentTypeDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseProgressionDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseSetDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.MuscleZoneDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.dao.ProfileDao
import com.estebancoloradogonzalez.tension.data.local.dao.RotationStateDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionDao
import com.estebancoloradogonzalez.tension.data.local.dao.SessionExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.WeightRecordDao
import com.estebancoloradogonzalez.tension.data.local.entity.AlertEntity
import com.estebancoloradogonzalez.tension.data.local.entity.DeloadEntity
import com.estebancoloradogonzalez.tension.data.local.entity.EquipmentTypeEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseMuscleZoneEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseProgressionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ExerciseSetEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ModuleEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ModuleVersionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.MuscleZoneEntity
import com.estebancoloradogonzalez.tension.data.local.entity.PlanAssignmentEntity
import com.estebancoloradogonzalez.tension.data.local.entity.ProfileEntity
import com.estebancoloradogonzalez.tension.data.local.entity.RotationStateEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionEntity
import com.estebancoloradogonzalez.tension.data.local.entity.SessionExerciseEntity
import com.estebancoloradogonzalez.tension.data.local.entity.WeightRecordEntity

@Database(
    entities = [
        ProfileEntity::class,
        WeightRecordEntity::class,
        RotationStateEntity::class,
        ModuleEntity::class,
        MuscleZoneEntity::class,
        EquipmentTypeEntity::class,
        ExerciseEntity::class,
        ExerciseMuscleZoneEntity::class,
        ModuleVersionEntity::class,
        PlanAssignmentEntity::class,
        SessionEntity::class,
        SessionExerciseEntity::class,
        ExerciseProgressionEntity::class,
        ExerciseSetEntity::class,
        AlertEntity::class,
        DeloadEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TensionDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightRecordDao(): WeightRecordDao
    abstract fun rotationStateDao(): RotationStateDao
    abstract fun moduleDao(): ModuleDao
    abstract fun muscleZoneDao(): MuscleZoneDao
    abstract fun equipmentTypeDao(): EquipmentTypeDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun moduleVersionDao(): ModuleVersionDao
    abstract fun planAssignmentDao(): PlanAssignmentDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionExerciseDao(): SessionExerciseDao
    abstract fun exerciseProgressionDao(): ExerciseProgressionDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun alertDao(): AlertDao
    abstract fun deloadDao(): DeloadDao
}
