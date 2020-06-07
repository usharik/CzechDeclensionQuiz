package com.usharik.app;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import dagger.android.AndroidInjection;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int[] MENU_IDS = {
            R.id.nav_quiz,
            R.id.nav_handbook,
            R.id.nav_words_with_errors,
            R.id.nav_settings,
            R.id.nav_about
    };

    private static final Map<Integer, Integer> MENU_POS = buildMenuPosMap();

    private static Map<Integer, Integer> buildMenuPosMap() {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i=0; i< MENU_IDS.length; i++) {
            map.put(MENU_IDS[i], i);
        }
        return map;
    }

    private DrawerLayout mDrawerLayout;

    private ViewPager2 mViewPager;

    @Inject
    AppState appState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);
        Log.i(getClass().getName(), "Start main activity!!!");

        setContentView(R.layout.main_activity);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mViewPager = findViewById(R.id.view_pager);
        mViewPager.setAdapter(new ViewSliderAdapter(this));
        mViewPager.setOffscreenPageLimit(5);
        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this::onNavigatorItemSelected);

        if (appState.currentNavigationItem == null) {
            appState.currentNavigationItem = R.id.nav_quiz;
        }
        navigationView.setCheckedItem(appState.currentNavigationItem);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        onNavigatorItemSelected(navigationView.getCheckedItem());
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @Override
            public void onPageSelected(int position) {
                navigationView.setCheckedItem(MENU_IDS[position]);
            }
        });
    }

    private boolean onNavigatorItemSelected(MenuItem item) {
        item.setChecked(true);
        mDrawerLayout.closeDrawers();
        appState.currentNavigationItem = item.getItemId();
        Integer pos = MENU_POS.get(item.getItemId());
        if (pos != null) {
            mViewPager.setCurrentItem(pos, false);
        }
        return true;
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
