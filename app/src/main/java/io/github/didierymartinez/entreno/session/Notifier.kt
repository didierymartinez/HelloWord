package io.github.didierymartinez.entreno.session

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.didierymartinez.entreno.R
import io.github.didierymartinez.entreno.data.ActiveSession
import io.github.didierymartinez.entreno.data.Catalog
import io.github.didierymartinez.entreno.data.Phase
import io.github.didierymartinez.entreno.data.Timer
import io.github.didierymartinez.entreno.data.current
import io.github.didierymartinez.entreno.domain.Alert
import io.github.didierymartinez.entreno.domain.Fmt
import io.github.didierymartinez.entreno.ui.MainActivity

/**
 * Dos tipos de notificación:
 *  - Avisos (alta prioridad, vibran): son los que Mi Fitness reenvía al reloj.
 *  - Sesión en curso (silenciosa y fija): controles en el teléfono y la pantalla de bloqueo.
 */
class Notifier(private val ctx: Context) {

    private val nm = NotificationManagerCompat.from(ctx)
    private var nextId = FIRST_ALERT_ID

    fun createChannels() {
        val alerts = NotificationChannel(CH_ALERTS, "Avisos al reloj", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Series, fin de descanso y bici. Actívalos en Mi Fitness para verlos en el reloj."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 150, 400)
        }
        val session = NotificationChannel(CH_SESSION, "Sesión en curso", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Temporizador y botones rápidos mientras entrenas."
            setShowBadge(false)
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(alerts, session))
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun alert(a: Alert) {
        if (!canPost()) return
        val n = NotificationCompat.Builder(ctx, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_entreno)
            .setContentTitle(a.title)
            .setContentText(a.text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(120_000)
            .setContentIntent(openApp())
            .build()
        nm.notify(nextId, n)
        nextId = if (nextId >= FIRST_ALERT_ID + 999) FIRST_ALERT_ID else nextId + 1
    }

    @SuppressLint("MissingPermission")
    fun updateOngoing(s: ActiveSession) {
        if (canPost()) nm.notify(ONGOING_ID, ongoing(s))
    }

    fun ongoing(s: ActiveSession?): Notification {
        val b = NotificationCompat.Builder(ctx, CH_SESSION)
            .setSmallIcon(R.drawable.ic_stat_entreno)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp())
        if (s == null) return b.setContentTitle("Entreno").build()

        val now = System.currentTimeMillis()
        val t = s.timer
        when (s.phase) {
            Phase.WARMUP, Phase.COOLDOWN -> {
                b.setContentTitle(if (s.phase == Phase.WARMUP) "🚴 Calentamiento" else "🚴 Bici final")
                when {
                    s.cardioForm -> b.setContentText("Terminado · anota distancia y calorías")
                    t == null -> b.setContentText("Listo para empezar")
                    !t.running -> {
                        b.setContentText("En pausa · quedan ${Fmt.clock(t.remaining(now) / 1000)}")
                        b.addAction(0, "Reanudar", action(WorkoutService.ACTION_RESUME))
                    }
                    else -> {
                        countdown(b, t)
                        b.setContentText("Bici estática")
                        b.addAction(0, "Pausa", action(WorkoutService.ACTION_PAUSE))
                    }
                }
            }
            Phase.STRENGTH -> {
                val cur = s.current()
                if (cur == null) {
                    b.setContentTitle("💪 Fuerza completa").setContentText("Pasa a la bici final")
                } else {
                    b.setContentTitle("${Catalog.name(cur.exerciseId)} · Serie ${cur.sets.size + 1}/${cur.targetSets}")
                    b.setContentText(Fmt.target(s.draftReps, s.draftWeight))
                    b.addAction(0, "✓ Serie hecha", action(WorkoutService.ACTION_SET_DONE))
                }
            }
            Phase.REST -> {
                b.setContentTitle("⏱ Descanso")
                if (t != null) countdown(b, t)
                s.current()?.let {
                    b.setContentText(
                        "Sig.: ${Catalog.name(it.exerciseId)} · ${it.sets.size + 1}/${it.targetSets} · " +
                            Fmt.target(s.draftReps, s.draftWeight),
                    )
                }
                b.addAction(0, "+30 s", action(WorkoutService.ACTION_ADD_30))
                b.addAction(0, "Saltar", action(WorkoutService.ACTION_SKIP_REST))
            }
            Phase.SUMMARY -> b.setContentTitle("Sesión terminada").setContentText("Guárdala en la app")
        }
        return b.build()
    }

    private fun countdown(b: NotificationCompat.Builder, t: Timer) {
        val end = t.endAt ?: return
        b.setUsesChronometer(true).setChronometerCountDown(true).setWhen(end).setShowWhen(true)
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        ctx,
        0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(name: String): PendingIntent = PendingIntent.getService(
        ctx,
        name.hashCode(),
        Intent(ctx, WorkoutService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CH_ALERTS = "avisos_reloj"
        const val CH_SESSION = "sesion"
        const val ONGOING_ID = 1
        private const val FIRST_ALERT_ID = 1000
    }
}
