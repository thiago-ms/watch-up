package br.com.shopper.watchup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.shopper.watchup.core.ui.theme.WatchUpTheme
import br.com.shopper.watchup.navigation.WatchUpApp

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
