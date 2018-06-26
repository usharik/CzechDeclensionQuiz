package com.usharik.app.fragment;

import android.databinding.Bindable;

import com.usharik.app.AppState;
import com.usharik.database.WordInfo;
import com.usharik.database.dao.DatabaseManager;
import com.usharik.app.BR;

import com.usharik.app.framework.ViewModelObservable;

import java.util.HashMap;

import javax.inject.Inject;

public class HandbookViewModel extends ViewModelObservable {

    private String[][] cases;

    private static HashMap<String, String> otherNouns = buildOtherNounMap();

    private static HashMap<String,String> buildOtherNounMap() {
        HashMap<String, String> res = new HashMap<>();
        res.put("pán", "syn, pes, doktor");
        res.put("hrad", "dům, rok, hotel");
        res.put("muž", "lékař, řidič, strýc");
        res.put("stroj", "konec, čaj, nůž");
        res.put("předseda", "děda, Jirka, Honza");
        res.put("soudce", "poradce");
        res.put("město", "auto, okno, jablko, zrcadlo");
        res.put("moře", "pole, nebe");
        res.put("kuře", "dítě, štěně, kotě, tele");
        res.put("stavení", "nádraží, náměstí, září, umění");
        res.put("žena", "kniha, matka, třída, houska");
        res.put("růže", "večeře, historie");
        res.put("píseň", "povodeň, pláž, loď");
        res.put("kost", "radost, starost");
        return res;
    }

    private final DatabaseManager databaseManager;
    private final AppState appState;

    @Inject
    public HandbookViewModel(final DatabaseManager databaseManager,
                             final AppState appState) {
        this.databaseManager = databaseManager;
        this.appState = appState;
        cases = new String[2][7];
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
