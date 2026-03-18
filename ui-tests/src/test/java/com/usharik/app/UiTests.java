package com.usharik.app;

import com.usharik.app.helpers.TestHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import static com.usharik.app.Parameters.APPIUM_URL;
import static com.usharik.app.Parameters.APP_PATH;
import static com.usharik.app.Parameters.DATA_JSON_PATH;
import static com.usharik.app.Parameters.DEFAULT_TIMEOUT;
import static com.usharik.app.Parameters.DRAG_DURATION;
import static com.usharik.app.Parameters.IMPLICIT_WAIT;
import static com.usharik.app.Parameters.SCREEN_STABILITY_DELAY;
import static com.usharik.app.Parameters.UI_UPDATE_DELAY;
import static com.usharik.app.UiConstants.ID_ACTION_CHECK;
import static com.usharik.app.UiConstants.ID_ACTION_NEXT;
import static com.usharik.app.UiConstants.ID_APP_LOGO;
import static com.usharik.app.UiConstants.ID_APP_NAME;
import static com.usharik.app.UiConstants.ID_APP_VERSION;
import static com.usharik.app.UiConstants.ID_BTN_NEXT_WORD;
import static com.usharik.app.UiConstants.ID_BTN_RATE_APP;
import static com.usharik.app.UiConstants.ID_BTN_STAY_HERE;
import static com.usharik.app.UiConstants.ID_BTN_TRY_AGAIN;
import static com.usharik.app.UiConstants.ID_CASES_CONTAINER;
import static com.usharik.app.UiConstants.ID_CASE_PLURAL;
import static com.usharik.app.UiConstants.ID_CASE_SINGULAR;
import static com.usharik.app.UiConstants.ID_CHECKBOX_HEADER;
import static com.usharik.app.UiConstants.ID_CURRENT_WORD;
import static com.usharik.app.UiConstants.ID_DIALOG_TITLE;
import static com.usharik.app.UiConstants.ID_GENDER_GROUP;
import static com.usharik.app.UiConstants.ID_GENDER_HEADER;
import static com.usharik.app.UiConstants.ID_NAV_MENU_ITEM;
import static com.usharik.app.UiConstants.ID_RADIO_GROUP;
import static com.usharik.app.UiConstants.ID_RADIO_GROUP_HEADER;
import static com.usharik.app.UiConstants.ID_WORDS_RECYCLER;
import static com.usharik.app.UiConstants.XPATH_NAV_DRAWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI Tests for Czech Declension Quiz App
 *
 * Tests the main quiz functionality including:
 * - Correct quiz solution with drag and drop
 * - Navigation between different screens
 * - Screen rotation handling
 */
public class UiTests {

    private static final Logger logger = LoggerFactory.getLogger(UiTests.class);

    private static final String TIMESTAMP = Long.toString(System.currentTimeMillis());

    private static AndroidDriver driver;
    private static TestHelper helper;
    private WebDriverWait wait;

    @BeforeAll
    public static void setupClass() {
        logger.info("Setting up test environment");

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

        logger.info("Using external Appium service at: {}", APPIUM_URL);
        final URL appiumServerUrl;
        try {
            appiumServerUrl = URI.create(APPIUM_URL).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Appium URL: " + APPIUM_URL, e);
        }

        logger.info("Initializing Android driver with app: {}", appFile.getAbsolutePath());
        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName("Android Device")
                .setApp(appFile.getAbsolutePath())
                .setNoReset(true)
                .setAutoGrantPermissions(true);

        driver = new AndroidDriver(appiumServerUrl, options);
        driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);

