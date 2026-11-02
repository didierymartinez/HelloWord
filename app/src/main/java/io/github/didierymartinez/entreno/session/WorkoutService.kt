package io.github.didierymartinez.entreno.session

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.didierymartinez.entreno.Graph
import io.github.didierymartinez.entreno.data.Phase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano mientras dura la sesión: mantiene vivos los
 * temporizadores con la pantalla apagada y lanza los avisos que llegan al reloj.
 */
class WorkoutService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, Notifier.ONGOING_ID, Graph.notifier.ongoing(Graph.session.state.value), type)

        scope.launch {
            Graph.session.state.collect { s ->
                if (s == null || s.phase == Phase.SUMMARY) {
                    shutdown()
                } else {
                    Graph.notifier.updateOngoing(s)
                    holdWakeLock(s.timer?.running == true)
                }
            }
        }
        scope.launch {
            while (isActive) {
                Graph.session.tick()
                delay(500)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_DONE -> Graph.session.completeSet()
            ACTION_SKIP_REST -> Graph.session.skipRest()
            ACTION_ADD_30 -> Graph.session.addRest(30)
            ACTION_PAUSE -> Graph.session.pauseTimer()
            ACTION_RESUME -> Graph.session.resumeTimer()
        }
        return START_NOT_STICKY
    }

    private fun holdWakeLock(on: Boolean) {
        if (on && wakeLock == null) {
            val pm = getSystemService(PowerManager::class.java)
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "entreno:temporizador").apply {
                setReferenceCounted(false)
                acquire(3 * 60 * 60 * 1000L)
            }
        } else if (!on) {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        }
    }

    private fun shutdown() {
        holdWakeLock(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        holdWakeLock(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SET_DONE = "entreno.SERIE_HECHA"
        const val ACTION_SKIP_REST = "entreno.SALTAR_DESCANSO"
        const val ACTION_ADD_30 = "entreno.MAS_30"
        const val ACTION_PAUSE = "entreno.PAUSA"
        const val ACTION_RESUME = "entreno.REANUDAR"

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, WorkoutService::class.java))
        }
    }
}
