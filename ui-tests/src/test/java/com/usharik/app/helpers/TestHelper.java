package com.usharik.app.helpers;

import com.google.gson.Gson;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for UI tests providing utilities for:
 * - Loading and parsing word data from JSON
 * - Taking screenshots
 * - Checking element existence
 */
public final class TestHelper {

    private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);

    // Get project root directory from system property (set by Gradle)
    // and construct absolute path to screenshots directory
    private static final String PROJECT_ROOT = System.getProperty("project.root");
    private static final String SCREENSHOT_PATH = PROJECT_ROOT + "/ui-tests/screenshots/";

    private final AndroidDriver driver;
    private final Map<String, WordInfo> wordDataCache;
    private final Gson gson;

    public TestHelper(AndroidDriver driver, String dataJsonPath) {
        this.driver = driver;
        this.wordDataCache = new HashMap<>();
        this.gson = new Gson();
        logger.info("Screenshots will be saved to: {}", SCREENSHOT_PATH);
        loadJsonData(dataJsonPath);
    }

    /**
     * Load all word data from JSON file into memory cache
     * @param jsonPath Path to the JSON data file
     */
    private void loadJsonData(String jsonPath) {
        logger.info("Loading word data from: {}", jsonPath);

        try (BufferedReader reader = new BufferedReader(new FileReader(jsonPath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    // Parse JSON line using GSON
                    WordInfo wordInfo = gson.fromJson(line, WordInfo.class);
                    if (wordInfo != null && wordInfo.word != null) {
                        wordDataCache.put(wordInfo.word, wordInfo);
                    } else {
                        logger.warn("Skipping line {} - invalid word data", lineNumber);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing JSON at line {}: {}", lineNumber, line, e);
                }
            }

            logger.info("Successfully loaded {} words from JSON", wordDataCache.size());
        } catch (IOException e) {
            logger.error("Error loading JSON data from: {}", jsonPath, e);
            throw new RuntimeException("Failed to load word data", e);
        }
    }



    /**
     * Take a screenshot and save it to the screenshots directory
     * @param paths Path components for the screenshot file
     */
    public void makeScreenshot(String... paths) {
        logger.debug("Taking screenshot: {}", String.join("/", paths));

        try {
            File tmpScreen = driver.getScreenshotAs(OutputType.FILE);
            Path destPath = Paths.get(SCREENSHOT_PATH, paths);
            Files.createDirectories(destPath.getParent());
            Files.copy(tmpScreen.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Screenshot saved to: {}", destPath);
        } catch (IOException e) {
            logger.error("Failed to save screenshot", e);
        }
    }

    /**
     * Get word cases from cached JSON data
     * @param word The word to get cases for
     * @return 2D array of cases [singular/plural][case1-7]
     */
    public String[][] getWordCases(String word) {
        logger.debug("Getting word cases for: {}", word);

        WordInfo wordInfo = wordDataCache.get(word);
        if (wordInfo == null) {
            logger.error("Word not found in cache: {}", word);
            throw new IllegalArgumentException("Word not found in cache: " + word);
        }

        if (wordInfo.cases == null) {
            logger.error("Word '{}' has no cases data", word);
            return new String[2][7];
        }

        return wordInfo.cases;
    }


}
