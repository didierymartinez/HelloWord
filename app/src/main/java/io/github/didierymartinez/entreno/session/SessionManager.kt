package io.github.didierymartinez.entreno.session

import android.content.Context
import io.github.didierymartinez.entreno.data.ActiveSession
import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.HeartData
import io.github.didierymartinez.entreno.data.Phase
import io.github.didierymartinez.entreno.data.Repository
import io.github.didierymartinez.entreno.domain.Alert
import io.github.didierymartinez.entreno.domain.SessionEngine
import io.github.didierymartinez.entreno.domain.Step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** Estado de la sesión en curso, compartido por la pantalla y el servicio. */
class SessionManager(
    private val ctx: Context,
    private val repo: Repository,
    private val notifier: Notifier,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow(repo.loadActive())
    val state: StateFlow<ActiveSession?> = _state.asStateFlow()

    private val settings get() = repo.settings.value
    private val history get() = repo.history.value

    private fun update(f: (ActiveSession, Long) -> Step) {
        val alerts: List<Alert>
        synchronized(this) {
            val cur = _state.value ?: return
            val step = f(cur, clock())
            if (step.session != cur) {
                _state.value = step.session
                repo.saveActive(step.session)
            }
            alerts = step.alerts
        }
        alerts.forEach(notifier::alert)
    }

    private fun change(f: (ActiveSession, Long) -> ActiveSession) = update { s, now -> Step(f(s, now)) }

    fun start(groups: List<DayGroup>) {
        synchronized(this) {
            val s = SessionEngine.newSession(UUID.randomUUID().toString(), clock(), groups, settings, history)
            _state.value = s
            repo.saveActive(s)
        }
        WorkoutService.start(ctx)
    }

    fun tick() = update { s, now -> SessionEngine.tick(s, now, settings.warnBeforeRestEnd) }

    fun startCardio() = change { s, now ->
        SessionEngine.startCardio(s, now, if (s.phase == Phase.WARMUP) settings.warmupMin else settings.cooldownMin)
    }

    fun pauseTimer() = change(SessionEngine::pause)
    fun resumeTimer() = change(SessionEngine::resume)
    fun finishCardio() = change(SessionEngine::finishCardio)
    fun saveCardioForm(km: Double?, kcal: Int?) = change { s, now -> SessionEngine.saveCardioForm(s, km, kcal, now) }
    fun skipCardio() = change(SessionEngine::skipCardio)

    fun completeSet() = update(SessionEngine::completeSet)
    fun skipRest() = change { s, _ -> SessionEngine.skipRest(s) }
    fun addRest(seconds: Int) = change { s, _ -> SessionEngine.addRest(s, seconds) }
    fun setDraft(reps: Int, weightKg: Double) = change { s, _ -> SessionEngine.setDraft(s, reps, weightKg) }
    fun select(uid: String) = change { s, _ -> SessionEngine.select(s, uid) }
    fun swap(exerciseId: String) = change { s, _ -> SessionEngine.swap(s, exerciseId, history) }
    fun add(exerciseId: String) = change { s, _ -> SessionEngine.add(s, exerciseId, settings.defaultRestSec, history) }
    fun skipExercise() = change { s, _ -> SessionEngine.skipExercise(s) }
    fun finishExercise() = change { s, _ -> SessionEngine.finishExercise(s) }
    fun undoLastSet() = change { s, _ -> SessionEngine.undoLastSet(s) }

    fun goToCooldown() = change { s, now ->
        if (settings.cooldownMin > 0) SessionEngine.goToCooldown(s) else SessionEngine.finishSession(s, now)
    }

    fun finishSession() = change(SessionEngine::finishSession)

    fun reopen() {
        change { s, _ -> SessionEngine.reopen(s) }
        WorkoutService.start(ctx)
    }

    /** [heart]: pulso ya leído en el resumen, si Mi Fitness ya lo había sincronizado. */
    fun saveAndClose(heart: HeartData? = null) {
        val s = _state.value ?: return
        repo.addSession(SessionEngine.toLog(s, clock()).copy(heart = heart))
        clear()
    }

    fun discard() = clear()

    private fun clear() {
        synchronized(this) {
            _state.value = null
            repo.saveActive(null)
        }
    }

    fun testAlert() = notifier.alert(Alert("💪 Entreno", "Prueba: si ves esto en el reloj, ¡todo listo!"))
}
