package com.app.vv.infscan.ui.create

import android.os.Bundle
import com.app.vv.infscan.domain.create.FormField
import com.app.vv.infscan.domain.create.FormValues
import com.app.vv.infscan.domain.model.WifiSecurity

/** Keeps a half-filled form across process death. */
object FormValuesSaver {
    private const val SECURITY = "security"
    private const val HIDDEN = "hidden"
    private const val START = "start"
    private const val END = "end"

    fun save(values: FormValues): Bundle = Bundle().apply {
        values.text.forEach { (field, text) -> putString(field.name, text) }
        putString(SECURITY, values.security.name)
        putBoolean(HIDDEN, values.hidden)
        values.startMillis?.let { putLong(START, it) }
        values.endMillis?.let { putLong(END, it) }
    }

    fun restore(bundle: Bundle): FormValues = FormValues(
        text = FormField.entries.mapNotNull { field -> bundle.getString(field.name)?.let { field to it } }.toMap(),
        security = bundle.getString(SECURITY)?.let { runCatching { WifiSecurity.valueOf(it) }.getOrNull() } ?: WifiSecurity.WPA,
        hidden = bundle.getBoolean(HIDDEN),
        startMillis = if (bundle.containsKey(START)) bundle.getLong(START) else null,
        endMillis = if (bundle.containsKey(END)) bundle.getLong(END) else null,
    )
}
