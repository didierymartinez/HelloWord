package io.github.didierymartinez.entreno.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.didierymartinez.entreno.Graph
import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.Settings
import io.github.didierymartinez.entreno.domain.Fmt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: Settings, onBack: () -> Unit) {
    fun update(f: (Settings) -> Settings) = Graph.repo.updateSettings(f)

    Scaffold(topBar = { BackTopBar("Ajustes", onBack) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle("Rotación de grupos")
            Text(
                "El orden en que se sugiere \"Hoy toca\". No va atada a días de la semana.",
                style = MaterialTheme.typography.bodySmall,
            )
            settings.rotation.forEachIndexed { i, g ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}. ${g.label}", Modifier.weight(1f))
                    IconButton(onClick = { update { it.copy(rotation = it.rotation.move(i, -1)) } }, enabled = i > 0) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
                    }
                    IconButton(
                        onClick = { update { it.copy(rotation = it.rotation.move(i, 1)) } },
                        enabled = i < settings.rotation.lastIndex,
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar")
                    }
                    IconButton(onClick = { update { it.copy(rotation = it.rotation - g) } }) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar")
                    }
                }
            }
            val missing = DayGroup.entries.filter { it !in settings.rotation }
            if (missing.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    missing.forEach { g ->
                        AssistChip(onClick = { update { it.copy(rotation = it.rotation + g) } }, label = { Text("+ ${g.label}") })
                    }
                }
            }

            SectionTitle("Bici estática")
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Stepper(
                    "Al inicio (min)",
                    "${settings.warmupMin}",
                    { update { it.copy(warmupMin = (it.warmupMin - 1).coerceAtLeast(0)) } },
                    { update { it.copy(warmupMin = it.warmupMin + 1) } },
                )
                Stepper(
                    "Al final (min)",
                    "${settings.cooldownMin}",
                    { update { it.copy(cooldownMin = (it.cooldownMin - 1).coerceAtLeast(0)) } },
                    { update { it.copy(cooldownMin = it.cooldownMin + 1) } },
                )
            }

            SectionTitle("Pesas")
            Stepper(
                "Descanso para ejercicios nuevos",
                Fmt.clock(settings.defaultRestSec.toLong()),
                { update { it.copy(defaultRestSec = (it.defaultRestSec - 15).coerceAtLeast(15)) } },
                { update { it.copy(defaultRestSec = it.defaultRestSec + 15) } },
                Modifier.fillMaxWidth(),
            )
            Text("Salto de peso con + / −")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.0, 2.5, 5.0).forEach { step ->
                    FilterChip(
                        selected = settings.weightStep == step,
                        onClick = { update { it.copy(weightStep = step) } },
                        label = { Text("${Fmt.kg(step)} kg") },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Avisar 10 s antes de terminar el descanso", Modifier.weight(1f))
                Switch(
                    checked = settings.warnBeforeRestEnd,
                    onCheckedChange = { v -> update { it.copy(warnBeforeRestEnd = v) } },
                )
            }

            SectionTitle("Reloj (Xiaomi Watch S1 Active)")
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Para que los avisos lleguen al reloj:")
                    Text("1. Abre Mi Fitness → tu reloj → Notificaciones de apps.")
                    Text("2. Activa las notificaciones y marca \"Entreno\".")
                    Text("3. En Android, quita la optimización de batería a Entreno y a Mi Fitness.")
                    Text("4. Pulsa el botón de abajo con la pantalla del teléfono apagada o bloqueada.")
                }
            }
            Button(onClick = { Graph.session.testAlert() }, modifier = Modifier.fillMaxWidth()) {
                Text("Enviar aviso de prueba al reloj")
            }
        }
    }
}

private fun <T> List<T>.move(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}
