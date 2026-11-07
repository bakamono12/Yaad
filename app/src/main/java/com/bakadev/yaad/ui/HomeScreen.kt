@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.bakadev.yaad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakadev.yaad.Days
import com.bakadev.yaad.HindiTime
import com.bakadev.yaad.Reminder
import com.bakadev.yaad.Scheduler
import com.bakadev.yaad.SetupItem
import java.util.Calendar

private val SUGGESTIONS = listOf(
    "दवाई लेनी है",
    "पानी पीना है",
    "खाना खाना है",
    "टहलने जाना है",
    "आराम करना है",
)

@Composable
fun HomeScreen(
    reminders: List<Reminder>,
    setup: List<SetupItem>,
    callName: String,
    newId: () -> Int,
    onSave: (Reminder) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
    onDelete: (Int) -> Unit,
    onPreview: (String) -> Unit,
    onCallNameChange: (String) -> Unit,
    onTest: () -> Unit,
) {
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var isNew by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("याद दिलाओ", fontWeight = FontWeight.Bold, fontSize = 26.sp) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "सेटिंग्स")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    isNew = true
                    editing = Reminder(newId(), 9, 0, "")
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(28.dp)) },
                text = { Text("नया रिमाइंडर", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                containerColor = Saffron,
                contentColor = Color.White,
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (setup.isNotEmpty()) item { SetupCard(setup) }

            val next = reminders.filter { it.enabled }.minByOrNull { Scheduler.nextTrigger(it) }
            if (next != null) item { NextCard(next) }

            if (reminders.isEmpty()) {
                item { EmptyState() }
            } else {
                item {
                    Text(
                        "सारे रिमाइंडर",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                items(reminders, key = { it.id }) { r ->
                    ReminderCard(
                        r,
                        onClick = { isNew = false; editing = r },
                        onToggle = { onToggle(r, it) },
                    )
                }
            }
        }
    }

    editing?.let { r ->
        EditSheet(
            initial = r,
            isNew = isNew,
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = { onDelete(r.id); editing = null },
            onPreview = onPreview,
        )
    }

    if (showSettings) {
        SettingsDialog(
            callName = callName,
            onDismiss = { showSettings = false },
            onSave = { onCallNameChange(it); showSettings = false },
            onTest = onTest,
        )
    }
}

// ---- cards ------------------------------------------------------------------

@Composable
private fun SetupCard(items: List<SetupItem>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WarnBg),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFB35C00))
                Spacer(Modifier.width(8.dp))
                Text("एक बार ये चालू कर दें", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "ताकि रिमाइंडर हर हाल में बोले — फ़ोन लॉक हो या साइलेंट पर।",
                fontSize = 16.sp,
                color = Muted,
            )
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.desc, fontSize = 15.sp, color = Muted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = item.fix) { Text("चालू करें", fontSize = 16.sp) }
                }
            }
        }
    }
}

