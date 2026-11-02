package io.github.didierymartinez.entreno.domain

import io.github.didierymartinez.entreno.data.Catalog
import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.ExerciseLog
import io.github.didierymartinez.entreno.data.Muscle
import io.github.didierymartinez.entreno.data.SessionLog
import io.github.didierymartinez.entreno.data.SetLog
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object Stats {

    fun daysBetween(from: Long, to: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
        ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(from).atZone(zone).toLocalDate(),
            Instant.ofEpochMilli(to).atZone(zone).toLocalDate(),
        ).toInt()

    /** Último día en que se trabajó cada grupo (por el grupo elegido o por los ejercicios hechos). */
    fun groupLastTrained(history: List<SessionLog>): Map<DayGroup, Long> {
        val out = mutableMapOf<DayGroup, Long>()
        for (s in history) {
            val groups = s.groups.toMutableSet()
            s.exercises.filter { it.sets.isNotEmpty() }
                .mapNotNullTo(groups) { Catalog.byId[it.exerciseId]?.group }
            for (g in groups) if ((out[g] ?: Long.MIN_VALUE) < s.startedAt) out[g] = s.startedAt
        }
        return out
    }

    fun groupDaysSince(history: List<SessionLog>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Map<DayGroup, Int> =
        groupLastTrained(history).mapValues { daysBetween(it.value, now, zone) }

    /** Días desde que cada músculo se trabajó como músculo principal. */
    fun muscleDaysSince(history: List<SessionLog>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Map<Muscle, Int> {
        val last = mutableMapOf<Muscle, Long>()
        for (s in history) for (e in s.exercises) {
            if (e.sets.isEmpty()) continue
            val ex = Catalog.byId[e.exerciseId] ?: continue
            for (m in ex.primary) if ((last[m] ?: Long.MIN_VALUE) < s.startedAt) last[m] = s.startedAt
        }
        return last.mapValues { daysBetween(it.value, now, zone) }
    }

    /**
     * "Hoy toca": el siguiente de la rotación después del último grupo hecho,
     * saltando los que se trabajaron hace menos de 2 días. No depende del día
     * de la semana, así que faltar un día no descuadra nada.
     */
    fun suggest(
        rotation: List<DayGroup>,
        history: List<SessionLog>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DayGroup? {
        if (rotation.isEmpty()) return null
        val days = groupDaysSince(history, now, zone)
        val lastGroup = history.maxByOrNull { it.startedAt }?.groups?.firstOrNull { it in rotation }
        val start = lastGroup?.let { rotation.indexOf(it) + 1 } ?: 0
        val ordered = rotation.indices.map { rotation[(start + it) % rotation.size] }
        return ordered.firstOrNull { (days[it] ?: Int.MAX_VALUE) >= 2 }
            ?: ordered.maxByOrNull { days[it] ?: Int.MAX_VALUE }
    }

    /** Series de la última vez que se hizo un ejercicio. */
    fun lastSetsFor(exerciseId: String, history: List<SessionLog>): Pair<SessionLog, List<SetLog>>? =
        history.sortedByDescending { it.startedAt }.firstNotNullOfOrNull { s ->
            s.exercises.lastOrNull { it.exerciseId == exerciseId && it.sets.isNotEmpty() }?.let { s to it.sets }
        }

    fun lastWeight(exerciseId: String, history: List<SessionLog>): Double? =
        lastSetsFor(exerciseId, history)?.second?.last()?.weightKg

    fun volume(exercises: List<ExerciseLog>): Double =
        exercises.sumOf { e -> e.sets.sumOf { it.reps * it.weightKg } }

    fun setCount(exercises: List<ExerciseLog>): Int = exercises.sumOf { it.sets.size }

    /** Ejercicios donde el peso máximo supera el mejor peso de sesiones anteriores. */
    fun prs(log: SessionLog, history: List<SessionLog>): List<Pair<String, Double>> {
        val previous = history.filter { it.id != log.id && it.startedAt < log.startedAt }
        return log.exercises.groupBy { it.exerciseId }.mapNotNull { (id, logs) ->
            val best = logs.flatMap { it.sets }.maxOfOrNull { it.weightKg } ?: return@mapNotNull null
            if (best <= 0.0) return@mapNotNull null
            val prev = previous.flatMap { it.exercises }.filter { it.exerciseId == id }
                .flatMap { it.sets }.maxOfOrNull { it.weightKg } ?: return@mapNotNull null
            if (best > prev) id to best else null
        }
    }

    /** Carga relativa (0..1) por músculo: series como principal cuentan 1, como secundario 0,5. */
    fun muscleLoad(exercises: List<ExerciseLog>): Map<Muscle, Float> {
        val load = mutableMapOf<Muscle, Float>()
        for (e in exercises) {
            val ex = Catalog.byId[e.exerciseId] ?: continue
            val n = e.sets.size.toFloat()
            if (n == 0f) continue
            ex.primary.forEach { load[it] = (load[it] ?: 0f) + n }
            ex.secondary.forEach { load[it] = (load[it] ?: 0f) + n * 0.5f }
        }
        val max = load.values.maxOrNull() ?: return emptyMap()
        return load.mapValues { it.value / max }
    }
}
