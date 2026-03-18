package com.usharik.app.fragment;

import androidx.databinding.Bindable;
import com.usharik.database.dao.DatabaseManager;
import com.usharik.app.BR;
import com.usharik.app.R;

import com.usharik.app.framework.ViewModelObservable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.Map;

import javax.inject.Inject;

public class HandbookViewModel extends ViewModelObservable {

    private static final int DEFAULT_SELECTED_GENDER = R.id.radioMasculine;
    private static final int DEFAULT_SELECTED_WORD_ID = R.id.pan;
    private static final String DEFAULT_SELECTED_WORD = "pán";

    private String[][] cases;
    private String selectedWord;
    private int selectedWordId;
    private int selectedGender;

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

    @Inject
    public HandbookViewModel(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.cases = new String[2][7];
        this.selectedGender = DEFAULT_SELECTED_GENDER;
        this.selectedWordId = DEFAULT_SELECTED_WORD_ID;
        applySelectedWord(DEFAULT_SELECTED_WORD, false);
    }

    @Bindable
    public int getSelectedGender() {
        return selectedGender;
    }

    public void setSelectedGender(int selectedGender) {
        this.selectedGender = selectedGender;
        notifyPropertyChanged(BR.selectedGender);
    }

    @Bindable
    public String[][] getCases() {
        return cases;
    }

    public void setSelectedWord(String selectedWord) {
        applySelectedWord(selectedWord, true);
    }

    private void applySelectedWord(String selectedWord, boolean notify) {
        this.selectedWord = selectedWord;
        databaseManager.getDocumentDb().getWordInfoByWord(selectedWord)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wordInfo -> {
                    cases = wordInfo.cases();
                    if (notify) {
                        notifyPropertyChanged(BR.cases);
                        notifyPropertyChanged(BR.otherNouns);
                    }
                });
    }

    @Bindable
    public int getSelectedWordId() {
        return selectedWordId;
    }

    public void setSelectedWordId(int selectedWordId) {
        this.selectedWordId = selectedWordId;
        notifyPropertyChanged(BR.selectedWordId);
    }

    @Bindable
    public String getOtherNouns() {
        return otherNouns.get(selectedWord);
    }
}
