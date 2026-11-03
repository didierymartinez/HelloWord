package io.github.didierymartinez.entreno.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.didierymartinez.entreno.Graph
import io.github.didierymartinez.entreno.data.CardioLog
import io.github.didierymartinez.entreno.data.Catalog
import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.SessionLog
import io.github.didierymartinez.entreno.domain.Fmt
import io.github.didierymartinez.entreno.domain.Stats

@Composable
fun HistoryScreen(history: List<SessionLog>, onBack: () -> Unit) {
    val sorted = remember(history) { history.sortedByDescending { it.startedAt } }
    var expanded by remember { mutableStateOf(sorted.firstOrNull()?.id?.let { setOf(it) } ?: emptySet()) }
    var toDelete by remember { mutableStateOf<SessionLog?>(null) }

    Scaffold(topBar = { BackTopBar("Historial", onBack) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (sorted.isEmpty()) item { Text("Todavía no hay sesiones guardadas.") }
            items(sorted, key = { it.id }) { s ->
                val open = s.id in expanded
                Card(
                    onClick = { expanded = if (open) expanded - s.id else expanded + s.id },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${Fmt.day(s.startedAt)} · ${s.groups.joinToString(" + ") { it.label }}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${Fmt.minutes(((s.endedAt - s.startedAt) / 1000).toInt())} · " +
                                "${Stats.setCount(s.exercises)} series · ${Fmt.kg(Stats.volume(s.exercises))} kg de volumen" +
                                (s.heart?.let { " · ❤️ ${it.overall.avg}/${it.overall.max}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (open) {
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            s.warmup?.let { Text(cardioLine("🚴 Bici inicio", it)) }
                            s.exercises.forEach { e ->
                                Text(Catalog.name(e.exerciseId), fontWeight = FontWeight.Medium)
                                Text(Fmt.setsLine(e.sets), style = MaterialTheme.typography.bodySmall)
                            }
                            s.cooldown?.let { Text(cardioLine("🚴 Bici final", it)) }
                            HeartSection(s, saved = s.heart, onFound = { Graph.repo.updateHeart(s.id, it) })
                            TextButton(onClick = { toDelete = s }) { Text("Borrar sesión") }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar sesión?") },
            text = { Text("Se borrará la sesión del ${Fmt.day(s.startedAt)}.") },
            confirmButton = {
                TextButton(onClick = {
                    Graph.repo.deleteSession(s.id)
                    toDelete = null
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
        )
    }
}

fun cardioLine(label: String, c: CardioLog): String = buildString {
    append("$label: ${Fmt.minutes(c.durationSec)}")
    c.distanceKm?.let { append(" · ${Fmt.kg(it)} km") }
    c.kcal?.let { append(" · $it kcal") }
}

@Composable
fun RecoveryScreen(history: List<SessionLog>, onBack: () -> Unit) {
    val now = remember(history) { System.currentTimeMillis() }
    val muscleDays = remember(history) { Stats.muscleDaysSince(history, now) }
    val groupDays = remember(history) { Stats.groupDaysSince(history, now) }

    Scaffold(topBar = { BackTopBar("Mapa de descanso", onBack) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Cuánto hace que trabajaste cada músculo. Enséñaselo al entrenador para decidir qué toca.")
            BodyMap(MapColors.recovery(muscleDays), Modifier.widthIn(max = 340.dp).align(Alignment.CenterHorizontally))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(MapColors.fresh, "≤ 1 día")
                Legend(MapColors.mid, "2–3 días")
                Legend(MapColors.hot, "4+ o nunca")
            }
            SectionTitle("Por grupo")
            DayGroup.entries.forEach { g ->
                Row(Modifier.fillMaxWidth()) {
                    Text(g.label, Modifier.weight(1f))
                    Text(Fmt.daysAgo(groupDays[g]), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
