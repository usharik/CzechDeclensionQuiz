package com.usharik.app;

import com.usharik.app.helpers.TestHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    // Configuration
    private static final String APP_PATH = System.getProperty("app.path", "app/release/app-release.apk");
    private static final String DATA_JSON_PATH = System.getProperty("data.json.path", "database/src/main/assets/data.json");
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration IMPLICIT_WAIT = Duration.ofSeconds(2);

    // UI element IDs
    private static final String ID_ACTION_NEXT = "com.usharik.app:id/action_next";
    private static final String ID_ACTION_CHECK = "com.usharik.app:id/action_check";
    private static final String ID_CURRENT_WORD = "com.usharik.app:id/word";
    private static final String ID_CASE_SINGULAR = "com.usharik.app:id/caseSingular";
    private static final String ID_CASE_PLURAL = "com.usharik.app:id/casePlural";
    private static final String ID_ALERT_TITLE = "com.usharik.app:id/alertTitle";
    private static final String ID_ALERT_BUTTON = "android:id/text1";
    private static final String ID_NAV_MENU_ITEM = "com.usharik.app:id/design_menu_item_text";
    private static final String XPATH_NAV_DRAWER = "//android.widget.ImageButton[@content-desc=\"Navigate up\"]";

    private static AndroidDriver driver;
    private static AppiumDriverLocalService service;
    private static TestHelper helper;
    private WebDriverWait wait;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
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

    @Before
    public void setupTest() {
        logger.info("Starting test: {}", testName.getMethodName());
        wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
    }

    @After
    public void tearDownTest() {
        logger.info("Finished test: {}", testName.getMethodName());
    }

    @AfterClass
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
        assertNotNull("Current word should not be null", currentWordText);
        logger.info("Testing word: {}", currentWordText);

        String[][] wordCases = helper.getWordCases(currentWordText);
        assertNotNull("Word cases should not be null", wordCases);

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

    /**
     * Test navigation between different app screens and screen rotation
     */
    @Test
    public void testNavigation() {
        logger.info("Testing navigation and screen rotation");

        final int screenCount = 5;
        for (int i = 0; i < screenCount; i++) {
            logger.info("Navigating to screen {}/{}", i + 1, screenCount);
            navigateToScreen(i);

            // Test screen rotation
            logger.debug("Testing landscape orientation");
            driver.rotate(ScreenOrientation.LANDSCAPE);
            waitForScreenStability();

            logger.debug("Testing portrait orientation");
            driver.rotate(ScreenOrientation.PORTRAIT);
            waitForScreenStability();
        }

        logger.info("Navigation test completed successfully");
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
        assertEquals("Expected 5 navigation items", 5, navButtons.size());
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

            assertTrue("Word " + (i + 1) + " should be displayed", wordElement.isDisplayed());

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
        assertEquals("Success message should be correct", "All is correct!", alertTitle.getText());

        List<WebElement> alertButtons = findElements(ID_ALERT_BUTTON);
        assertEquals("Should have 4 dialog options", 4, alertButtons.size());

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
        dragAndDrop.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), targetCenterX, targetCenterY));
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
            Thread.sleep(500);
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
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for UI update", e);
        }
    }
}
