package io.github.didierymartinez.entreno.domain

import io.github.didierymartinez.entreno.data.ActiveSession
import io.github.didierymartinez.entreno.data.CardioKind
import io.github.didierymartinez.entreno.data.CardioLog
import io.github.didierymartinez.entreno.data.Catalog
import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.ExerciseLog
import io.github.didierymartinez.entreno.data.Phase
import io.github.didierymartinez.entreno.data.PlanItem
import io.github.didierymartinez.entreno.data.SessionLog
import io.github.didierymartinez.entreno.data.SetLog
import io.github.didierymartinez.entreno.data.Settings
import io.github.didierymartinez.entreno.data.Timer
import io.github.didierymartinez.entreno.data.current
import java.util.UUID

/** Aviso que se publica como notificación (y llega al reloj vía Mi Fitness). */
data class Alert(val title: String, val text: String)

data class Step(val session: ActiveSession, val alerts: List<Alert> = emptyList())

/**
 * Lógica pura de la sesión: recibe un estado y devuelve el siguiente.
 * No toca Android, así que se puede probar con tests normales.
 */
object SessionEngine {

    private val newUid: () -> String = { UUID.randomUUID().toString() }

    fun newSession(
        id: String,
        now: Long,
        groups: List<DayGroup>,
        settings: Settings,
        history: List<SessionLog>,
        uid: () -> String = newUid,
    ): ActiveSession {
        val plan = groups.flatMap { Catalog.templates[it].orEmpty() }.map {
            planItem(it.exerciseId, it.sets, it.reps, it.restSec, history, uid)
        }
        val first = plan.firstOrNull()
        return ActiveSession(
            id = id,
            startedAt = now,
            groups = groups,
            plan = plan,
            currentUid = first?.uid,
            phase = if (settings.warmupMin > 0) Phase.WARMUP else Phase.STRENGTH,
            draftReps = first?.targetReps ?: 0,
            draftWeight = first?.weightKg ?: 0.0,
        )
    }

    private fun planItem(
        exerciseId: String,
        sets: Int,
        reps: Int,
        restSec: Int,
        history: List<SessionLog>,
        uid: () -> String,
    ) = PlanItem(
        uid = uid(),
        exerciseId = exerciseId,
        targetSets = sets,
        targetReps = reps,
        weightKg = Stats.lastWeight(exerciseId, history) ?: 0.0,
        restSec = restSec,
    )

    // ---------- Bici ----------

    fun startCardio(s: ActiveSession, now: Long, minutes: Int): ActiveSession {
        val total = maxOf(1, minutes) * 60
        return s.copy(timer = Timer(totalSec = total, endAt = now + total * 1000L), cardioForm = false)
    }

    fun pause(s: ActiveSession, now: Long): ActiveSession {
        val t = s.timer ?: return s
        val end = t.endAt ?: return s
        return s.copy(timer = t.copy(endAt = null, pausedMs = maxOf(0L, end - now)))
    }

    fun resume(s: ActiveSession, now: Long): ActiveSession {
        val t = s.timer ?: return s
        if (t.running) return s
        return s.copy(timer = t.copy(endAt = now + (t.pausedMs ?: 0L), pausedMs = null))
    }

    fun finishCardio(s: ActiveSession, now: Long): ActiveSession {
        val t = s.timer
        val elapsed = if (t == null) 0 else ((t.totalSec * 1000L - t.remaining(now)) / 1000L).toInt()
        return when (s.phase) {
            Phase.WARMUP -> s.copy(warmup = CardioLog(CardioKind.WARMUP, elapsed), timer = null, cardioForm = true)
            Phase.COOLDOWN -> s.copy(cooldown = CardioLog(CardioKind.COOLDOWN, elapsed), timer = null, cardioForm = true)
            else -> s
        }
    }

    fun saveCardioForm(s: ActiveSession, km: Double?, kcal: Int?, now: Long): ActiveSession {
        val updated = when (s.phase) {
            Phase.WARMUP -> s.copy(warmup = s.warmup?.copy(distanceKm = km, kcal = kcal))
            Phase.COOLDOWN -> s.copy(cooldown = s.cooldown?.copy(distanceKm = km, kcal = kcal))
            else -> s
        }
        return afterCardio(updated.copy(cardioForm = false), now)
    }

    fun skipCardio(s: ActiveSession, now: Long): ActiveSession =
        afterCardio(s.copy(timer = null, cardioForm = false), now)

    private fun afterCardio(s: ActiveSession, now: Long): ActiveSession = when (s.phase) {
        Phase.WARMUP -> s.copy(phase = Phase.STRENGTH)
        Phase.COOLDOWN -> s.copy(phase = Phase.SUMMARY, endedAt = now)
        else -> s
    }

    // ---------- Fuerza ----------

    fun setDraft(s: ActiveSession, reps: Int, weightKg: Double): ActiveSession =
        s.copy(draftReps = reps.coerceAtLeast(0), draftWeight = weightKg.coerceAtLeast(0.0))

