package io.github.didierymartinez.entreno.data

import io.github.didierymartinez.entreno.data.DayGroup as G
import io.github.didierymartinez.entreno.data.Muscle as M

/** Catálogo de ejercicios y plantillas por grupo. */
object Catalog {

    private fun ex(
        id: String,
        name: String,
        group: G,
        primary: List<M>,
        secondary: List<M> = emptyList(),
        bw: Boolean = false,
    ) = Exercise(id, name, group, primary, secondary, bw)

    val exercises: List<Exercise> = listOf(
        // Pecho
        ex("press_banca", "Press banca", G.PECHO, listOf(M.PECHO), listOf(M.TRICEPS, M.HOMBROS)),
        ex("press_inclinado_manc", "Press inclinado con mancuernas", G.PECHO, listOf(M.PECHO), listOf(M.HOMBROS, M.TRICEPS)),
        ex("press_mancuernas", "Press plano con mancuernas", G.PECHO, listOf(M.PECHO), listOf(M.TRICEPS, M.HOMBROS)),
        ex("press_maquina", "Press de pecho en máquina", G.PECHO, listOf(M.PECHO), listOf(M.TRICEPS)),
        ex("aperturas_manc", "Aperturas con mancuernas", G.PECHO, listOf(M.PECHO)),
        ex("cruce_poleas", "Cruce de poleas", G.PECHO, listOf(M.PECHO)),
        ex("pec_deck", "Pec deck (contractor)", G.PECHO, listOf(M.PECHO)),
        ex("fondos", "Fondos en paralelas", G.PECHO, listOf(M.PECHO), listOf(M.TRICEPS, M.HOMBROS), bw = true),
        ex("flexiones", "Flexiones", G.PECHO, listOf(M.PECHO), listOf(M.TRICEPS, M.HOMBROS), bw = true),

        // Espalda
        ex("dominadas", "Dominadas", G.ESPALDA, listOf(M.DORSALES), listOf(M.BICEPS), bw = true),
        ex("jalon_pecho", "Jalón al pecho", G.ESPALDA, listOf(M.DORSALES), listOf(M.BICEPS)),
        ex("remo_barra", "Remo con barra", G.ESPALDA, listOf(M.DORSALES, M.TRAPECIO), listOf(M.BICEPS, M.LUMBAR)),
        ex("remo_mancuerna", "Remo con mancuerna", G.ESPALDA, listOf(M.DORSALES), listOf(M.BICEPS)),
        ex("remo_polea", "Remo en polea baja", G.ESPALDA, listOf(M.DORSALES, M.TRAPECIO), listOf(M.BICEPS)),
        ex("remo_maquina", "Remo en máquina", G.ESPALDA, listOf(M.DORSALES, M.TRAPECIO), listOf(M.BICEPS)),
        ex("pullover_polea", "Pullover en polea", G.ESPALDA, listOf(M.DORSALES)),
        ex("peso_muerto", "Peso muerto", G.ESPALDA, listOf(M.LUMBAR, M.GLUTEOS, M.ISQUIOS), listOf(M.TRAPECIO, M.DORSALES)),
        ex("hiperextensiones", "Hiperextensiones", G.ESPALDA, listOf(M.LUMBAR), listOf(M.GLUTEOS), bw = true),

        // Brazo
        ex("curl_barra", "Curl con barra", G.BRAZO, listOf(M.BICEPS), listOf(M.ANTEBRAZO)),
        ex("curl_mancuernas", "Curl con mancuernas", G.BRAZO, listOf(M.BICEPS), listOf(M.ANTEBRAZO)),
        ex("curl_martillo", "Curl martillo", G.BRAZO, listOf(M.BICEPS, M.ANTEBRAZO)),
        ex("curl_predicador", "Curl predicador", G.BRAZO, listOf(M.BICEPS)),
        ex("curl_polea", "Curl en polea", G.BRAZO, listOf(M.BICEPS)),
        ex("press_frances", "Press francés", G.BRAZO, listOf(M.TRICEPS)),
        ex("extension_polea", "Extensión de tríceps en polea", G.BRAZO, listOf(M.TRICEPS)),
        ex("extension_cabeza", "Extensión sobre la cabeza", G.BRAZO, listOf(M.TRICEPS)),
        ex("patada_triceps", "Patada de tríceps", G.BRAZO, listOf(M.TRICEPS)),
        ex("fondos_banco", "Fondos en banco", G.BRAZO, listOf(M.TRICEPS), listOf(M.PECHO), bw = true),
        ex("curl_muneca", "Curl de muñeca", G.BRAZO, listOf(M.ANTEBRAZO)),

        // Pierna
        ex("sentadilla", "Sentadilla", G.PIERNA, listOf(M.CUADRICEPS, M.GLUTEOS), listOf(M.ISQUIOS, M.LUMBAR)),
        ex("prensa", "Prensa", G.PIERNA, listOf(M.CUADRICEPS, M.GLUTEOS), listOf(M.ISQUIOS)),
        ex("hack", "Sentadilla hack", G.PIERNA, listOf(M.CUADRICEPS), listOf(M.GLUTEOS)),
        ex("extension_cuadriceps", "Extensión de cuádriceps", G.PIERNA, listOf(M.CUADRICEPS)),
        ex("curl_femoral", "Curl femoral", G.PIERNA, listOf(M.ISQUIOS)),
        ex("peso_muerto_rumano", "Peso muerto rumano", G.PIERNA, listOf(M.ISQUIOS, M.GLUTEOS), listOf(M.LUMBAR)),
        ex("zancadas", "Zancadas", G.PIERNA, listOf(M.CUADRICEPS, M.GLUTEOS), listOf(M.ISQUIOS)),
        ex("hip_thrust", "Hip thrust", G.PIERNA, listOf(M.GLUTEOS), listOf(M.ISQUIOS)),
        ex("abductores", "Abductores en máquina", G.PIERNA, listOf(M.GLUTEOS)),
        ex("gemelos_pie", "Gemelos de pie", G.PIERNA, listOf(M.GEMELOS)),
        ex("gemelos_sentado", "Gemelos sentado", G.PIERNA, listOf(M.GEMELOS)),

        // Hombro
        ex("press_militar", "Press militar", G.HOMBRO, listOf(M.HOMBROS), listOf(M.TRICEPS)),
        ex("press_hombro_manc", "Press de hombro con mancuernas", G.HOMBRO, listOf(M.HOMBROS), listOf(M.TRICEPS)),
        ex("elevaciones_laterales", "Elevaciones laterales", G.HOMBRO, listOf(M.HOMBROS)),
        ex("elevaciones_frontales", "Elevaciones frontales", G.HOMBRO, listOf(M.HOMBROS)),
        ex("pajaros", "Pájaros (deltoides posterior)", G.HOMBRO, listOf(M.HOMBROS), listOf(M.TRAPECIO)),
        ex("face_pull", "Face pull", G.HOMBRO, listOf(M.HOMBROS, M.TRAPECIO)),
        ex("encogimientos", "Encogimientos", G.HOMBRO, listOf(M.TRAPECIO)),

        // Abdomen
        ex("crunch", "Crunch", G.ABDOMEN, listOf(M.ABDOMEN), bw = true),
        ex("elevacion_piernas", "Elevación de piernas", G.ABDOMEN, listOf(M.ABDOMEN), bw = true),
        ex("crunch_polea", "Crunch en polea", G.ABDOMEN, listOf(M.ABDOMEN)),
        ex("rueda", "Rueda abdominal", G.ABDOMEN, listOf(M.ABDOMEN), bw = true),
    )

