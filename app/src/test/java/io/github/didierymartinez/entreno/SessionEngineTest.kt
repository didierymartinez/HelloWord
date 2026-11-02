package io.github.didierymartinez.entreno

import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.Phase
import io.github.didierymartinez.entreno.data.Settings
import io.github.didierymartinez.entreno.data.current
import io.github.didierymartinez.entreno.domain.SessionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {

    private var n = 0
    private val uid = { "u${n++}" }
    private val t0 = 1_700_000_000_000L

    private fun session(settings: Settings = Settings()) =
        SessionEngine.newSession("s1", t0, listOf(DayGroup.PECHO), settings, emptyList(), uid)

    @Test
    fun `nueva sesion empieza con calentamiento y plan de la plantilla`() {
        val s = session()
        assertEquals(Phase.WARMUP, s.phase)
        assertEquals("press_banca", s.current()?.exerciseId)
        assertEquals(5, s.plan.size)
        assertEquals(10, s.draftReps)
    }

    @Test
    fun `sin calentamiento empieza directo en pesas`() {
        assertEquals(Phase.STRENGTH, session(Settings(warmupMin = 0)).phase)
    }

    @Test
    fun `la bici avisa a la mitad y al terminar`() {
        var s = SessionEngine.startCardio(session(), t0, 10)
        val half = SessionEngine.tick(s, t0 + 5 * 60_000L, true)
        assertEquals(1, half.alerts.size)
        assertTrue(half.session.timer!!.halfNotified)
        // No repite el aviso de la mitad.
        assertTrue(SessionEngine.tick(half.session, t0 + 6 * 60_000L, true).alerts.isEmpty())

        val end = SessionEngine.tick(half.session, t0 + 10 * 60_000L, true)
        s = end.session
        assertEquals(1, end.alerts.size)
        assertTrue(s.cardioForm)
        assertNull(s.timer)
        assertEquals(600, s.warmup?.durationSec)

        s = SessionEngine.saveCardioForm(s, 4.2, 80, t0 + 11 * 60_000L)
        assertEquals(Phase.STRENGTH, s.phase)
        assertEquals(4.2, s.warmup?.distanceKm!!, 0.001)
    }

    @Test
    fun `pausa conserva el tiempo restante`() {
        var s = SessionEngine.startCardio(session(), t0, 10)
        s = SessionEngine.pause(s, t0 + 60_000L)
        assertEquals(9 * 60_000L, s.timer!!.remaining(t0 + 5 * 60_000L))
        s = SessionEngine.resume(s, t0 + 5 * 60_000L)
        assertEquals(8 * 60_000L, s.timer!!.remaining(t0 + 6 * 60_000L))
    }

    @Test
    fun `serie hecha inicia descanso y al terminar avisa la siguiente serie`() {
        var s = SessionEngine.skipCardio(session(), t0)
        s = SessionEngine.setDraft(s, 10, 60.0)
        val done = SessionEngine.completeSet(s, t0)
        s = done.session
        assertEquals(Phase.REST, s.phase)
        assertEquals(1, s.current()!!.sets.size)
        assertEquals(120, s.timer!!.totalSec)

        val warn = SessionEngine.tick(s, t0 + 111_000L, true)
        assertEquals(1, warn.alerts.size)
        assertTrue(warn.session.timer!!.warned)

        val over = SessionEngine.tick(warn.session, t0 + 120_000L, true)
        assertEquals(Phase.STRENGTH, over.session.phase)
        assertEquals(1, over.alerts.size)
        assertTrue(over.alerts[0].title.contains("Press banca"))
        assertTrue(over.alerts[0].text.contains("60 kg"))
    }

    @Test
    fun `sin aviso de 10 s si esta desactivado`() {
        val s = SessionEngine.completeSet(SessionEngine.skipCardio(session(), t0), t0).session
        assertTrue(SessionEngine.tick(s, t0 + 111_000L, false).alerts.isEmpty())
    }

    @Test
    fun `al completar las series pasa al siguiente ejercicio con su peso`() {
        var s = SessionEngine.skipCardio(session(), t0)
        repeat(4) { i -> s = SessionEngine.skipRest(SessionEngine.completeSet(s, t0 + i).session) }
        assertEquals("press_inclinado_manc", s.current()?.exerciseId)
        assertTrue(s.plan[0].finished)
        assertEquals(12, s.draftReps)
    }

    @Test
    fun `terminar todo deja la sesion sin ejercicio actual`() {
        var s = SessionEngine.skipCardio(session(), t0)
        var i = 0L
        while (s.currentUid != null) s = SessionEngine.skipRest(SessionEngine.completeSet(s, t0 + i++).session)
        assertNull(s.timer)
        assertEquals(Phase.STRENGTH, s.phase)
        assertTrue(s.plan.all { it.finished })
    }

    @Test
    fun `cambiar ejercicio sin series lo reemplaza`() {
        val s = SessionEngine.swap(SessionEngine.skipCardio(session(), t0), "press_maquina", emptyList(), uid)
        assertEquals("press_maquina", s.current()?.exerciseId)
        assertEquals(5, s.plan.size)
    }

    @Test
    fun `cambiar ejercicio con series hechas inserta uno nuevo con las series que faltan`() {
        var s = SessionEngine.skipCardio(session(), t0)
        s = SessionEngine.skipRest(SessionEngine.completeSet(s, t0).session)
        s = SessionEngine.swap(s, "press_maquina", emptyList(), uid)
        assertEquals(6, s.plan.size)
        assertTrue(s.plan[0].finished)
        assertEquals("press_maquina", s.current()?.exerciseId)
        assertEquals(3, s.current()?.targetSets)
    }

    @Test
    fun `anadir ejercicio lo pone despues del actual`() {
        val s = SessionEngine.add(SessionEngine.skipCardio(session(), t0), "curl_barra", 90, emptyList(), uid)
        assertEquals("curl_barra", s.plan[1].exerciseId)
        assertEquals("press_banca", s.current()?.exerciseId)
    }

    @Test
    fun `saltar y deshacer`() {
        var s = SessionEngine.skipCardio(session(), t0)
        s = SessionEngine.setDraft(s, 8, 50.0)
        s = SessionEngine.completeSet(s, t0).session
        s = SessionEngine.undoLastSet(s)
        assertEquals(Phase.STRENGTH, s.phase)
        assertTrue(s.current()!!.sets.isEmpty())
        assertEquals(8, s.draftReps)

        s = SessionEngine.skipExercise(s)
        assertTrue(s.plan[0].skipped)
        assertEquals("press_inclinado_manc", s.current()?.exerciseId)
    }

    @Test
    fun `el resumen solo guarda ejercicios con series`() {
        var s = SessionEngine.skipCardio(session(), t0)
        s = SessionEngine.setDraft(s, 10, 60.0)
        s = SessionEngine.completeSet(s, t0).session
        s = SessionEngine.finishSession(s, t0 + 3_600_000L)
        assertEquals(Phase.SUMMARY, s.phase)
        val log = SessionEngine.toLog(s, t0 + 3_600_000L)
        assertEquals(1, log.exercises.size)
        assertNotNull(log.endedAt)
        assertFalse(log.groups.isEmpty())
    }
}
