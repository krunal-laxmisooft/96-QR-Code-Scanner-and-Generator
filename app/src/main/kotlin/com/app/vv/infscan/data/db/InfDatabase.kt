package com.app.vv.infscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = true)
abstract class InfDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