    val byId: Map<String, Exercise> = exercises.associateBy { it.id }

    val templates: Map<G, List<TemplateItem>> = mapOf(
        G.PECHO to listOf(
            TemplateItem("press_banca", 4, 10, 120),
            TemplateItem("press_inclinado_manc", 3, 12),
            TemplateItem("aperturas_manc", 3, 12, 60),
            TemplateItem("cruce_poleas", 3, 15, 60),
            TemplateItem("fondos", 3, 10),
        ),
        G.ESPALDA to listOf(
            TemplateItem("jalon_pecho", 4, 10),
            TemplateItem("remo_barra", 4, 10, 120),
            TemplateItem("remo_mancuerna", 3, 12),
            TemplateItem("remo_polea", 3, 12),
            TemplateItem("pullover_polea", 3, 15, 60),
        ),
        G.BRAZO to listOf(
            TemplateItem("curl_barra", 4, 10),
            TemplateItem("press_frances", 4, 10),
            TemplateItem("curl_martillo", 3, 12, 60),
            TemplateItem("extension_polea", 3, 12, 60),
            TemplateItem("curl_predicador", 3, 12, 60),
        ),
        G.PIERNA to listOf(
            TemplateItem("sentadilla", 4, 10, 150),
            TemplateItem("prensa", 4, 12, 120),
            TemplateItem("extension_cuadriceps", 3, 15),
            TemplateItem("curl_femoral", 3, 12),
            TemplateItem("gemelos_pie", 4, 15, 60),
        ),
        G.HOMBRO to listOf(
            TemplateItem("press_militar", 4, 10, 120),
            TemplateItem("elevaciones_laterales", 4, 15, 60),
            TemplateItem("pajaros", 3, 15, 60),
            TemplateItem("face_pull", 3, 15, 60),
            TemplateItem("encogimientos", 3, 12, 60),
        ),
        G.ABDOMEN to listOf(
            TemplateItem("crunch", 3, 20, 45),
            TemplateItem("elevacion_piernas", 3, 15, 45),
            TemplateItem("rueda", 3, 10, 60),
        ),
    )

    fun name(id: String): String = byId[id]?.name ?: id

    fun byGroup(group: G): List<Exercise> = exercises.filter { it.group == group }

    /** Ejercicios que trabajan algún músculo principal en común (para máquinas ocupadas). */
    fun alternatives(id: String): List<Exercise> {
        val base = byId[id] ?: return emptyList()
        return exercises
            .filter { it.id != id && it.primary.any { m -> m in base.primary } }
            .sortedBy { if (it.group == base.group) 0 else 1 }
    }

    fun defaultsFor(id: String): TemplateItem? =
        templates.values.flatten().firstOrNull { it.exerciseId == id }
}
