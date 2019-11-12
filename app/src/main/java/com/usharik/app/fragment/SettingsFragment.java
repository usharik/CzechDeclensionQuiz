package com.usharik.app.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import com.usharik.app.AppState;
import com.usharik.app.R;
import com.usharik.app.databinding.SettingsFragmentBinding;
import com.usharik.app.framework.ViewFragment;

import javax.inject.Inject;

import static android.content.Context.MODE_PRIVATE;

public class SettingsFragment extends ViewFragment<SettingsViewModel> {

    public static final String SHARED_PREFERENCES = "czech_declension_quiz";
    public static final String GENDER_FILTER_KEY = "genderFilterStr";
    public static final String SWITCH_OFF_ANIMATION = "switchOffAnimation";

    @Inject
    AppState appState;

    private SettingsFragmentBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.settings_fragment, container, false);
        binding.setViewModel(getViewModel());
        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).edit();
        editor.putString(GENDER_FILTER_KEY, appState.genderFilterStr);
        editor.putBoolean(SWITCH_OFF_ANIMATION, appState.switchOffAnimation);
        editor.apply();
    }

    @Override
    protected Class<SettingsViewModel> getViewModelClass() {
        return SettingsViewModel.class;
    }
}
