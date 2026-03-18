package com.usharik.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppStateTest {

    @Test
    public void defaultsExposeOnlyGlobalSettingValues() {
        AppState appState = new AppState();

        assertEquals(Gender.ALL, appState.getGenderFilterStr());
        assertEquals(-1, appState.getGenderFilterId());
        assertFalse(appState.getSwitchOffAnimation());
        assertEquals(0, appState.getWordsCountSinceLastAd());
        assertTrue(appState.getWordsWithErrors().isEmpty());
    }
}