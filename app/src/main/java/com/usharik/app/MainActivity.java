package com.usharik.app;

import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.usharik.app.fragment.AboutFragment;
import com.usharik.app.fragment.DeclensionQuizFragment;
import com.usharik.app.fragment.HandbookFragment;
import com.usharik.app.fragment.SettingsFragment;
import com.usharik.app.fragment.WordsWithErrorsFragment;
import dagger.android.AndroidInjection;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {

    DrawerLayout mDrawerLayout;

    @Inject
    AppState appState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);
        Log.i(getClass().getName(), "Start main activity!!!");

        setContentView(R.layout.main_activity);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this::onNavigatorItemSelected);

        if (appState.currentNavigationItem == null) {
            appState.currentNavigationItem = R.id.nav_quiz;
        }
        navigationView.setCheckedItem(appState.currentNavigationItem);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        onNavigatorItemSelected(navigationView.getCheckedItem());
    }

    private boolean onNavigatorItemSelected(MenuItem item) {
        item.setChecked(true);
        mDrawerLayout.closeDrawers();
        appState.currentNavigationItem = item.getItemId();
        switch (item.getItemId()) {
            case R.id.nav_quiz:
                replaceFragment(R.id.fragmentContainer, DeclensionQuizFragment.class);
                return true;
            case R.id.nav_words_with_errors:
                replaceFragment(R.id.fragmentContainer, WordsWithErrorsFragment.class);
                return true;
            case R.id.nav_handbook:
                replaceFragment(R.id.fragmentContainer, HandbookFragment.class);
                return true;
            case R.id.nav_settings:
                replaceFragment(R.id.fragmentContainer, SettingsFragment.class);
                return true;
            case R.id.nav_about:
                replaceFragment(R.id.fragmentContainer, AboutFragment.class);
                return true;
        }
        return true;
    }

    private void replaceFragment(@IdRes int containerId, Class<? extends Fragment> fragmentClass) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(fragmentClass.getSimpleName());
        if (fragment == null) {
            try {
                fragment = fragmentClass.newInstance();
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
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
}
