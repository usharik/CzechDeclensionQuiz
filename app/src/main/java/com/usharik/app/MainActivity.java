package com.usharik.app;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.usharik.app.fragment.AboutFragment;
import com.usharik.app.fragment.DeclensionQuizFragment;
import com.usharik.app.fragment.HandbookFragment;
import com.usharik.app.fragment.SettingsFragment;
import com.usharik.app.fragment.WordsWithErrorsFragment;

public class MainActivity extends AppCompatActivity {

    DrawerLayout mDrawerLayout;
    DeclensionQuizFragment declensionQuizFragment;
    WordsWithErrorsFragment wordsWithErrorsFragment;
    HandbookFragment handbookFragment;
    SettingsFragment settingsFragment;
    AboutFragment aboutFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(getClass().getName(), "Start main activity!!!");

        setContentView(R.layout.main_activity);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this::onNavigatorItemSelected);
        navigationView.setCheckedItem(R.id.nav_quiz);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        replaceFragment(R.id.fragmentContainer, new DeclensionQuizFragment(), null);

        declensionQuizFragment = new DeclensionQuizFragment();
        wordsWithErrorsFragment = new WordsWithErrorsFragment();
        handbookFragment = new HandbookFragment();
        settingsFragment = new SettingsFragment();
        aboutFragment = new AboutFragment();
    }

    private boolean onNavigatorItemSelected(MenuItem item) {
        item.setChecked(true);
        mDrawerLayout.closeDrawers();
        switch (item.getItemId()) {
            case R.id.nav_quiz:
                replaceFragment(R.id.fragmentContainer, declensionQuizFragment, null);
                return true;
            case R.id.nav_words_with_errors:
                replaceFragment(R.id.fragmentContainer, wordsWithErrorsFragment, null);
                return true;
            case R.id.nav_handbook:
                replaceFragment(R.id.fragmentContainer, handbookFragment, null);
                return true;
            case R.id.nav_settings:
                replaceFragment(R.id.fragmentContainer, settingsFragment, null);
                return true;
            case R.id.nav_about:
                replaceFragment(R.id.fragmentContainer, aboutFragment, null);
                return true;
        }
        return true;
    }

    private void replaceFragment(@IdRes int containerId, Fragment fragment, String name) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(containerId, fragment);
        transaction.addToBackStack(name);
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
