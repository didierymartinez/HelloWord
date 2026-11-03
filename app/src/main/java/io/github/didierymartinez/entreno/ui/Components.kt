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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
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

/** Paleta: lavanda claro, morado como acento, naranja para datos y un azul noche para destacar. */
object Brand {
    val Purple = Color(0xFF7B6CF6)
    val PurpleLight = Color(0xFFA99DFF)
    val Lavender = Color(0xFFEEEDFB)
    val Orange = Color(0xFFFF8A3D)
    val OrangeLight = Color(0xFFFFB27A)
    val Night = Color(0xFF2B2A45)
    val NightDeep = Color(0xFF1B1A2E)
    val Ink = Color(0xFF26243A)
}

@Composable
fun EntrenoTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Brand.PurpleLight,
            onPrimary = Color(0xFF221A5C),
            primaryContainer = Color(0xFF332D5E),
            onPrimaryContainer = Color(0xFFE6E1FF),
            secondary = Brand.OrangeLight,
            onSecondary = Color(0xFF3D1C00),
            secondaryContainer = Color(0xFF3A2C3A),
            onSecondaryContainer = Color(0xFFFFDCC4),
            tertiary = Color(0xFF8FD3B0),
            tertiaryContainer = Color(0xFF2C2B48),
            onTertiaryContainer = Color(0xFFE6E4FF),
            background = Color(0xFF131221),
            onBackground = Color(0xFFE7E5F5),
            surface = Color(0xFF131221),
            onSurface = Color(0xFFE7E5F5),
            surfaceVariant = Color(0xFF2A2840),
            onSurfaceVariant = Color(0xFFBDB9D6),
            surfaceContainerLowest = Color(0xFF0E0D19),
            surfaceContainerLow = Color(0xFF1A1929),
            surfaceContainer = Color(0xFF1E1D2F),
            surfaceContainerHigh = Color(0xFF242338),
            surfaceContainerHighest = Color(0xFF2E2C44),
            outline = Color(0xFF6E6A8A),
            outlineVariant = Color(0xFF3B3955),
        )
    } else {
        lightColorScheme(
            primary = Brand.Purple,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8E4FF),
            onPrimaryContainer = Color(0xFF221A5C),
            secondary = Color(0xFFE8711F),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFEBDD),
            onSecondaryContainer = Color(0xFF3D1C00),
            tertiary = Color(0xFF2E8B62),
            tertiaryContainer = Brand.Night,
            onTertiaryContainer = Color.White,
            background = Brand.Lavender,
            onBackground = Brand.Ink,
            surface = Brand.Lavender,
            onSurface = Brand.Ink,
            surfaceVariant = Color(0xFFE6E4F4),
            onSurfaceVariant = Color(0xFF6B6886),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFFAF9FF),
            surfaceContainer = Color.White,
            surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color(0xFFF3F1FD),
            outline = Color(0xFFB4B1CC),
            outlineVariant = Color(0xFFDDDAEE),
        )
    }
    val base = Typography()
    val typography = base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.8.sp),
        labelMedium = base.labelMedium.copy(letterSpacing = 0.8.sp),
    )
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
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
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
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
