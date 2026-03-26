package com.usharik.app.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.usharik.app.BuildConfig;
import com.usharik.app.MainActivity;
import com.usharik.app.R;
import com.usharik.app.ads.AdsPolicy;
import com.usharik.app.ads.BannerAdController;
import com.usharik.app.databinding.FragmentQuizModeSelectionBinding;
import com.usharik.app.service.FirebaseAnalyticsService;
import com.usharik.app.utils.HapticFeedback;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class QuizModeSelectionFragment extends Fragment {

    @Inject
    FirebaseAnalyticsService analyticsService;

    @Inject
    AdsPolicy adsPolicy;

    private FragmentQuizModeSelectionBinding binding;
    private BannerAdController bannerAdController;

    @Override
    public void onAttach(@NonNull Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_quiz_mode_selection, container, false);
        binding.btnFullTable.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            logButtonClick("FULL_TABLE");
            selectQuizMode(DeclensionQuizFragment.class);
        });
        binding.btnOneCase.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            logButtonClick("ONE_CASE");
            selectQuizMode(SingleCaseQuizFragment.class);
        });
        binding.btnWordsWithErrors.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            logButtonClick("WORDS_WITH_ERRORS");
            openPage(WordsWithErrorsFragment.class);
        });
        binding.btnHandbook.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            logButtonClick("HANDBOOK");
            openPage(HandbookFragment.class);
        });
        binding.btnSettings.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            logButtonClick("SETTINGS");
            openPage(SettingsFragment.class);
        });
        binding.btnAbout.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            logButtonClick("ABOUT");
            openPage(AboutFragment.class);
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bannerAdController = new BannerAdController(adsPolicy);
        bannerAdController.bind(requireContext(), binding.adViewContainer,
                BuildConfig.ADMOB_HUB_BANNER_AD_UNIT_ID);
    }

    @Override
    public void onResume() {
        super.onResume();
        bannerAdController.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        bannerAdController.onPause();
    }

    @Override
    public void onDestroyView() {
        bannerAdController.onDestroyView();
        bannerAdController = null;
        super.onDestroyView();
        binding = null;
    }

    private void selectQuizMode(Class<? extends Fragment> quizClass) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openQuizMode(quizClass);
        } else {
            Log.e(getClass().getName(), "Host activity does not support quiz navigation");
        }
    }

    private void openPage(Class<? extends Fragment> fragmentClass) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openPage(fragmentClass);
        } else {
            Log.e(getClass().getName(), "Host activity does not support page navigation");
        }
    }

    /**
     * Log button click to Firebase Analytics
     */
    private void logButtonClick(String buttonName) {
        analyticsService.logButtonClick("HUB_BUTTON_CLICK", buttonName);
    }
}
