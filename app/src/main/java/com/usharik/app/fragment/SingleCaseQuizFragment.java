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

import com.google.android.material.button.MaterialButton;
import com.usharik.app.AppState;
import com.usharik.app.R;
import com.usharik.app.databinding.FragmentSingleCaseQuizBinding;
import com.usharik.app.framework.ViewFragment;

import java.util.List;

import javax.inject.Inject;

public class SingleCaseQuizFragment extends ViewFragment<SingleCaseQuizViewModel> {

    private static final int COLOR_CORRECT = 0xFF4CAF50;
    private static final int COLOR_INCORRECT = 0xFFF44336;

    @Inject
    AppState appState;

    private FragmentSingleCaseQuizBinding binding;
    private List<String> currentAnswers;
    private Observable.OnPropertyChangedCallback viewModelCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_single_case_quiz, container, false);
        binding.setViewModel(getViewModel());

        setupAnswerButtons();
        setupNextButton();

        viewModelCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                refreshAnswerButtons();
            }
        };
        getViewModel().addOnPropertyChangedCallback(viewModelCallback);

        if (appState.getWordInfo() == null) {
            getViewModel().nextWord(false);
        } else {
            refreshAnswerButtons();
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModelCallback != null) {
            getViewModel().removeOnPropertyChangedCallback(viewModelCallback);
            viewModelCallback = null;
        }
        binding = null;
    }

    private void setupAnswerButtons() {
        binding.btnAnswer1.setOnClickListener(v -> onAnswerSelected(0));
        binding.btnAnswer2.setOnClickListener(v -> onAnswerSelected(1));
        binding.btnAnswer3.setOnClickListener(v -> onAnswerSelected(2));
        binding.btnAnswer4.setOnClickListener(v -> onAnswerSelected(3));
    }

    private void setupNextButton() {
        binding.btnNext.setEnabled(false);
        binding.btnNext.setOnClickListener(v -> {
            getViewModel().nextStep();
        });
    }

    private void refreshAnswerButtons() {
        currentAnswers = getViewModel().buildAnswers();
        if (currentAnswers.isEmpty()) return;

        MaterialButton[] buttons = answerButtons();
        for (int i = 0; i < 4; i++) {
            if (i < currentAnswers.size()) {
                buttons[i].setText(currentAnswers.get(i));
                buttons[i].setEnabled(true);
                buttons[i].setVisibility(View.VISIBLE);
                buttons[i].setBackgroundTintList(null);
            } else {
                buttons[i].setVisibility(View.GONE);
            }
        }
        binding.btnNext.setEnabled(false);
    }

    private void onAnswerSelected(int index) {
        String correct = getViewModel().getCorrectAnswer();
        MaterialButton[] buttons = answerButtons();
        for (int i = 0; i < 4; i++) {
            buttons[i].setEnabled(false);
            if (currentAnswers.get(i).equals(correct)) {
                buttons[i].setBackgroundTintList(ColorStateList.valueOf(COLOR_CORRECT));
            } else if (i == index) {
                buttons[i].setBackgroundTintList(ColorStateList.valueOf(COLOR_INCORRECT));
            }
        }
        binding.btnNext.setEnabled(true);
    }

    private MaterialButton[] answerButtons() {
        return new MaterialButton[]{
                binding.btnAnswer1,
                binding.btnAnswer2,
                binding.btnAnswer3,
                binding.btnAnswer4
        };
    }

    @Override
    protected Class<SingleCaseQuizViewModel> getViewModelClass() {
        return SingleCaseQuizViewModel.class;
    }
}
