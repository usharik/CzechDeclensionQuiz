package com.usharik.app.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.usharik.app.AppState;
import com.usharik.app.BuildConfig;
import com.usharik.app.R;
import com.usharik.app.ads.AdManager;
import com.usharik.app.databinding.FragmentSingleCaseQuizBinding;
import com.usharik.app.framework.ViewFragment;
import com.usharik.app.utils.HapticFeedback;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;

public class SingleCaseQuizFragment extends ViewFragment<SingleCaseQuizViewModel> {

    private static final int PRESSES_PER_INTERSTITIAL_ATTEMPT = 5;
    private static final double INTERSTITIAL_SHOW_PROBABILITY = 0.4d;

    private FragmentSingleCaseQuizBinding binding;
    private Observable.OnPropertyChangedCallback viewModelCallback;
    private AdView adView;

    @Inject
    AppState appState;

    @Inject
    AdManager adManager;

    @Inject
    FirebaseAnalytics firebaseAnalytics;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_single_case_quiz, container, false);
        binding.setViewModel(getViewModel());

        setupAnswerButtons();
        setupNextCaseButton();
        setupNextWordButton();

        viewModelCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                refreshAnswerButtons();
            }
        };
        getViewModel().addOnPropertyChangedCallback(viewModelCallback);

        if (!getViewModel().hasCurrentWord()) {
            getViewModel().nextWord(false);
        } else {
            refreshAnswerButtons();
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupBannerAd();
        adManager.loadAd(requireActivity(), BuildConfig.ADMOB_SINGLE_CASE_QUIZ_INTERSTITIAL_AD_UNIT_ID);
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
        if (viewModelCallback != null) {
            getViewModel().removeOnPropertyChangedCallback(viewModelCallback);
            viewModelCallback = null;
        }
        binding = null;
    }

    private void setupBannerAd() {
        adView = new AdView(requireContext());
        adView.setAdUnitId(BuildConfig.ADMOB_SINGLE_CASE_QUIZ_AD_UNIT_ID);
        adView.setAdSize(AdSize.BANNER);
        binding.adViewContainer.removeAllViews();
        binding.adViewContainer.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private void setupAnswerButtons() {
        binding.btnAnswer1.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            onAnswerSelected(0);
        });
        binding.btnAnswer2.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            onAnswerSelected(1);
        });
        binding.btnAnswer3.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            onAnswerSelected(2);
        });
        binding.btnAnswer4.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            onAnswerSelected(3);
        });
    }

    private void setupNextCaseButton() {
        binding.btnNextCase.setEnabled(false);
        binding.btnNextCase.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            logButtonClick("NEXT_CASE");
            continueWithPotentialInterstitial(getViewModel()::nextStep);
        });
    }

    private void setupNextWordButton() {
        binding.btnNextWord.setOnClickListener(v -> {
            HapticFeedback.light(requireContext());
            logButtonClick("NEXT_WORD");
            continueWithPotentialInterstitial(() -> getViewModel().nextWord(false));
        });
    }

    private void continueWithPotentialInterstitial(Runnable action) {
        int pressCount = appState.incrementSingleCaseNavigationPressCount();
        if (shouldShowInterstitial(pressCount)) {
            adManager.showAd(
                    requireActivity(),
                    BuildConfig.ADMOB_SINGLE_CASE_QUIZ_INTERSTITIAL_AD_UNIT_ID,
                    action
            );
            return;
        }

        action.run();
    }

    private boolean shouldShowInterstitial(int pressCount) {
        return pressCount % PRESSES_PER_INTERSTITIAL_ATTEMPT == 0
                && ThreadLocalRandom.current().nextDouble() < INTERSTITIAL_SHOW_PROBABILITY;
    }

    private void refreshAnswerButtons() {
        MaterialButton[] buttons = answerButtons();
        List<String> answers = getViewModel().getAnswers();
        boolean answered = getViewModel().isAnswered();
        String correct = getViewModel().getCorrectAnswer();
        int defaultColor = requireContext().getColor(R.color.colorSurfaceVariant);
        int colorCorrect = getResources().getColor(R.color.colorCorrect, null);

        for (int i = 0; i < 4; i++) {
            if (i < answers.size()) {
                String answer = answers.get(i);
                buttons[i].setText(answer);
                buttons[i].setEnabled(!answered);
                buttons[i].setVisibility(View.VISIBLE);
                int buttonTint = answered && answer.equals(correct) ? colorCorrect : defaultColor;
                ColorStateList buttonColor = ColorStateList.valueOf(buttonTint);
                buttons[i].setBackgroundTintList(buttonColor);
            } else {
                buttons[i].setVisibility(View.GONE);
            }
        }
        binding.btnNextCase.setEnabled(answered);
    }

    private void onAnswerSelected(int index) {
        List<String> answers = getViewModel().getAnswers();
        if (getViewModel().isAnswered() || index >= answers.size()) {
            return;
        }

        String correct = getViewModel().getCorrectAnswer();
        String selected = answers.get(index);
        boolean isCorrect = selected.equals(correct);
        MaterialButton[] buttons = answerButtons();

        // Get colors from theme resources
        int colorCorrect = getResources().getColor(R.color.colorCorrect, null);
        int colorIncorrect = getResources().getColor(R.color.colorIncorrect, null);

        // Haptic feedback based on answer correctness
        if (isCorrect) {
            HapticFeedback.success(requireContext());
        } else {
            HapticFeedback.error(requireContext());
        }

        logAnswerSelection(isCorrect, selected, correct);

        for (int i = 0; i < answers.size(); i++) {
            buttons[i].setEnabled(false);
            if (answers.get(i).equals(correct)) {
                buttons[i].setBackgroundTintList(ColorStateList.valueOf(colorCorrect));
            } else if (i == index) {
                buttons[i].setBackgroundTintList(ColorStateList.valueOf(colorIncorrect));
            }
        }
        getViewModel().markAnswered();
        binding.btnNextCase.setEnabled(true);
    }

    private MaterialButton[] answerButtons() {
        return new MaterialButton[]{
                binding.btnAnswer1,
                binding.btnAnswer2,
                binding.btnAnswer3,
                binding.btnAnswer4
        };
    }

    /**
     * Log answer selection to Firebase Analytics
     */
    private void logAnswerSelection(boolean isCorrect, String selected, String correct) {
        Bundle bundle = new Bundle();
        bundle.putString("RESULT", isCorrect ? "CORRECT" : "INCORRECT");
        bundle.putString("SELECTED_ANSWER", selected);
        bundle.putString("CORRECT_ANSWER", correct);
        bundle.putString("WORD", getViewModel().getWord());
        bundle.putString("CASE", getViewModel().getCurrentCaseName());
        firebaseAnalytics.logEvent("SINGLE_CASE_ANSWER", bundle);
    }

    /**
     * Log button click to Firebase Analytics
     */
    private void logButtonClick(String buttonName) {
        Bundle bundle = new Bundle();
        bundle.putString("BUTTON", buttonName);
        bundle.putString("WORD", getViewModel().getWord());
        firebaseAnalytics.logEvent("SINGLE_CASE_NAVIGATION", bundle);
    }

    @Override
    protected Class<SingleCaseQuizViewModel> getViewModelClass() {
        return SingleCaseQuizViewModel.class;
    }
}
