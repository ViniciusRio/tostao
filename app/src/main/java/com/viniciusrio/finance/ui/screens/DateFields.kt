package com.viniciusrio.finance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.viniciusrio.finance.domain.Periods
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Botão que abre o seletor de data. O DatePicker trabalha em UTC; convertemos por epochDay. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateButton(label: String, date: LocalDate, today: LocalDate, onDate: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier.heightIn(min = 48.dp)) {
        Icon(Icons.Outlined.CalendarToday, null)
        Text("  $label: ${Periods.dayLabel(date, today)}")
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDate(LocalDate.ofEpochDay(it / 86_400_000L)) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) { DatePicker(state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeButton(time: LocalTime, onTime: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier.heightIn(min = 48.dp)) {
        Icon(Icons.Outlined.Schedule, null)
        Text("  %02d:%02d".format(time.hour, time.minute))
    }
    if (open) {
        val state = rememberTimePickerState(time.hour, time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onTime(LocalTime.of(state.hour, state.minute)); open = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
            text = { TimePicker(state) },
        )
    }
}

@Composable
fun DateTimeRow(date: LocalDate, time: LocalTime, today: LocalDate, onDate: (LocalDate) -> Unit, onTime: (LocalTime) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateButton("Data", date, today, onDate, Modifier.weight(1f))
        TimeButton(time, onTime)
    }
}
