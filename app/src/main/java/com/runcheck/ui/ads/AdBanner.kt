package com.runcheck.ui.ads

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.runcheck.BuildConfig
import com.runcheck.pro.ProState

@SuppressLint("MissingPermission")
@Composable
fun AdBanner(
    proState: ProState,
    modifier: Modifier = Modifier
) {
    if (proState.isPro) return

    val adUnitId = if (BuildConfig.DEBUG) {
        TEST_AD_UNIT_ID
    } else {
        BuildConfig.ADMOB_BANNER_ID
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun DetailScreenAdBanner(
    modifier: Modifier = Modifier,
    viewModel: AdBannerViewModel = hiltViewModel()
) {
    val proState by viewModel.proState.collectAsStateWithLifecycle()
    AdBanner(proState = proState, modifier = modifier)
}

private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
