package com.usharik.app.fragment;

import androidx.databinding.Bindable;
import com.usharik.app.AppState;
import com.usharik.app.BR;
import com.usharik.app.Gender;
import com.usharik.app.R;
import com.usharik.app.framework.ViewModelObservable;
import com.usharik.app.service.FirebaseAnalyticsService;

import javax.inject.Inject;

public class SettingsViewModel extends ViewModelObservable {

    private final AppState appState;
    private final FirebaseAnalyticsService analyticsService;

    @Inject
    SettingsViewModel(AppState appState,
                      FirebaseAnalyticsService analyticsService) {
        this.appState = appState;
        this.analyticsService = analyticsService;
    }

    @Bindable
    public int getGenderFilterId() {
        return appState.getGenderFilterId();
    }

    public void setGenderFilterId(int genderFilterId) {
        String str = "all";
        if (genderFilterId == R.id.radioAnimateMasculine) {
            str = Gender.ANIMATE_MASCULINE;
        } else if (genderFilterId == R.id.radioInanimateMasculine) {
            str = Gender.INANIMATE_MASCULINE;
        } else if (genderFilterId == R.id.radioFeminine) {
            str = Gender.FEMININE;
        } else if (genderFilterId == R.id.radioNeuter) {
            str = Gender.NEUTER;
        }
        appState.setGenderFilterStr(str);
    }

    @Bindable
    public boolean isSwitchOffAnimation() {
        return appState.getSwitchOffAnimation();
    }

    public void setSwitchOffAnimation(boolean switchOffAnimation) {
        appState.setSwitchOffAnimation(switchOffAnimation);
        notifyPropertyChanged(BR.switchOffAnimation);
        analyticsService.logSettings(switchOffAnimation);
    }
}
