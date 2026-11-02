package io.github.didierymartinez.entreno.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.didierymartinez.entreno.Graph
import io.github.didierymartinez.entreno.data.ActiveSession
import io.github.didierymartinez.entreno.data.Catalog
import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.Exercise
import io.github.didierymartinez.entreno.data.Phase
import io.github.didierymartinez.entreno.data.PlanItem
import io.github.didierymartinez.entreno.data.SessionLog
import io.github.didierymartinez.entreno.data.Settings
import io.github.didierymartinez.entreno.data.current
import io.github.didierymartinez.entreno.domain.Fmt
import io.github.didierymartinez.entreno.domain.SessionEngine
import io.github.didierymartinez.entreno.domain.Stats
import kotlinx.coroutines.delay

private enum class PickerMode { SWAP, ADD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(s: ActiveSession, history: List<SessionLog>, settings: Settings) {
    if (settings.keepScreenOn) KeepScreenOn()
    val now by rememberNow()
    LaunchedEffect(Unit) {
        while (true) {
            Graph.session.tick()
            delay(500)
        }
    }
    var menu by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.groups.joinToString(" + ") { it.label })
                        val end = s.endedAt ?: now
                        Text("⏱ ${Fmt.clock((end - s.startedAt) / 1000)}", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Más") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (s.phase != Phase.SUMMARY) {
                            DropdownMenuItem(
                                text = { Text("Terminar sesión") },
                                onClick = {
                                    menu = false
                                    Graph.session.finishSession()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Descartar sesión") },
                            onClick = {
                                menu = false
                                confirmDiscard = true
                            },
                        )
                    }
                },
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (s.phase) {
                Phase.WARMUP, Phase.COOLDOWN -> CardioPanel(s, settings, now)
                Phase.STRENGTH, Phase.REST -> StrengthPanel(s, history, settings, now)
                Phase.SUMMARY -> SummaryPanel(s, history, now)
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Descartar la sesión?") },
            text = { Text("No se guardará nada de esta sesión.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    Graph.session.discard()
                }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancelar") } },
        )
    }
}

// ---------- Bici ----------

@Composable
private fun CardioPanel(s: ActiveSession, settings: Settings, now: Long) {
    val warm = s.phase == Phase.WARMUP
    val minutes = if (warm) settings.warmupMin else settings.cooldownMin
    val t = s.timer
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (warm) "🚴 Calentamiento" else "🚴 Bici final",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        when {
            s.cardioForm -> CardioForm(durationSec = (if (warm) s.warmup else s.cooldown)?.durationSec ?: 0)
            t == null -> {
                Text("Bici estática · $minutes min", style = MaterialTheme.typography.titleLarge)
                Text(
                    "El reloj vibrará a la mitad y al terminar. Si quieres el pulso, inicia también " +
                        "\"Bicicleta estática\" en el reloj.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { Graph.session.startCardio() }, Modifier.fillMaxWidth().height(64.dp)) {
                    Text("▶ Empezar bici", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = { Graph.session.skipCardio() }, Modifier.fillMaxWidth()) {
                    Text(if (warm) "Saltar e ir a las pesas" else "Saltar y terminar")
                }
            }
            else -> {
                CountdownRing(t.remaining(now), t.totalSec * 1000L, if (t.running) "Restante" else "En pausa")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (t.running) {
                        OutlinedButton(onClick = { Graph.session.pauseTimer() }) { Text("Pausa") }
                    } else {
                        Button(onClick = { Graph.session.resumeTimer() }) { Text("Reanudar") }
                    }
                    Button(onClick = { Graph.session.finishCardio() }) { Text("Terminar") }
                }
            }
        }
    }
}

