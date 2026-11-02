package io.github.didierymartinez.entreno.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.didierymartinez.entreno.data.Muscle
import io.github.didierymartinez.entreno.domain.Fmt
import kotlinx.coroutines.delay

private val Orange = Color(0xFFFF7043)
private val OrangeDark = Color(0xFFD84315)

@Composable
fun EntrenoTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Orange,
            onPrimary = Color(0xFF3A0B00),
            primaryContainer = Color(0xFF4A2418),
            onPrimaryContainer = Color(0xFFFFDBCF),
            secondary = Color(0xFFFFB74D),
            secondaryContainer = Color(0xFF3A3328),
            onSecondaryContainer = Color(0xFFF5E1C8),
            tertiary = Color(0xFF81C784),
            tertiaryContainer = Color(0xFF213A26),
            onTertiaryContainer = Color(0xFFD4F0D6),
        )
    } else {
        lightColorScheme(
            primary = OrangeDark,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDBCF),
            onPrimaryContainer = Color(0xFF3A0B00),
            secondary = Color(0xFFEF6C00),
            secondaryContainer = Color(0xFFFFE8CC),
            onSecondaryContainer = Color(0xFF2E1A00),
            tertiary = Color(0xFF2E7D32),
            tertiaryContainer = Color(0xFFC8E6C9),
            onTertiaryContainer = Color(0xFF0B2E0F),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

object MapColors {
    val hot = Color(0xFFE53935)
    val warm = Color(0xFFFFA726)
    val mid = Color(0xFFFFB300)
    val fresh = Color(0xFF43A047)

    /** Carga de una sesión: naranja (poco) → rojo (mucho). */
    fun load(load: Map<Muscle, Float>): Map<Muscle, Color> =
        load.filterValues { it > 0f }.mapValues { lerp(warm, hot, it.value) }

    /** Primarios en rojo, secundarios en naranja. */
    fun exercise(primary: List<Muscle>, secondary: List<Muscle>): Map<Muscle, Color> =
        secondary.associateWith { warm } + primary.associateWith { hot }

    /** Descanso: verde ≤1 día, amarillo 2–3, rojo 4+ o nunca. */
    fun recovery(days: Map<Muscle, Int>): Map<Muscle, Color> = Muscle.entries.associateWith {
        val d = days[it]
        when {
            d == null || d >= 4 -> hot
            d >= 2 -> mid
            else -> fresh
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
        },
    )
}

@Composable
fun Stepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        RoundButton("−", onMinus)
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 88.dp).padding(horizontal = 6.dp),
        )
        RoundButton("+", onPlus)
    }
}

@Composable
private fun RoundButton(text: String, onClick: () -> Unit, size: Dp = 52.dp) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) { Text(text, fontSize = 26.sp) }
}

@Composable
fun CountdownRing(
    remainingMs: Long,
    totalMs: Long,
    label: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 230.dp,
) {
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { if (totalMs <= 0) 0f else (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 14.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                Fmt.clock((remainingMs + 999) / 1000),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

/** Hora actual que se refresca 4 veces por segundo (para los contadores). */
@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(250)
    }
}

/** Mantiene la pantalla encendida mientras la sesión está abierta. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
