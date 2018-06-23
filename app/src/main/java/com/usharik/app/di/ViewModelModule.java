package com.usharik.app.di;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.usharik.app.fragment.DeclensionQuizViewModel;
import com.usharik.app.fragment.HandbookViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

/**
 * Created by macbook on 09.02.18.
 */

@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(DeclensionQuizViewModel.class)
    abstract ViewModel bindDeclensionQuizViewModel(DeclensionQuizViewModel userViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(HandbookViewModel.class)
    abstract ViewModel bindHandbookViewModel(HandbookViewModel userViewModel);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(AppViewModelFactory factory);
}
