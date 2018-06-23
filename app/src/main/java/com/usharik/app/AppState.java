package com.usharik.app;

import com.example.database.WordInfo;
import com.usharik.app.fragment.DeclensionQuizViewModel;

/**
 * Created by macbook on 07/03/2018.
 */

public class AppState {
    public WordInfo wordInfo;
    public DeclensionQuizViewModel.WordTextModel[] wordTextModels = new DeclensionQuizViewModel.WordTextModel[14];
    public String[][] correctAnswers = new String[2][7];
    public int[][] actualAnswers = new int[2][7];
}
