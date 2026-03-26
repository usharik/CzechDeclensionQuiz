package com.usharik.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import com.usharik.app.BuildConfig;
import com.usharik.app.R;
import com.usharik.app.ads.BannerAdController;
import com.usharik.app.ads.InterstitialAdPolicy;
import com.usharik.app.databinding.HandbookFragmentBinding;
import com.usharik.app.framework.ViewFragment;
import com.usharik.app.service.FirebaseAnalyticsService;
import com.usharik.app.utils.HapticFeedback;

import javax.inject.Inject;

public class HandbookFragment extends ViewFragment<HandbookViewModel> {

    @Inject
    FirebaseAnalyticsService analyticsService;

    @Inject
    InterstitialAdPolicy adPolicy;

    private HandbookFragmentBinding binding;
    private BannerAdController bannerAdController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.handbook_fragment, container, false);
        binding.setViewModel(getViewModel());

        // Add haptic feedback to gender selection
        binding.genderGroup.setOnCheckedChangeListener(this::onGenderCheckedChangeListener);

        binding.wordGroupMasculine.setOnCheckedChangeListener(this::onWordCheckedChangeListener);
        binding.wordGroupFeminine.setOnCheckedChangeListener(this::onWordCheckedChangeListener);
        binding.wordGroupNeuter.setOnCheckedChangeListener(this::onWordCheckedChangeListener);
        int selectedGender = getViewModel().getSelectedGender();
        if (selectedGender == R.id.radioMasculine) {
            binding.wordGroupMasculine.check(getViewModel().getSelectedWordId());
        } else if (selectedGender == R.id.radioFeminine) {
            binding.wordGroupFeminine.check(getViewModel().getSelectedWordId());
        } else if (selectedGender == R.id.radioNeuter) {
            binding.wordGroupNeuter.check(getViewModel().getSelectedWordId());
        }

        analyticsService.logHandbookOpen();

        return binding.getRoot();
    }

    private void onGenderCheckedChangeListener(RadioGroup group, int checkedId) {
        // Light haptic feedback when selecting gender
        HapticFeedback.light(requireContext());
    }

    private void onWordCheckedChangeListener(RadioGroup group, int checkedId) {
        RadioButton checkedRadioButton = group.findViewById(checkedId);
        if (checkedRadioButton == null) {
            return;
        }
        boolean isChecked = checkedRadioButton.isChecked();
        if (isChecked) {
            // Light haptic feedback when selecting a word paradigm
            HapticFeedback.light(requireContext());
            getViewModel().setSelectedWord(checkedRadioButton.getText().toString());
            getViewModel().setSelectedWordId(checkedId);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bannerAdController = new BannerAdController(adPolicy);
        bannerAdController.bind(requireContext(), binding.adViewContainer,
                BuildConfig.ADMOB_BANNER_AD_UNIT_ID);
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

    @Override
    protected Class<HandbookViewModel> getViewModelClass() {
        return HandbookViewModel.class;
    }
}
