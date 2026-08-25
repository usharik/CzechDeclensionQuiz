package com.usharik.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.usharik.app.App

/** Hides a failed banner rather than leaving an inert grey rectangle on screen. */
@Composable
fun BannerAd(app: App, unitId: String, modifier: Modifier = Modifier) {
    if (!app.adPolicy.areAdsEnabled()) return
    var failed by remember(unitId) { mutableStateOf(false) }
    if (failed) return
    val context = LocalContext.current
    val adView = remember(unitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = unitId
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) { failed = true }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
    AndroidView(factory = { adView }, modifier = modifier.fillMaxWidth().height(50.dp))
}
