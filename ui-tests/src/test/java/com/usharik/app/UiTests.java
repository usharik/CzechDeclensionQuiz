package com.usharik.app;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.usharik.app.Parameters.APPIUM_URL;
import static com.usharik.app.Parameters.APP_PATH;
import static com.usharik.app.Parameters.DATA_JSON_PATH;
import static com.usharik.app.Parameters.DEFAULT_TIMEOUT;
import static com.usharik.app.Parameters.DRAG_DURATION;
import static com.usharik.app.Parameters.IMPLICIT_WAIT;
import static com.usharik.app.Parameters.SCREEN_STABILITY_DELAY;
import static com.usharik.app.Parameters.UI_UPDATE_DELAY;
import static com.usharik.app.UiConstants.APP_PACKAGE;
import static com.usharik.app.UiConstants.ID_ACTION_NEXT;
import static com.usharik.app.UiConstants.ID_APP_LOGO;
import static com.usharik.app.UiConstants.ID_APP_NAME;
import static com.usharik.app.UiConstants.ID_APP_VERSION;
import static com.usharik.app.UiConstants.ID_BTN_ABOUT;
import static com.usharik.app.UiConstants.ID_BTN_ANSWER_1;
import static com.usharik.app.UiConstants.ID_BTN_ANSWER_2;
import static com.usharik.app.UiConstants.ID_BTN_ANSWER_3;
import static com.usharik.app.UiConstants.ID_BTN_ANSWER_4;
import static com.usharik.app.UiConstants.ID_BTN_FULL_TABLE;
import static com.usharik.app.UiConstants.ID_BTN_HANDBOOK;
import static com.usharik.app.UiConstants.ID_BTN_LEAVE_QUIZ;
import static com.usharik.app.UiConstants.ID_BTN_NEXT_CASE;
import static com.usharik.app.UiConstants.ID_BTN_NEXT_WORD;
import static com.usharik.app.UiConstants.ID_BTN_ONE_CASE;
import static com.usharik.app.UiConstants.ID_BTN_RATE_APP;
import static com.usharik.app.UiConstants.ID_BTN_SETTINGS;
import static com.usharik.app.UiConstants.ID_BTN_STAY_HERE;
import static com.usharik.app.UiConstants.ID_BTN_TRY_AGAIN;
import static com.usharik.app.UiConstants.ID_BTN_WORDS_WITH_ERRORS;
import static com.usharik.app.UiConstants.ID_CASES_CONTAINER;
import static com.usharik.app.UiConstants.ID_CASE_PLURAL;
import static com.usharik.app.UiConstants.ID_CASE_SINGULAR;
import static com.usharik.app.UiConstants.ID_CHECKBOX_HEADER;
import static com.usharik.app.UiConstants.ID_CURRENT_WORD;
import static com.usharik.app.UiConstants.ID_DIALOG_TITLE;
import static com.usharik.app.UiConstants.ID_GENDER_GROUP;
import static com.usharik.app.UiConstants.ID_GENDER_HEADER;
import static com.usharik.app.UiConstants.ID_RADIO_GROUP;
import static com.usharik.app.UiConstants.ID_RADIO_GROUP_HEADER;
import static com.usharik.app.UiConstants.ID_TITLE_QUIZ_MODE;
import static com.usharik.app.UiConstants.ID_TOOLBAR;
import static com.usharik.app.UiConstants.ID_TV_CASE_NAME;
import static com.usharik.app.UiConstants.ID_TV_CASE_QUESTION;
import static com.usharik.app.UiConstants.ID_TV_NUMBER_LABEL;
import static com.usharik.app.UiConstants.ID_TV_WORD;
import static com.usharik.app.UiConstants.ID_WORDS_RECYCLER;
import com.usharik.app.helpers.TestHelper;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

public class UiTests {

    private static final Logger logger = LoggerFactory.getLogger(UiTests.class);
    private static final String TIMESTAMP = Long.toString(System.currentTimeMillis());
    private static final String XPATH_TOOLBAR_HOME = "//*[@resource-id='" + ID_TOOLBAR + "']//android.widget.ImageButton[1]";

    private static AndroidDriver driver;
    private static TestHelper helper;
    private WebDriverWait wait;

