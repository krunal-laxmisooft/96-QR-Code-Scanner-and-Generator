package com.app.vv.infscan.data.repo

import com.app.vv.infscan.data.db.HistoryDao
import com.app.vv.infscan.data.db.HistoryEntity
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.domain.model.NewHistoryItem
import com.app.vv.infscan.domain.model.ScanSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface HistoryRepository {
    fun observe(kind: HistoryKind): Flow<List<HistoryItem>>
    fun observeCount(kind: HistoryKind): Flow<Int>
    fun observeItem(id: Long): Flow<HistoryItem?>
    suspend fun get(id: Long): HistoryItem?
    suspend fun latest(kind: HistoryKind, limit: Int): List<HistoryItem>
    suspend fun all(): List<HistoryItem>
    suspend fun add(item: NewHistoryItem): Long
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun delete(id: Long)
    suspend fun restore(item: HistoryItem)
    suspend fun clear()
    suspend fun purgeOlderThan(retentionDays: Int, now: Long): Int
}

@Singleton
class RoomHistoryRepository @Inject constructor(private val dao: HistoryDao) : HistoryRepository {

    override fun observe(kind: HistoryKind): Flow<List<HistoryItem>> =
        dao.observeByKind(kind.name).map { list -> list.map { it.toDomain() } }

    override fun observeCount(kind: HistoryKind): Flow<Int> = dao.observeCount(kind.name)

    override fun observeItem(id: Long): Flow<HistoryItem?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun get(id: Long): HistoryItem? = dao.getById(id)?.toDomain()

    override suspend fun latest(kind: HistoryKind, limit: Int): List<HistoryItem> =
        dao.latest(kind.name, limit).map { it.toDomain() }

    override suspend fun all(): List<HistoryItem> = dao.all().map { it.toDomain() }

    override suspend fun add(item: NewHistoryItem): Long = dao.insert(
        HistoryEntity(
            kind = item.kind.name,
            codeKind = item.codeKind.name,
            raw = item.raw,
            title = item.title,
            format = item.format.name,
            source = item.source.name,
            createdAt = item.createdAt,
        ),
    )

    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun restore(item: HistoryItem) {
        dao.insert(
            HistoryEntity(
                id = item.id,
                kind = item.kind.name,
                codeKind = item.codeKind.name,
                raw = item.raw,
                title = item.title,
                format = item.format.name,
                source = item.source.name,
                createdAt = item.createdAt,
                favorite = item.favorite,
            ),
        )
    }

    override suspend fun clear() = dao.clear()

    override suspend fun purgeOlderThan(retentionDays: Int, now: Long): Int {
        if (retentionDays <= 0) return 0
        return dao.deleteOlderThan(now - TimeUnit.DAYS.toMillis(retentionDays.toLong()))
    }

    private fun HistoryEntity.toDomain() = HistoryItem(
        id = id,
        kind = HistoryKind.valueOf(kind),
        codeKind = CodeKind.valueOf(codeKind),
        raw = raw,
        title = title,
        format = CodeFormat.valueOf(format),
        source = ScanSource.valueOf(source),
        createdAt = createdAt,
        favorite = favorite,
    )
}
