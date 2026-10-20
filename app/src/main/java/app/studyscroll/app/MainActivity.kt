package app.studyscroll.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.studyscroll.ui.theme.StudyScrollTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as StudyScrollApplication

        setContent {
            val settings by app.settingsRepository.settingsFlow.collectAsState(
                initial = app.studyscroll.data.repository.AppSettings()
            )

            StudyScrollTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    StudyScrollNavGraph(
                        navController = navController,
                        app = app
                    )
                }
            }
        }
    }
}
