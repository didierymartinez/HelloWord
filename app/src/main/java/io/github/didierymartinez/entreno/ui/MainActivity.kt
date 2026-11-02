package io.github.didierymartinez.entreno.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.didierymartinez.entreno.Graph
import io.github.didierymartinez.entreno.data.Phase
import io.github.didierymartinez.entreno.session.WorkoutService

class MainActivity : ComponentActivity() {

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Si la app se cerró a mitad de sesión, se retoma donde iba.
        Graph.session.state.value?.let { if (it.phase != Phase.SUMMARY) WorkoutService.start(this) }
        setContent { EntrenoTheme { AppRoot() } }
    }
}

enum class Screen { HOME, HISTORY, RECOVERY, SETTINGS }

@Composable
fun AppRoot() {
    val session by Graph.session.state.collectAsState()
    val history by Graph.repo.history.collectAsState()
    val settings by Graph.repo.settings.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    val active = session
    if (active != null) {
        SessionScreen(active, history, settings)
    } else {
        BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
        val back = { screen = Screen.HOME }
        when (screen) {
            Screen.HOME -> HomeScreen(history, settings, onNavigate = { screen = it })
            Screen.HISTORY -> HistoryScreen(history, onBack = back)
            Screen.RECOVERY -> RecoveryScreen(history, onBack = back)
            Screen.SETTINGS -> SettingsScreen(settings, onBack = back)
        }
    }
}
