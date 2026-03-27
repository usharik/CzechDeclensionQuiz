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
import com.usharik.app.UiLanguage;
import com.usharik.app.UiLanguageManager;
import com.usharik.app.databinding.SettingsFragmentBinding;
import com.usharik.app.framework.ViewFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        binding.btnUiLanguage.setOnClickListener(v -> showUiLanguageDialog());
        binding.btnUiLanguage.setText(UiLanguageManager.getSelectedLanguageLabel(requireContext()));
        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE).edit();
        editor.putString(GENDER_FILTER_KEY, appState.getGenderFilterStr());
        editor.putBoolean(SWITCH_OFF_ANIMATION, appState.getSwitchOffAnimation());
        editor.apply();
    }

    @Override
    protected Class<SettingsViewModel> getViewModelClass() {
        return SettingsViewModel.class;
    }

    private void showUiLanguageDialog() {
        UiLanguage[] options = UiLanguageManager.getAvailableLanguages();
        UiLanguage currentLanguage = UiLanguageManager.getSelectedLanguage(requireContext());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.choose_app_language)
                .setSingleChoiceItems(
                        UiLanguageManager.getLanguageLabels(requireContext()),
                        UiLanguageManager.indexOf(currentLanguage),
                        (dialog, which) -> {
                            UiLanguage selectedLanguage = options[which];
                            UiLanguageManager.saveAndApplyLanguage(requireContext(), selectedLanguage);
                            binding.btnUiLanguage.setText(UiLanguageManager.getSelectedLanguageLabel(requireContext()));
                            dialog.dismiss();
                        }
                )
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
