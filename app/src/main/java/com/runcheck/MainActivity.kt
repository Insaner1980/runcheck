package com.runcheck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.ui.navigation.RuncheckNavHost
import com.runcheck.ui.theme.RuncheckTheme
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkRoute = intent?.getStringExtra(NotificationHelper.EXTRA_NAVIGATE_TO)
        setContent {
            RuncheckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RuncheckNavHost(deepLinkRoute = deepLinkRoute)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Recreate so the new deep-link route is picked up by RuncheckNavHost
        recreate()
    }
}
