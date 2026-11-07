package com.app.vv.infscan.data.repo

import com.app.vv.infscan.domain.model.HistoryItem
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object CsvExporter {
    private val timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun toCsv(items: List<HistoryItem>): String = buildString {
        appendLine("id,kind,type,format,source,created_at,favorite,title,content")
        items.forEach { item ->
            appendLine(
                listOf(
                    item.id.toString(),
                    item.kind.name.lowercase(),
                    item.codeKind.name.lowercase(),
                    item.format.name.lowercase(),
                    item.source.name.lowercase(),
                    timestamp.format(Instant.ofEpochMilli(item.createdAt).atOffset(ZoneOffset.UTC)),
                    item.favorite.toString(),
                    item.title,
                    item.raw,
                ).joinToString(",") { escape(it) },
            )
        }
    }

    private fun escape(value: String): String {
        val guarded = if (value.firstOrNull() in FORMULA_PREFIXES) "'$value" else value
        return if (guarded.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + guarded.replace("\"", "\"\"") + "\""
        } else {
            guarded
        }
    }

    private val FORMULA_PREFIXES = setOf('=', '+', '-', '@')
}