        logger.info("Initializing test helper with data from: {}", DATA_JSON_PATH);
        helper = new TestHelper(driver, DATA_JSON_PATH);
    }

    @BeforeEach
    public void setupTest(TestInfo testInfo) {
        logger.info("Starting test: {}", testInfo.getDisplayName());
        wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
    }

    @AfterEach
    public void tearDownTest(TestInfo testInfo) {
        logger.info("Finished test: {}", testInfo.getDisplayName());
    }

    @AfterAll
    public static void tearDownClass() {
        logger.info("Tearing down test environment");

        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                logger.error("Error quitting driver", e);
            }
        }

        logger.info("Test environment cleanup complete");
    }

    /**
     * Test that verifies the complete quiz workflow:
     * 1. Navigate to quiz screen
     * 2. Get a fresh word
     * 3. Drag and drop all word forms to correct positions
     * 4. Verify the solution is correct
     */
    @Test
    public void testCorrectQuizSolution() {
        navigateToQuizScreen();

        clickNextWord();

        List<WebElement> caseSingular = findElements(ID_CASE_SINGULAR);
        List<WebElement> casePlural = findElements(ID_CASE_PLURAL);
        WebElement currentWord = findElement(ID_CURRENT_WORD);
        WebElement checkButton = findElement(ID_ACTION_CHECK);

        String currentWordText = currentWord.getText();
        assertNotNull(currentWordText, "Current word should not be null");
        logger.info("Testing word: {}", currentWordText);

        String[][] wordCases = helper.getWordCases(currentWordText);
        assertNotNull(wordCases, "Word cases should not be null");

        helper.makeScreenshot(TIMESTAMP, "before_solution.png");

        placeAllWordForms(wordCases, caseSingular, casePlural);

        helper.makeScreenshot(TIMESTAMP, "after_solution.png");

        checkButton.click();

        verifySuccessDialog();
    }
    
    @Test
    public void testIncorrectQuizSolution() {
        navigateToQuizScreen();

        clickNextWord();

        List<WebElement> caseSingular = findElements(ID_CASE_SINGULAR);
        List<WebElement> casePlural = findElements(ID_CASE_PLURAL);
        WebElement currentWord = findElement(ID_CURRENT_WORD);
        WebElement checkButton = findElement(ID_ACTION_CHECK);

        String currentWordText = currentWord.getText();
        assertNotNull(currentWordText, "Current word should not be null");
        logger.info("Testing word with intentional errors: {}", currentWordText);

        String[][] wordCases = helper.getWordCases(currentWordText);
        assertNotNull(wordCases, "Word cases should not be null");

        helper.makeScreenshot(TIMESTAMP, "before_incorrect_solution.png");

        // Create an intentionally incorrect solution:
        // - First N words: placed correctly
        // - Next M words: placed in WRONG positions (swap singular/plural)
        // - Remaining words: left unplaced
        logger.info("Placing words with intentional errors");

        List<String> initialPoolWords = getVisibleWordPoolWordTexts();
        int wordsToPlace = Math.min(10, initialPoolWords.size());
        logger.info("Visible words in pool: {}, placing first {}", initialPoolWords.size(), wordsToPlace);

        for (int i = 0; i < wordsToPlace; i++) {
            String wordText = initialPoolWords.get(i);
            WebElement wordElement = findWordPoolWordByText(wordText);

            logger.debug("Processing word {}: {}", i + 1, wordText);

            WebElement targetCell;
            if (i < 6) {
                targetCell = getProperCell(wordText, wordCases, caseSingular, casePlural);
                logger.debug("Placing word {} correctly", i + 1);
            } else {
                targetCell = getWrongCell(wordText, wordCases, caseSingular, casePlural);
                logger.debug("Placing word {} INCORRECTLY (swapped singular/plural)", i + 1);
            }

            performDragAndDrop(wordElement, targetCell);
            waitForUiUpdate();
        }

        helper.makeScreenshot(TIMESTAMP, "after_incorrect_solution.png");

        checkButton.click();
        waitForUiUpdate();

        helper.makeScreenshot(TIMESTAMP, "after_error_check.png");
        
        WebElement toast = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                AppiumBy.xpath("//android.widget.Toast[1]")
            )
        );
        String toastText = toast.getAttribute("text");
        logger.info("Toast message: {}", toastText);
        assertEquals("There're some errors.", toastText, "Error toast should show correct message");

        List<WebElement> alertDialogs = driver.findElements(AppiumBy.id(ID_DIALOG_TITLE));
        assertTrue(alertDialogs.isEmpty(), "Success dialog should not appear for incorrect solution");

        logger.info("Incorrect quiz solution test completed successfully");
    }
    
    @Test
    public void testNavigateToQuizScreen() {
        navigateToScreen(0);

        // Verify quiz screen elements are present
        WebElement currentWord = waitForVisibleElement(ID_CURRENT_WORD);
        assertNotNull(currentWord, "Current word should be displayed");

        WebElement checkButton = waitForVisibleElement(ID_ACTION_CHECK);
        assertNotNull(checkButton, "Check button should be present");

        WebElement nextButton = waitForVisibleElement(ID_ACTION_NEXT);
        assertNotNull(nextButton, "Next button should be present");

        // Test rotation
        testScreenRotationForCurrentScreen(ID_CURRENT_WORD, "Current word");

        logger.info("Quiz screen verified successfully");
    }
    
    @Test
    public void testNavigateToWordsWithErrorsScreen() {
        navigateToScreen(1);

        // Verify words with errors screen elements are present
        // The screen shows cases container which displays word declensions
        WebElement casesContainer = waitForVisibleElement(ID_CASES_CONTAINER);
        assertNotNull(casesContainer, "Cases container should be displayed");

        // Test rotation
        testScreenRotationForCurrentScreen(ID_CASES_CONTAINER, "Cases container");

        logger.info("Words with Errors screen verified successfully");
    }
    
    @Test
    public void testNavigateToHandbookScreen() {
        navigateToScreen(2);

        // Verify handbook screen elements are present
        WebElement genderHeader = waitForVisibleElement(ID_GENDER_HEADER);
        assertNotNull(genderHeader, "Gender header should be displayed");
        assertEquals("Gender of noun", genderHeader.getText(), "Gender header should have correct text");

        WebElement genderGroup = waitForVisibleElement(ID_GENDER_GROUP);
        assertNotNull(genderGroup, "Gender radio group should be present");

        // Test rotation
        testScreenRotationForCurrentScreen(ID_GENDER_HEADER, "Gender header");

        logger.info("Handbook screen verified successfully");
    }
    
    @Test
    public void testNavigateToSettingsScreen() {
        navigateToScreen(3);

        // Verify settings screen elements are present
        WebElement radioGroupHeader = waitForVisibleElement(ID_RADIO_GROUP_HEADER);
        assertNotNull(radioGroupHeader, "Radio group header should be displayed");
        assertEquals("Word filter by gender", radioGroupHeader.getText(), "Radio group header should have correct text");

        WebElement radioGroup = waitForVisibleElement(ID_RADIO_GROUP);
        assertNotNull(radioGroup, "Radio group should be present");

        WebElement checkboxHeader = waitForVisibleElement(ID_CHECKBOX_HEADER);
        assertNotNull(checkboxHeader, "Checkbox header should be displayed");
        assertEquals("Additional settings", checkboxHeader.getText(), "Checkbox header should have correct text");

        // Test rotation
        testScreenRotationForCurrentScreen(ID_RADIO_GROUP_HEADER, "Radio group header");

        logger.info("Settings screen verified successfully");
    }
    
    @Test
    public void testNavigateToAboutScreen() {
        navigateToScreen(4);

        // Verify about screen elements are present
        WebElement appName = waitForVisibleElement(ID_APP_NAME);
        assertNotNull(appName, "App name should be displayed");
        assertEquals("Czech declension quiz", appName.getText(), "App name should be correct");

        WebElement appLogo = waitForVisibleElement(ID_APP_LOGO);
        assertNotNull(appLogo, "App logo should be present");

        WebElement appVersion = waitForVisibleElement(ID_APP_VERSION);
        assertNotNull(appVersion, "App version should be displayed");
        
        testScreenRotationForCurrentScreen(ID_APP_NAME, "App name");

        logger.info("About screen verified successfully");
    }

    /**
     * Helper method to test screen rotation for the current screen
     * @param elementId The ID of an element to verify after rotation
     * @param elementName Human-readable name of the element for logging
     */
    private void testScreenRotationForCurrentScreen(String elementId, String elementName) {
        // Test landscape orientation
        logger.debug("Rotating to landscape");
        driver.rotate(ScreenOrientation.LANDSCAPE);
        waitForScreenStability();

        WebElement elementLandscape = waitForVisibleElement(elementId);
        assertNotNull(elementLandscape, elementName + " should be visible in landscape");

        // Test portrait orientation
        logger.debug("Rotating to portrait");
        driver.rotate(ScreenOrientation.PORTRAIT);
        waitForScreenStability();

        WebElement elementPortrait = waitForVisibleElement(elementId);
        assertNotNull(elementPortrait, elementName + " should be visible in portrait");

        logger.debug("Screen rotation test completed for {}", elementName);
    }

    // ========== Helper Methods ==========

    /**
     * Navigate to the quiz screen (first screen)
     */
    private void navigateToQuizScreen() {
        logger.debug("Navigating to quiz screen");
        navigateToScreen(0);
    }

    /**
     * Navigate to a specific screen by index
     * @param screenIndex The index of the screen (0-4)
     */
    private void navigateToScreen(int screenIndex) {
        logger.debug("Opening navigation drawer");
        WebElement drawerButton;
        try {
            drawerButton = wait.until(
                ExpectedConditions.elementToBeClickable(AppiumBy.xpath(XPATH_NAV_DRAWER))
            );
        } catch (Exception e) {
            logger.error("Navigation drawer button not found. Page source:\n{}", driver.getPageSource());
            throw e;
        }
        drawerButton.click();

        logger.debug("Selecting screen at index: {}", screenIndex);
        List<WebElement> navButtons = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(AppiumBy.id(ID_NAV_MENU_ITEM))
        );
        assertEquals(5, navButtons.size(), "Expected 5 navigation items");
        navButtons.get(screenIndex).click();

        waitForScreenStability();
    }

    /**
     * Click the "Next Word" button to get a fresh word
     */
    private void clickNextWord() {
        logger.debug("Clicking next word button");
        WebElement nextButton = wait.until(
            ExpectedConditions.elementToBeClickable(AppiumBy.id(ID_ACTION_NEXT))
        );
        nextButton.click();
        waitForScreenStability();
    }

    /**
     * Place all 14 word forms in their correct positions
     */
    private void placeAllWordForms(String[][] wordCases,
                                    List<WebElement> caseSingular,
                                    List<WebElement> casePlural) {
        logger.info("Placing all visible word forms from RecyclerView pool");

        int placedCount = 0;
        int safetyGuard = 0;
        while (true) {
            List<WebElement> poolItems = findVisibleWordPoolItems();
            if (poolItems.isEmpty()) {
                break;
            }
            WebElement wordElement = poolItems.getFirst();
            String wordText = wordElement.getText();
            logger.debug("Processing visible pool word {}: {}", placedCount + 1, wordText);

            WebElement targetCell = getProperCell(wordText, wordCases, caseSingular, casePlural);
            performDragAndDrop(wordElement, targetCell);
            waitForUiUpdate();

            placedCount++;
            safetyGuard++;
            if (safetyGuard > 30) {
                throw new IllegalStateException("Safety guard hit while placing words. Possible drag/drop regression.");
            }
        }

        logger.info("All visible word forms placed successfully. Count={}", placedCount);
    }

    /**
     * Verify that the success dialog appears with correct content
     */
    private void verifySuccessDialog() {
        logger.debug("Verifying success dialog");

        WebElement dialogTitle = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_DIALOG_TITLE))
        );

        helper.makeScreenshot(TIMESTAMP, "success_dialog_open.png");

        assertEquals("All is correct!", dialogTitle.getText(), "Success message should be correct");

        assertNotNull(findElement(ID_BTN_NEXT_WORD), "Next word button should be present");
        assertNotNull(findElement(ID_BTN_STAY_HERE), "Stay here button should be present");
        assertNotNull(findElement(ID_BTN_TRY_AGAIN), "Try again button should be present");
        assertNotNull(findElement(ID_BTN_RATE_APP), "Rate app button should be present");

        logger.debug("Dismissing success dialog via Next word");
        findElement(ID_BTN_NEXT_WORD).click();
    }

    /**
     * Perform drag and drop operation from source to target element
     */
    private void performDragAndDrop(WebElement source, WebElement target) {
        logger.debug("Performing drag and drop");

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence dragAndDrop = new Sequence(finger, 1);

        // Calculate source center point
        org.openqa.selenium.Point sourceLocation = source.getLocation();
        org.openqa.selenium.Dimension sourceSize = source.getSize();
        int sourceCenterX = sourceLocation.getX() + sourceSize.getWidth() / 2;
        int sourceCenterY = sourceLocation.getY() + sourceSize.getHeight() / 2;

        // Calculate target center point
        org.openqa.selenium.Point targetLocation = target.getLocation();
        org.openqa.selenium.Dimension targetSize = target.getSize();
        int targetCenterX = targetLocation.getX() + targetSize.getWidth() / 2;
        int targetCenterY = targetLocation.getY() + targetSize.getHeight() / 2;

        // Build drag and drop sequence
        dragAndDrop.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), sourceCenterX, sourceCenterY));
        dragAndDrop.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        dragAndDrop.addAction(finger.createPointerMove(Duration.ofMillis(DRAG_DURATION), PointerInput.Origin.viewport(), targetCenterX, targetCenterY));
        dragAndDrop.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(dragAndDrop));
    }

    /**
     * Find the proper cell (singular or plural, case 1-7) for a given word
     * @param word The word to find the cell for
     * @param wordCases 2D array of word cases [singular/plural][case1-7]
     * @param caseSingular List of singular case elements
     * @param casePlural List of plural case elements
     * @return The target cell element
     */
    private WebElement getProperCell(String word, String[][] wordCases,
                                     List<WebElement> caseSingular, List<WebElement> casePlural) {
        logger.debug("Finding proper cell for word: {}", word);

        int rowIndex = -1;
        int colIndex = -1;

        // Find the word in the cases array
        outerLoop:
        for (int i = 0; i < wordCases.length; i++) {
            for (int j = 0; j < wordCases[i].length; j++) {
                if (wordCases[i][j] != null && wordCases[i][j].equals(word)) {
                    rowIndex = i;
                    colIndex = j;
                    wordCases[i][j] = null; // Mark as used
                    break outerLoop;
                }
            }
        }

        if (rowIndex == -1) {
            throw new IllegalStateException("Could not find proper cell for word: " + word);
        }

        // Return the appropriate cell (singular or plural)
        List<WebElement> targetList = (rowIndex == 0) ? caseSingular : casePlural;
        logger.debug("Word '{}' belongs to {} case {}", word, (rowIndex == 0 ? "singular" : "plural"), colIndex + 1);

        return targetList.get(colIndex);
    }

    /**
     * Find the WRONG cell for a given word (for testing error handling)
     * This intentionally swaps singular and plural to create errors
     * @param word The word to find the wrong cell for
     * @param wordCases 2D array of word cases [singular/plural][case1-7]
     * @param caseSingular List of singular case elements
     * @param casePlural List of plural case elements
     * @return The wrong target cell element
     */
    private WebElement getWrongCell(String word, String[][] wordCases,
                                    List<WebElement> caseSingular, List<WebElement> casePlural) {
        logger.debug("Finding WRONG cell for word: {}", word);

        int rowIndex = -1;
        int colIndex = -1;

        // Find where this word belongs
        outerLoop:
        for (int i = 0; i < wordCases.length; i++) {
            for (int j = 0; j < wordCases[i].length; j++) {
                if (wordCases[i][j] != null && wordCases[i][j].equals(word)) {
                    rowIndex = i;
                    colIndex = j;
                    wordCases[i][j] = null; // Mark as used
                    break outerLoop;
                }
            }
        }

        if (rowIndex == -1) {
            throw new IllegalStateException("Could not find cell for word: " + word);
        }

        // Return the OPPOSITE cell (swap singular and plural)
        List<WebElement> targetList = (rowIndex == 0) ? casePlural : caseSingular;
        logger.debug("Word '{}' belongs to {} but placing in {} case {} (WRONG)",
                     word,
                     (rowIndex == 0 ? "singular" : "plural"),
                     (rowIndex == 0 ? "plural" : "singular"),
                     colIndex + 1);

        return targetList.get(colIndex);
    }

    // ========== Utility Methods ==========

    /**
     * Find a single element by ID with explicit wait
     */
    private WebElement findElement(String id) {
        return waitForVisibleElement(id);
    }

    /**
     * Find a single visible element by ID and refresh stale references during UI transitions.
     */
    private WebElement waitForVisibleElement(String id) {
        return wait.until(
            ExpectedConditions.refreshed(
                ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(id))
            )
        );
    }

    /**
     * Find multiple elements by ID with explicit wait
     */
    private List<WebElement> findElements(String id) {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(AppiumBy.id(id)));
    }

    /**
     * Wait for screen to stabilize after navigation or rotation
     */
    private void waitForScreenStability() {
        try {
            Thread.sleep(SCREEN_STABILITY_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for screen stability", e);
        }
    }

    /**
     * Wait for UI to update after drag and drop
     */
    private void waitForUiUpdate() {
        try {
            Thread.sleep(UI_UPDATE_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for UI update", e);
        }
    }

    private List<WebElement> findVisibleWordPoolItems() {
        String xpath = "//androidx.recyclerview.widget.RecyclerView[@resource-id='" + ID_WORDS_RECYCLER + "']"
                + "//android.widget.TextView[string-length(@text) > 0]";
        return driver.findElements(AppiumBy.xpath(xpath));
    }

    private List<String> getVisibleWordPoolWordTexts() {
        return findVisibleWordPoolItems().stream()
                .map(WebElement::getText)
                .toList();
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
        // Fallback for strings containing both quotes: concat('a',"'",'b')
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