@Composable
private fun NextCard(r: Reminder) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SaffronSoft),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("अगला रिमाइंडर", fontSize = 16.sp, color = Color(0xFF5C2400))
            Spacer(Modifier.height(4.dp))
            Text(nextLabel(r), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text(r.text, fontSize = 22.sp, color = Saffron, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReminderCard(r: Reminder, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val faded = if (r.enabled) 1f else 0.45f
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(92.dp)) {
                Text(
                    HindiTime.short(r.hour, r.minute),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Saffron.copy(alpha = faded),
                )
                Text(HindiTime.period(r.hour), fontSize = 16.sp, color = Muted.copy(alpha = faded))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    r.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink.copy(alpha = faded),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (r.enabled) Days.summary(r.days) else "बंद है",
                    fontSize = 15.sp,
                    color = Muted,
                )
            }
            Switch(checked = r.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Notifications,
            contentDescription = null,
            tint = Saffron.copy(alpha = 0.5f),
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("अभी कोई रिमाइंडर नहीं है", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "नीचे \"नया रिमाइंडर\" दबाकर जोड़ें",
            fontSize = 18.sp,
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun nextLabel(r: Reminder): String {
    val at = Calendar.getInstance().apply { timeInMillis = Scheduler.nextTrigger(r) }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    fun same(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    val day = when {
        same(at, today) -> "आज"
        same(at, tomorrow) -> "कल"
        else -> Days.full.getValue(at.get(Calendar.DAY_OF_WEEK))
    }
    return "$day ${HindiTime.display(r.hour, r.minute)}"
}

// ---- add / edit sheet ---------------------------------------------------------

@Composable
private fun EditSheet(
    initial: Reminder,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: () -> Unit,
    onPreview: (String) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(initial.text) }
    var days by remember { mutableStateOf(initial.days) }
    val time = rememberTimePickerState(initial.hour, initial.minute, is24Hour = false)
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = Cream,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (isNew) "नया रिमाइंडर" else "रिमाइंडर बदलें",
                style = MaterialTheme.typography.titleLarge,
            )

            StepLabel("1", "क्या याद दिलाना है?")
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 22.sp),
                placeholder = { Text("जैसे: दवाई लेनी है", fontSize = 20.sp) },
                maxLines = 3,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SUGGESTIONS.forEach { s ->
                    SuggestionChip(onClick = { text = s }, label = { Text(s, fontSize = 16.sp) })
                }
            }

            StepLabel("2", "कितने बजे?")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = time)
            }

            StepLabel("3", "किस दिन?")
            FilterChip(
                selected = days.size == 7,
                onClick = { days = if (days.size == 7) emptySet() else Reminder.ALL_DAYS },
                label = { Text("रोज़", fontSize = 18.sp) },
                leadingIcon = if (days.size == 7) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Days.ORDER.forEach { d ->
                    DayToggle(Days.short.getValue(d), d in days) {
                        days = if (d in days) days - d else days + d
                    }
                }
            }
            if (days.isEmpty()) {
                Text("कम से कम एक दिन चुनें", color = Danger, fontSize = 16.sp)
            }

            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { onPreview(text.ifBlank { "यह एक उदाहरण है" }) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("सुनकर देखें", fontSize = 18.sp)
            }
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            text = text.trim(),
                            hour = time.hour,
                            minute = time.minute,
                            days = days,
                            enabled = true,
                        )
                    )
                },
                enabled = text.isNotBlank() && days.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("सेव करें", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            if (!isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Danger)
                    Spacer(Modifier.width(6.dp))
                    Text("यह रिमाइंडर हटाएँ", color = Danger, fontSize = 18.sp)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("हटाना है?") },
            text = { Text("\"${initial.text}\" वाला रिमाइंडर हट जाएगा।") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("हाँ, हटाएँ", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("नहीं") }
            },
        )
    }
}

@Composable
private fun StepLabel(n: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(Saffron),
            contentAlignment = Alignment.Center,
        ) {
            Text(n, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DayToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) Saffron else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else Ink,
            maxLines = 1,
        )
    }
}

// ---- settings -----------------------------------------------------------------

@Composable
private fun SettingsDialog(
    callName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onTest: () -> Unit,
) {
    var name by remember { mutableStateOf(callName) }
    var tested by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("सेटिंग्स") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("रिमाइंडर किसे बुलाकर बोले?", fontSize = 16.sp, color = Muted)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("जैसे: मम्मी") },
                    textStyle = TextStyle(fontSize = 20.sp),
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { onTest(); tested = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("🔔 10 सेकंड में टेस्ट करें", fontSize = 16.sp) }
                if (tested) {
                    Text(
                        "अब फ़ोन लॉक करके इंतज़ार करें — 10 सेकंड में बोलेगा।",
                        fontSize = 15.sp,
                        color = DoneGreen,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text("सेव करें") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("बंद करें") } },
    )
}
