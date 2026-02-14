package com.estebancoloradogonzalez.tension.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session",
    foreignKeys = [
        ForeignKey(
            entity = ModuleVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["module_version_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["module_version_id"]),
        Index(value = ["status"]),
        Index(value = ["deload_id"]),
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "module_version_id")
    val moduleVersionId: Long,

    @ColumnInfo(name = "deload_id")
    val deloadId: Long? = null,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "status", defaultValue = "IN_PROGRESS")
    val status: String = "IN_PROGRESS",
)
