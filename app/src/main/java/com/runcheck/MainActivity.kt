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
import androidx.lifecycle.lifecycleScope
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.di.DatabaseModule
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.ui.navigation.RuncheckNavHost
import com.runcheck.ui.navigation.Screen
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deepLinkRoute = mutableStateOf<String?>(null)

    @Inject
    lateinit var proPurchaseManager: ProPurchaseManager

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
                        onConsumeDeepLink = { deepLinkRoute.value = null },
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

    @Suppress("TooGenericExceptionCaught") // Billing refresh must not crash the resumed activity.
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                proPurchaseManager.refreshPurchaseStatus()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to refresh purchases on resume", e)
            }
        }
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

    private companion object {
        private const val TAG = "MainActivity"
    }
}
