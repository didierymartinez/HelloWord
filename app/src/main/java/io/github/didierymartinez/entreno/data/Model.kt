package io.github.didierymartinez.entreno.data

import kotlinx.serialization.Serializable

/** Músculos que se pintan en el mapa corporal. */
enum class Muscle(val label: String) {
    PECHO("Pecho"),
    DORSALES("Dorsales"),
    TRAPECIO("Trapecio"),
    LUMBAR("Lumbar"),
    HOMBROS("Hombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    ANTEBRAZO("Antebrazo"),
    ABDOMEN("Abdomen"),
    CUADRICEPS("Cuádriceps"),
    ISQUIOS("Isquiotibiales"),
    GLUTEOS("Glúteos"),
    GEMELOS("Gemelos"),
}

/** Grupos de la rutina diaria ("hoy toca…"). */
enum class DayGroup(val label: String) {
    PECHO("Pecho"),
    ESPALDA("Espalda"),
    BRAZO("Brazo"),
    PIERNA("Pierna"),
    HOMBRO("Hombro"),
    ABDOMEN("Abdomen"),
}

/** Ejercicio del catálogo (no se guarda: vive en [Catalog]). */
data class Exercise(
    val id: String,
    val name: String,
    val group: DayGroup,
    val primary: List<Muscle>,
    val secondary: List<Muscle> = emptyList(),
    val bodyweight: Boolean = false,
)

/** Ejercicio por defecto dentro de la plantilla de un grupo. */
data class TemplateItem(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val restSec: Int = 90,
)

// ---------- Historial (lo que se guarda) ----------

@Serializable
data class SetLog(val reps: Int, val weightKg: Double, val at: Long)

@Serializable
data class ExerciseLog(val exerciseId: String, val sets: List<SetLog>)

@Serializable
enum class CardioKind { WARMUP, COOLDOWN }

@Serializable
data class CardioLog(
    val kind: CardioKind,
    val durationSec: Int,
    val distanceKm: Double? = null,
    val kcal: Int? = null,
    /** Inicio y fin reales del bloque (para separar el pulso por tramos). */
    val startedAt: Long? = null,
    val endedAt: Long? = null,
)

/** Una medición de pulso (ya promediada en tramos cortos). */
@Serializable
data class HrPoint(val t: Long, val bpm: Int)

/** Pulso medio / máximo / mínimo de un tramo, calculado con los datos originales. */
@Serializable
data class HrStat(val label: String, val avg: Int, val max: Int, val min: Int)

/** Pulso y calorías de la sesión leídos de Health Connect (vienen del reloj vía Mi Fitness). */
@Serializable
data class HeartData(
    /** Puntos promediados cada 15 s, para la gráfica. */
    val points: List<HrPoint>,
    /** Toda la sesión. */
    val overall: HrStat,
    /** Bici inicio, pesas, bici final. */
    val segments: List<HrStat> = emptyList(),
    val activeKcal: Int? = null,
    val totalKcal: Int? = null,
    val fetchedAt: Long,
)

@Serializable
data class SessionLog(
    val id: String,
    val startedAt: Long,
    val endedAt: Long,
    val groups: List<DayGroup>,
    val warmup: CardioLog? = null,
    val cooldown: CardioLog? = null,
    val exercises: List<ExerciseLog> = emptyList(),
    val heart: HeartData? = null,
)

@Serializable
data class Settings(
    val rotation: List<DayGroup> = listOf(
        DayGroup.PECHO, DayGroup.ESPALDA, DayGroup.BRAZO, DayGroup.PIERNA, DayGroup.HOMBRO,
    ),
    val warmupMin: Int = 10,
    val cooldownMin: Int = 15,
    val defaultRestSec: Int = 90,
    val weightStep: Double = 2.5,
    val warnBeforeRestEnd: Boolean = true,
    val keepScreenOn: Boolean = false,
)

// ---------- Sesión en curso ----------

@Serializable
enum class Phase { WARMUP, STRENGTH, REST, COOLDOWN, SUMMARY }

@Serializable
data class PlanItem(
    val uid: String,
    val exerciseId: String,
    val targetSets: Int,
    val targetReps: Int,
    val weightKg: Double,
    val restSec: Int,
    val sets: List<SetLog> = emptyList(),
    val finished: Boolean = false,
    val skipped: Boolean = false,
)

@Serializable
data class Timer(
    val totalSec: Int,
    /** Momento de fin si está corriendo; null si está en pausa. */
    val endAt: Long? = null,
    /** Milisegundos restantes cuando está en pausa. */
    val pausedMs: Long? = null,
    val warned: Boolean = false,
    val halfNotified: Boolean = false,
) {
    val running: Boolean get() = endAt != null

    fun remaining(now: Long): Long =
        endAt?.let { maxOf(0L, it - now) } ?: (pausedMs ?: totalSec * 1000L)
}

@Serializable
data class ActiveSession(
    val id: String,
    val startedAt: Long,
    val groups: List<DayGroup>,
    val plan: List<PlanItem>,
    val currentUid: String?,
    val phase: Phase,
    val timer: Timer? = null,
    /** Mostrando el formulario opcional tras la bici (distancia / calorías). */
    val cardioForm: Boolean = false,
    val warmup: CardioLog? = null,
    val cooldown: CardioLog? = null,
    val draftReps: Int = 0,
    val draftWeight: Double = 0.0,
    val endedAt: Long? = null,
    /** Cuándo arrancó la bici en curso (las pausas cuentan dentro del tramo). */
    val cardioStartedAt: Long? = null,
)

fun ActiveSession.current(): PlanItem? = plan.firstOrNull { it.uid == currentUid }
