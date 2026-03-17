package com.usharik.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.usharik.app.AppState;
import com.usharik.app.BuildConfig;
import com.usharik.app.R;
import com.usharik.app.adapter.WordChipAdapter;
import com.usharik.app.databinding.WordsWithErrorsFragmentBinding;
import com.usharik.app.framework.ViewFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class WordsWithErrorsFragment extends ViewFragment<WordsWithErrorsViewModel> {

    @Inject
    AppState appState;

    private WordsWithErrorsFragmentBinding binding;
    private WordChipAdapter chipAdapter;
    private AdView adView;

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
        setupWordChips();
        setupBannerAd();
    }

    private void setupWordChips() {
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(requireContext());
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        layoutManager.setJustifyContent(JustifyContent.FLEX_START);

        chipAdapter = new WordChipAdapter(
                getViewModel().getSelectedWord(),
                word -> getViewModel().setSelectedWord(word)
        );

        binding.wordsRecyclerView.setLayoutManager(layoutManager);
        binding.wordsRecyclerView.setHasFixedSize(false);
        binding.wordsRecyclerView.setAdapter(chipAdapter);
    }

    private void setupBannerAd() {
        adView = new AdView(requireContext());
        adView.setAdUnitId(BuildConfig.ADMOB_BANNER_AD_UNIT_ID);
        adView.setAdSize(AdSize.BANNER);
        binding.adViewContainer.removeAllViews();
        binding.adViewContainer.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adView != null) {
            adView.resume();
            if (binding != null && binding.adViewContainer != null && adView.getParent() == null) {
                binding.adViewContainer.removeAllViews();
                binding.adViewContainer.addView(adView);
            }
        }

        List<String> words = new ArrayList<>(appState.getWordsWithErrors().keySet());
        chipAdapter.setWords(words, getViewModel().getSelectedWord());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected Class<WordsWithErrorsViewModel> getViewModelClass() {
        return WordsWithErrorsViewModel.class;
    }
}


