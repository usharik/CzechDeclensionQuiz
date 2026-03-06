package com.usharik.app.helpers;

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
    private final Map<String, String> wordDataCache;

    public TestHelper(AndroidDriver driver, String dataJsonPath) {
        this.driver = driver;
        this.wordDataCache = new HashMap<>();
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

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Extract word from JSON line
                String word = extractWord(line);
                if (word != null) {
                    wordDataCache.put(word, line);
                }
            }

            logger.info("Successfully loaded {} words from JSON", wordDataCache.size());
        } catch (IOException e) {
            logger.error("Error loading JSON data from: {}", jsonPath, e);
            throw new RuntimeException("Failed to load word data", e);
        }
    }

    /**
     * Extract word field from JSON line
     * @param json JSON string containing word data
     * @return The extracted word, or null if not found
     */
    private String extractWord(String json) {
        int wordStart = json.indexOf("\"word\":\"");
        if (wordStart == -1) {
            return null;
        }
        wordStart += 8; // Length of "word":"
        int wordEnd = json.indexOf("\"", wordStart);
        if (wordEnd == -1) {
            return null;
        }
        return json.substring(wordStart, wordEnd);
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

        String json = wordDataCache.get(word);
        if (json == null) {
            logger.error("Word not found in cache: {}", word);
            throw new IllegalArgumentException("Word not found in cache: " + word);
        }

        // Parse the JSON to extract cases
        // JSON format: {"wordId":..., "word":"...", "cases":[["case1",...],["case1",...]]}
        return parseCasesFromJson(json);
    }

    /**
     * Parse cases array from JSON string
     * @param json JSON string containing cases data
     * @return 2D array of cases [singular/plural][case1-7]
     */
    private String[][] parseCasesFromJson(String json) {
        // Parse JSON to extract cases array
        // JSON format: {"cases":[["case1","case2",...],["case1","case2",...]]}
        String[][] cases = new String[2][7];

        int casesStart = json.indexOf("\"cases\":");
        if (casesStart == -1) {
            logger.warn("No 'cases' field found in JSON");
            return cases;
        }

        // Find the start of the cases array
        int arrayStart = json.indexOf("[[", casesStart);
        if (arrayStart == -1) {
            logger.warn("No cases array start found in JSON");
            return cases;
        }

        // Find the end of the cases array (matching ]])
        int arrayEnd = json.indexOf("]]", arrayStart);
        if (arrayEnd == -1) {
            logger.warn("No cases array end found in JSON");
            return cases;
        }

        // Extract the content between [[ and ]]
        String casesContent = json.substring(arrayStart + 2, arrayEnd);

        // Split by "],["  to separate singular and plural
        String[] numberArrays = casesContent.split("],\\[");

        if (numberArrays.length >= 2) {
            // Parse singular cases (first array)
            cases[0] = parseStringArray(numberArrays[0]);

            // Parse plural cases (second array)
            cases[1] = parseStringArray(numberArrays[1]);
        } else {
            logger.warn("Expected 2 arrays (singular/plural), found: {}", numberArrays.length);
        }

        return cases;
    }

    /**
     * Parse a string array from JSON format
     * @param arrayContent String containing array content (e.g., "str1","str2","str3")
     * @return Array of parsed strings
     */
    private String[] parseStringArray(String arrayContent) {
        String[] result = new String[7];

        // Parse quoted strings from the array
        // Handle format: "str1","str2","str3"
        java.util.List<String> values = new java.util.ArrayList<>();
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);

            if (c == '"') {
                if (inQuote) {
                    // End of quoted string
                    values.add(current.toString());
                    current = new StringBuilder();
                    inQuote = false;
                } else {
                    // Start of quoted string
                    inQuote = true;
                }
            } else if (inQuote) {
                // Inside quoted string
                current.append(c);
            }
        }

        // Copy to result array
        for (int i = 0; i < Math.min(7, values.size()); i++) {
            result[i] = values.get(i);
        }

        logger.debug("Parsed {} values from array", values.size());

        return result;
    }
}
