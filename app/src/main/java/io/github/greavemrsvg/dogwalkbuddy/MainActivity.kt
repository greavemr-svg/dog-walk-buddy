package io.github.greavemrsvg.dogwalkbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.greavemrsvg.dogwalkbuddy.ui.home.HomeScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DogWalkBuddyTheme {
                HomeScreen()
            }
        }
    }
}
