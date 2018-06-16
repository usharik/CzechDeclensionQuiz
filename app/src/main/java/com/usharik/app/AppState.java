package com.usharik.app;

import com.example.database.WordInfo;

/**
 * Created by macbook on 07/03/2018.
 */

public class AppState {
    WordInfo wordInfo;
    MainViewModel.WordTextModel[] wordTextModels = new MainViewModel.WordTextModel[14];
    String[][] correctAnswers = new String[2][7];
    int[][] actualAnswers = new int[2][7];
}
