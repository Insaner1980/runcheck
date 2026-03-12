package com.devicepulse

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.devicepulse.domain.repository.UserPreferencesRepository
import com.devicepulse.ui.navigation.DevicePulseNavHost
import com.devicepulse.ui.theme.DevicePulseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !isLocationEnabled()) {
            showLocationPrompt = true
        }
    }

    private var showLocationPrompt by mutableStateOf(false)

    private val locationModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != LocationManager.MODE_CHANGED_ACTION) return
            showLocationPrompt = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !isLocationEnabled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val permissionEducationSeen by preferencesRepository.getPermissionEducationSeen()
                .collectAsStateWithLifecycle(initialValue = false)

            var showPermissionEducation by rememberSaveable { mutableStateOf(false) }
            var showLocationServicesDialog by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(permissionEducationSeen) {
                if (!permissionEducationSeen && missingRuntimePermissions().isNotEmpty()) {
                    showPermissionEducation = true
                } else if (
                    permissionEducationSeen &&
                    hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    !isLocationEnabled()
                ) {
                    showLocationServicesDialog = true
                }
            }

            DevicePulseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DevicePulseNavHost()
                }

                if (showPermissionEducation) {
                    AlertDialog(
                        onDismissRequest = {
                            showPermissionEducation = false
                            markPermissionEducationSeen()
                        },
                        title = { Text(text = getString(R.string.permissions_intro_title)) },
                        text = { Text(text = getString(R.string.permissions_intro_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showPermissionEducation = false
                                    markPermissionEducationSeen()
                                    requestAppPermissions()
                                }
                            ) {
                                Text(text = getString(R.string.permissions_intro_continue))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showPermissionEducation = false
                                    markPermissionEducationSeen()
                                }
                            ) {
                                Text(text = getString(R.string.permissions_intro_not_now))
                            }
                        }
                    )
                }

                if (showLocationServicesDialog || showLocationPrompt) {
                    AlertDialog(
                        onDismissRequest = {
                            showLocationServicesDialog = false
                            showLocationPrompt = false
                        },
                        title = { Text(text = getString(R.string.location_services_title)) },
                        text = { Text(text = getString(R.string.location_services_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showLocationServicesDialog = false
                                    showLocationPrompt = false
                                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                }
                            ) {
                                Text(text = getString(R.string.location_services_open_settings))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showLocationServicesDialog = false
                                    showLocationPrompt = false
                                }
                            ) {
                                Text(text = getString(R.string.permissions_intro_not_now))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !isLocationEnabled()) {
            showLocationPrompt = true
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            locationModeReceiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        unregisterReceiver(locationModeReceiver)
        super.onStop()
    }

    private fun requestAppPermissions() {
        val permissions = missingRuntimePermissions()
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !isLocationEnabled()) {
            showLocationPrompt = true
        }
    }

    private fun missingRuntimePermissions(): List<String> = buildList {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun markPermissionEducationSeen() {
        lifecycleScope.launch {
            preferencesRepository.setPermissionEducationSeen(true)
        }
    }
}
