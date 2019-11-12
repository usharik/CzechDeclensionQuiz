package com.usharik.app.di;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.usharik.app.fragment.DeclensionQuizViewModel;
import com.usharik.app.fragment.HandbookViewModel;

import com.usharik.app.fragment.SettingsViewModel;
import com.usharik.app.fragment.WordsWithErrorsViewModel;

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
    @IntoMap
    @ViewModelKey(SettingsViewModel.class)
    abstract ViewModel bindOptionsViewModel(SettingsViewModel userViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(WordsWithErrorsViewModel.class)
    abstract ViewModel bindWordsWithErrorsViewModel(WordsWithErrorsViewModel userViewModel);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(AppViewModelFactory factory);
}
