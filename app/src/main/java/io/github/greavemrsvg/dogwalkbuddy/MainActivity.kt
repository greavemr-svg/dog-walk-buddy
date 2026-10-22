package io.github.greavemrsvg.dogwalkbuddy

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.greavemrsvg.dogwalkbuddy.ui.nav.DogWalkBuddyNavHost
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DogWalkBuddyTheme {
                DogWalkBuddyNavHost(container = (application as DogWalkBuddyApp).container)
            }
        }
    }

    /**
     * The activity handles `uiMode` itself (AC7), so it is not re-created on a light/dark switch. The system bar
     * icon colours are set once by [enableEdgeToEdge], so they are set again here for the new theme.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdge()
    }
}
