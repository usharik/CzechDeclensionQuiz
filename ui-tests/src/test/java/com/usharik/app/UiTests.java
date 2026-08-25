package com.usharik.app;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

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
import org.openqa.selenium.By;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.usharik.app.Parameters.APPIUM_URL;
import static com.usharik.app.Parameters.APP_PATH;
import static com.usharik.app.Parameters.DEFAULT_TIMEOUT;
import static com.usharik.app.Parameters.IMPLICIT_WAIT;
import static com.usharik.app.Parameters.SCREEN_STABILITY_DELAY;
import static com.usharik.app.Parameters.UI_UPDATE_DELAY;
import static com.usharik.app.UiConstants.APP_PACKAGE;
import static com.usharik.app.UiConstants.TAG_APP_BAR_TITLE;
import static com.usharik.app.UiConstants.TAG_BTN_ABOUT;
import static com.usharik.app.UiConstants.TAG_BTN_ERRORS;
import static com.usharik.app.UiConstants.TAG_BTN_FULL;
import static com.usharik.app.UiConstants.TAG_BTN_HANDBOOK;
import static com.usharik.app.UiConstants.TAG_BTN_SETTINGS;
import static com.usharik.app.UiConstants.TAG_BTN_SINGLE;
import static com.usharik.app.UiConstants.TAG_FULL_POOL_WORD_PREFIX;
import static com.usharik.app.UiConstants.TAG_FULL_WORD;
import static com.usharik.app.UiConstants.TAG_HUB_SCREEN;
import static com.usharik.app.UiConstants.TAG_QUIT_DIALOG;
import static com.usharik.app.UiConstants.TAG_QUIT_LEAVE;
import static com.usharik.app.UiConstants.TAG_SC_ANSWER_0;
import static com.usharik.app.UiConstants.TAG_SC_NEXT_CASE;
import static com.usharik.app.UiConstants.TAG_SC_QUESTION;
import static com.usharik.app.UiConstants.TAG_SC_WORD;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

/**
 * Appium smoke tests for the Compose UI. Functional coverage (drag-and-drop,
 * dialogs, counters, per-screen behavior) lives in the app's instrumentation
 * tests (app/src/androidTest); this suite only verifies that the release-like
 * APK installs, launches, and the main navigation flows work end to end.
 */
public class UiTests {

    private static final Logger logger = LoggerFactory.getLogger(UiTests.class);