@Composable
private fun CardioForm(durationSec: Int) {
    var km by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    Text("Hecho: ${Fmt.clock(durationSec.toLong())}", style = MaterialTheme.typography.titleLarge)
    Text("Si quieres, anota lo que marca la bici (opcional):")
    OutlinedTextField(
        value = km,
        onValueChange = { v -> km = v.filter { it.isDigit() || it == '.' || it == ',' } },
        label = { Text("Distancia (km)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = kcal,
        onValueChange = { v -> kcal = v.filter { it.isDigit() } },
        label = { Text("Calorías") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = {
            Graph.session.saveCardioForm(
                km.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 },
                kcal.toIntOrNull()?.takeIf { it > 0 },
            )
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) { Text("Guardar y seguir") }
    TextButton(onClick = { Graph.session.saveCardioForm(null, null) }) { Text("Omitir") }
}

// ---------- Pesas ----------

@Composable
private fun StrengthPanel(s: ActiveSession, history: List<SessionLog>, settings: Settings, now: Long) {
    var picker by remember { mutableStateOf<PickerMode?>(null) }
    val cur = s.current()
    val hasSets = s.plan.any { it.sets.isNotEmpty() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val t = s.timer
        if (s.phase == Phase.REST && t != null) {
            item { RestCard(t.remaining(now), t.totalSec * 1000L) }
        }
        item {
            if (cur == null) {
                AllDoneCard(settings, onAdd = { picker = PickerMode.ADD })
            } else {
                ExerciseCard(s, cur, history, settings, resting = s.phase == Phase.REST, onSwap = { picker = PickerMode.SWAP })
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Plan de hoy", Modifier.weight(1f))
                TextButton(onClick = { picker = PickerMode.ADD }) { Text("+ Añadir ejercicio") }
            }
        }
        items(s.plan, key = { it.uid }) { p -> PlanRow(p, p.uid == s.currentUid) { Graph.session.select(p.uid) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasSets) {
                    TextButton(onClick = { Graph.session.undoLastSet() }) { Text("↩ Deshacer última serie") }
                }
                if (cur != null) {
                    OutlinedButton(onClick = { Graph.session.goToCooldown() }, Modifier.fillMaxWidth()) {
                        Text("🚴 Terminar pesas e ir a la bici final")
                    }
                }
            }
        }
    }

    when (picker) {
        PickerMode.SWAP -> cur?.let { item ->
            ExercisePicker(
                title = "Cambiar ${Catalog.name(item.exerciseId)} por…",
                suggested = Catalog.alternatives(item.exerciseId),
                initialGroup = Catalog.byId[item.exerciseId]?.group ?: s.groups.first(),
                onPick = {
                    picker = null
                    Graph.session.swap(it.id)
                },
                onDismiss = { picker = null },
            )
        }
        PickerMode.ADD -> ExercisePicker(
            title = "Añadir ejercicio",
            suggested = emptyList(),
            initialGroup = s.groups.firstOrNull() ?: DayGroup.PECHO,
            onPick = {
                picker = null
                Graph.session.add(it.id)
            },
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}

@Composable
private fun RestCard(remainingMs: Long, totalMs: Long) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CountdownRing(remainingMs, totalMs, "Descanso", diameter = 180.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { Graph.session.addRest(30) }) { Text("+30 s") }
                Button(onClick = { Graph.session.skipRest() }) { Text("Saltar descanso") }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    s: ActiveSession,
    item: PlanItem,
    history: List<SessionLog>,
    settings: Settings,
    resting: Boolean,
    onSwap: () -> Unit,
) {
    val ex = Catalog.byId[item.exerciseId]
    val last = remember(item.exerciseId, history) { Stats.lastSetsFor(item.exerciseId, history) }
    val setNo = item.sets.size + 1
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (resting) Text("SIGUIENTE", style = MaterialTheme.typography.labelLarge)
            Text(Catalog.name(item.exerciseId), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (ex != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Principal: ${ex.primary.joinToString { it.label }}", style = MaterialTheme.typography.bodySmall)
                        if (ex.secondary.isNotEmpty()) {
                            Text("Secundario: ${ex.secondary.joinToString { it.label }}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    BodyMap(MapColors.exercise(ex.primary, ex.secondary), Modifier.width(130.dp))
                }
            }
            Text(
                if (setNo <= item.targetSets) "Serie $setNo de ${item.targetSets}" else "Serie extra ($setNo)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Stepper(
                "Reps",
                "${s.draftReps}",
                { Graph.session.setDraft(s.draftReps - 1, s.draftWeight) },
                { Graph.session.setDraft(s.draftReps + 1, s.draftWeight) },
                Modifier.fillMaxWidth(),
            )
            Stepper(
                if (ex?.bodyweight == true && s.draftWeight <= 0.0) "Lastre (kg)" else "Peso (kg)",
                Fmt.kg(s.draftWeight),
                { Graph.session.setDraft(s.draftReps, s.draftWeight - settings.weightStep) },
                { Graph.session.setDraft(s.draftReps, s.draftWeight + settings.weightStep) },
                Modifier.fillMaxWidth(),
            )
            if (item.sets.isNotEmpty()) Text("Hoy: ${Fmt.setsLine(item.sets)}", style = MaterialTheme.typography.bodyMedium)
            last?.let { (session, sets) ->
                Text("Última vez (${Fmt.day(session.startedAt)}): ${Fmt.setsLine(sets)}", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { Graph.session.completeSet() }, Modifier.fillMaxWidth().height(72.dp)) {
                Text("✓ SERIE HECHA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onSwap) { Text("Cambiar") }
                TextButton(onClick = { Graph.session.skipExercise() }) { Text("Saltar") }
                TextButton(onClick = { Graph.session.finishExercise() }) { Text("Siguiente ej.") }
            }
        }
    }
}

@Composable
private fun AllDoneCard(settings: Settings, onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("💪 ¡Pesas completas!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (settings.cooldownMin > 0) {
                Button(onClick = { Graph.session.goToCooldown() }, Modifier.fillMaxWidth().height(56.dp)) {
                    Text("🚴 Bici final (${settings.cooldownMin} min)")
                }
            }
            OutlinedButton(onClick = onAdd, Modifier.fillMaxWidth()) { Text("Añadir otro ejercicio") }
            TextButton(onClick = { Graph.session.finishSession() }) { Text("Terminar sin bici") }
        }
    }
}

@Composable
private fun PlanRow(p: PlanItem, isCurrent: Boolean, onClick: () -> Unit) {
    val status = when {
        isCurrent -> "▶"
        p.finished -> "✓"
        p.skipped -> "⏭"
        else -> "○"
    }
    ListItem(
        headlineContent = { Text(Catalog.name(p.exerciseId), fontWeight = if (isCurrent) FontWeight.Bold else null) },
        supportingContent = {
            Text("${p.sets.size}/${p.targetSets} series · ${p.targetReps} reps" + if (p.weightKg > 0) " · ${Fmt.kg(p.weightKg)} kg" else "")
        },
        leadingContent = { Text(status, style = MaterialTheme.typography.titleLarge) },
        colors = ListItemDefaults.colors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePicker(
    title: String,
    suggested: List<Exercise>,
    initialGroup: DayGroup,
    onPick: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var group by remember { mutableStateOf(initialGroup) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayGroup.entries.forEach { g ->
                        FilterChip(selected = g == group, onClick = { group = g }, label = { Text(g.label) })
                    }
                }
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    if (suggested.isNotEmpty()) {
                        item { SectionTitle("Mismo músculo") }
                        items(suggested, key = { "s-" + it.id }) { ex -> PickerRow(ex, onPick) }
                        item { HorizontalDivider(Modifier.padding(vertical = 6.dp)) }
                        item { SectionTitle(group.label) }
                    }
                    items(Catalog.byGroup(group), key = { it.id }) { ex -> PickerRow(ex, onPick) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun PickerRow(ex: Exercise, onPick: (Exercise) -> Unit) {
    ListItem(
        headlineContent = { Text(ex.name) },
        supportingContent = { Text(ex.primary.joinToString { it.label }) },
        modifier = Modifier.clickable { onPick(ex) },
    )
}

// ---------- Resumen ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryPanel(s: ActiveSession, history: List<SessionLog>, now: Long) {
    val log = remember(s) { SessionEngine.toLog(s, s.endedAt ?: now) }
    val prs = remember(log, history) { Stats.prs(log, history) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("¡Sesión terminada!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Duración", Fmt.minutes(((log.endedAt - log.startedAt) / 1000).toInt()))
            Stat("Series", "${Stats.setCount(log.exercises)}")
            Stat("Volumen", "${Fmt.kg(Stats.volume(log.exercises))} kg")
            log.warmup?.let { Stat("Bici inicio", Fmt.minutes(it.durationSec)) }
            log.cooldown?.let { Stat("Bici final", Fmt.minutes(it.durationSec)) }
        }
        if (prs.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(14.dp).fillMaxWidth()) {
                    prs.forEach { (id, w) -> Text("🏆 Récord: ${Catalog.name(id)} · ${Fmt.kg(w)} kg", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        SectionTitle("Músculos trabajados")
        BodyMap(
            MapColors.load(Stats.muscleLoad(log.exercises)),
            Modifier.widthIn(max = 300.dp).align(Alignment.CenterHorizontally),
        )
        SectionTitle("Ejercicios")
        if (log.exercises.isEmpty()) Text("No se registraron series.")
        log.exercises.forEach { e ->
            Column {
                Text(Catalog.name(e.exerciseId), fontWeight = FontWeight.Medium)
                Text(Fmt.setsLine(e.sets), style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = { Graph.session.saveAndClose() }, Modifier.fillMaxWidth().height(56.dp)) { Text("Guardar sesión") }
        OutlinedButton(onClick = { Graph.session.reopen() }, Modifier.fillMaxWidth()) { Text("Seguir entrenando") }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
