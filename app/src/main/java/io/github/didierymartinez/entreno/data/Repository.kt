package io.github.didierymartinez.entreno.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Guarda historial, ajustes y la sesión en curso como JSON en el almacenamiento interno. */
class Repository(private val dir: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _history = MutableStateFlow<List<SessionLog>>(read(HISTORY) ?: emptyList())
    val history: StateFlow<List<SessionLog>> = _history.asStateFlow()

    private val _settings = MutableStateFlow(read<Settings>(SETTINGS) ?: Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun loadActive(): ActiveSession? = read(ACTIVE)

    @Synchronized
    fun saveActive(session: ActiveSession?) {
        if (session == null) File(dir, ACTIVE).delete() else write(ACTIVE, session)
    }

    @Synchronized
    fun addSession(log: SessionLog) {
        val list = _history.value.filter { it.id != log.id } + log
        _history.value = list
        write(HISTORY, list)
    }

    @Synchronized
    fun deleteSession(id: String) {
        val list = _history.value.filter { it.id != id }
        _history.value = list
        write(HISTORY, list)
    }

    @Synchronized
    fun updateSettings(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        _settings.value = next
        write(SETTINGS, next)
    }

    private inline fun <reified T> read(name: String): T? {
        val file = File(dir, name)
        if (!file.exists()) return null
        return try {
            json.decodeFromString<T>(file.readText())
        } catch (e: Exception) {
            // No perder datos: se aparta el archivo dañado en vez de sobrescribirlo.
            file.renameTo(File(dir, "$name.danado-${System.currentTimeMillis()}"))
            null
        }
    }

    private inline fun <reified T> write(name: String, value: T) {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(json.encodeToString(value))
        tmp.renameTo(File(dir, name))
    }

    private companion object {
        const val HISTORY = "historial.json"
        const val SETTINGS = "ajustes.json"
        const val ACTIVE = "sesion_activa.json"
    }
}
