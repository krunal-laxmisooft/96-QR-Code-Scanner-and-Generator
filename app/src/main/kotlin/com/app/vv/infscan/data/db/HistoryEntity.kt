package com.app.vv.infscan.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "history", indices = [Index("kind", "createdAt")])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val codeKind: String,
    val raw: String,
    val title: String,
    val format: String,
    val source: String,
    val createdAt: Long,
    val favorite: Boolean = false,
)
