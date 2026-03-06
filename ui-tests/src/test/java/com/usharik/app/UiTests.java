package com.usharik.app;

import com.usharik.app.helpers.TestHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
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
import java.time.Duration;
import java.util.List;

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

    // Configuration - Paths
    private static final String APP_PATH = System.getProperty("app.path", "app/release/app-release.apk");
    private static final String DATA_JSON_PATH = System.getProperty("data.json.path", "database/src/main/assets/data.json");

    // Configuration - Timeouts (in seconds)
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(
        Long.parseLong(System.getProperty("test.timeout.default", "10")));
    private static final Duration IMPLICIT_WAIT = Duration.ofSeconds(
        Long.parseLong(System.getProperty("test.timeout.implicit", "2")));

    // Configuration - Delays (in milliseconds)
    private static final long SCREEN_STABILITY_DELAY = Long.parseLong(
        System.getProperty("test.delay.screen.stability", "100"));
    private static final long UI_UPDATE_DELAY = Long.parseLong(
        System.getProperty("test.delay.ui.update", "100"));
    private static final long DRAG_DURATION = Long.parseLong(
        System.getProperty("test.delay.drag.duration", "100"));

    // UI element IDs - Quiz Screen
    private static final String ID_ACTION_NEXT = "com.usharik.app:id/action_next";
    private static final String ID_ACTION_CHECK = "com.usharik.app:id/action_check";
    private static final String ID_CURRENT_WORD = "com.usharik.app:id/word";
    private static final String ID_CASE_SINGULAR = "com.usharik.app:id/caseSingular";
    private static final String ID_CASE_PLURAL = "com.usharik.app:id/casePlural";
    private static final String ID_ALERT_TITLE = "com.usharik.app:id/alertTitle";
    private static final String ID_ALERT_BUTTON = "android:id/text1";

    // UI element IDs - Navigation
    private static final String ID_NAV_MENU_ITEM = "com.usharik.app:id/design_menu_item_text";
    private static final String XPATH_NAV_DRAWER = "//android.widget.ImageButton[@content-desc=\"Navigate up\"]";

    // UI element IDs - Words with Errors Screen
    private static final String ID_WORDS_WITH_ERRORS_FLOW = "com.usharik.app:id/wordsWithErrorsFlow";
    private static final String ID_CASES_CONTAINER = "com.usharik.app:id/casesContainer";

    // UI element IDs - Handbook Screen
    private static final String ID_GENDER_HEADER = "com.usharik.app:id/genderHeader";
    private static final String ID_GENDER_GROUP = "com.usharik.app:id/genderGroup";

    // UI element IDs - Settings Screen
    private static final String ID_RADIO_GROUP_HEADER = "com.usharik.app:id/radioGroupHeader";
    private static final String ID_RADIO_GROUP = "com.usharik.app:id/radioGroup";
    private static final String ID_CHECKBOX_HEADER = "com.usharik.app:id/checkBoxHeader";

    // UI element IDs - About Screen
    private static final String ID_APP_NAME = "com.usharik.app:id/appName";
    private static final String ID_APP_LOGO = "com.usharik.app:id/appLogo";
    private static final String ID_APP_VERSION = "com.usharik.app:id/appVersion";

    private static AndroidDriver driver;
    private static AppiumDriverLocalService service;
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

        logger.info("Starting Appium service");
        service = AppiumDriverLocalService.buildDefaultService();
        service.start();

        logger.info("Initializing Android driver with app: {}", appFile.getAbsolutePath());
        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName("Android Device")
                .setApp(appFile.getAbsolutePath())
                .setNoReset(true)
                .setAutoGrantPermissions(true);

        driver = new AndroidDriver(service.getUrl(), options);
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

        if (service != null && service.isRunning()) {
            try {
                service.stop();
            } catch (Exception e) {
                logger.error("Error stopping Appium service", e);
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
        logger.info("Testing correct quiz solution workflow");

        // Navigate to quiz screen
        navigateToQuizScreen();

        // Get a fresh word to ensure clean state
        clickNextWord();

        // Find UI elements
        List<WebElement> caseSingular = findElements(ID_CASE_SINGULAR);
        List<WebElement> casePlural = findElements(ID_CASE_PLURAL);
        WebElement currentWord = findElement(ID_CURRENT_WORD);
        WebElement checkButton = findElement(ID_ACTION_CHECK);

        String currentWordText = currentWord.getText();
        assertNotNull(currentWordText, "Current word should not be null");
        logger.info("Testing word: {}", currentWordText);

        String[][] wordCases = helper.getWordCases(currentWordText);
        assertNotNull(wordCases, "Word cases should not be null");

        String timestamp = Long.toString(System.currentTimeMillis());
        helper.makeScreenshot(timestamp, "before_solution.png");

        // Place all 14 word forms in correct positions
        placeAllWordForms(wordCases, caseSingular, casePlural);

        helper.makeScreenshot(timestamp, "after_solution.png");

        // Check the solution
        checkButton.click();

        // Verify success dialog
        verifySuccessDialog();
    }
    
    @Test
    public void testIncorrectQuizSolution() {
        logger.info("Testing incorrect quiz solution workflow");

        // Navigate to quiz screen
        navigateToQuizScreen();

        // Get a fresh word to ensure clean state
        clickNextWord();

        // Find UI elements
        List<WebElement> caseSingular = findElements(ID_CASE_SINGULAR);
        List<WebElement> casePlural = findElements(ID_CASE_PLURAL);
        WebElement currentWord = findElement(ID_CURRENT_WORD);
        WebElement checkButton = findElement(ID_ACTION_CHECK);

        String currentWordText = currentWord.getText();
        assertNotNull(currentWordText, "Current word should not be null");
        logger.info("Testing word with intentional errors: {}", currentWordText);

        String[][] wordCases = helper.getWordCases(currentWordText);
        assertNotNull(wordCases, "Word cases should not be null");

        String timestamp = Long.toString(System.currentTimeMillis());
        helper.makeScreenshot(timestamp, "before_incorrect_solution.png");

        // Create an intentionally incorrect solution:
        // - First 6 words: placed correctly
        // - Next 4 words: placed in WRONG positions (swap singular/plural)
        // - Last 4 words: not placed at all (left empty)
        logger.info("Placing words with intentional errors: 6 correct, 4 wrong, 4 missing");

        for (int i = 0; i < 10; i++) {
            String wordId = "com.usharik.app:id/word" + (i + 1);
            WebElement wordElement = findElement(wordId);

            String wordText = wordElement.getText();
            logger.debug("Processing word {}: {}", i + 1, wordText);

            WebElement targetCell;
            if (i < 6) {
                // First 6 words: place correctly
                targetCell = getProperCell(wordText, wordCases, caseSingular, casePlural);
                logger.debug("Placing word {} correctly", i + 1);
            } else {
                // Next 4 words (indices 6-9): place in wrong position
                // Swap singular and plural to create errors
                targetCell = getWrongCell(wordText, wordCases, caseSingular, casePlural);
                logger.debug("Placing word {} INCORRECTLY (swapped singular/plural)", i + 1);
            }

            performDragAndDrop(wordElement, targetCell);
            waitForUiUpdate();
        }
        // Words 11-14 are intentionally not placed (left in word pool)

        helper.makeScreenshot(timestamp, "after_incorrect_solution.png");

        // Check the solution
        checkButton.click();
        waitForUiUpdate();

        helper.makeScreenshot(timestamp, "after_error_check.png");
        
        WebElement toast = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                AppiumBy.xpath("//android.widget.Toast[1]")
            )
        );
        String toastText = toast.getAttribute("text");
        logger.info("Toast message: {}", toastText);
        assertEquals("There're some errors.", toastText, "Error toast should show correct message");

        // Verify that success dialog did NOT appear
        List<WebElement> alertDialogs = driver.findElements(AppiumBy.id(ID_ALERT_TITLE));
        assertTrue(alertDialogs.isEmpty(), "Success dialog should not appear for incorrect solution");

        logger.info("Incorrect quiz solution test completed successfully");
    }
    
    @Test
    public void testNavigateToQuizScreen() {
        logger.info("Testing navigation to Quiz screen");

        navigateToScreen(0);

        // Verify quiz screen elements are present
        WebElement currentWord = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_CURRENT_WORD))
        );
        assertNotNull(currentWord, "Current word should be displayed");
        assertTrue(currentWord.isDisplayed(), "Current word should be visible");

        WebElement checkButton = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_ACTION_CHECK))
        );
        assertNotNull(checkButton, "Check button should be present");

        WebElement nextButton = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_ACTION_NEXT))
        );
        assertNotNull(nextButton, "Next button should be present");

        // Test rotation
        testScreenRotationForCurrentScreen(ID_CURRENT_WORD, "Current word");

        logger.info("Quiz screen verified successfully");
    }
    
    @Test
    public void testNavigateToWordsWithErrorsScreen() {
        logger.info("Testing navigation to Words with Errors screen");

        navigateToScreen(1);

        // Verify words with errors screen elements are present
        // The screen shows cases container which displays word declensions
        WebElement casesContainer = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_CASES_CONTAINER))
        );
        assertNotNull(casesContainer, "Cases container should be displayed");
        assertTrue(casesContainer.isDisplayed(), "Cases container should be visible");

        // Test rotation
        testScreenRotationForCurrentScreen(ID_CASES_CONTAINER, "Cases container");

        logger.info("Words with Errors screen verified successfully");
    }
    
    @Test
    public void testNavigateToHandbookScreen() {
        logger.info("Testing navigation to Handbook screen");

        navigateToScreen(2);

        // Verify handbook screen elements are present
        WebElement genderHeader = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_GENDER_HEADER))
        );
        assertNotNull(genderHeader, "Gender header should be displayed");
        assertTrue(genderHeader.isDisplayed(), "Gender header should be visible");
        assertEquals("Gender of noun", genderHeader.getText(), "Gender header should have correct text");

        WebElement genderGroup = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_GENDER_GROUP))
        );
        assertNotNull(genderGroup, "Gender radio group should be present");
        assertTrue(genderGroup.isDisplayed(), "Gender radio group should be visible");

        // Test rotation
        testScreenRotationForCurrentScreen(ID_GENDER_HEADER, "Gender header");

        logger.info("Handbook screen verified successfully");
    }
    
    @Test
    public void testNavigateToSettingsScreen() {
        logger.info("Testing navigation to Settings screen");

        navigateToScreen(3);

        // Verify settings screen elements are present
        WebElement radioGroupHeader = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_RADIO_GROUP_HEADER))
        );
        assertNotNull(radioGroupHeader, "Radio group header should be displayed");
        assertTrue(radioGroupHeader.isDisplayed(), "Radio group header should be visible");
        assertEquals("Word filter by gender", radioGroupHeader.getText(), "Radio group header should have correct text");

        WebElement radioGroup = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_RADIO_GROUP))
        );
        assertNotNull(radioGroup, "Radio group should be present");
        assertTrue(radioGroup.isDisplayed(), "Radio group should be visible");

        WebElement checkboxHeader = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_CHECKBOX_HEADER))
        );
        assertNotNull(checkboxHeader, "Checkbox header should be displayed");
        assertTrue(checkboxHeader.isDisplayed(), "Checkbox header should be visible");
        assertEquals("Additional settings", checkboxHeader.getText(), "Checkbox header should have correct text");

        // Test rotation
        testScreenRotationForCurrentScreen(ID_RADIO_GROUP_HEADER, "Radio group header");

        logger.info("Settings screen verified successfully");
    }
    
    @Test
    public void testNavigateToAboutScreen() {
        logger.info("Testing navigation to About screen");

        navigateToScreen(4);

        // Verify about screen elements are present
        WebElement appName = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_APP_NAME))
        );
        assertNotNull(appName, "App name should be displayed");
        assertTrue(appName.isDisplayed(), "App name should be visible");
        assertEquals("Czech declension quiz", appName.getText(), "App name should be correct");

        WebElement appLogo = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_APP_LOGO))
        );
        assertNotNull(appLogo, "App logo should be present");
        assertTrue(appLogo.isDisplayed(), "App logo should be visible");

        WebElement appVersion = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_APP_VERSION))
        );
        assertNotNull(appVersion, "App version should be displayed");
        assertTrue(appVersion.isDisplayed(), "App version should be visible");
        
        testScreenRotationForCurrentScreen(ID_APP_NAME, "App name");

        logger.info("About screen verified successfully");
    }

    /**
     * Helper method to test screen rotation for the current screen
     * @param elementId The ID of an element to verify after rotation
     * @param elementName Human-readable name of the element for logging
     */
    private void testScreenRotationForCurrentScreen(String elementId, String elementName) {
        logger.debug("Testing screen rotation for {}", elementName);

        // Test landscape orientation
        logger.debug("Rotating to landscape");
        driver.rotate(ScreenOrientation.LANDSCAPE);
        waitForScreenStability();

        WebElement elementLandscape = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(elementId))
        );
        assertTrue(elementLandscape.isDisplayed(), elementName + " should be visible in landscape");

        // Test portrait orientation
        logger.debug("Rotating to portrait");
        driver.rotate(ScreenOrientation.PORTRAIT);
        waitForScreenStability();

        WebElement elementPortrait = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(elementId))
        );
        assertTrue(elementPortrait.isDisplayed(), elementName + " should be visible in portrait");

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
        WebElement drawerButton = wait.until(
            ExpectedConditions.elementToBeClickable(AppiumBy.xpath(XPATH_NAV_DRAWER))
        );
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
        logger.info("Placing all 14 word forms");

        for (int i = 0; i < 14; i++) {
            String wordId = "com.usharik.app:id/word" + (i + 1);
            WebElement wordElement = findElement(wordId);

            assertTrue(wordElement.isDisplayed(), "Word " + (i + 1) + " should be displayed");

            String wordText = wordElement.getText();
            logger.debug("Processing word {}: {}", i + 1, wordText);

            WebElement targetCell = getProperCell(wordText, wordCases, caseSingular, casePlural);
            performDragAndDrop(wordElement, targetCell);

            // Small delay to let the UI update
            waitForUiUpdate();
        }

        logger.info("All word forms placed successfully");
    }



    /**
     * Verify that the success dialog appears with correct content
     */
    private void verifySuccessDialog() {
        logger.debug("Verifying success dialog");

        WebElement alertTitle = wait.until(
            ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_ALERT_TITLE))
        );
        assertEquals("All is correct!", alertTitle.getText(), "Success message should be correct");

        List<WebElement> alertButtons = findElements(ID_ALERT_BUTTON);
        assertEquals(4, alertButtons.size(), "Should have 4 dialog options");

        logger.debug("Dismissing success dialog");
        alertButtons.getFirst().click();
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
        return wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.id(id)));
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
}
