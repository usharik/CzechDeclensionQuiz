package com.usharik.app.helpers;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class TestHelper {

    private static final String SCREENSHOT_PATH = "/Users/macbook/IdeaProjects/CzechDeclensionQuiz/ui-tests/screenshots/";

    private AndroidDriver drv;

    private Connection conn;

    public TestHelper(AndroidDriver drv, Connection conn) {
        this.drv = drv;
        this.conn = conn;
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

    public String[][] getWordCases(String word) throws Exception {
        PreparedStatement preparedStatement = conn.prepareStatement(
                "select A.case_num, A.number, A.word " +
                        "from CASES_OF_NOUN as A " +
                        "inner join WORD as B on A.word_id = B.id " +
                        "where B.word = ?");
        preparedStatement.setString(1, word);
        ResultSet resultSet = preparedStatement.executeQuery();
        String[][] cases = new String[2][7];
        while (resultSet.next()) {
            if (resultSet.getString(2).equals("nS")) {
                cases[0][resultSet.getInt(1) - 1] = resultSet.getString(3).replaceAll("[0-9]", "");
            } else {
                cases[1][resultSet.getInt(1) - 1] = resultSet.getString(3).replaceAll("[0-9]", "");
            }
        }
        return cases;
    }
}
