package com.usharik.app;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class TestHelper {

    private static final String SCREENSHOT_PATH = "/Users/macbook/IdeaProjects/CzechDeclensionQuiz/ui-tests/screenshots/";

    private AndroidDriver drv;

    public TestHelper(AndroidDriver drv) {
        this.drv = drv;
    }

    public void makeScreenshot(String... paths) {
        File tmpScreen = drv.getScreenshotAs(OutputType.FILE);
        Path destPath = Paths.get(SCREENSHOT_PATH, paths);
        try {
            Files.createDirectories(destPath);
            Files.copy(tmpScreen.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isElementExistsById(String id) {
        return drv.findElementsById(id).size() > 0;
    }
}
