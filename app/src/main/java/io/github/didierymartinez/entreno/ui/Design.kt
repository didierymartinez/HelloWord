package io.github.didierymartinez.entreno.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.didierymartinez.entreno.R

/** Etiqueta corta en mayúsculas sobre un dato ("ENTRENOS", "HOY TOCA"…). */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
}

/** Tarjeta blanca sin sombra, la base de casi todas las pantallas. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = container)
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors) { Column(Modifier.padding(18.dp), content = content) }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors) { Column(Modifier.padding(18.dp), content = content) }
    }
}

/** Gradiente morado de las tarjetas destacadas (sustituye a las fotos del diseño). */
@Composable
fun heroBrush(): Brush = if (isSystemInDarkTheme()) {
    Brush.linearGradient(listOf(Color(0xFF4A3FB8), Color(0xFF2B2A45)))
} else {
    Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF9C8CFF), Color(0xFFC3B8FF)))
}

/** Tarjeta grande con degradado y una pesa decorativa de fondo. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(heroBrush()),
    ) {
        Decoration()
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun BoxScope.Decoration() {
    Icon(
        painter = painterResource(R.drawable.ic_stat_entreno),
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 12.dp, end = 12.dp)
            .size(132.dp)
            .alpha(0.14f),
    )
}

/** Número destacado con su etiqueta. [dark] usa el azul noche del diseño. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    sub: String? = null,
    dark: Boolean = false,
    onClick: (() -> Unit)? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val container = if (dark) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val ink = if (dark) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
    val muted = if (dark) ink.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    SoftCard(modifier, onClick, container) {
        Overline(label, color = if (dark) ink else MaterialTheme.colorScheme.onSurfaceVariant)
        if (value.isNotBlank()) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                Text(value, style = MaterialTheme.typography.displaySmall, color = if (dark) Color(0xFFB9F1FF) else ink)
                unit?.let {
                    Text(" $it", style = MaterialTheme.typography.titleSmall, color = muted, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }
        sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = muted) }
        extra()
    }
}

/**
 * Barras pequeñas (volumen por día). Una sola serie en naranja: las barras sin
 * dato se dibujan como un punto neutro para que se vea el día vacío.
 */
@Composable
fun MiniBars(values: List<Double>, labels: List<String>, modifier: Modifier = Modifier) {
    val bar = MaterialTheme.colorScheme.secondary.let { if (isSystemInDarkTheme()) it else Color(0xFFFF9A52) }
    val empty = MaterialTheme.colorScheme.outlineVariant
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val n = values.size.coerceAtLeast(1)
            val gap = 6.dp.toPx()
            val w = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, v ->
                val x = i * (w + gap)
                val h = if (v <= 0) w.coerceAtMost(8.dp.toPx()) else (v / max).toFloat() * size.height
                drawRoundRect(
                    color = if (v <= 0) empty else bar,
                    topLeft = Offset(x, size.height - h),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(w / 2, w / 2),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "8.540 ⁺¹²%  KG LEVANTADOS ESTA SEMANA". */
@Composable
fun BigNumber(value: String, label: String, delta: Int?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text(value, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
            delta?.let {
                Text(
                    (if (it >= 0) "+" else "") + "$it%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }
        }
        Overline(label, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Botón redondo morado con texto debajo ("Compartir", "Historial"…). */
@Composable
fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
    }
}
