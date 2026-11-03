package io.github.didierymartinez.entreno

import io.github.didierymartinez.entreno.data.CardioKind
import io.github.didierymartinez.entreno.data.CardioLog
import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.ExerciseLog
import io.github.didierymartinez.entreno.data.HrPoint
import io.github.didierymartinez.entreno.data.SessionLog
import io.github.didierymartinez.entreno.data.SetLog
import io.github.didierymartinez.entreno.data.Settings
import io.github.didierymartinez.entreno.domain.Heart
import io.github.didierymartinez.entreno.domain.SegmentKind
import io.github.didierymartinez.entreno.domain.SessionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartTest {

    private val t0 = 1_700_000_000_000L
    private val min = 60_000L

    /** 10 min de bici, 40 de pesas, 15 de bici. */
    private val log = SessionLog(
        id = "s",
        startedAt = t0,
        endedAt = t0 + 66 * min,
        groups = listOf(DayGroup.PECHO),
        warmup = CardioLog(CardioKind.WARMUP, 600, startedAt = t0 + 30_000, endedAt = t0 + 10 * min + 30_000),
        cooldown = CardioLog(CardioKind.COOLDOWN, 900, startedAt = t0 + 51 * min, endedAt = t0 + 66 * min),
        exercises = listOf(ExerciseLog("press_banca", listOf(SetLog(10, 60.0, t0 + 15 * min), SetLog(10, 60.0, t0 + 18 * min)))),
    )

    @Test
    fun `tramos de bici y pesas segun las horas registradas`() {
        val segs = Heart.segments(log)
        assertEquals(listOf(SegmentKind.BIKE, SegmentKind.STRENGTH, SegmentKind.BIKE), segs.map { it.kind })
        assertEquals(t0 + 10 * min + 30_000, segs[1].start)
        assertEquals(t0 + 51 * min, segs[1].end)
    }

    @Test
    fun `sesiones antiguas sin horas de bici usan toda la sesion como pesas`() {
        val old = log.copy(warmup = CardioLog(CardioKind.WARMUP, 600), cooldown = null)
        val segs = Heart.segments(old)
        assertEquals(1, segs.size)
        assertEquals(old.startedAt, segs[0].start)
        assertEquals(old.endedAt, segs[0].end)
    }

    @Test
    fun `resumen por tramo usa los datos originales y conserva el pico`() {
        val raw = buildList {
            for (s in 0 until 66 * 60 step 5) {
                val t = t0 + s * 1000L
                val bpm = when {
                    t < t0 + 11 * min -> 120
                    t < t0 + 51 * min -> 100
                    else -> 130
                }
                add(HrPoint(t, bpm))
            }
            add(HrPoint(t0 + 18 * min + 1000, 171)) // pico al terminar una serie
            add(HrPoint(t0 - 10 * min, 70)) // fuera de la sesión: se ignora
        }
        val data = Heart.build(log, raw, activeKcal = 420, totalKcal = null, now = t0 + 70 * min)
        assertNotNull(data)
        data!!
        assertEquals(171, data.overall.max)
        assertEquals(3, data.segments.size)
        assertEquals(120, data.segments[0].avg)
        assertEquals(171, data.segments[1].max)
        assertEquals(130, data.segments[2].avg)
        assertEquals(420, data.activeKcal)
        // 66 min en tramos de 15 s (uno más si el inicio no cae justo al borde de un tramo).
        assertTrue(data.points.size in 264..265)
    }

    @Test
    fun `sin pulso dentro de la sesion no hay datos`() {
        assertNull(Heart.build(log, listOf(HrPoint(t0 - 5 * min, 80)), null, null, t0))
        assertNull(Heart.build(log, emptyList(), null, null, t0))
    }

    @Test
    fun `reducir promedia por tramos`() {
        val pts = listOf(HrPoint(0, 100), HrPoint(5_000, 110), HrPoint(16_000, 150))
        val down = Heart.downsample(pts)
        assertEquals(listOf(105, 150), down.map { it.bpm })
    }

    @Test
    fun `la bici guarda inicio y fin reales`() {
        var s = SessionEngine.newSession("x", t0, listOf(DayGroup.PECHO), Settings(), emptyList())
        s = SessionEngine.startCardio(s, t0 + 1_000, 10)
        // El tick llega 3 s tarde: el tramo termina cuando acabó el temporizador.
        s = SessionEngine.tick(s, t0 + 1_000 + 10 * min + 3_000, true).session
        assertEquals(t0 + 1_000, s.warmup?.startedAt)
        assertEquals(t0 + 1_000 + 10 * min, s.warmup?.endedAt)
        assertNull(s.cardioStartedAt)
    }
}
