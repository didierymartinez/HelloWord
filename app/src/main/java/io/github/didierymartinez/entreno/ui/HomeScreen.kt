package io.github.didierymartinez.entreno.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@Composable
fun HomeScreen(history: List<SessionLog>, settings: Settings, onNavigate: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val now = remember(history) { System.currentTimeMillis() }
    val suggestion = remember(history, settings) { Stats.suggest(settings.rotation, history, now) }
    val groupDays = remember(history) { Stats.groupDaysSince(history, now) }
    val week = remember(history) { Stats.week(history, now) }
    val recent = remember(history) { history.sortedByDescending { it.startedAt }.take(7) }
    val lastHeart = remember(history) { history.filter { it.heart != null }.maxByOrNull { it.startedAt }?.heart }
    val muscleDays = remember(history) { Stats.muscleDaysSince(history, now) }
    var picking by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline("Entreno")
                        Text("RESUMEN DE\nENTRENO", style = MaterialTheme.typography.headlineMedium)
                    }
                    IconButton(
                        onClick = { onNavigate(Screen.SETTINGS) },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
                    ) { Icon(Icons.Default.Settings, contentDescription = "Ajustes") }
                }
            }
            item {
                TodayHero(
                    sessionNumber = history.size + 1,
                    suggestion = suggestion,
                    daysAgo = suggestion?.let { groupDays[it] },
                    onStart = { Graph.session.start(listOf(it)) },
                    onPick = { picking = true },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatTile(
                        label = "Entrenos",
                        value = "${week.sessions}",
                        sub = "en los últimos 7 días",
                        dark = true,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Volumen diario",
                        value = "",
                        sub = "kg por día · 7 días",
                        modifier = Modifier.weight(1.25f),
                    ) {
                        MiniBars(week.dailyVolume, week.dayInitials, Modifier.padding(top = 10.dp))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatTile(
                        label = "Pulso medio",
                        value = lastHeart?.overall?.avg?.toString() ?: "—",
                        unit = if (lastHeart != null) "ppm" else null,
                        sub = lastHeart?.let { "máx ${it.overall.max} · último entreno" } ?: "conecta Health Connect",
                        modifier = Modifier.weight(1f),
                        onClick = if (lastHeart == null) ({ onNavigate(Screen.SETTINGS) }) else null,
                    )
                    StatTile(
                        label = "Descanso",
                        value = "",
                        sub = "Toca para ver el mapa",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.RECOVERY) },
                    ) {
                        BodyMap(MapColors.recovery(muscleDays), Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    BigNumber(
                        value = Fmt.kg(week.volumeKg),
                        label = "kg levantados esta semana",
                        delta = week.changePct,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        RoundAction(Icons.Default.Share, "Compartir", { shareText(ctx, ShareText.recent(history, now)) })
                        RoundAction(Icons.AutoMirrored.Filled.List, "Historial", { onNavigate(Screen.HISTORY) })
                        RoundAction(Icons.Default.Favorite, "Descanso", { onNavigate(Screen.RECOVERY) })
                    }
                }
            }
            item { Overline("Últimos días", Modifier.padding(top = 6.dp)) }
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

@Composable
private fun TodayHero(
    sessionNumber: Int,
    suggestion: DayGroup?,
    daysAgo: Int?,
    onStart: (DayGroup) -> Unit,
    onPick: () -> Unit,
) {
    HeroCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("SESIÓN ", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text("%02d".format(sessionNumber), style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
        Text(
            if (suggestion != null) "HOY TOCA\n${suggestion.label.uppercase()}" else "¿QUÉ\nENTRENAMOS?",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp),
        )
        Overline(
            if (suggestion != null) "Plan flexible · última vez ${Fmt.daysAgo(daysAgo)}" else "Configura tu rotación en ajustes",
            color = Color.White.copy(alpha = 0.85f),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (suggestion != null) {
                Button(
                    onClick = { onStart(suggestion) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand.Purple),
                    modifier = Modifier.height(52.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Empezar", style = MaterialTheme.typography.titleMedium)
                }
            }
            TextButton(onClick = onPick, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text(if (suggestion != null) "Elegir otro grupo" else "Elegir grupo")
            }
        }
    }
}

@Composable
private fun SessionRow(s: SessionLog, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline(Fmt.day(s.startedAt))
                Text(
                    s.groups.joinToString(" + ") { it.label },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${Stats.setCount(s.exercises)} series · " + Fmt.minutes(((s.endedAt - s.startedAt) / 1000).toInt()) +
                        (s.heart?.let { " · ❤️ ${it.overall.avg} ppm" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Fmt.kg(Stats.volume(s.exercises)), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Overline("kg")
            }
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
