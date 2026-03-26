package com.usharik.app.ads;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

/**
 * Centralises all banner-ad lifecycle logic so that individual Fragments
 * never need to touch the AdMob SDK directly.
 *
 * <p>Typical usage in a Fragment:
 * <pre>
 *   // field
 *   private BannerAdController bannerAdController;
 *
 *   // onViewCreated
 *   bannerAdController = new BannerAdController(adPolicy);
 *   bannerAdController.bind(requireContext(), binding.adViewContainer, adUnitId);
 *
 *   // onResume / onPause / onDestroyView delegate to the controller
 * </pre>
 *
 * <p>If {@link AdsPolicy#areAdsEnabled()} returns {@code false} the
 * container is hidden and no AdView is created or loaded — the ad SDK is never touched.
 */
public class BannerAdController {

    private final AdsPolicy adPolicy;
    private AdView adView;

    public BannerAdController(AdsPolicy adPolicy) {
        this.adPolicy = adPolicy;
    }

    /**
     * Create, attach, and load the banner ad into {@code container}.
     *
     * <p>Must be called from {@code onViewCreated}.  Safe to call on every
     * view recreation because it always calls {@code removeAllViews()} first.
     *
     * @param context   any non-null context (use {@code requireContext()})
     * @param container the {@link ViewGroup} that will host the banner
     * @param adUnitId  AdMob banner ad-unit ID (from {@code BuildConfig})
     */
    public void bind(Context context, ViewGroup container, String adUnitId) {
        if (!adPolicy.areAdsEnabled()) {
            container.setVisibility(View.GONE);
            return;
        }

        adView = new AdView(context);
        adView.setAdUnitId(adUnitId);
        adView.setAdSize(AdSize.BANNER);

        container.removeAllViews();
        container.addView(adView);

        adView.loadAd(new AdRequest.Builder().build());
    }

    /** Call from {@code Fragment.onResume()}. */
    public void onResume() {
        if (adView != null) {
            adView.resume();
        }
    }

    /** Call from {@code Fragment.onPause()}. */
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
    }

    /**
     * Destroy the AdView and release references.
     *
     * <p>Must be called from {@code Fragment.onDestroyView()} — <em>not</em>
     * {@code onDestroy()} — to avoid dangling references to the old view tree.
     */
    public void onDestroyView() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
    }
}
