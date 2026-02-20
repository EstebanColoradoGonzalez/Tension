package com.estebancoloradogonzalez.tension.di

import com.estebancoloradogonzalez.tension.data.repository.AlertRepositoryImpl
import com.estebancoloradogonzalez.tension.data.repository.ExerciseRepositoryImpl
import com.estebancoloradogonzalez.tension.data.repository.MetricsRepositoryImpl
import com.estebancoloradogonzalez.tension.data.repository.PlanRepositoryImpl
import com.estebancoloradogonzalez.tension.data.repository.ProfileRepositoryImpl
import com.estebancoloradogonzalez.tension.data.repository.SessionRepositoryImpl
import com.estebancoloradogonzalez.tension.domain.repository.AlertRepository
import com.estebancoloradogonzalez.tension.domain.repository.ExerciseRepository
import com.estebancoloradogonzalez.tension.domain.repository.MetricsRepository
import com.estebancoloradogonzalez.tension.domain.repository.PlanRepository
import com.estebancoloradogonzalez.tension.domain.repository.ProfileRepository
import com.estebancoloradogonzalez.tension.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl,
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        impl: ExerciseRepositoryImpl,
    ): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindPlanRepository(
        impl: PlanRepositoryImpl,
    ): PlanRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl,
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindMetricsRepository(
        impl: MetricsRepositoryImpl,
    ): MetricsRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(
        impl: AlertRepositoryImpl,
    ): AlertRepository
}
