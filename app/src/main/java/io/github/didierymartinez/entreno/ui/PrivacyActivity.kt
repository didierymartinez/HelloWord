package io.github.didierymartinez.entreno.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pantalla que Health Connect muestra cuando el usuario pregunta para qué se usan los datos. */
class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EntrenoTheme {
                Scaffold(topBar = { BackTopBar("Uso de datos de salud") { finish() } }) { pad ->
                    Column(
                        Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Entreno lee de Health Connect, solo mientras usas la app:")
                        Text("• Frecuencia cardíaca, para mostrar tu pulso medio y máximo en cada sesión de gym.")
                        Text("• Calorías activas y totales, para el resumen de cada sesión.")
                        Text("Solo se leen los datos de las horas en que entrenaste.")
                        Text("Los datos se guardan únicamente en este teléfono, dentro de la app. No se envían a ningún servidor ni se comparten con nadie.")
                        Text("Entreno nunca escribe ni modifica datos en Health Connect.")
                        Text("Puedes retirar el permiso cuando quieras desde Health Connect → Permisos de apps → Entreno.")
                    }
                }
            }
        }
    }
}
