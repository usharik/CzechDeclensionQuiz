package com.usharik.app.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.usharik.app.App

/**
 * Anchored adaptive banner. Hides a failed banner rather than leaving an inert grey rectangle on screen.
 *
 * The ad is requested [widthFraction] of the available width wide and centered within it.
 */
@Composable
fun BannerAd(app: App, unitId: String, modifier: Modifier = Modifier, widthFraction: Float = 0.8f) {
    if (!app.adPolicy.areAdsEnabled()) return
    var failed by remember(unitId) { mutableStateOf(false) }
    if (failed) return
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val context = LocalContext.current
        val adWidthDp = (maxWidth.value * widthFraction).toInt()
        val size = remember(adWidthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
        }
        BannerAdView(unitId, size, onFailed = { failed = true }, Modifier.width(adWidthDp.dp).wrapContentHeight())
    }
}

@Composable
private fun BannerAdView(unitId: String, size: AdSize, onFailed: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adView = remember(unitId, size) {
        AdView(context).apply {
            setAdSize(size)
            adUnitId = unitId
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) { onFailed() }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }
    AndroidView(factory = { adView }, modifier = modifier)
}
