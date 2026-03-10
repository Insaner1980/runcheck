package com.devicepulse

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.data.preferences.UserPreferencesRepository
import com.devicepulse.domain.model.ThemeMode
import com.devicepulse.ui.navigation.DevicePulseNavHost
import com.devicepulse.ui.theme.DevicePulseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Results handled silently — features degrade gracefully without permissions */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAppPermissions()
        setContent {
            val prefs by preferencesRepository.getPreferences()
                .collectAsStateWithLifecycle(initialValue = null)

            val darkTheme = when (prefs?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            DevicePulseTheme(
                darkTheme = darkTheme,
                amoledBlack = prefs?.amoledBlack ?: false,
                dynamicColor = prefs?.dynamicColors ?: true
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DevicePulseNavHost()
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
