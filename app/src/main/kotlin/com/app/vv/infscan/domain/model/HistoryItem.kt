package com.app.vv.infscan.domain.model

data class HistoryItem(
    val id: Long,
    val kind: HistoryKind,
    val codeKind: CodeKind,
    val raw: String,
    val title: String,
    val format: CodeFormat,
    val source: ScanSource,
    val createdAt: Long,
    val favorite: Boolean,
)

data class NewHistoryItem(
    val kind: HistoryKind,
    val codeKind: CodeKind,
    val raw: String,
    val title: String,
    val format: CodeFormat,
    val source: ScanSource,
    val createdAt: Long,
)
