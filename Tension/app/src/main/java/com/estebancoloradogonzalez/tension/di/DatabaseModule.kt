package com.estebancoloradogonzalez.tension.di

import android.content.Context
import androidx.room.Room
import com.estebancoloradogonzalez.tension.data.local.dao.EquipmentTypeDao
import com.estebancoloradogonzalez.tension.data.local.dao.ExerciseDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleDao
import com.estebancoloradogonzalez.tension.data.local.dao.ModuleVersionDao
import com.estebancoloradogonzalez.tension.data.local.dao.MuscleZoneDao
import com.estebancoloradogonzalez.tension.data.local.dao.PlanAssignmentDao
import com.estebancoloradogonzalez.tension.data.local.dao.ProfileDao
import com.estebancoloradogonzalez.tension.data.local.dao.RotationStateDao
import com.estebancoloradogonzalez.tension.data.local.dao.WeightRecordDao
import com.estebancoloradogonzalez.tension.data.local.database.TensionDatabase
import com.estebancoloradogonzalez.tension.data.local.seed.PrepopulateCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTensionDatabase(
        @ApplicationContext context: Context,
    ): TensionDatabase {
        return Room.databaseBuilder(
            context,
            TensionDatabase::class.java,
            "tension_database",
        )
            .addCallback(PrepopulateCallback())
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideProfileDao(database: TensionDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideWeightRecordDao(database: TensionDatabase): WeightRecordDao {
        return database.weightRecordDao()
    }

    @Provides
    fun provideRotationStateDao(database: TensionDatabase): RotationStateDao {
        return database.rotationStateDao()
    }

    @Provides
    fun provideModuleDao(database: TensionDatabase): ModuleDao {
        return database.moduleDao()
    }

    @Provides
    fun provideMuscleZoneDao(database: TensionDatabase): MuscleZoneDao {
        return database.muscleZoneDao()
    }

    @Provides
    fun provideEquipmentTypeDao(database: TensionDatabase): EquipmentTypeDao {
        return database.equipmentTypeDao()
    }

    @Provides
    fun provideExerciseDao(database: TensionDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    fun provideModuleVersionDao(database: TensionDatabase): ModuleVersionDao {
        return database.moduleVersionDao()
    }

    @Provides
    fun providePlanAssignmentDao(database: TensionDatabase): PlanAssignmentDao {
        return database.planAssignmentDao()
    }
}
