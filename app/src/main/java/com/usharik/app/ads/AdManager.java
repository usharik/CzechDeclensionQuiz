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

/**
 * Manages interstitial ads for the quiz application.
 * Shows ads after every 5 words completed.
 */
@Singleton
public class AdManager {
    private static final String TAG = "AdManager";

    private InterstitialAd interstitialAd;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    
    @Inject
    public AdManager() {
    }
    
    /**
     * Load an interstitial ad.
     * Should be called after showing an ad or when the app starts.
     */
    public void loadAd(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "Cannot load ad: activity is null");
            return;
        }
        
        if (isLoadingAd || interstitialAd != null) {
            Log.d(TAG, "Ad already loaded or loading");
            return;
        }
        
        isLoadingAd = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        // Use BuildConfig to get the correct ad unit ID for debug/release
        String adUnitId = BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID;
        Log.d(TAG, "Loading ad with unit ID: " + adUnitId);

        InterstitialAd.load(
            activity,
            adUnitId,
            adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    Log.i(TAG, "Interstitial ad loaded successfully");
                    interstitialAd = ad;
                    isLoadingAd = false;
                    setupAdCallbacks();
                }
                
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.w(TAG, "Failed to load interstitial ad: " + loadAdError.getMessage());
                    interstitialAd = null;
                    isLoadingAd = false;
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
        if (activity == null) {
            Log.w(TAG, "Cannot show ad: activity is null");
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
        
        if (interstitialAd != null) {
            isShowingAd = true;
            
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.i(TAG, "Ad was dismissed");
                    interstitialAd = null;
                    isShowingAd = false;
                    
                    // Load next ad
                    loadAd(activity);
                    
                    // Run callback
                    if (onAdClosed != null) {
                        onAdClosed.run();
                    }
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.w(TAG, "Ad failed to show: " + adError.getMessage());
                    interstitialAd = null;
                    isShowingAd = false;
                    
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
            Log.d(TAG, "Ad not ready yet, loading...");
            loadAd(activity);
            
            // Run callback immediately if ad not ready
            if (onAdClosed != null) {
                onAdClosed.run();
            }
        }
    }
    
    private void setupAdCallbacks() {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.i(TAG, "Ad dismissed");
                    interstitialAd = null;
                    isShowingAd = false;
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.w(TAG, "Ad failed to show: " + adError.getMessage());
                    interstitialAd = null;
                    isShowingAd = false;
                }
            });
        }
    }
    
    /**
     * Check if an ad is ready to be shown
     */
    public boolean isAdReady() {
        return interstitialAd != null && !isShowingAd;
    }
}

