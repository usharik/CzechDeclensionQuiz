package com.usharik.app;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import android.util.Log;
import android.view.MenuItem;
import com.usharik.app.fragment.AboutFragment;
import com.usharik.app.fragment.DeclensionQuizFragment;
import com.usharik.app.fragment.HandbookFragment;
import com.usharik.app.fragment.QuizModeSelectionFragment;
import com.usharik.app.fragment.SettingsFragment;
import com.usharik.app.fragment.SingleCaseQuizFragment;
import com.usharik.app.fragment.WordsWithErrorsFragment;
import com.usharik.app.notification.NotificationHelper;
import dagger.android.AndroidInjection;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "czech_declension_quiz";
    private static final String PREF_RATIONALE_SHOWN = "notification_rationale_shown";

    @Inject
    NotificationHelper notificationHelper;

    /**
     * Launcher for the POST_NOTIFICATIONS runtime permission (Android 13+).
     * Must be registered before onCreate returns.
     */
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        Log.i(getClass().getName(),
                                "POST_NOTIFICATIONS permission " + (granted ? "granted" : "denied"));
                        if (granted) {
                            // Show a one-time welcome notification on the very first grant.
                            notificationHelper.showWelcomeNotificationIfNeeded(getApplicationContext());
                        }
                    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);
        Log.i(getClass().getName(), "Start main activity!!!");

        setContentView(R.layout.main_activity);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        // Route the toolbar home button through the back-pressed dispatcher so that
        // quiz fragments can intercept it with their OnBackPressedCallback.
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_home_black_24dp);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isHubVisible()) {
                    finish();
                } else {
                    openHubFragment();
                }
            }
        });

        if (savedInstanceState == null) {
            openHubFragment();
        } else {
            updateTitleForCurrentFragment();
        }

        // Android 13+ requires a runtime permission to post notifications.
        // We request it here so the user sees the dialog on first launch.
        requestNotificationPermissionIfNeeded();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_RATIONALE_SHOWN, false)) {
            // User has already seen our rationale dialog; go straight to the system prompt
            // (handles the case where the app was reinstalled or the OS reset the permission).
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        // Show our custom rationale dialog first so the user understands why we need the permission.
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notification_rationale_title)
                .setMessage(R.string.notification_rationale_body)
                .setPositiveButton(R.string.notification_rationale_enable, (dialog, which) -> {
                    prefs.edit().putBoolean(PREF_RATIONALE_SHOWN, true).apply();
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .setNegativeButton(R.string.notification_rationale_not_now, (dialog, which) -> {
                    prefs.edit().putBoolean(PREF_RATIONALE_SHOWN, true).apply();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    public void openHubFragment() {
        openTopLevelFragment(new QuizModeSelectionFragment(), QuizModeSelectionFragment.class.getSimpleName());
        setTitle(R.string.hub_title);
    }

    public void openQuizMode(Class<? extends Fragment> quizClass) {
        try {
            pushFragment(createFragment(quizClass), quizClass.getSimpleName());
            setTitle(resolveTitle(quizClass));
        } catch (ReflectiveOperationException e) {
            Log.e(getClass().getName(), "Can't open quiz mode", e);
        }
    }

    public void openPage(Class<? extends Fragment> fragmentClass) {
        try {
            pushFragment(createFragment(fragmentClass), fragmentClass.getSimpleName());
            setTitle(resolveTitle(fragmentClass));
        } catch (ReflectiveOperationException e) {
            Log.e(getClass().getName(), "Can't open page", e);
        }
    }

    public void openTopLevelFragment(Fragment fragment, String tag) {
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        manager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment, tag)
                .commit();
    }

    public void pushFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    private Fragment createFragment(Class<? extends Fragment> fragmentClass) throws ReflectiveOperationException {
        return fragmentClass.getDeclaredConstructor().newInstance();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isHubVisible() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        return fragment instanceof QuizModeSelectionFragment;
    }

    private void updateTitleForCurrentFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (fragment == null) {
            setTitle(R.string.hub_title);
        } else {
            setTitle(resolveTitle(fragment.getClass()));
        }
    }

    private int resolveTitle(Class<? extends Fragment> fragmentClass) {
        if (QuizModeSelectionFragment.class.equals(fragmentClass)) return R.string.hub_title;
        if (DeclensionQuizFragment.class.equals(fragmentClass)) return R.string.quiz_mode_full_table;
        if (SingleCaseQuizFragment.class.equals(fragmentClass)) return R.string.quiz_mode_one_case;
        if (WordsWithErrorsFragment.class.equals(fragmentClass)) return R.string.words_with_errors;
        if (HandbookFragment.class.equals(fragmentClass)) return R.string.handbook;
        if (SettingsFragment.class.equals(fragmentClass)) return R.string.settings;
        if (AboutFragment.class.equals(fragmentClass)) return R.string.about;
        return R.string.app_name;
    }
}
