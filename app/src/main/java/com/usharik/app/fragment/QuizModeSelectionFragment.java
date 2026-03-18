package com.usharik.app.fragment;

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

import com.usharik.app.R;
import com.usharik.app.databinding.FragmentQuizModeSelectionBinding;

public class QuizModeSelectionFragment extends Fragment {

    private FragmentQuizModeSelectionBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_quiz_mode_selection, container, false);
        binding.btnFullTable.setOnClickListener(v -> navigateTo(DeclensionQuizFragment.class));
        binding.btnOneCase.setOnClickListener(v -> navigateTo(SingleCaseQuizFragment.class));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void navigateTo(Class<? extends Fragment> fragmentClass) {
        FragmentManager manager = requireActivity().getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(fragmentClass.getSimpleName());
        if (fragment == null) {
            try {
                fragment = fragmentClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
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
