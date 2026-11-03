package io.github.didierymartinez.entreno.domain

import io.github.didierymartinez.entreno.data.HeartData
import io.github.didierymartinez.entreno.data.HrPoint
import io.github.didierymartinez.entreno.data.HrStat
import io.github.didierymartinez.entreno.data.SessionLog
import kotlin.math.roundToInt

enum class SegmentKind { BIKE, STRENGTH }

data class Segment(val label: String, val start: Long, val end: Long, val kind: SegmentKind)

data class HrSummary(val avg: Int, val max: Int, val min: Int, val count: Int)

object Heart {

    /** Margen alrededor de la sesión al pedir datos (relojes y teléfono pueden no ir en hora exacta). */
    const val MARGIN_MS = 60_000L

    /** Tramo de cada sample tras reducir: suficiente para ver cada serie sin guardar miles de puntos. */
    const val BUCKET_MS = 15_000L

    fun fetchWindow(log: SessionLog): Pair<Long, Long> =
        (log.startedAt - MARGIN_MS) to (log.endedAt + MARGIN_MS)

    /** Bici inicio / pesas / bici final, según las horas registradas. */
    fun segments(log: SessionLog): List<Segment> {
        val out = mutableListOf<Segment>()
        val warm = log.warmup?.takeIf { it.startedAt != null && it.endedAt != null }
        val cool = log.cooldown?.takeIf { it.startedAt != null && it.endedAt != null }
        warm?.let { out += Segment("🚴 Bici inicio", it.startedAt!!, it.endedAt!!, SegmentKind.BIKE) }
        val strengthStart = warm?.endedAt ?: log.startedAt
        val strengthEnd = cool?.startedAt ?: log.endedAt
        if (log.exercises.isNotEmpty() && strengthEnd > strengthStart) {
            out += Segment("🏋️ Pesas", strengthStart, strengthEnd, SegmentKind.STRENGTH)
        }
        cool?.let { out += Segment("🚴 Bici final", it.startedAt!!, it.endedAt!!, SegmentKind.BIKE) }
        return out
    }

    fun summarize(points: List<HrPoint>, start: Long = Long.MIN_VALUE, end: Long = Long.MAX_VALUE): HrSummary? {
        val inRange = points.filter { it.t in start..end }
        if (inRange.isEmpty()) return null
        return HrSummary(
            avg = inRange.map { it.bpm }.average().roundToInt(),
            max = inRange.maxOf { it.bpm },
            min = inRange.minOf { it.bpm },
            count = inRange.size,
        )
    }

    /**
     * Promedia en tramos de [bucketMs]. El máximo real se conserva aparte
     * (ver [HeartData]) porque promediar aplana los picos de cada serie.
     */
    fun downsample(points: List<HrPoint>, bucketMs: Long = BUCKET_MS): List<HrPoint> {
        if (points.isEmpty()) return emptyList()
        return points.sortedBy { it.t }
            .groupBy { it.t / bucketMs }
            .map { (bucket, pts) -> HrPoint(bucket * bucketMs + bucketMs / 2, pts.map { it.bpm }.average().roundToInt()) }
    }

    /** Arma los datos a guardar a partir de las mediciones originales. Null si no hay pulso en la sesión. */
    fun build(log: SessionLog, raw: List<HrPoint>, activeKcal: Int?, totalKcal: Int?, now: Long): HeartData? {
        val inSession = raw.filter { it.t in log.startedAt..log.endedAt && it.bpm in 25..250 }
        val all = summarize(inSession) ?: return null
        val segStats = segments(log).mapNotNull { seg ->
            summarize(inSession, seg.start, seg.end)?.let { HrStat(seg.label, it.avg, it.max, it.min) }
        }
        return HeartData(
            points = downsample(inSession),
            overall = HrStat("Sesión", all.avg, all.max, all.min),
            segments = segStats,
            activeKcal = activeKcal,
            totalKcal = totalKcal,
            fetchedAt = now,
        )
    }

    /** Horas en que se marcó cada serie (para dibujarlas sobre la gráfica). */
    fun setTimes(log: SessionLog): List<Long> =
        log.exercises.flatMap { e -> e.sets.map { it.at } }.filter { it > 0 }.sorted()
}
