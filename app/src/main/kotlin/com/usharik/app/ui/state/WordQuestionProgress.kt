package com.usharik.app.ui.state

/** Pure question-order rules shared by the single-case quiz and its unit tests. */
object WordQuestionProgress {
    const val FORMS_PER_NUMBER = 7

    fun index(caseIndex: Int, plural: Boolean): Int = caseIndex + if (plural) FORMS_PER_NUMBER else 0

    /**
     * A word is complete once its current available form has been answered and no later available
     * form remains. This handles paradigms that intentionally omit one or more trailing forms.
     */
    fun isFinalAnsweredForm(forms: List<String>, currentIndex: Int, answered: Boolean): Boolean =
        answered && forms.drop(currentIndex + 1).none { it.isNotEmpty() }
}
