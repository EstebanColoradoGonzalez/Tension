package com.estebancoloradogonzalez.tension.data.local.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class PrepopulateCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        PrepopulateFacade.populate(db)
    }

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        PrepopulateFacade.populate(db)
    }
}
