package com.usharik.app.fragment;

import com.usharik.app.SingleCaseQuizState;
import com.usharik.app.service.LastWordStore;
import com.usharik.app.service.WordService;
import com.usharik.database.WordInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class SingleCaseQuizViewModelTest {

    @Before
    public void setUp() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(s -> Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(s -> Schedulers.trampoline());
    }

    @After
    public void tearDown() {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }

    @Test
    public void nextWord_tryAgain_resetsToFirstSingularCaseAndClearsAnswered() {
        SingleCaseQuizState state = new SingleCaseQuizState();
        state.setWordInfo(createWordInfo());
        state.setCurrentCase(3);
        state.setPlural(true);
        state.setAnswered(true);

        SingleCaseQuizViewModel viewModel = new SingleCaseQuizViewModel(
                new FakeWordService(), null, Locale.ENGLISH, state);

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

        SingleCaseQuizViewModel viewModel = new SingleCaseQuizViewModel(
                new FakeWordService(), null, Locale.ENGLISH, state);

        viewModel.nextStep();

        assertEquals(0, viewModel.getCurrentCaseIndex());
        assertEquals(SingleCaseQuizViewModel.PLURAL, viewModel.getCurrentNumber());
        assertFalse(viewModel.isAnswered());
        assertEquals("word-pl-0", viewModel.getCorrectAnswer());
        assertTrue(viewModel.getAnswers().contains("word-pl-0"));
    }

    @Test
    public void getTranslation_usesInjectedAppLocale() {
        SingleCaseQuizState state = new SingleCaseQuizState();
        state.setWordInfo(createWordInfo());

        SingleCaseQuizViewModel viewModel = new SingleCaseQuizViewModel(
                new FakeWordService(), null, Locale.forLanguageTag("ru-RU"), state);

        assertEquals("роза", viewModel.getTranslation());
    }

    @Test
    public void restoreOrNextWord_usesSavedWord() {
        SingleCaseQuizState state = new SingleCaseQuizState();
        FakeWordService wordService = new FakeWordService();
        FakeLastWordStore store = new FakeLastWordStore();
        store.saveLastWord(LastWordStore.MODE_SINGLE_CASE, "word");

        SingleCaseQuizViewModel viewModel = new SingleCaseQuizViewModel(
                wordService, null, Locale.ENGLISH, state, store);

        viewModel.restoreOrNextWord();

        assertEquals("word", viewModel.getWord());
        assertEquals(0, viewModel.getCurrentCaseIndex());
        assertEquals(SingleCaseQuizViewModel.SINGULAR, viewModel.getCurrentNumber());
        assertFalse(viewModel.isAnswered());
        assertFalse(wordService.getNextWordCalled);
    }

    @Test
    public void restoreOrNextWord_fallsBackToRandom_whenNoSavedWord() {
        SingleCaseQuizState state = new SingleCaseQuizState();
        FakeWordService wordService = new FakeWordService();
        FakeLastWordStore store = new FakeLastWordStore();

        SingleCaseQuizViewModel viewModel = new SingleCaseQuizViewModel(
                wordService, null, Locale.ENGLISH, state, store);

        viewModel.restoreOrNextWord();

        assertEquals("random", viewModel.getWord());
        assertTrue(wordService.getNextWordCalled);
    }

    private WordInfo createWordInfo() {
        return createWordInfo("word");
    }

    private static WordInfo createWordInfo(String word) {
        String[][] cases = new String[][]{
                {"word-sg-0", "word-sg-1", "word-sg-2", "word-sg-3", "word-sg-4", "word-sg-5", "word-sg-6"},
                {"word-pl-0", "word-pl-1", "word-pl-2", "word-pl-3", "word-pl-4", "word-pl-5", "word-pl-6"}
        };
        return new WordInfo(1L, word, cases, "роза", "rose", "f", "noun");
    }

    private static final class FakeWordService extends WordService {
        boolean getNextWordCalled;

        FakeWordService() {
            super(null, null, null);
        }

        @Override
        public Maybe<WordInfo> getWordByName(String word) {
            return "word".equals(word) ? Maybe.just(createWordInfo(word)) : Maybe.empty();
        }

        @Override
        public Single<WordInfo> getNextWord(WordInfo currentWord) {
            getNextWordCalled = true;
            return Single.just(createWordInfo("random"));
        }
    }

    private static final class FakeLastWordStore implements LastWordStore {
        private final Map<String, String> words = new HashMap<>();

        @Override
        public void saveLastWord(String modeKey, String word) {
            words.put(modeKey, word);
        }

        @Override
        public String getLastWord(String modeKey) {
            return words.get(modeKey);
        }

        @Override
        public void clear(String modeKey) {
            words.remove(modeKey);
        }
    }
}