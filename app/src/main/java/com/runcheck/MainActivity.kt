package com.runcheck

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deepLinkRoute = mutableStateOf<String?>(null)
    private val appThemeViewModel: AppThemeViewModel by viewModels()
    private val systemBarsReady = AtomicBoolean(false)

    @Inject
    lateinit var proPurchaseManager: ProPurchaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            shouldKeepSplashOnScreen(
                themeMode = appThemeViewModel.themeMode.value,
                systemBarsReady = systemBarsReady.get(),
            )
        }
        lifecycleScope.launch {
            appThemeViewModel.themeMode
                .filterNotNull()
                .collect { themeMode ->
                    applySystemBarAppearance(themeMode)
                    systemBarsReady.set(true)
                }
        }
        checkDatabaseReset()
        deepLinkRoute.value = consumeNotificationRoute(intent)
        setContent {
            val themeMode by appThemeViewModel.themeMode.collectAsStateWithLifecycle()
            themeMode?.let { readyThemeMode ->
                RuncheckTheme(themeMode = readyThemeMode) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        RuncheckNavHost(
                            deepLinkRoute = deepLinkRoute.value,
                            onConsumeDeepLink = { deepLinkRoute.value = null },
                        )
                    }
                }
            }
        }
    }

    private fun applySystemBarAppearance(themeMode: com.runcheck.domain.model.ThemeMode) {
        val systemInDarkTheme =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        val appearance =
            resolveSystemBarAppearance(
                themeMode = themeMode,
                systemInDarkTheme = systemInDarkTheme,
            )
        val style =
            if (appearance.isDarkTheme) {
                SystemBarStyle.dark(scrim = Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    scrim = Color.TRANSPARENT,
                    darkScrim = getColor(R.color.system_navigation_bar_contrast_scrim),
                )
            }
        enableEdgeToEdge(
            statusBarStyle = style,
            navigationBarStyle = style,
        )
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
