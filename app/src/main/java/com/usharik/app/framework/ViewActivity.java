package com.usharik.app.framework;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import dagger.android.AndroidInjection;
import dagger.android.DispatchingAndroidInjector;

/**
 * Created by macbook on 17.02.2018.
 */

public abstract class ViewActivity<T extends ViewModel> extends AppCompatActivity {
    private T viewModel;

    @Inject
    DispatchingAndroidInjector<Fragment> dispatchingAndroidInjector;

    @Inject
    ViewModelProvider.Factory appViewModelFactory;

    protected abstract Class<T> getViewModelClass();

    @Override
    protected void onResume() {
        super.onResume();
        AndroidInjection.inject(this);
        viewModel = ViewModelProviders
                .of(this, appViewModelFactory)
                .get(getViewModelClass());
    }

    public T getViewModel() {
        return viewModel;
    }
}
