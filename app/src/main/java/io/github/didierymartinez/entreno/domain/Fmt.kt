package io.github.didierymartinez.entreno.domain

import io.github.didierymartinez.entreno.data.SetLog
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

object Fmt {
    private val es: Locale = Locale.forLanguageTag("es")

    private val number = DecimalFormat("#,##0.#", DecimalFormatSymbols(es))

    /** 57.5 -> "57,5"; 60.0 -> "60"; 9060.0 -> "9.060". */
    fun kg(w: Double): String = synchronized(number) { number.format((w * 10).roundToLong() / 10.0) }

    /** "10 × 60 kg" o "10 reps" si no hay peso. */
    fun target(reps: Int, weightKg: Double): String =
        if (weightKg <= 0.0) "$reps reps" else "$reps × ${kg(weightKg)} kg"

    /** "10×60 · 10×60 · 8×57,5" (sin peso: "12 · 12 · 10"). */
    fun setsLine(sets: List<SetLog>): String =
        sets.joinToString(" · ") { if (it.weightKg <= 0.0) "${it.reps}" else "${it.reps}×${kg(it.weightKg)}" }

    /** m:ss, o h:mm:ss si pasa de una hora. */
    fun clock(totalSec: Long): String {
        val s = maxOf(0L, totalSec)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }

    fun minutes(totalSec: Int): String {
        val m = (totalSec + 30) / 60
        return if (m >= 60) "${m / 60} h ${"%02d".format(m % 60)} min" else "$m min"
    }

    fun day(ts: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofPattern("EEE d MMM", es).withZone(zone).format(Instant.ofEpochMilli(ts))

    fun daysAgo(days: Int?): String = when (days) {
        null -> "nunca"
        0 -> "hoy"
        1 -> "ayer"
        else -> "hace $days días"
    }
}
