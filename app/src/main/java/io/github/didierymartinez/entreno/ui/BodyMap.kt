package io.github.didierymartinez.entreno.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import io.github.didierymartinez.entreno.data.Muscle
import kotlin.math.min

/** Zona muscular en un cuerpo de 100 × 200 unidades. */
private data class Region(val muscle: Muscle, val back: Boolean, val cx: Float, val cy: Float, val rx: Float, val ry: Float)

private fun pair(m: Muscle, back: Boolean, cx: Float, cy: Float, rx: Float, ry: Float) =
    listOf(Region(m, back, cx, cy, rx, ry), Region(m, back, 100f - cx, cy, rx, ry))

private val regions: List<Region> = buildList {
    // Frente
    addAll(pair(Muscle.HOMBROS, false, 26f, 36f, 7f, 6f))
    addAll(pair(Muscle.PECHO, false, 41f, 44f, 9f, 7f))
    addAll(pair(Muscle.BICEPS, false, 22.5f, 56f, 4.5f, 9f))
    addAll(pair(Muscle.ANTEBRAZO, false, 22.5f, 82f, 4.5f, 10f))
    add(Region(Muscle.ABDOMEN, false, 50f, 72f, 9f, 15f))
    addAll(pair(Muscle.CUADRICEPS, false, 40.5f, 122f, 7f, 20f))
    // Espalda
    add(Region(Muscle.TRAPECIO, true, 50f, 34f, 12f, 6f))
    addAll(pair(Muscle.HOMBROS, true, 26f, 36f, 7f, 6f))
    addAll(pair(Muscle.DORSALES, true, 40f, 56f, 8f, 12f))
    addAll(pair(Muscle.TRICEPS, true, 22.5f, 56f, 4.5f, 9f))
    addAll(pair(Muscle.ANTEBRAZO, true, 22.5f, 82f, 4.5f, 10f))
    add(Region(Muscle.LUMBAR, true, 50f, 84f, 8f, 7f))
    addAll(pair(Muscle.GLUTEOS, true, 41f, 102f, 8f, 8f))
    addAll(pair(Muscle.ISQUIOS, true, 40.5f, 130f, 7f, 16f))
    addAll(pair(Muscle.GEMELOS, true, 40.5f, 164f, 6f, 13f))
}

/** Mapa corporal frente/espalda. Los músculos sin color se dibujan neutros. */
@Composable
fun BodyMap(colors: Map<Muscle, Color>, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val idle = MaterialTheme.colorScheme.outlineVariant
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(210f / 200f)) {
            val sc = min(size.width / 210f, size.height / 200f)
            for (fig in 0..1) {
                val ox = fig * 110f
                fun at(x: Float, y: Float) = Offset((ox + x) * sc, y * sc)
                fun box(w: Float, h: Float) = Size(w * sc, h * sc)
                val r = CornerRadius(8f * sc)

                drawCircle(base, radius = 10f * sc, center = at(50f, 13f))
                drawRect(base, topLeft = at(45f, 20f), size = box(10f, 8f))
                drawRoundRect(base, topLeft = at(30f, 26f), size = box(40f, 70f), cornerRadius = r)
                drawRoundRect(base, topLeft = at(16f, 30f), size = box(13f, 72f), cornerRadius = r)
                drawRoundRect(base, topLeft = at(71f, 30f), size = box(13f, 72f), cornerRadius = r)
                drawRoundRect(base, topLeft = at(31f, 92f), size = box(18f, 98f), cornerRadius = r)
                drawRoundRect(base, topLeft = at(51f, 92f), size = box(18f, 98f), cornerRadius = r)

                for (reg in regions) {
                    if (reg.back != (fig == 1)) continue
                    drawOval(
                        color = colors[reg.muscle] ?: idle,
                        topLeft = at(reg.cx - reg.rx, reg.cy - reg.ry),
                        size = box(2 * reg.rx, 2 * reg.ry),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Frente", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            Text("Espalda", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
        }
    }
}
