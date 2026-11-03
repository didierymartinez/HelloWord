package io.github.didierymartinez.entreno.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.didierymartinez.entreno.Graph
import io.github.didierymartinez.entreno.data.HeartData
import io.github.didierymartinez.entreno.data.HrPoint
import io.github.didierymartinez.entreno.data.SessionLog
import io.github.didierymartinez.entreno.domain.Heart
import io.github.didierymartinez.entreno.domain.Segment
import io.github.didierymartinez.entreno.domain.SegmentKind
import io.github.didierymartinez.entreno.session.HealthConnect
import kotlinx.coroutines.launch
import kotlin.math.abs

private sealed interface HrUi {
    data object Loading : HrUi
    data object Idle : HrUi
    data object NeedsPermission : HrUi
    data class Unavailable(val availability: HealthConnect.Availability) : HrUi
    data object NoData : HrUi
    data class Error(val message: String) : HrUi
}

/**
 * Pulso de una sesión. Lo busca en Health Connect si aún no estaba guardado
 * y avisa con [onFound] para guardarlo.
 */
@Composable
fun HeartSection(log: SessionLog, saved: HeartData?, onFound: (HeartData) -> Unit) {
    val ctx = LocalContext.current
    val hc = Graph.health
    val scope = rememberCoroutineScope()
    var data by remember(log.id) { mutableStateOf(saved) }
    var ui by remember(log.id) { mutableStateOf<HrUi>(if (saved != null) HrUi.Idle else HrUi.Loading) }

    val refresh: () -> Unit = {
        scope.launch {
            ui = HrUi.Loading
            ui = when (val r = hc.fetch(log)) {
                is HealthConnect.Result.Found -> {
                    data = r.data
                    onFound(r.data)
                    HrUi.Idle
                }
                HealthConnect.Result.NoData -> if (data != null) HrUi.Idle else HrUi.NoData
                HealthConnect.Result.NoPermission -> HrUi.NeedsPermission
                is HealthConnect.Result.Unavailable -> HrUi.Unavailable(r.availability)
                is HealthConnect.Result.Failed -> HrUi.Error(r.message)
            }
        }
    }
    val askPermission = rememberLauncherForActivityResult(hc.permissionContract()) { refresh() }
    LaunchedEffect(log.id) { if (saved == null) refresh() }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("❤️ Pulso del reloj", Modifier.weight(1f))
                if (ui == HrUi.Idle || ui == HrUi.NoData) TextButton(onClick = refresh) { Text("Actualizar") }
            }
            when (val state = ui) {
                HrUi.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                HrUi.Idle -> Unit
                HrUi.NeedsPermission -> {
                    Text("Permite que Entreno lea el pulso y las calorías que Mi Fitness guarda en Health Connect.")
                    Button(onClick = { askPermission.launch(hc.permissions) }, Modifier.fillMaxWidth()) {
                        Text("Conectar Health Connect")
                    }
                    TextButton(onClick = { runCatching { ctx.startActivity(hc.openIntent()) } }) {
                        Text("Abrir Health Connect")
                    }
                }
                is HrUi.Unavailable -> {
                    Text(
                        if (state.availability == HealthConnect.Availability.NEEDS_UPDATE) {
                            "Health Connect necesita actualizarse."
                        } else {
                            "Este teléfono no tiene Health Connect. Instálalo desde Play Store."
                        },
                    )
                    Button(onClick = { runCatching { ctx.startActivity(hc.openIntent()) } }) { Text("Abrir Play Store") }
                }
                HrUi.NoData -> Text(
                    "Aún no hay pulso de esta sesión. Abre Mi Fitness para que sincronice el reloj y pulsa " +
                        "Actualizar. Si sigue vacío, revisa que Mi Fitness comparta la frecuencia cardíaca " +
                        "con Health Connect.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                is HrUi.Error -> Text("No se pudo leer Health Connect: ${state.message}")
            }
            data?.let { HeartDetails(log, it) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeartDetails(log: SessionLog, data: HeartData) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HrStatBox("Medio", "${data.overall.avg} ppm")
        HrStatBox("Máximo", "${data.overall.max} ppm")
        when {
            data.activeKcal != null -> HrStatBox("Calorías activas", "${data.activeKcal} kcal")
            data.totalKcal != null -> HrStatBox("Calorías totales", "${data.totalKcal} kcal")
        }
    }
    if (data.segments.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            data.segments.forEach { seg ->
                Row {
                    Text(seg.label, Modifier.weight(1f))
                    Text("medio ${seg.avg} · máx ${seg.max}", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    HeartChart(
        points = data.points,
        segments = Heart.segments(log),
        sets = Heart.setTimes(log),
        start = log.startedAt,
        end = log.endedAt,
    )
    Text(
        "Franjas: bici · Marcas abajo: cada serie · Mantén pulsado para ver el valor",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HrStatBox(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

/** Línea de pulso de una sola serie, con un eje, franjas de bici y marcas de series. */
@Composable
fun HeartChart(
    points: List<HrPoint>,
    segments: List<Segment>,
    sets: List<Long>,
    start: Long,
    end: Long,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2 || end <= start) {
        Text("Hay muy pocas mediciones para dibujar la gráfica.", style = MaterialTheme.typography.bodySmall)
        return
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val bandColor = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val strongInk = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = ink)
    val tipStyle = MaterialTheme.typography.labelMedium.copy(color = strongInk, fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer()
    var touchX by remember(points) { mutableStateOf<Float?>(null) }

    val lo = ((points.minOf { it.bpm } - 5) / 10) * 10
    val hi = maxOf(lo + 20, ((points.maxOf { it.bpm } + 14) / 10) * 10)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(points) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    touchX = down.position.x
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        touchX = change.position.x
                        change.consume()
                    } while (event.changes.any { it.pressed })
                    touchX = null
                }
            },
    ) {
        val left = 32.dp.toPx()
        val top = 22.dp.toPx()
        val bottom = 18.dp.toPx()
        val w = size.width - left
        val h = size.height - top - bottom
        fun x(t: Long) = left + (t - start).toFloat() / (end - start) * w
        fun y(bpm: Int) = top + (1f - (bpm - lo).toFloat() / (hi - lo)) * h

        // Franjas de bici.
        segments.filter { it.kind == SegmentKind.BIKE }.forEach {
            val x0 = x(it.start).coerceIn(left, size.width)
            val x1 = x(it.end).coerceIn(left, size.width)
            if (x1 > x0) drawRect(bandColor, topLeft = Offset(x0, top), size = Size(x1 - x0, h))
        }

        // Cuadrícula y eje Y (un solo eje).
        listOf(lo, (lo + hi) / 2, hi).forEach { v ->
            drawLine(gridColor, Offset(left, y(v)), Offset(size.width, y(v)), strokeWidth = 1.dp.toPx())
            val tl = measurer.measure("$v", labelStyle)
            drawText(tl, topLeft = Offset(left - tl.size.width - 4.dp.toPx(), y(v) - tl.size.height / 2f))
        }

        // Eje X en minutos desde el inicio.
        val totalMin = ((end - start) / 60_000L).toInt()
        listOf(0, totalMin / 2, totalMin).distinct().forEach { m ->
            val tl = measurer.measure(if (m == 0) "0" else "$m min", labelStyle)
            val px = x(start + m * 60_000L)
            drawText(tl, topLeft = Offset((px - tl.size.width / 2f).coerceIn(left, size.width - tl.size.width), top + h + 2.dp.toPx()))
        }

        // Una marca por serie.
        sets.forEach { t ->
            val px = x(t)
            if (px in left..size.width) {
                drawLine(ink, Offset(px, top + h - 6.dp.toPx()), Offset(px, top + h), strokeWidth = 2.dp.toPx())
            }
        }

        // Línea de pulso; se corta si faltan datos más de 2 minutos.
        val path = Path()
        points.forEachIndexed { i, p ->
            val gap = i > 0 && p.t - points[i - 1].t > 120_000L
            if (i == 0 || gap) path.moveTo(x(p.t), y(p.bpm)) else path.lineTo(x(p.t), y(p.bpm))
        }
        drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Cursor al mantener pulsado.
        touchX?.let { tx ->
            val t = start + ((tx - left) / w * (end - start)).toLong()
            val p = points.minBy { abs(it.t - t) }
            val px = x(p.t)
            val py = y(p.bpm)
            drawLine(ink, Offset(px, top), Offset(px, top + h), strokeWidth = 1.dp.toPx())
            drawCircle(surface, radius = 6.dp.toPx(), center = Offset(px, py))
            drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(px, py))
            val min = ((p.t - start) / 60_000L).coerceAtLeast(0)
            val tl = measurer.measure("${p.bpm} ppm · min $min", tipStyle)
            drawText(tl, topLeft = Offset((px - tl.size.width / 2f).coerceIn(0f, size.width - tl.size.width), 0f))
        }
    }
}
