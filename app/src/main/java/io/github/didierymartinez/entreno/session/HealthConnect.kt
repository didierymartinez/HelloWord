package io.github.didierymartinez.entreno.session

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.github.didierymartinez.entreno.data.HeartData
import io.github.didierymartinez.entreno.data.HrPoint
import io.github.didierymartinez.entreno.data.SessionLog
import io.github.didierymartinez.entreno.domain.Heart
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Lee de Health Connect el pulso y las calorías que Mi Fitness copia desde el reloj.
 * Solo lectura: la app nunca escribe datos de salud.
 */
class HealthConnect(private val ctx: Context) {

    enum class Availability { AVAILABLE, NEEDS_UPDATE, NOT_INSTALLED }

    sealed interface Result {
        data class Found(val data: HeartData) : Result
        /** Health Connect responde, pero aún no tiene pulso de esa hora (falta sincronizar el reloj). */
        data object NoData : Result
        data object NoPermission : Result
        data class Unavailable(val availability: Availability) : Result
        data class Failed(val message: String) : Result
    }

    val heartPermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val activeKcalPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val totalKcalPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    val permissions: Set<String> = setOf(heartPermission, activeKcalPermission, totalKcalPermission)

    private val client by lazy { HealthConnectClient.getOrCreate(ctx) }

    fun availability(): Availability = when (HealthConnectClient.getSdkStatus(ctx)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_UPDATE
        else -> Availability.NOT_INSTALLED
    }

    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    suspend fun granted(): Set<String> =
        if (availability() == Availability.AVAILABLE) client.permissionController.getGrantedPermissions() else emptySet()

    suspend fun hasHeartPermission(): Boolean = heartPermission in granted()

    suspend fun fetch(log: SessionLog, now: Long = System.currentTimeMillis()): Result {
        val availability = availability()
        if (availability != Availability.AVAILABLE) return Result.Unavailable(availability)
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            if (heartPermission !in granted) return Result.NoPermission
            val (from, to) = Heart.fetchWindow(log)
            val start = Instant.ofEpochMilli(from)
            val end = Instant.ofEpochMilli(to)
            val raw = readHeartRate(start, end)
            val session = TimeRangeFilter.between(Instant.ofEpochMilli(log.startedAt), Instant.ofEpochMilli(log.endedAt))
            val active = if (activeKcalPermission in granted) readKcal(session, active = true) else null
            val total = if (totalKcalPermission in granted) readKcal(session, active = false) else null
            Heart.build(log, raw, active, total, now)?.let { Result.Found(it) } ?: Result.NoData
        } catch (e: SecurityException) {
            Result.NoPermission
        } catch (e: Exception) {
            Result.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private suspend fun readHeartRate(start: Instant, end: Instant): List<HrPoint> {
        val out = mutableListOf<HrPoint>()
        var token: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    pageToken = token,
                ),
            )
            for (record in response.records) {
                for (sample in record.samples) {
                    if (!sample.time.isBefore(start) && !sample.time.isAfter(end)) {
                        out += HrPoint(sample.time.toEpochMilli(), sample.beatsPerMinute.toInt())
                    }
                }
            }
            token = response.pageToken
        } while (token != null)
        return out.sortedBy { it.t }
    }

    private suspend fun readKcal(range: TimeRangeFilter, active: Boolean): Int? {
        val metric = if (active) ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL else TotalCaloriesBurnedRecord.ENERGY_TOTAL
        val result = client.aggregate(AggregateRequest(metrics = setOf(metric), timeRangeFilter = range))
        return result[metric]?.inKilocalories?.roundToInt()?.takeIf { it > 0 }
    }

    /** Abre Health Connect (o la tienda para instalarlo / actualizarlo). */
    fun openIntent(): Intent = if (availability() == Availability.AVAILABLE) {
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
    } else {
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding"))
            .setPackage("com.android.vending")
    }

    companion object {
        const val PROVIDER = "com.google.android.apps.healthdata"
    }
}
