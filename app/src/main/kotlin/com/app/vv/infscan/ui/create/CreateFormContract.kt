package com.app.vv.infscan.ui.create

import com.app.vv.infscan.domain.create.CreateType
import com.app.vv.infscan.domain.create.FormField
import com.app.vv.infscan.domain.create.FormValues
import com.app.vv.infscan.domain.model.WifiSecurity

data class CreateFormUiState(
    val type: CreateType,
    val values: FormValues = FormValues(),
    val errors: Map<FormField, Int> = emptyMap(),
    val passwordVisible: Boolean = false,
)

sealed interface CreateFormEvent {
    data class FieldChanged(val field: FormField, val text: String) : CreateFormEvent
    data class SecuritySelected(val security: WifiSecurity) : CreateFormEvent
    data class HiddenChanged(val hidden: Boolean) : CreateFormEvent
    data class StartChanged(val millis: Long) : CreateFormEvent
    data class EndChanged(val millis: Long) : CreateFormEvent
    data object TogglePassword : CreateFormEvent
    data object Submit : CreateFormEvent
}

sealed interface CreateFormEffect {
    data class OpenPreview(val id: Long) : CreateFormEffect
}
