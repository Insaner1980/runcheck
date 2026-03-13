package com.devicepulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.devicepulse.ui.navigation.DevicePulseNavHost
import com.devicepulse.ui.theme.DevicePulseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevicePulseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DevicePulseNavHost()
                }
            }
        }
    }
}