    fun completeSet(s: ActiveSession, now: Long): Step {
        val cur = s.current() ?: return Step(s)
        val sets = cur.sets + SetLog(s.draftReps, s.draftWeight, now)
        val updated = cur.copy(sets = sets, finished = sets.size >= cur.targetSets, weightKg = s.draftWeight)
        val plan = s.plan.replace(updated)
        val next = if (!updated.finished) updated else nextPending(plan, cur.uid)
            ?: return Step(s.copy(plan = plan, currentUid = null, phase = Phase.STRENGTH, timer = null))
        val sameItem = next.uid == cur.uid
        val base = s.copy(
            plan = plan,
            currentUid = next.uid,
            draftReps = if (sameItem) s.draftReps else next.targetReps,
            draftWeight = if (sameItem) s.draftWeight else next.weightKg,
        )
        if (cur.restSec <= 0) return Step(base.copy(phase = Phase.STRENGTH, timer = null))
        return Step(
            base.copy(phase = Phase.REST, timer = Timer(totalSec = cur.restSec, endAt = now + cur.restSec * 1000L)),
        )
    }

    fun skipRest(s: ActiveSession): ActiveSession =
        if (s.phase == Phase.REST) s.copy(phase = Phase.STRENGTH, timer = null) else s

    fun addRest(s: ActiveSession, seconds: Int): ActiveSession {
        val t = s.timer ?: return s
        if (s.phase != Phase.REST) return s
        return s.copy(
            timer = t.copy(
                totalSec = t.totalSec + seconds,
                endAt = t.endAt?.plus(seconds * 1000L),
                pausedMs = t.pausedMs?.plus(seconds * 1000L),
                warned = false,
            ),
        )
    }

    /** Saltar a un ejercicio del plan (también uno ya hecho, para meter otra serie). */
    fun select(s: ActiveSession, uid: String): ActiveSession {
        val item = s.plan.firstOrNull { it.uid == uid } ?: return s
        val reopened = item.copy(finished = false, skipped = false)
        val last = item.sets.lastOrNull()
        return s.copy(
            plan = s.plan.replace(reopened),
            currentUid = uid,
            phase = if (s.phase == Phase.REST) Phase.REST else Phase.STRENGTH,
            draftReps = last?.reps ?: item.targetReps,
            draftWeight = last?.weightKg ?: item.weightKg,
        )
    }

    /** Cambiar el ejercicio actual (máquina ocupada) por otro. */
    fun swap(
        s: ActiveSession,
        exerciseId: String,
        history: List<SessionLog>,
        uid: () -> String = newUid,
    ): ActiveSession {
        val cur = s.current() ?: return add(s, exerciseId, 90, history, uid)
        val weight = Stats.lastWeight(exerciseId, history) ?: 0.0
        if (cur.sets.isEmpty()) {
            val replaced = cur.copy(exerciseId = exerciseId, weightKg = weight)
            return s.copy(plan = s.plan.replace(replaced), draftReps = cur.targetReps, draftWeight = weight)
        }
        // Ya había series: se cierra el actual y el nuevo hereda las series que faltaban.
        val closed = cur.copy(finished = true)
        val item = PlanItem(
            uid = uid(),
            exerciseId = exerciseId,
            targetSets = (cur.targetSets - cur.sets.size).coerceAtLeast(1),
            targetReps = cur.targetReps,
            weightKg = weight,
            restSec = cur.restSec,
        )
        val idx = s.plan.indexOfFirst { it.uid == cur.uid }
        val plan = s.plan.replace(closed).toMutableList().apply { add(idx + 1, item) }
        return s.copy(plan = plan, currentUid = item.uid, draftReps = item.targetReps, draftWeight = weight)
    }

    /** Añadir un ejercicio justo después del actual (lo que mande el entrenador). */
    fun add(
        s: ActiveSession,
        exerciseId: String,
        defaultRestSec: Int,
        history: List<SessionLog>,
        uid: () -> String = newUid,
    ): ActiveSession {
        val d = Catalog.defaultsFor(exerciseId)
        val item = planItem(exerciseId, d?.sets ?: 3, d?.reps ?: 12, d?.restSec ?: defaultRestSec, history, uid)
        val idx = s.plan.indexOfFirst { it.uid == s.currentUid }
        val plan = s.plan.toMutableList().apply { if (idx < 0) add(item) else add(idx + 1, item) }
        val becomeCurrent = s.currentUid == null
        return s.copy(
            plan = plan,
            currentUid = if (becomeCurrent) item.uid else s.currentUid,
            phase = if (becomeCurrent && s.phase != Phase.REST) Phase.STRENGTH else s.phase,
            draftReps = if (becomeCurrent) item.targetReps else s.draftReps,
            draftWeight = if (becomeCurrent) item.weightKg else s.draftWeight,
        )
    }

    fun skipExercise(s: ActiveSession): ActiveSession = closeCurrent(s) { it.copy(skipped = true) }

    /** Dar por terminado el ejercicio actual aunque falten series. */
    fun finishExercise(s: ActiveSession): ActiveSession =
        closeCurrent(s) { if (it.sets.isEmpty()) it.copy(skipped = true) else it.copy(finished = true) }

