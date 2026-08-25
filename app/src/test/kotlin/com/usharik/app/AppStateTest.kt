package com.usharik.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {
    @Test fun defaultsAreSafeForANewInstallation() {
        AppState().also { state ->
            assertEquals(Gender.ALL, state.getGenderFilterStr())
            assertFalse(state.getSwitchOffAnimation())
            assertTrue(state.getWordsWithErrors().isEmpty())
        }
    }
}
