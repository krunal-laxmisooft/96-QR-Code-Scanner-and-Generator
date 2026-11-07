package com.app.vv.infscan.ui.nav

import kotlinx.serialization.Serializable

@Serializable data object OnboardingDest
@Serializable data object ScanDest
@Serializable data object CreateDest
@Serializable data object HistoryDest
@Serializable data object SettingsDest
@Serializable data class ResultDest(val id: Long)
@Serializable data class CreateFormDest(val type: String)
@Serializable data class CreatePreviewDest(val id: Long)
