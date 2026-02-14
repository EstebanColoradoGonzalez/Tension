package com.estebancoloradogonzalez.tension.data.local.seed

import androidx.sqlite.db.SupportSQLiteDatabase

object PrepopulateFacade {

    fun populate(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            ModuleSeeder.seed(db)
            ExerciseSeeder.seed(db)
            PlanSeeder.seed(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