    private static AndroidDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    public static void setupClass() {
        File appFile = new File(APP_PATH);
        if (!appFile.exists()) {
            throw new IllegalStateException("APK file not found at: " + appFile.getAbsolutePath());
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
        assertButtonText(TAG_BTN_FULL, "Full declension table quiz");
        assertButtonText(TAG_BTN_SINGLE, "One case at a time quiz");
        assertButtonText(TAG_BTN_ERRORS, "Words with errors");
        assertButtonText(TAG_BTN_HANDBOOK, "Handbook");
        assertButtonText(TAG_BTN_SETTINGS, "Settings");
        assertButtonText(TAG_BTN_ABOUT, "About");
    }

    @Test
    public void testNavigateToPagesAndBack() {
        openPageAndReturn(TAG_BTN_ERRORS, "Words with errors");
        openPageAndReturn(TAG_BTN_HANDBOOK, "Handbook");
        openPageAndReturn(TAG_BTN_SETTINGS, "Settings");
        openPageAndReturn(TAG_BTN_ABOUT, "About");
    }

    @Test
    public void testFullDeclensionQuizOpensAndQuitsViaDialog() {
        waitForVisibleElement(TAG_BTN_FULL).click();
        waitForScreenStability();

        // A word is shown and the word bank contains draggable words.
        assertFalse(waitForVisibleElement(TAG_FULL_WORD).getText().isEmpty());
        assertFalse(driver.findElements(byTagPrefix(TAG_FULL_POOL_WORD_PREFIX)).isEmpty());

        // Back opens the quit-quiz dialog; "Leave quiz" returns to the hub.
        driver.navigate().back();
        assertNotNull(waitForVisibleElement(TAG_QUIT_DIALOG));
        waitForVisibleElement(TAG_QUIT_LEAVE).click();
        waitForHubScreen();
    }

    @Test
    public void testSingleCaseQuizAnswerFlow() {
        waitForVisibleElement(TAG_BTN_SINGLE).click();
        waitForScreenStability();

        assertFalse(waitForVisibleElement(TAG_SC_WORD).getText().isEmpty());
        assertFalse(waitForVisibleElement(TAG_SC_QUESTION).getText().isEmpty());
        assertFalse(waitForVisibleElement(TAG_SC_NEXT_CASE).isEnabled());

        // Answering (right or wrong) locks the answers and unlocks "Next case".
        waitForVisibleElement(TAG_SC_ANSWER_0).click();
        waitForUiUpdate();
        assertTrue(waitForVisibleElement(TAG_SC_NEXT_CASE).isEnabled());
        assertFalse(waitForVisibleElement(TAG_SC_ANSWER_0).isEnabled());

        // Advancing resets the state for the next question.
        waitForVisibleElement(TAG_SC_NEXT_CASE).click();
        waitForScreenStability();
        assertTrue(waitForVisibleElement(TAG_SC_ANSWER_0).isEnabled());
        assertFalse(waitForVisibleElement(TAG_SC_NEXT_CASE).isEnabled());

        // Back opens the shared quit-quiz dialog; leave to the hub.
        driver.navigate().back();
        assertNotNull(waitForVisibleElement(TAG_QUIT_DIALOG));
        waitForVisibleElement(TAG_QUIT_LEAVE).click();
        waitForHubScreen();
    }

    private void ensureHubScreen() {
        for (int i = 0; i < 3; i++) {
            if (isElementVisible(TAG_HUB_SCREEN, Duration.ofSeconds(2))) {
                return;
            }
            driver.navigate().back();
            dismissQuitOverlayIfVisible();
            waitForScreenStability();
        }

        driver.activateApp(APP_PACKAGE);
        waitForHubScreen();
    }

    private void openPageAndReturn(String buttonTag, String expectedTitle) {
        waitForVisibleElement(buttonTag).click();
        waitForScreenStability();
        assertEquals(expectedTitle, waitForVisibleElement(TAG_APP_BAR_TITLE).getText());
        driver.navigate().back();
        waitForHubScreen();
    }

    private void assertHubVisible() {
        assertNotNull(waitForVisibleElement(TAG_HUB_SCREEN));
        assertEquals("Czech Declension Quiz", waitForVisibleElement(TAG_APP_BAR_TITLE).getText());
    }

    private void waitForHubScreen() {
        waitForScreenStability();
        assertHubVisible();
    }

    /**
     * If the quit-quiz dialog is visible (identified by the "Leave quiz" button),
     * click that button so navigation proceeds to the hub.
     */
    private void dismissQuitOverlayIfVisible() {
        if (isElementVisible(TAG_QUIT_LEAVE, Duration.ofSeconds(2))) {
            try {
                waitForVisibleElement(TAG_QUIT_LEAVE).click();
                waitForScreenStability();
            } catch (Exception ignored) {
            }
        }
    }

    /** The label lives on a child text node of the tagged button container. */
    private void assertButtonText(String tag, String expectedText) {
        By label = AppiumBy.xpath("//*[@resource-id='" + tag + "']//*[@text='" + expectedText + "']");
        assertNotNull(wait.until(ExpectedConditions.visibilityOfElementLocated(label)));
    }

    /**
     * Compose test tags are exposed as bare resource-ids (no package prefix),
     * so they are matched via xpath instead of AppiumBy.id, which would
     * prepend "com.usharik.app:id/".
     */
    private By byTag(String tag) {
        return AppiumBy.xpath("//*[@resource-id='" + tag + "']");
    }

    private By byTagPrefix(String tagPrefix) {
        return AppiumBy.xpath("//*[starts-with(@resource-id, '" + tagPrefix + "')]");
    }

    private WebElement waitForVisibleElement(String tag) {
        return wait.until(ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(byTag(tag))));
    }

    private boolean isElementVisible(String tag, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(byTag(tag)))
                    .isDisplayed();
        } catch (Exception ignored) {
            return false;
        }
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
}

