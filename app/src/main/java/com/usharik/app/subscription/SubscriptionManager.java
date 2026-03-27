package com.usharik.app.subscription;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.usharik.app.BuildConfig;
import com.usharik.app.fragment.SettingsFragment;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SubscriptionManager implements PremiumAccess {
    private static final String SUBSCRIPTION_PLAN_KEY = "subscriptionPlan";
    private static final String SUBSCRIPTION_SOURCE_KEY = "subscriptionSource";
    private static final String SUBSCRIPTION_PRODUCT_ID_KEY = "subscriptionProductId";

    private final SharedPreferences preferences;
    private SubscriptionStatus status;

    @Inject
    public SubscriptionManager(Application application) {
        preferences = application.getSharedPreferences(SettingsFragment.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        status = readStatus();
    }

    @Override
    public synchronized boolean hasPremiumAccess() {
        return status.hasPremiumAccess();
    }

    @Override
    public synchronized SubscriptionStatus getStatus() {
        return status;
    }

    public synchronized void grantPremiumFromBilling(String productId) {
        updateStatus(SubscriptionStatus.premium(SubscriptionSource.PLAY_BILLING, productId));
    }

    public synchronized void clearSubscription() {
        updateStatus(SubscriptionStatus.free());
    }

    public synchronized void setDebugPremiumEnabled(boolean enabled) {
        updateStatus(enabled
                ? SubscriptionStatus.premium(SubscriptionSource.DEBUG, BuildConfig.PLAY_SUBSCRIPTION_PRODUCT_ID)
                : SubscriptionStatus.free());
    }

    private SubscriptionStatus readStatus() {
        SubscriptionPlan plan = SubscriptionPlan.fromPreference(preferences.getString(SUBSCRIPTION_PLAN_KEY, null));
        SubscriptionSource source = SubscriptionSource.fromPreference(preferences.getString(SUBSCRIPTION_SOURCE_KEY, null));
        String productId = preferences.getString(SUBSCRIPTION_PRODUCT_ID_KEY, "");
        if (plan == SubscriptionPlan.PREMIUM) {
            return SubscriptionStatus.premium(source, productId);
        }
        return SubscriptionStatus.free();
    }

    private void updateStatus(SubscriptionStatus newStatus) {
        status = newStatus;
        preferences.edit()
                .putString(SUBSCRIPTION_PLAN_KEY, newStatus.getPlan().preferenceValue())
                .putString(SUBSCRIPTION_SOURCE_KEY, newStatus.getSource().preferenceValue())
                .putString(SUBSCRIPTION_PRODUCT_ID_KEY, newStatus.getProductId())
                .apply();
    }
}