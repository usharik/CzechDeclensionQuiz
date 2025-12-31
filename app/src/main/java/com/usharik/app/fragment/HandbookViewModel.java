package com.usharik.app.fragment;

import androidx.databinding.Bindable;
import com.usharik.app.AppState;
import com.usharik.database.WordInfo;
import com.usharik.database.dao.DatabaseManager;
import com.usharik.app.BR;

import com.usharik.app.framework.ViewModelObservable;

import java.util.Map;

import javax.inject.Inject;

public class HandbookViewModel extends ViewModelObservable {

    private String[][] cases;

    private static final Map<String, String> otherNouns = Map.ofEntries(
        Map.entry("pán", "syn, pes, doktor"),
        Map.entry("hrad", "dům, rok, hotel"),
        Map.entry("muž", "lékař, řidič, strýc"),
        Map.entry("stroj", "konec, čaj, nůž"),
        Map.entry("předseda", "děda, Jirka, Honza"),
        Map.entry("soudce", "poradce"),
        Map.entry("město", "auto, okno, jablko, zrcadlo"),
        Map.entry("moře", "pole, nebe"),
        Map.entry("kuře", "dítě, štěně, kotě, tele"),
        Map.entry("stavení", "nádraží, náměstí, září, umění"),
        Map.entry("žena", "kniha, matka, třída, houska"),
        Map.entry("růže", "večeře, historie"),
        Map.entry("píseň", "povodeň, pláž, loď"),
        Map.entry("kost", "radost, starost")
    );


    private final DatabaseManager databaseManager;
    private final AppState appState;

    @Inject
    public HandbookViewModel(final DatabaseManager databaseManager,
                             final AppState appState) {
        this.databaseManager = databaseManager;
        this.appState = appState;
        this.cases = new String[2][7];
    }

    @Bindable
    public int getSelectedGender() {
        return appState.selectedGender;
    }

    public void setSelectedGender(int selectedGender) {
        appState.selectedGender = selectedGender;
        notifyPropertyChanged(BR.selectedGender);
    }

    @Bindable
    public String[][] getCases() {
        return cases;
    }

    public void setSelectedWord(String selectedWord) {
        appState.selectedWord = selectedWord;
        WordInfo wordInfo = databaseManager.getDocumentDb().getWordInfoByWord(selectedWord).blockingGet();
        cases = wordInfo.cases;
        notifyPropertyChanged(BR.cases);
        notifyPropertyChanged(BR.otherNouns);
    }

    @Bindable
    public int getSelectedWordId() {
        return appState.selectedWordId;
    }

    public void setSelectedWordId(int selectedWordId) {
        appState.selectedWordId = selectedWordId;
        notifyPropertyChanged(BR.selectedWordId);
    }

    @Bindable
    public String getOtherNouns() {
        return otherNouns.get(appState.selectedWord);
    }
}
