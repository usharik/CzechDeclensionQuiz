package com.usharik.app.ui.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordQuestionProgressTest {
    @Test fun lastAvailableFormCanPrecedeTrailingEmptyForms() {
        val forms = List(14) { "form-$it" }.toMutableList().also {
            it[12] = ""
            it[13] = ""
        }

        assertTrue(WordQuestionProgress.isFinalAnsweredForm(forms, currentIndex = 11, answered = true))
    }

    @Test fun unansweredOrRemainingAvailableFormIsNotComplete() {
        val forms = List(14) { "form-$it" }

        assertFalse(WordQuestionProgress.isFinalAnsweredForm(forms, currentIndex = 13, answered = false))
        assertFalse(WordQuestionProgress.isFinalAnsweredForm(forms, currentIndex = 11, answered = true))
    }

    @Test fun indexFollowsSingularThenPluralQuestionOrder() {
        assertTrue(WordQuestionProgress.index(caseIndex = 6, plural = false) == 6)
        assertTrue(WordQuestionProgress.index(caseIndex = 0, plural = true) == 7)
        assertTrue(WordQuestionProgress.index(caseIndex = 6, plural = true) == 13)
    }
}
