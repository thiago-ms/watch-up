package br.com.watchup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.watchup.core.ui.theme.WatchUpTheme
import br.com.watchup.navigation.WatchUpApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WatchUpTheme {
                WatchUpApp()
            }
        }
    }
}
