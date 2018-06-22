package com.usharik.app.di;

import com.usharik.app.fragment.DeclensionQuizFragment;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Created by macbook on 10.02.18.
 */

@Module
public abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract DeclensionQuizFragment contributeDeclensionQuizFragment();
}
