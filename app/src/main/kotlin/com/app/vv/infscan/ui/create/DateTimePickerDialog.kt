package com.app.vv.infscan.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.ui.components.ButtonKind
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Space
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Picks a date, then a time, and returns epoch millis in the device time zone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(initialMillis: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val c = InfTheme.colors
    val zone = ZoneId.systemDefault()
    val initial = Instant.ofEpochMilli(initialMillis).atZone(zone)
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initial.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = false)
    var step by remember { mutableStateOf(DateStep.DATE) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = Shapes.dialog, color = c.surface) {
            Column(Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm), horizontalAlignment = Alignment.CenterHorizontally) {
                if (step == DateStep.DATE) {
                    DatePicker(dateState, showModeToggle = false, colors = DatePickerDefaults.colors(containerColor = c.surface))
                } else {
                    TimePicker(timeState, colors = TimePickerDefaults.colors(containerColor = c.surface))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm, Alignment.End)) {
                    InfButton(stringResource(R.string.action_cancel), onDismiss, kind = ButtonKind.Text, height = 48.dp, fillWidth = false)
                    if (step == DateStep.DATE) {
                        InfButton(stringResource(R.string.action_next), { step = DateStep.TIME }, height = 48.dp, fillWidth = false)
                    } else {
                        InfButton(stringResource(R.string.action_ok), {
                            val day = dateState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: LocalDate.now(zone)
                            onConfirm(day.atTime(timeState.hour, timeState.minute).atZone(zone).toInstant().toEpochMilli())
                        }, height = 48.dp, fillWidth = false)
                    }
                }
            }
        }
    }
}

private enum class DateStep { DATE, TIME }
