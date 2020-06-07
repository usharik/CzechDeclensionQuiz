package com.usharik.app;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.usharik.app.fragment.*;

public class ViewSliderAdapter extends FragmentStateAdapter {

    private final Fragment[] fragments = {
            new DeclensionQuizFragment(),
            new HandbookFragment(),
            new WordsWithErrorsFragment(),
            new SettingsFragment(),
            new AboutFragment()
    };

    public ViewSliderAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.i(getClass().getSimpleName(), "Scroll to fragment " + position);
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return fragments.length;
    }
}