    @BeforeAll
    public static void setupClass() {
        File appFile = new File(APP_PATH);
        if (!appFile.exists()) {
            throw new IllegalStateException("APK file not found at: " + appFile.getAbsolutePath());
        }

        File dataFile = new File(DATA_JSON_PATH);
        if (!dataFile.exists()) {
            throw new IllegalStateException("Data JSON file not found at: " + dataFile.getAbsolutePath());
        }

        if (APPIUM_URL.isEmpty()) {
            throw new IllegalStateException("Missing required system property 'appium.url'");
        }

        final URL appiumServerUrl;
        try {
            appiumServerUrl = URI.create(APPIUM_URL).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Appium URL: " + APPIUM_URL, e);
        }

        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName("Android Device")
                .setApp(appFile.getAbsolutePath())
                .setNoReset(true)
                .setAutoGrantPermissions(true);

        driver = new AndroidDriver(appiumServerUrl, options);
        driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);
        helper = new TestHelper(driver, DATA_JSON_PATH);
    }

    @BeforeEach
    public void setupTest(TestInfo testInfo) {
        logger.info("Starting test: {}", testInfo.getDisplayName());
        wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
        ensureHubScreen();
    }

    @AfterEach
    public void tearDownTest(TestInfo testInfo) {
        try {
            driver.rotate(ScreenOrientation.PORTRAIT);
        } catch (Exception ignored) {
        }
        logger.info("Finished test: {}", testInfo.getDisplayName());
    }

    @AfterAll
    public static void tearDownClass() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testHubScreenShowsAllNavigationButtons() {
        assertHubVisible();
        assertButtonText(ID_BTN_FULL_TABLE, "Full declension table quiz");
        assertButtonText(ID_BTN_ONE_CASE, "One case at a time quiz");
        assertButtonText(ID_BTN_WORDS_WITH_ERRORS, "Words with errors");
        assertButtonText(ID_BTN_HANDBOOK, "Handbook");
        assertButtonText(ID_BTN_SETTINGS, "Settings");
        assertButtonText(ID_BTN_ABOUT, "About");
    }

    @Test
    public void testFullQuizNavigationReturnsToHubViaBack() {
        openFullQuizFromHub();
        assertDeclensionQuizVisible();

        navigateBackFromQuiz();
        waitForHubScreen();

        openFullQuizFromHub();
        assertDeclensionQuizVisible();
        navigateBackFromQuiz();
        waitForHubScreen();
    }

    @Test
    public void testNavigateToWordsWithErrorsScreen() {
        openPageFromHub(ID_BTN_WORDS_WITH_ERRORS, ID_CASES_CONTAINER);
        assertNotNull(waitForVisibleElement(ID_CASES_CONTAINER));
        driver.navigate().back();
        waitForHubScreen();
    }

    @Test
    public void testNavigateToHandbookScreen() {
        openPageFromHub(ID_BTN_HANDBOOK, ID_GENDER_HEADER);

        WebElement genderHeader = waitForVisibleElement(ID_GENDER_HEADER);
        assertEquals("Gender of noun", genderHeader.getText());
        assertNotNull(waitForVisibleElement(ID_GENDER_GROUP));

        driver.navigate().back();
        waitForHubScreen();
    }

    @Test
    public void testNavigateToSettingsScreen() {
        openPageFromHub(ID_BTN_SETTINGS, ID_RADIO_GROUP_HEADER);

        WebElement radioGroupHeader = waitForVisibleElement(ID_RADIO_GROUP_HEADER);
        assertEquals("Word filter by gender", radioGroupHeader.getText());
        assertNotNull(waitForVisibleElement(ID_RADIO_GROUP));

        WebElement checkboxHeader = waitForVisibleElement(ID_CHECKBOX_HEADER);
        assertEquals("Additional settings", checkboxHeader.getText());

        driver.navigate().back();
        waitForHubScreen();
    }

    @Test
    public void testNavigateToAboutScreen() {
        openPageFromHub(ID_BTN_ABOUT, ID_APP_NAME);

        WebElement appName = waitForVisibleElement(ID_APP_NAME);
        assertEquals("Czech declension quiz", appName.getText());
        assertNotNull(waitForVisibleElement(ID_APP_LOGO));
        assertTrue(waitForVisibleElement(ID_APP_VERSION).getText().startsWith("Version "));

        driver.navigate().back();
        waitForHubScreen();
    }

    @Test
    public void testCorrectQuizSolution() {
        openFullQuizFromHub();
        clickNextWord();

        List<WebElement> caseSingular = findElements(ID_CASE_SINGULAR);
        List<WebElement> casePlural = findElements(ID_CASE_PLURAL);
        String currentWordText = findElement(ID_CURRENT_WORD).getText();
        assertNotNull(currentWordText);

        String[][] wordCases = helper.getWordCases(currentWordText);
        helper.makeScreenshot(TIMESTAMP, "before_solution.png");
        placeAllWordForms(wordCases, caseSingular, casePlural);
        helper.makeScreenshot(TIMESTAMP, "after_solution.png");

        // Dialog should appear automatically after all correct answers are placed
        verifySuccessDialog();
    }

    @Test
    public void testIncorrectQuizSolution() {
        openFullQuizFromHub();
        clickNextWord();

        List<WebElement> caseSingular = findElements(ID_CASE_SINGULAR);
        List<WebElement> casePlural = findElements(ID_CASE_PLURAL);
        String currentWordText = findElement(ID_CURRENT_WORD).getText();
        assertNotNull(currentWordText);

        String[][] wordCases = helper.getWordCases(currentWordText);
        helper.makeScreenshot(TIMESTAMP, "before_incorrect_solution.png");

        // Try to place some words incorrectly
        List<String> initialPoolWords = getVisibleWordPoolWordTexts();
        int wordsToTry = Math.min(3, initialPoolWords.size());
        for (int i = 0; i < wordsToTry; i++) {
            String wordText = initialPoolWords.get(i);
            WebElement wordElement = findWordPoolWordByText(wordText);
            // Try to place in wrong cell
            WebElement targetCell = getWrongCell(wordText, wordCases, caseSingular, casePlural);
            performDragAndDrop(wordElement, targetCell);
            waitForUiUpdate();
            // Wait for animation and word to return to pool
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        helper.makeScreenshot(TIMESTAMP, "after_incorrect_attempts.png");

        // Success dialog should NOT appear since quiz is not complete
        assertTrue(driver.findElements(AppiumBy.id(ID_DIALOG_TITLE)).isEmpty());

        // Word pool should still have words (incorrect ones were returned)
        List<WebElement> poolWords = findVisibleWordPoolItems();
        assertTrue(poolWords.size() > 0);
    }

    @Test
    public void testSingleCaseQuizScreen() {
        openOneCaseQuizFromHub();

        assertFalse(waitForVisibleElement(ID_TV_WORD).getText().isEmpty());
        assertFalse(waitForVisibleElement(ID_TV_CASE_NAME).getText().isEmpty());
        assertEquals("Singular", waitForVisibleElement(ID_TV_NUMBER_LABEL).getText());
        assertFalse(waitForVisibleElement(ID_TV_CASE_QUESTION).getText().isEmpty());

        assertNotNull(waitForVisibleElement(ID_BTN_ANSWER_1));
        assertNotNull(waitForVisibleElement(ID_BTN_ANSWER_2));
        assertNotNull(waitForVisibleElement(ID_BTN_ANSWER_3));
        assertNotNull(waitForVisibleElement(ID_BTN_ANSWER_4));
        assertFalse(waitForVisibleElement(ID_BTN_NEXT_CASE).isEnabled());

        navigateBackFromQuiz();
        waitForHubScreen();
    }

    @Test
    public void testSingleCaseQuizAnswerInteraction() {
        openOneCaseQuizFromHub();

        WebElement nextCaseButton = waitForVisibleElement(ID_BTN_NEXT_CASE);
        assertFalse(nextCaseButton.isEnabled());

        waitForVisibleElement(ID_BTN_ANSWER_1).click();
        waitForUiUpdate();

        WebElement updatedNextCaseButton = waitForVisibleElement(ID_BTN_NEXT_CASE);
        assertTrue(updatedNextCaseButton.isEnabled());
        assertFalse(waitForVisibleElement(ID_BTN_ANSWER_1).isEnabled());

        updatedNextCaseButton.click();
        waitForScreenStability();

        assertTrue(waitForVisibleElement(ID_BTN_ANSWER_1).isEnabled());
        assertFalse(waitForVisibleElement(ID_BTN_NEXT_CASE).isEnabled());

        navigateBackFromQuiz();
        waitForHubScreen();
    }

    private void ensureHubScreen() {
        if (isElementVisible(ID_TITLE_QUIZ_MODE, Duration.ofSeconds(2))) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            if (isElementVisible(ID_TITLE_QUIZ_MODE, Duration.ofSeconds(2))) {
                return;
            }

            if (isElementVisible(ID_TOOLBAR, Duration.ofSeconds(1))) {
                try {
                    clickToolbarHome();
                    dismissQuitOverlayIfVisible();
                    if (isElementVisible(ID_TITLE_QUIZ_MODE, Duration.ofSeconds(2))) {
                        return;
                    }
                } catch (Exception ignored) {
                }
            }

            driver.navigate().back();
            dismissQuitOverlayIfVisible();
            waitForScreenStability();
        }

        driver.activateApp(APP_PACKAGE);
        waitForHubScreen();
    }

    private void openFullQuizFromHub() {
        waitForVisibleElement(ID_BTN_FULL_TABLE).click();
        waitForScreenStability();
        assertDeclensionQuizVisible();
    }

    private void openOneCaseQuizFromHub() {
        waitForVisibleElement(ID_BTN_ONE_CASE).click();
        waitForScreenStability();
        waitForVisibleElement(ID_TV_WORD);
    }

    private void openPageFromHub(String buttonId, String expectedScreenId) {
        waitForVisibleElement(buttonId).click();
        waitForScreenStability();
        waitForVisibleElement(expectedScreenId);
    }

    private void assertDeclensionQuizVisible() {
        assertNotNull(waitForVisibleElement(ID_CURRENT_WORD));
        assertNotNull(waitForVisibleElement(ID_ACTION_NEXT));
    }

    private void assertHubVisible() {
        WebElement title = waitForVisibleElement(ID_TITLE_QUIZ_MODE);
        assertEquals("Open page", title.getText());
        assertNotNull(waitForVisibleElement(ID_BTN_FULL_TABLE));
        assertNotNull(waitForVisibleElement(ID_BTN_ONE_CASE));
    }

    private void waitForHubScreen() {
        waitForScreenStability();
        assertHubVisible();
    }

    /** Press back from a quiz screen and dismiss the quit-confirmation overlay if it appears. */
    private void navigateBackFromQuiz() {
        driver.navigate().back();
        dismissQuitOverlayIfVisible();
    }

    /**
     * If the quit-quiz dialog is visible (identified by the "Leave quiz" button),
     * click that button so navigation proceeds to the hub.
     */
    private void dismissQuitOverlayIfVisible() {
        if (isElementVisible(ID_BTN_LEAVE_QUIZ, Duration.ofSeconds(3))) {
            try {
                waitForVisibleElement(ID_BTN_LEAVE_QUIZ).click();
                waitForScreenStability();
            } catch (Exception ignored) {
            }
        }
    }

    private void clickToolbarHome() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId("Navigate up"))).click();
        } catch (Exception ignored) {
            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(XPATH_TOOLBAR_HOME))).click();
        }
        waitForScreenStability();
    }

    private void clickNextWord() {
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.id(ID_ACTION_NEXT))).click();
        waitForScreenStability();
    }

    private void assertButtonText(String id, String expectedText) {
        assertEquals(expectedText.toLowerCase(), waitForVisibleElement(id).getText().toLowerCase());
    }

    private void placeAllWordForms(String[][] wordCases, List<WebElement> caseSingular, List<WebElement> casePlural) {
        int placedCount = 0;
        int safetyGuard = 0;
        while (true) {
            List<WebElement> poolItems = findVisibleWordPoolItems();
            if (poolItems.isEmpty()) {
                break;
            }
            WebElement wordElement = poolItems.get(0);
            WebElement targetCell = getProperCell(wordElement.getText(), wordCases, caseSingular, casePlural);
            performDragAndDrop(wordElement, targetCell);
            waitForUiUpdate();

            placedCount++;
            safetyGuard++;
            if (safetyGuard > 30) {
                throw new IllegalStateException("Safety guard hit while placing words. Possible drag/drop regression.");
            }
        }
        logger.info("Placed {} forms", placedCount);
    }

    private void verifySuccessDialog() {
        WebElement dialogTitle = wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_DIALOG_TITLE)));
        helper.makeScreenshot(TIMESTAMP, "success_dialog_open.png");

        assertEquals("All is correct!", dialogTitle.getText());
        assertNotNull(findElement(ID_BTN_NEXT_WORD));
        assertNotNull(findElement(ID_BTN_STAY_HERE));
        assertNotNull(findElement(ID_BTN_TRY_AGAIN));
        assertNotNull(findElement(ID_BTN_RATE_APP));

        findElement(ID_BTN_NEXT_WORD).click();
    }

    private void performDragAndDrop(WebElement source, WebElement target) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence dragAndDrop = new Sequence(finger, 1);

        org.openqa.selenium.Point sourceLocation = source.getLocation();
        org.openqa.selenium.Dimension sourceSize = source.getSize();
        int sourceCenterX = sourceLocation.getX() + sourceSize.getWidth() / 2;
        int sourceCenterY = sourceLocation.getY() + sourceSize.getHeight() / 2;

        org.openqa.selenium.Point targetLocation = target.getLocation();
        org.openqa.selenium.Dimension targetSize = target.getSize();
        int targetCenterX = targetLocation.getX() + targetSize.getWidth() / 2;
        int targetCenterY = targetLocation.getY() + targetSize.getHeight() / 2;

        dragAndDrop.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), sourceCenterX, sourceCenterY));
        dragAndDrop.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        dragAndDrop.addAction(finger.createPointerMove(Duration.ofMillis(DRAG_DURATION), PointerInput.Origin.viewport(), targetCenterX, targetCenterY));
        dragAndDrop.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(dragAndDrop));
    }

    private WebElement getProperCell(String word, String[][] wordCases, List<WebElement> caseSingular, List<WebElement> casePlural) {
        int rowIndex = -1;
        int colIndex = -1;
        outerLoop:
        for (int i = 0; i < wordCases.length; i++) {
            for (int j = 0; j < wordCases[i].length; j++) {
                if (wordCases[i][j] != null && wordCases[i][j].equals(word)) {
                    rowIndex = i;
                    colIndex = j;
                    wordCases[i][j] = null;
                    break outerLoop;
                }
            }
        }

        if (rowIndex == -1) {
            throw new IllegalStateException("Could not find proper cell for word: " + word);
        }
        return (rowIndex == 0 ? caseSingular : casePlural).get(colIndex);
    }

    private WebElement getWrongCell(String word, String[][] wordCases, List<WebElement> caseSingular, List<WebElement> casePlural) {
        int rowIndex = -1;
        int colIndex = -1;
        outerLoop:
        for (int i = 0; i < wordCases.length; i++) {
            for (int j = 0; j < wordCases[i].length; j++) {
                if (wordCases[i][j] != null && wordCases[i][j].equals(word)) {
                    rowIndex = i;
                    colIndex = j;
                    wordCases[i][j] = null;
                    break outerLoop;
                }
            }
        }

        if (rowIndex == -1) {
            throw new IllegalStateException("Could not find cell for word: " + word);
        }
        return (rowIndex == 0 ? casePlural : caseSingular).get(colIndex);
    }

    private WebElement findElement(String id) {
        return waitForVisibleElement(id);
    }

    private WebElement waitForVisibleElement(String id) {
        return wait.until(ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(id))));
    }

    private boolean isElementVisible(String id, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(id)))
                    .isDisplayed();
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<WebElement> findElements(String id) {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(AppiumBy.id(id)));
    }

    private void waitForScreenStability() {
        sleep(SCREEN_STABILITY_DELAY);
    }

    private void waitForUiUpdate() {
        sleep(UI_UPDATE_DELAY);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for UI", e);
        }
    }

    private List<WebElement> findVisibleWordPoolItems() {
        String xpath = "//androidx.recyclerview.widget.RecyclerView[@resource-id='" + ID_WORDS_RECYCLER + "']"
                + "//android.widget.TextView[string-length(@text) > 0]";
        return driver.findElements(AppiumBy.xpath(xpath));
    }

    private List<String> getVisibleWordPoolWordTexts() {
        return findVisibleWordPoolItems().stream().map(WebElement::getText).toList();
    }

    private WebElement findWordPoolWordByText(String wordText) {
        String escaped = escapeXPathText(wordText);
        String xpath = "//androidx.recyclerview.widget.RecyclerView[@resource-id='" + ID_WORDS_RECYCLER + "']"
                + "//android.widget.TextView[@text=" + escaped + "]";
        return wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath(xpath)));
    }

    private String escapeXPathText(String text) {
        if (!text.contains("'")) {
            return "'" + text + "'";
        }
        if (!text.contains("\"")) {
            return "\"" + text + "\"";
        }
        String[] parts = text.split("'");
        StringBuilder sb = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(", \"'\", ");
            }
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }
}
