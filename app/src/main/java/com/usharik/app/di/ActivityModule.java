package com.usharik.app.di;

import com.usharik.app.MainActivity;
import com.usharik.app.fragment.DeclensionQuizFragment;
import com.usharik.app.fragment.HandbookFragment;

import com.usharik.app.fragment.SettingsFragment;
import com.usharik.app.fragment.WordsWithErrorsFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Created by macbook on 10.02.18.
 */

@Module
public abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract MainActivity contributeMainActivity();

    @ContributesAndroidInjector
    abstract DeclensionQuizFragment contributeDeclensionQuizFragment();

    @ContributesAndroidInjector
    abstract HandbookFragment contributeHandbookFragment();

    @ContributesAndroidInjector
    abstract SettingsFragment contributeOptionsFragment();

    @ContributesAndroidInjector
    abstract WordsWithErrorsFragment contributeWordsWithErrorsFragment();
}
