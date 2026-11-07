package com.app.vv.infscan.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE kind = :kind ORDER BY createdAt DESC, id DESC")
    fun observeByKind(kind: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE id = :id")
    fun observeById(id: Long): Flow<HistoryEntity?>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getById(id: Long): HistoryEntity?

    @Query("SELECT * FROM history WHERE kind = :kind ORDER BY createdAt DESC, id DESC LIMIT :limit")
    suspend fun latest(kind: String, limit: Int): List<HistoryEntity>

    @Query("SELECT * FROM history ORDER BY createdAt DESC, id DESC")
    suspend fun all(): List<HistoryEntity>

    @Query("SELECT COUNT(*) FROM history WHERE kind = :kind")
    fun observeCount(kind: String): Flow<Int>

    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Query("UPDATE history SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("DELETE FROM history WHERE createdAt < :before AND favorite = 0")
    suspend fun deleteOlderThan(before: Long): Int
}
