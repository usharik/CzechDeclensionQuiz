package com.usharik.app.fragment;

import com.usharik.app.SingleCaseQuizState;
import com.usharik.app.service.WordService;
import com.usharik.database.WordInfo;

import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleCaseQuizViewModelTest {

    @Test
    public void nextWord_tryAgain_resetsToFirstSingularCaseAndClearsAnswered() {
        SingleCaseQuizState state = new SingleCaseQuizState();
        state.setWordInfo(createWordInfo());
        state.setCurrentCase(3);
        state.setPlural(true);
        state.setAnswered(true);

        SingleCaseQuizViewModel viewModel = new SingleCaseQuizViewModel(new FakeWordService(), Locale.ENGLISH, state);

        viewModel.nextWord(true);

        assertEquals(0, viewModel.getCurrentCaseIndex());
        assertEquals(SingleCaseQuizViewModel.SINGULAR, viewModel.getCurrentNumber());
        assertFalse(viewModel.isAnswered());
        assertEquals("word-sg-0", viewModel.getCorrectAnswer());
        assertTrue(viewModel.getAnswers().contains("word-sg-0"));
    }

    @Test
    public void nextStep_advancesFromLastSingularCaseToFirstPluralCase() {
        SingleCaseQuizState state = new SingleCaseQuizState();
        state.setWordInfo(createWordInfo());
        state.setCurrentCase(6);
        state.setPlural(false);
        state.setCorrectAnswer("word-sg-6");
        state.setAnswers(List.of("word-sg-6", "word-sg-1", "word-pl-1", "word-pl-2"));
        state.setAnswered(true);

        SingleCaseQuizViewModel viewModel = new SingleCaseQuizViewModel(new FakeWordService(), Locale.ENGLISH, state);

        viewModel.nextStep();

        assertEquals(0, viewModel.getCurrentCaseIndex());
        assertEquals(SingleCaseQuizViewModel.PLURAL, viewModel.getCurrentNumber());
        assertFalse(viewModel.isAnswered());
        assertEquals("word-pl-0", viewModel.getCorrectAnswer());
        assertTrue(viewModel.getAnswers().contains("word-pl-0"));
    }

    private WordInfo createWordInfo() {
        String[][] cases = new String[][]{
                {"word-sg-0", "word-sg-1", "word-sg-2", "word-sg-3", "word-sg-4", "word-sg-5", "word-sg-6"},
                {"word-pl-0", "word-pl-1", "word-pl-2", "word-pl-3", "word-pl-4", "word-pl-5", "word-pl-6"}
        };
        return new WordInfo(1L, "word", cases, "", "", "f", "rose");
    }

    private static final class FakeWordService extends WordService {
        FakeWordService() {
            super(null, null, null);
        }
    }
}