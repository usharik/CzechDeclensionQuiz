package com.usharik.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.databinding.DataBindingUtil;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.usharik.app.AppState;
import com.usharik.app.BuildConfig;
import com.usharik.app.R;
import com.usharik.app.databinding.WordsWithErrorsFragmentBinding;
import com.usharik.app.framework.ViewFragment;

import javax.inject.Inject;

public class WordsWithErrorsFragment extends ViewFragment<WordsWithErrorsViewModel> {

    private final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
    );

    @Inject
    AppState appState;

    private WordsWithErrorsFragmentBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.words_with_errors_fragment, container, false);
        binding.setViewModel(getViewModel());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load banner ad
        setupBannerAd();

        // Apply insets to banner container - standard Android approach
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.adViewContainer, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) v.getLayoutParams();
            params.bottomMargin = insets.bottom;
            v.setLayoutParams(params);
            return windowInsets;
        });
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
    public void onResume() {
        super.onResume();

        binding.wordsWithErrorsFlow.removeAllViews();
        for (String word : appState.getWordsWithErrors().keySet()) {
            RadioButton radioButton = new RadioButton(new ContextThemeWrapper(getContext(), R.style.WordRadioButtonWithPadding), null, -1);
            radioButton.setText(word);
            radioButton.setLayoutParams(layoutParams);
            radioButton.setChecked(word.equals(getViewModel().getSelectedWord()));
            radioButton.setId(RadioButton.generateViewId());
            radioButton.setOnClickListener(this::onRadioButtonClick);
            binding.wordsWithErrorsFlow.addView(radioButton);
        }
    }

    private void onRadioButtonClick(View view) {
        for(int i=0; i<binding.wordsWithErrorsFlow.getChildCount(); i++) {
            View child = binding.wordsWithErrorsFlow.getChildAt(i);
            if (child instanceof RadioButton radioButton) {
                radioButton.setChecked(child.equals(view));
            }
        }
        if (view instanceof RadioButton rb) {
            if (rb.isChecked()) {
                getViewModel().setSelectedWord(rb.getText().toString());
            }
        }
    }

    @Override
    protected Class<WordsWithErrorsViewModel> getViewModelClass() {
        return WordsWithErrorsViewModel.class;
    }
}
