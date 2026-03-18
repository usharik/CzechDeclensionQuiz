package com.usharik.app;

import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import com.usharik.app.fragment.AboutFragment;
import com.usharik.app.fragment.DeclensionQuizFragment;
import com.usharik.app.fragment.HandbookFragment;
import com.usharik.app.fragment.QuizModeSelectionFragment;
import com.usharik.app.fragment.SettingsFragment;
import com.usharik.app.fragment.SingleCaseQuizFragment;
import com.usharik.app.fragment.WordsWithErrorsFragment;
import dagger.android.AndroidInjection;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_CURRENT_NAVIGATION_ITEM = "state_current_navigation_item";
    private static final String STATE_SELECTED_QUIZ_MODE = "state_selected_quiz_mode";

    DrawerLayout mDrawerLayout;
    private int currentNavigationItem = R.id.nav_quiz;
    private String selectedQuizMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);
        Log.i(getClass().getName(), "Start main activity!!!");

        setContentView(R.layout.main_activity);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        NavigationView navigationView = findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            currentNavigationItem = savedInstanceState.getInt(STATE_CURRENT_NAVIGATION_ITEM, R.id.nav_quiz);
            selectedQuizMode = savedInstanceState.getString(STATE_SELECTED_QUIZ_MODE);
        }

        navigationView.setNavigationItemSelectedListener(this::onNavigatorItemSelected);

        navigationView.setCheckedItem(currentNavigationItem);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }
        MenuItem selectedItem = navigationView.getMenu().findItem(currentNavigationItem);
        onNavigatorItemSelected(selectedItem != null ? selectedItem : navigationView.getMenu().findItem(R.id.nav_quiz));
    }

    private boolean onNavigatorItemSelected(MenuItem item) {
        if (item == null) {
            return false;
        }
        item.setChecked(true);
        mDrawerLayout.closeDrawers();
        setTitle(item.getTitle());
        currentNavigationItem = item.getItemId();
        int itemId = item.getItemId();
        if (itemId == R.id.nav_quiz) {
            navigateToQuiz();
            return true;
        } else if (itemId == R.id.nav_words_with_errors) {
            replaceFragment(R.id.fragmentContainer, WordsWithErrorsFragment.class);
            return true;
        } else if (itemId == R.id.nav_handbook) {
            replaceFragment(R.id.fragmentContainer, HandbookFragment.class);
            return true;
        } else if (itemId == R.id.nav_settings) {
            replaceFragment(R.id.fragmentContainer, SettingsFragment.class);
            return true;
        } else if (itemId == R.id.nav_about) {
            replaceFragment(R.id.fragmentContainer, AboutFragment.class);
            return true;
        }
        return true;
    }

    private void navigateToQuiz() {
        if (selectedQuizMode == null) {
            // No mode chosen yet — show mode selection
            replaceFragment(R.id.fragmentContainer, QuizModeSelectionFragment.class);
            return;
        }

        Class<? extends Fragment> quizClass = resolveQuizClass(selectedQuizMode);
        if (quizClass == null) {
            selectedQuizMode = null;
            replaceFragment(R.id.fragmentContainer, QuizModeSelectionFragment.class);
            return;
        }

        // Place QuizModeSelectionFragment in the backstack so Back from the quiz returns to it,
        // then show the selected quiz on top — this is the only way to reach mode selection.
        FragmentManager manager = getSupportFragmentManager();
        try {
            manager.beginTransaction()
                    .replace(R.id.fragmentContainer,
                            QuizModeSelectionFragment.class.getDeclaredConstructor().newInstance(),
                            QuizModeSelectionFragment.class.getSimpleName())
                    .addToBackStack(QuizModeSelectionFragment.class.getSimpleName())
                    .commit();
            manager.executePendingTransactions();

            manager.beginTransaction()
                    .replace(R.id.fragmentContainer,
                            quizClass.getDeclaredConstructor().newInstance(),
                            quizClass.getSimpleName())
                    .addToBackStack(quizClass.getSimpleName())
                    .commit();
        } catch (ReflectiveOperationException e) {
            Log.e(getClass().getName(), "Can't navigate to quiz", e);
            replaceFragment(R.id.fragmentContainer, QuizModeSelectionFragment.class);
        }
    }

    private Class<? extends Fragment> resolveQuizClass(String name) {
        if (DeclensionQuizFragment.class.getSimpleName().equals(name)) return DeclensionQuizFragment.class;
        if (SingleCaseQuizFragment.class.getSimpleName().equals(name)) return SingleCaseQuizFragment.class;
        return null;
    }

    public void openQuizMode(Class<? extends Fragment> quizClass) {
        selectedQuizMode = quizClass.getSimpleName();
        replaceFragment(R.id.fragmentContainer, quizClass);
    }

    private void replaceFragment(@IdRes int containerId, Class<? extends Fragment> fragmentClass) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(fragmentClass.getSimpleName());
        if (fragment == null) {
            try {
                fragment = fragmentClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                Log.e(getClass().getName(), "Can't create fragment class", e);
                return;
            }
        }
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(containerId, fragment);
        transaction.addToBackStack(fragmentClass.getSimpleName());
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_NAVIGATION_ITEM, currentNavigationItem);
        outState.putString(STATE_SELECTED_QUIZ_MODE, selectedQuizMode);
    }
}
