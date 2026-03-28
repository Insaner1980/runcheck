package com.runcheck

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.runcheck.di.DatabaseModule
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.ui.navigation.RuncheckNavHost
import com.runcheck.ui.navigation.Screen
import com.runcheck.ui.theme.RuncheckTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deepLinkRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkDatabaseReset()
        deepLinkRoute.value = consumeNotificationRoute(intent)
        setContent {
            RuncheckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RuncheckNavHost(
                        deepLinkRoute = deepLinkRoute.value,
                        onDeepLinkConsumed = { deepLinkRoute.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkRoute.value = consumeNotificationRoute(intent)
    }

    private fun checkDatabaseReset() {
        val prefs = getSharedPreferences(DatabaseModule.DB_EVENT_PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(DatabaseModule.KEY_DB_RESET, false)) {
            prefs.edit().remove(DatabaseModule.KEY_DB_RESET).apply()
            Toast
                .makeText(
                    this,
                    getString(R.string.database_reset_notice),
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    private fun consumeNotificationRoute(intent: Intent?): String? {
        val route =
            intent
                ?.getStringExtra(NotificationHelper.EXTRA_NAVIGATE_TO)
                ?.takeIf(Screen::isDirectRoute)
        intent?.removeExtra(NotificationHelper.EXTRA_NAVIGATE_TO)
        return route
    }
}
