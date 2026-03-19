package com.usharik.app.ads;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.usharik.app.BuildConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages interstitial ads for the quiz application.
 * Shows ads after every 5 words completed.
 */
@Singleton
public class AdManager {
    private static final String TAG = "AdManager";

    private final Map<String, InterstitialAd> interstitialAds = new HashMap<>();
    private final Set<String> loadingAdUnitIds = new HashSet<>();
    private boolean isShowingAd = false;
    
    @Inject
    public AdManager() {
    }
    
    /**
     * Load an interstitial ad.
     * Should be called after showing an ad or when the app starts.
     */
    public void loadAd(Activity activity) {
        loadAd(activity, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID);
    }

    public void loadAd(Activity activity, String adUnitId) {
        if (activity == null) {
            Log.w(TAG, "Cannot load ad: activity is null");
            return;
        }

        if (adUnitId == null || adUnitId.isBlank()) {
            Log.w(TAG, "Cannot load ad: ad unit id is blank");
            return;
        }

        if (loadingAdUnitIds.contains(adUnitId) || interstitialAds.containsKey(adUnitId)) {
            Log.d(TAG, "Ad already loaded or loading for unit ID: " + adUnitId);
            return;
        }

        loadingAdUnitIds.add(adUnitId);
        AdRequest adRequest = new AdRequest.Builder().build();

        Log.d(TAG, "Loading ad with unit ID: " + adUnitId);

        InterstitialAd.load(
            activity,
            adUnitId,
            adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    Log.i(TAG, "Interstitial ad loaded successfully");
                    interstitialAds.put(adUnitId, ad);
                    loadingAdUnitIds.remove(adUnitId);
                }
                
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.w(TAG, "Failed to load interstitial ad: " + loadAdError.getMessage());
                    interstitialAds.remove(adUnitId);
                    loadingAdUnitIds.remove(adUnitId);
                }
            }
        );
    }
    
    /**
     * Show the interstitial ad if it's loaded.
     * @param activity The activity to show the ad in
     * @param onAdClosed Callback to run after ad is closed
     */
    public void showAd(Activity activity, Runnable onAdClosed) {
        showAd(activity, BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID, onAdClosed);
    }

    public void showAd(Activity activity, String adUnitId, Runnable onAdClosed) {
        if (activity == null) {
            Log.w(TAG, "Cannot show ad: activity is null");
            if (onAdClosed != null) {
                onAdClosed.run();
            }
            return;
        }

        if (adUnitId == null || adUnitId.isBlank()) {
            Log.w(TAG, "Cannot show ad: ad unit id is blank");
            if (onAdClosed != null) {
                onAdClosed.run();
            }
            return;
        }
        
        if (isShowingAd) {
            Log.d(TAG, "Ad is already showing");
            if (onAdClosed != null) {
                onAdClosed.run();
            }
            return;
        }

        InterstitialAd interstitialAd = interstitialAds.get(adUnitId);
        if (interstitialAd != null) {
            isShowingAd = true;
            
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.i(TAG, "Ad was dismissed");
                    interstitialAds.remove(adUnitId);
                    isShowingAd = false;
                    
                    // Load next ad
                    loadAd(activity, adUnitId);
                    
                    // Run callback
                    if (onAdClosed != null) {
                        onAdClosed.run();
                    }
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.w(TAG, "Ad failed to show: " + adError.getMessage());
                    interstitialAds.remove(adUnitId);
                    isShowingAd = false;
                    loadAd(activity, adUnitId);
                    
                    // Run callback even if ad failed
                    if (onAdClosed != null) {
                        onAdClosed.run();
                    }
                }
                
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.i(TAG, "Ad showed full screen content");
                }
            });
            
            interstitialAd.show(activity);
        } else {
            Log.d(TAG, "Ad not ready yet for unit ID " + adUnitId + ", loading...");
            loadAd(activity, adUnitId);
            
            // Run callback immediately if ad not ready
            if (onAdClosed != null) {
                onAdClosed.run();
            }
        }
    }
}