    private fun closeCurrent(s: ActiveSession, close: (PlanItem) -> PlanItem): ActiveSession {
        val cur = s.current() ?: return s
        val plan = s.plan.replace(close(cur))
        val next = nextPending(plan, cur.uid)
        return s.copy(
            plan = plan,
            currentUid = next?.uid,
            draftReps = next?.targetReps ?: 0,
            draftWeight = next?.weightKg ?: 0.0,
        )
    }

    fun undoLastSet(s: ActiveSession): ActiveSession {
        val (item, set) = s.plan.flatMap { p -> p.sets.map { p to it } }.maxByOrNull { it.second.at } ?: return s
        val restored = item.copy(sets = item.sets - set, finished = false, skipped = false)
        return s.copy(
            plan = s.plan.replace(restored),
            currentUid = item.uid,
            phase = Phase.STRENGTH,
            timer = null,
            draftReps = set.reps,
            draftWeight = set.weightKg,
        )
    }

    fun goToCooldown(s: ActiveSession): ActiveSession =
        s.copy(phase = Phase.COOLDOWN, timer = null, cardioForm = false)

    fun finishSession(s: ActiveSession, now: Long): ActiveSession {
        val withCardio = if ((s.phase == Phase.WARMUP || s.phase == Phase.COOLDOWN) && s.timer != null) {
            finishCardio(s, now)
        } else {
            s
        }
        return withCardio.copy(phase = Phase.SUMMARY, timer = null, cardioForm = false, endedAt = now)
    }

    /** Volver a entrenar desde el resumen (por si se terminó sin querer). */
    fun reopen(s: ActiveSession): ActiveSession =
        if (s.phase == Phase.SUMMARY) s.copy(phase = Phase.STRENGTH, endedAt = null) else s

    // ---------- Reloj ----------

    fun tick(s: ActiveSession, now: Long, warnBeforeRestEnd: Boolean): Step {
        val t = s.timer ?: return Step(s)
        val end = t.endAt ?: return Step(s)
        val remaining = end - now
        when (s.phase) {
            Phase.REST -> {
                if (remaining <= 0) {
                    return Step(s.copy(phase = Phase.STRENGTH, timer = null), listOfNotNull(s.current()?.let { goAlert(it, s) }))
                }
                if (warnBeforeRestEnd && !t.warned && remaining <= 10_000 && t.totalSec > 15) {
                    val next = s.current()
                    val text = next?.let { "Sig.: ${Catalog.name(it.exerciseId)} · Serie ${it.sets.size + 1}/${it.targetSets}" }
                        ?: "Prepárate"
                    return Step(s.copy(timer = t.copy(warned = true)), listOf(Alert("⏱ 10 s de descanso", text)))
                }
            }
            Phase.WARMUP, Phase.COOLDOWN -> {
                val warm = s.phase == Phase.WARMUP
                if (remaining <= 0) {
                    val alert = if (warm) {
                        val next = s.current()
                        Alert(
                            "🚴 Calentamiento terminado",
                            next?.let { "Ahora: ${Catalog.name(it.exerciseId)} · ${it.targetSets}×${it.targetReps}" } ?: "¡A por las pesas!",
                        )
                    } else {
                        Alert("🚴 Bici final terminada", "¡Buen entreno! Guarda la sesión")
                    }
                    return Step(finishCardio(s, now), listOf(alert))
                }
                if (!t.halfNotified && t.totalSec >= 120 && remaining <= t.totalSec * 500L) {
                    val title = if (warm) "🚴 Mitad del calentamiento" else "🚴 Mitad de la bici final"
                    return Step(
                        s.copy(timer = t.copy(halfNotified = true)),
                        listOf(Alert(title, "Quedan ${Fmt.clock(remaining / 1000)}")),
                    )
                }
            }
            else -> Unit
        }
        return Step(s)
    }

    private fun goAlert(item: PlanItem, s: ActiveSession): Alert = Alert(
        "▶ ${Catalog.name(item.exerciseId)} · ${item.sets.size + 1}/${item.targetSets}",
        "¡Vamos! " + Fmt.target(s.draftReps, s.draftWeight),
    )

    // ---------- Utilidades ----------

    fun nextPending(plan: List<PlanItem>, fromUid: String?): PlanItem? {
        val idx = plan.indexOfFirst { it.uid == fromUid }
        val order = if (idx < 0) plan else plan.drop(idx + 1) + plan.take(idx + 1)
        return order.firstOrNull { !it.finished && !it.skipped }
    }

    fun toLog(s: ActiveSession, now: Long): SessionLog {
        val exercises = s.plan.filter { it.sets.isNotEmpty() }.map { ExerciseLog(it.exerciseId, it.sets) }
        val groups = (s.groups + exercises.mapNotNull { Catalog.byId[it.exerciseId]?.group }).distinct()
        return SessionLog(
            id = s.id,
            startedAt = s.startedAt,
            endedAt = s.endedAt ?: now,
            groups = groups,
            warmup = s.warmup,
            cooldown = s.cooldown,
            exercises = exercises,
        )
    }

    private fun List<PlanItem>.replace(item: PlanItem): List<PlanItem> =
        map { if (it.uid == item.uid) item else it }
}
