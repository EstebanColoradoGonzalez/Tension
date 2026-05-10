package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deload",
    indices = [
        Index(value = ["status"]),
    ],
)
data class DeloadEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE",

    @ColumnInfo(name = "activation_date")
    val activationDate: String,

    @ColumnInfo(name = "completion_date")
    val completionDate: String? = null,
)
