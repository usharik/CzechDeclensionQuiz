package com.usharik.app.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.usharik.app.AppState;
import com.usharik.app.R;
import com.usharik.app.databinding.HandbookFragmentBinding;
import com.usharik.app.framework.ViewFragment;

import javax.inject.Inject;

public class HandbookFragment extends ViewFragment<HandbookViewModel> {

    @Inject
    AppState appState;

    private HandbookFragmentBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.handbook_fragment, container, false);
        binding.setViewModel(getViewModel());
        binding.wordGroup.setOnCheckedChangeListener(this::onWordCheckedChangeListener);
        binding.genderGroup.check(getViewModel().getSelectedGender());
        binding.wordGroup.check(getViewModel().getSelectedWordId());
        return binding.getRoot();
    }

    private void onWordCheckedChangeListener(RadioGroup group, int checkedId)
    {
        RadioButton checkedRadioButton = group.findViewById(checkedId);
        if (checkedRadioButton == null) {
            return;
        }
        boolean isChecked = checkedRadioButton.isChecked();
        if (isChecked)
        {
            getViewModel().setSelectedWord(checkedRadioButton.getText().toString());
            getViewModel().setSelectedWordId(checkedId);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected Class<HandbookViewModel> getViewModelClass() {
        return HandbookViewModel.class;
    }
}
