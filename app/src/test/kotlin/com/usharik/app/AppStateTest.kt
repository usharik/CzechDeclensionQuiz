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
            assertEquals(AppState.DEFAULT_HANDBOOK_GENDER, state.getHandbookGender())
            assertEquals(mapOf(AppState.DEFAULT_HANDBOOK_GENDER to AppState.DEFAULT_HANDBOOK_PARADIGM), state.getHandbookParadigmByGender())
        }
    }

    @Test fun handbookGenderIsPersisted() {
        AppState().also { state ->
            state.setHandbookGender("FEMININE")
            assertEquals("FEMININE", state.getHandbookGender())
        }
    }

    @Test fun handbookParadigmIsPersistedPerGender() {
        AppState().also { state ->
            state.setHandbookParadigm("MASCULINE", "hrad")
            state.setHandbookParadigm("FEMININE", "růže")
            assertEquals("hrad", state.getHandbookParadigmByGender()["MASCULINE"])
            assertEquals("růže", state.getHandbookParadigmByGender()["FEMININE"])
        }
    }
}
