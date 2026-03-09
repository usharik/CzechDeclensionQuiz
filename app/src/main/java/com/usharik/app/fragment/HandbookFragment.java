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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.usharik.app.AppState;
import com.usharik.app.BuildConfig;
import com.usharik.app.R;
import com.usharik.app.databinding.HandbookFragmentBinding;
import com.usharik.app.framework.ViewFragment;

import javax.inject.Inject;

public class HandbookFragment extends ViewFragment<HandbookViewModel> {

    @Inject
    AppState appState;

    @Inject
    FirebaseAnalytics firebaseAnalytics;

    private HandbookFragmentBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.handbook_fragment, container, false);
        binding.setViewModel(getViewModel());
        binding.wordGroupMasculine.setOnCheckedChangeListener(this::onWordCheckedChangeListener);
        binding.wordGroupFeminine.setOnCheckedChangeListener(this::onWordCheckedChangeListener);
        binding.wordGroupNeuter.setOnCheckedChangeListener(this::onWordCheckedChangeListener);
        getViewModel().setSelectedGender(getViewModel().getSelectedGender() != -1 ? getViewModel().getSelectedGender() : R.id.radioMasculine);
        getViewModel().setSelectedWordId(getViewModel().getSelectedWordId() != -1 ? getViewModel().getSelectedWordId() : R.id.pan);
        int selectedGender = getViewModel().getSelectedGender();
        if (selectedGender == R.id.radioMasculine) {
            binding.wordGroupMasculine.check(getViewModel().getSelectedWordId());
        } else if (selectedGender == R.id.radioFeminine) {
            binding.wordGroupFeminine.check(getViewModel().getSelectedWordId());
        } else if (selectedGender == R.id.radioNeuter) {
            binding.wordGroupNeuter.check(getViewModel().getSelectedWordId());
        }

        Bundle bundle = new Bundle();
        bundle.putString("HANDBOOK_FRAGMENT", "OPEN");
        firebaseAnalytics.logEvent("HANDBOOK_FRAGMENT", bundle);

        return binding.getRoot();
    }

    private void onWordCheckedChangeListener(RadioGroup group, int checkedId) {
        RadioButton checkedRadioButton = group.findViewById(checkedId);
        if (checkedRadioButton == null) {
            return;
        }
        boolean isChecked = checkedRadioButton.isChecked();
        if (isChecked) {
            getViewModel().setSelectedWord(checkedRadioButton.getText().toString());
            getViewModel().setSelectedWordId(checkedId);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load banner ad
        setupBannerAd();

        // Apply insets to banner container
        applyBannerInsets();
    }

    private void applyBannerInsets() {
        if (binding != null && binding.adViewContainer != null) {
            // Get root window insets directly
            androidx.core.view.WindowInsetsCompat rootInsets = androidx.core.view.ViewCompat.getRootWindowInsets(binding.adViewContainer);
            if (rootInsets != null) {
                androidx.core.graphics.Insets insets = rootInsets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                );
                // Apply bottom padding directly
                binding.adViewContainer.setPadding(0, 0, 0, insets.bottom);
                android.util.Log.d("BannerInsets", "Applied bottom padding: " + insets.bottom + "px");
            } else {
                // Fallback: use display metrics to calculate navigation bar height
                android.content.res.Resources resources = requireContext().getResources();
                int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    int navBarHeight = resources.getDimensionPixelSize(resourceId);
                    binding.adViewContainer.setPadding(0, 0, 0, navBarHeight);
                    android.util.Log.d("BannerInsets", "Applied fallback padding: " + navBarHeight + "px");
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-apply insets every time fragment becomes visible
        applyBannerInsets();
    }

    private void setupBannerAd() {
        // Create AdView programmatically to set adUnitId from BuildConfig
        AdView adView = new AdView(requireContext());
        adView.setAdUnitId(BuildConfig.ADMOB_BANNER_AD_UNIT_ID);
        adView.setAdSize(AdSize.BANNER);

        // Add AdView to container
        binding.adViewContainer.removeAllViews();
        binding.adViewContainer.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    protected Class<HandbookViewModel> getViewModelClass() {
        return HandbookViewModel.class;
    }
}
