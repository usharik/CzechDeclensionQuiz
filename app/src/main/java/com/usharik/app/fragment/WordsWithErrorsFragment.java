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
import com.usharik.app.AppState;
import com.usharik.app.BuildConfig;
import com.usharik.app.R;
import com.usharik.app.adapter.WordChipAdapter;
import com.usharik.app.ads.BannerAdController;
import com.usharik.app.ads.InterstitialAdPolicy;
import com.usharik.app.databinding.WordsWithErrorsFragmentBinding;
import com.usharik.app.framework.ViewFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class WordsWithErrorsFragment extends ViewFragment<WordsWithErrorsViewModel> {

    @Inject
    AppState appState;

    @Inject
    InterstitialAdPolicy adPolicy;

    private WordsWithErrorsFragmentBinding binding;
    private WordChipAdapter chipAdapter;
    private BannerAdController bannerAdController;

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
        bannerAdController = new BannerAdController(adPolicy);
        bannerAdController.bind(requireContext(), binding.adViewContainer,
                BuildConfig.ADMOB_BANNER_AD_UNIT_ID);
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

    @Override
    public void onResume() {
        super.onResume();

        bannerAdController.onResume();

        List<String> words = new ArrayList<>(appState.getWordsWithErrors().keySet());
        chipAdapter.setWords(words, getViewModel().getSelectedWord());
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
    protected Class<WordsWithErrorsViewModel> getViewModelClass() {
        return WordsWithErrorsViewModel.class;
    }
}


