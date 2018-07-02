package com.usharik.app.fragment;

import android.databinding.Bindable;
import com.usharik.app.AppState;
import com.usharik.app.BR;
import com.usharik.app.Gender;
import com.usharik.app.R;
import com.usharik.app.framework.ViewModelObservable;

import javax.inject.Inject;

public class SettingsViewModel extends ViewModelObservable {

    private final AppState appState;

    @Inject
    SettingsViewModel(AppState appState) {
        this.appState = appState;
    }

    @Bindable
    public int getGenderFilterId() {
        return appState.genderFilterId;
    }

    public void setGenderFilterId(int genderFilterId) {
        String str = "all";
        switch (genderFilterId) {
            case R.id.radioAnimateMasculine:
                str = Gender.ANIMATE_MASCULINE;
                break;
            case R.id.radioInanimateMasculine:
                str = Gender.INANIMATE_MASCULINE;
                break;
            case R.id.radioFeminine:
                str = Gender.FEMININE;
                break;
            case R.id.radioNeuter:
                str = Gender.NEUTER;
                break;
        }
        appState.genderFilterId = genderFilterId;
        appState.genderFilterStr = str;
    }

    @Bindable
    public boolean isSwitchOffAnimation() {
        return appState.switchOffAnimation;
    }

    public void setSwitchOffAnimation(boolean switchOffAnimation) {
        appState.switchOffAnimation = switchOffAnimation;
        notifyPropertyChanged(BR.switchOffAnimation);
    }
}
