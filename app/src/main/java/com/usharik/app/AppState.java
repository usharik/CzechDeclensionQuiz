package com.usharik.app;

import com.usharik.app.service.WordService;

/**
 * Created by macbook on 07/03/2018.
 */

public class AppState {
    WordService.WordInfo wordInfo;
    MainViewModel.WordTextModel[] wordTextModels = new MainViewModel.WordTextModel[14];
    String[][] correctAnswers = new String[2][7];
    int[][] actualAnswers = new int[2][7];
}
