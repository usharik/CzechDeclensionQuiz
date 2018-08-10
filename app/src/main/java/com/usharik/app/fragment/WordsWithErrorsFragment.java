package com.usharik.app.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.usharik.app.AppState;
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
    public void onResume() {
        super.onResume();
        binding.wordsWithErrorsFlow.removeAllViews();
        for (String word : appState.wordsWithErrors.keySet()) {
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
            if (child instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) child;
                radioButton.setChecked(child.equals(view));
            }
        }
        if (view instanceof RadioButton) {
            RadioButton rb = (RadioButton) view;
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
