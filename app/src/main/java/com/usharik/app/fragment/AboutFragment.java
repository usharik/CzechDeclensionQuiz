package com.usharik.app.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.usharik.app.BuildConfig;
import com.usharik.app.R;
import com.usharik.app.notification.NotificationHelper;
import com.usharik.app.subscription.SubscriptionManager;
import com.usharik.app.subscription.SubscriptionSource;
import com.usharik.app.subscription.SubscriptionStatus;
import com.usharik.app.utils.HapticFeedback;

import java.text.DateFormat;
import java.util.Date;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

public class AboutFragment extends DaggerFragment {

    @Inject
    NotificationHelper notificationHelper;

    @Inject
    SubscriptionManager subscriptionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvVersion = requireView().findViewById(R.id.appVersion);
        TextView tvDateOfBuild = requireView().findViewById(R.id.appDateOfBuild);
        tvVersion.setText(getResources().getString(R.string.version,
                BuildConfig.VERSION_NAME,
                BuildConfig.BUILD_TYPE,
                BuildConfig.GIT_COMMIT_HASH));
        String buildDateStr = DateFormat.getInstance().format(new Date(BuildConfig.TIMESTAMP));
        tvDateOfBuild.setText(getResources().getString(R.string.date_of_build, buildDateStr));
        bindSubscriptionViews();
        requireView().findViewById(R.id.rateApp).setOnClickListener(this::onRateAppClick);
        requireView().findViewById(R.id.privacyPolicy).setOnClickListener(this::onPrivacyPolicyClick);
        requireView().findViewById(R.id.manageSubscription).setOnClickListener(this::onManageSubscriptionClick);

        View testNotificationButton = requireView().findViewById(R.id.testNotification);
        if (BuildConfig.DEBUG) {
            testNotificationButton.setVisibility(View.VISIBLE);
            testNotificationButton.setOnClickListener(this::onTestNotificationClick);
        } else {
            testNotificationButton.setVisibility(View.GONE);
        }
    }

    public void onRateAppClick(View view) {
        HapticFeedback.light(requireContext());
        String packageName = requireActivity().getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(requireContext(), R.string.rate_app_unavailable, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onPrivacyPolicyClick(View view) {
        HapticFeedback.light(requireContext());
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://raw.githubusercontent.com/usharik/CzechDeclensionQuiz/refs/heads/main/privacy_policy.md")));
    }

    private void onManageSubscriptionClick(View view) {
        HapticFeedback.light(requireContext());
        SubscriptionStatus status = subscriptionManager.getStatus();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.premium_subscription)
                .setMessage(buildSubscriptionMessage(status))
                .setPositiveButton(android.R.string.ok, null);

        if (BuildConfig.DEBUG) {
            builder.setNeutralButton(
                    status.hasPremiumAccess() ? R.string.disable_debug_premium : R.string.enable_debug_premium,
                    (dialog, which) -> {
                        subscriptionManager.setDebugPremiumEnabled(!status.hasPremiumAccess());
                        bindSubscriptionViews();
                    }
            );
        }

        builder.show();
    }

    private void onTestNotificationClick(View view) {
        HapticFeedback.light(requireContext());
        notificationHelper.showDailyReminder(requireContext(), false, 1, 0, 0);
        Toast.makeText(requireContext(), R.string.test_notification_sent, Toast.LENGTH_SHORT).show();
    }

    private void bindSubscriptionViews() {
        View manageSubscription = requireView().findViewById(R.id.manageSubscription);
        TextView subscriptionStatus = requireView().findViewById(R.id.subscriptionStatus);
        SubscriptionStatus status = subscriptionManager.getStatus();

        if (BuildConfig.DEBUG) {
            manageSubscription.setVisibility(View.VISIBLE);
            subscriptionStatus.setVisibility(View.VISIBLE);
            subscriptionStatus.setText(status.hasPremiumAccess()
                    ? R.string.subscription_status_premium
                    : R.string.subscription_status_free);
        } else {
            manageSubscription.setVisibility(View.GONE);
            subscriptionStatus.setVisibility(View.GONE);
        }
    }

    private String buildSubscriptionMessage(SubscriptionStatus status) {
        if (status.getSource() == SubscriptionSource.DEBUG) {
            return getString(R.string.subscription_debug_message);
        }
        if (!BuildConfig.SUBSCRIPTIONS_ENABLED) {
            return getString(R.string.subscription_coming_soon_message);
        }
        return status.hasPremiumAccess()
                ? getString(R.string.subscription_active_message)
                : getString(R.string.subscription_inactive_message);
    }
}
