package com.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gymtracker.ui.GymApp
import com.gymtracker.ui.theme.GymTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as GymTrackerApp).container
        setContent {
            val mode = container.settings.themeMode.collectAsState()
            GymTrackerTheme(mode.value) {
                GymApp(container)
            }
        }
    }
}
