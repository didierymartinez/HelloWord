package io.github.didierymartinez.entreno.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.didierymartinez.entreno.Graph
import io.github.didierymartinez.entreno.data.Catalog
import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.SessionLog
import io.github.didierymartinez.entreno.data.Settings
import io.github.didierymartinez.entreno.domain.Fmt
import io.github.didierymartinez.entreno.domain.Stats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(history: List<SessionLog>, settings: Settings, onNavigate: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val now = remember(history) { System.currentTimeMillis() }
    val suggestion = remember(history, settings) { Stats.suggest(settings.rotation, history, now) }
    val groupDays = remember(history) { Stats.groupDaysSince(history, now) }
    val recent = remember(history) { history.sortedByDescending { it.startedAt }.take(7) }
    var picking by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entreno") },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TodayCard(
                    suggestion = suggestion,
                    groupDays = groupDays,
                    rotation = settings.rotation,
                    onStart = { Graph.session.start(listOf(it)) },
                    onPick = { picking = true },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onNavigate(Screen.RECOVERY) }, Modifier.weight(1f)) { Text("Mapa de descanso") }
                    OutlinedButton(onClick = { onNavigate(Screen.HISTORY) }, Modifier.weight(1f)) { Text("Historial") }
                }
            }
            item {
                OutlinedButton(
                    onClick = { shareText(ctx, ShareText.recent(history, now)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir con el entrenador")
                }
            }
            item { SectionTitle("Últimos días", Modifier.padding(top = 8.dp)) }
            if (recent.isEmpty()) {
                item { Text("Aún no hay entrenamientos. ¡Empieza el primero!") }
            }
            items(recent, key = { it.id }) { s -> SessionRow(s) { onNavigate(Screen.HISTORY) } }
        }
    }

    if (picking) {
        GroupPickerDialog(
            initial = listOfNotNull(suggestion),
            onDismiss = { picking = false },
            onStart = {
                picking = false
                Graph.session.start(it)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayCard(
    suggestion: DayGroup?,
    groupDays: Map<DayGroup, Int>,
    rotation: List<DayGroup>,
    onStart: (DayGroup) -> Unit,
    onPick: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("HOY TOCA", style = MaterialTheme.typography.labelLarge)
            if (suggestion != null) {
                Text(suggestion.label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Última vez: ${Fmt.daysAgo(groupDays[suggestion])}")
                Button(onClick = { onStart(suggestion) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Empezar", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text("Configura tu rotación en Ajustes, o elige un grupo.")
            }
            TextButton(onClick = onPick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Elegir otro grupo")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rotation.forEach { g ->
                    val d = groupDays[g]
                    Text(
                        "${g.label}: ${if (d == null) "—" else "${d} d"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(s: SessionLog, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "${Fmt.day(s.startedAt)} · ${s.groups.joinToString(" + ") { it.label }}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${s.exercises.size} ejercicios · ${Stats.setCount(s.exercises)} series · " +
                    Fmt.minutes(((s.endedAt - s.startedAt) / 1000).toInt()) +
                    (s.heart?.let { " · ❤️ ${it.overall.avg} ppm" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupPickerDialog(initial: List<DayGroup>, onDismiss: () -> Unit, onStart: (List<DayGroup>) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Qué vas a entrenar?") },
        text = {
            Column {
                Text("Puedes elegir más de uno (por ejemplo, lo que diga el entrenador).")
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayGroup.entries.forEach { g ->
                        FilterChip(
                            selected = g in selected,
                            onClick = { selected = if (g in selected) selected - g else selected + g },
                            label = { Text(g.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onStart(selected) }, enabled = selected.isNotEmpty()) { Text("Empezar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Resumen en texto para mandarle al entrenador (WhatsApp, etc.). */
object ShareText {
    fun recent(history: List<SessionLog>, now: Long, days: Int = 14): String {
        val since = now - days * 86_400_000L
        val list = history.filter { it.startedAt >= since }.sortedBy { it.startedAt }
        if (list.isEmpty()) return "No he entrenado en los últimos $days días."
        return buildString {
            appendLine("Mis entrenos (últimos $days días):")
            for (s in list) {
                appendLine()
                appendLine("${Fmt.day(s.startedAt)} · ${s.groups.joinToString(" + ") { it.label }}")
                s.warmup?.let { appendLine("  🚴 Bici ${Fmt.minutes(it.durationSec)}") }
                for (e in s.exercises) appendLine("  • ${Catalog.name(e.exerciseId)}: ${Fmt.setsLine(e.sets)}")
                s.cooldown?.let { appendLine("  🚴 Bici ${Fmt.minutes(it.durationSec)}") }
                s.heart?.let { appendLine("  ❤️ Pulso medio ${it.overall.avg} · máx ${it.overall.max} ppm") }
            }
        }.trimEnd()
    }
}

fun shareText(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    ctx.startActivity(Intent.createChooser(send, "Compartir"))
}
