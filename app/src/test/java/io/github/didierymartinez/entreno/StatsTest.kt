package io.github.didierymartinez.entreno

import io.github.didierymartinez.entreno.data.DayGroup
import io.github.didierymartinez.entreno.data.ExerciseLog
import io.github.didierymartinez.entreno.data.Muscle
import io.github.didierymartinez.entreno.data.SessionLog
import io.github.didierymartinez.entreno.data.SetLog
import io.github.didierymartinez.entreno.domain.Fmt
import io.github.didierymartinez.entreno.domain.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class StatsTest {

    private val utc = ZoneOffset.UTC
    private val day = 86_400_000L
    private val now = 1_700_000_000_000L
    private val rotation = listOf(DayGroup.PECHO, DayGroup.ESPALDA, DayGroup.BRAZO, DayGroup.PIERNA)

    private fun log(id: String, daysAgo: Int, group: DayGroup, vararg ex: Pair<String, Double>) = SessionLog(
        id = id,
        startedAt = now - daysAgo * day,
        endedAt = now - daysAgo * day + 3_600_000L,
        groups = listOf(group),
        exercises = ex.map { (exId, w) -> ExerciseLog(exId, listOf(SetLog(10, w, 0))) },
    )

    @Test
    fun `sin historial sugiere el primero de la rotacion`() {
        assertEquals(DayGroup.PECHO, Stats.suggest(rotation, emptyList(), now, utc))
    }

    @Test
    fun `despues de pecho toca espalda aunque haya faltado dias`() {
        val h = listOf(log("a", 4, DayGroup.PECHO, "press_banca" to 60.0))
        assertEquals(DayGroup.ESPALDA, Stats.suggest(rotation, h, now, utc))
    }

    @Test
    fun `salta grupos trabajados hace menos de 2 dias`() {
        val h = listOf(
            log("a", 3, DayGroup.PECHO),
            log("b", 1, DayGroup.BRAZO),
            log("c", 0, DayGroup.PECHO),
        )
        // Tras pecho iría espalda (nunca) — se mantiene.
        assertEquals(DayGroup.ESPALDA, Stats.suggest(rotation, h, now, utc))
        // Si el último fue espalda, brazo (ayer) se salta y toca pierna.
        val h2 = h + log("d", 0, DayGroup.ESPALDA).copy(startedAt = now + 1)
        assertEquals(DayGroup.PIERNA, Stats.suggest(rotation, h2, now + 2, utc))
    }

    @Test
    fun `dias por musculo usa los musculos principales`() {
        val h = listOf(log("a", 2, DayGroup.PECHO, "press_banca" to 60.0))
        val days = Stats.muscleDaysSince(h, now, utc)
        assertEquals(2, days[Muscle.PECHO])
        assertTrue(Muscle.TRICEPS !in days)
    }

    @Test
    fun `record cuando se supera el peso anterior`() {
        val old = log("a", 7, DayGroup.PECHO, "press_banca" to 60.0)
        val today = log("b", 0, DayGroup.PECHO, "press_banca" to 62.5, "aperturas_manc" to 14.0)
        val prs = Stats.prs(today, listOf(old, today))
        assertEquals(listOf("press_banca" to 62.5), prs)
    }

    @Test
    fun `ultimo peso y volumen`() {
        val h = listOf(log("a", 7, DayGroup.PECHO, "press_banca" to 55.0), log("b", 2, DayGroup.PECHO, "press_banca" to 60.0))
        assertEquals(60.0, Stats.lastWeight("press_banca", h)!!, 0.0)
        assertEquals(600.0, Stats.volume(h[1].exercises), 0.0)
    }

    @Test
    fun `carga muscular normalizada`() {
        val load = Stats.muscleLoad(listOf(ExerciseLog("press_banca", List(4) { SetLog(10, 60.0, 0) })))
        assertEquals(1f, load[Muscle.PECHO]!!, 0f)
        assertEquals(0.5f, load[Muscle.TRICEPS]!!, 0f)
    }

    @Test
    fun `formato de pesos y tiempos`() {
        assertEquals("57,5", Fmt.kg(57.5))
        assertEquals("60", Fmt.kg(60.0))
        assertEquals("9.060", Fmt.kg(9060.0))
        assertEquals("10 × 60 kg", Fmt.target(10, 60.0))
        assertEquals("12 reps", Fmt.target(12, 0.0))
        assertEquals("1:30", Fmt.clock(90))
        assertEquals("1:05:00", Fmt.clock(3900))
    }

    @Test
    fun `resumen semanal compara con la semana anterior`() {
        val h = listOf(
            log("a", 0, DayGroup.PECHO, "press_banca" to 60.0),   // 600 kg hoy
            log("b", 3, DayGroup.ESPALDA, "remo_barra" to 40.0),  // 400 kg
            log("c", 9, DayGroup.PECHO, "press_banca" to 50.0),   // 500 kg semana anterior
            log("d", 20, DayGroup.PIERNA, "sentadilla" to 80.0),  // fuera de las dos semanas
        )
        val w = Stats.week(h, now, utc)
        assertEquals(2, w.sessions)
        assertEquals(1000.0, w.volumeKg, 0.0)
        assertEquals(500.0, w.previousVolumeKg, 0.0)
        assertEquals(100, w.changePct)
        assertEquals(7, w.dailyVolume.size)
        assertEquals(600.0, w.dailyVolume.last(), 0.0)
        assertEquals(400.0, w.dailyVolume[3], 0.0)
        assertEquals(7, w.dayInitials.size)
    }

    @Test
    fun `volumen de la ultima sesion del mismo grupo`() {
        val old = log("a", 7, DayGroup.PECHO, "press_banca" to 50.0)
        val other = log("b", 3, DayGroup.ESPALDA, "remo_barra" to 40.0)
        val today = log("c", 0, DayGroup.PECHO, "press_banca" to 60.0)
        assertEquals(500.0, Stats.previousSameGroupVolume(today, listOf(old, other, today))!!, 0.0)
        assertEquals(null, Stats.previousSameGroupVolume(other, listOf(old, other)))
    }
}
