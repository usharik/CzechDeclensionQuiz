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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.usharik.app.AppState;
import com.usharik.app.R;
import com.usharik.app.databinding.FragmentQuizModeSelectionBinding;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class QuizModeSelectionFragment extends Fragment {

    @Inject
    AppState appState;

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
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void selectQuizMode(Class<? extends Fragment> quizClass) {
        appState.setSelectedQuizMode(quizClass.getSimpleName());
        navigateTo(quizClass);
    }

    private void navigateTo(Class<? extends Fragment> fragmentClass) {
        FragmentManager manager = requireActivity().getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(fragmentClass.getSimpleName());
        if (fragment == null) {
            try {
                fragment = fragmentClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                Log.e(getClass().getName(), "Can't create fragment", e);
                return;
            }
        }
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment, fragmentClass.getSimpleName());
        transaction.addToBackStack(fragmentClass.getSimpleName());
        transaction.commit();
    }
}
