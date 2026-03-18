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

import com.usharik.app.MainActivity;
import com.usharik.app.R;
import com.usharik.app.databinding.FragmentQuizModeSelectionBinding;

import dagger.android.support.AndroidSupportInjection;

public class QuizModeSelectionFragment extends Fragment {

    private FragmentQuizModeSelectionBinding binding;

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
        binding.btnFullTable.setOnClickListener(v -> selectQuizMode(DeclensionQuizFragment.class));
        binding.btnOneCase.setOnClickListener(v -> selectQuizMode(SingleCaseQuizFragment.class));
        binding.btnWordsWithErrors.setOnClickListener(v -> openPage(WordsWithErrorsFragment.class));
        binding.btnHandbook.setOnClickListener(v -> openPage(HandbookFragment.class));
        binding.btnSettings.setOnClickListener(v -> openPage(SettingsFragment.class));
        binding.btnAbout.setOnClickListener(v -> openPage(AboutFragment.class));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
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
}
