package com.usharik.app;

import com.example.database.WordInfo;

/**
 * Created by macbook on 07/03/2018.
 */

public class AppState {
    public WordInfo wordInfo;
    public MainViewModel.WordTextModel[] wordTextModels = new MainViewModel.WordTextModel[14];
    public String[][] correctAnswers = new String[2][7];
    public int[][] actualAnswers = new int[2][7];
}
