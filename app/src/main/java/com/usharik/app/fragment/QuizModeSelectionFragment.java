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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.usharik.app.BuildConfig;
import com.usharik.app.MainActivity;
import com.usharik.app.R;
import com.usharik.app.databinding.FragmentQuizModeSelectionBinding;
import com.usharik.app.utils.HapticFeedback;

import dagger.android.support.AndroidSupportInjection;

public class QuizModeSelectionFragment extends Fragment {

    private FragmentQuizModeSelectionBinding binding;
    private AdView adView;

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
            selectQuizMode(DeclensionQuizFragment.class);
        });
        binding.btnOneCase.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            selectQuizMode(SingleCaseQuizFragment.class);
        });
        binding.btnWordsWithErrors.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            openPage(WordsWithErrorsFragment.class);
        });
        binding.btnHandbook.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            openPage(HandbookFragment.class);
        });
        binding.btnSettings.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            openPage(SettingsFragment.class);
        });
        binding.btnAbout.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            openPage(AboutFragment.class);
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupBannerAd();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
            if (binding != null && adView.getParent() == null) {
                binding.adViewContainer.removeAllViews();
                binding.adViewContainer.addView(adView);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onDestroyView() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        super.onDestroyView();
        binding = null;
    }

    private void setupBannerAd() {
        adView = new AdView(requireContext());
        adView.setAdUnitId(BuildConfig.ADMOB_HUB_BANNER_AD_UNIT_ID);
        adView.setAdSize(AdSize.BANNER);
        binding.adViewContainer.removeAllViews();
        binding.adViewContainer.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
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
}
