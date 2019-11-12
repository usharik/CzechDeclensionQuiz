package com.usharik.app;

import com.usharik.app.helpers.TestHelper;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.ElementOption;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class UiTests {

    private static final String APP_PATH = "/Users/macbook/IdeaProjects/CzechDeclensionQuiz/app/release/app-release.apk";

    private static AndroidDriver drv;
    private static AppiumDriverLocalService service;
    private static TestHelper helper;
    private static Connection conn;

    @BeforeClass
    public static void init() throws ClassNotFoundException, SQLException {
        service = AppiumDriverLocalService.buildDefaultService();
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Android Device");
        capabilities.setCapability(MobileCapabilityType.APP, APP_PATH);
        capabilities.setCapability(MobileCapabilityType.NO_RESET, true);
        capabilities.setCapability(AndroidMobileCapabilityType.NO_SIGN, true);
        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UiAutomator2");

        service.start();
        drv = new AndroidDriver(service, capabilities);

        Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:../utils/slovnik-database";
        conn = DriverManager.getConnection(url);

        helper = new TestHelper(drv, conn);
    }

    @AfterClass
    public static void release() {
        if (service.isRunning()) {
            service.stop();
        }
    }

    @Test
    public void testCorrectQuizSolution() throws Exception {
        drv.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

        goToFragmentByNum(0);

        List<WebElement> caseSingular = drv.findElementsById("com.usharik.app:id/caseSingular");
        List<WebElement> casePlural = drv.findElementsById("com.usharik.app:id/casePlural");
        WebElement currentWord = drv.findElementById("com.usharik.app:id/word");
        WebElement checkButton = drv.findElementById("com.usharik.app:id/action_check");

        String currentWordText = currentWord.getText();
        String[][] wordCases = helper.getWordCases(currentWordText);

        String timestamp = Long.toString(System.currentTimeMillis());

        helper.makeScreenshot(timestamp, "scr_before.png");

        for (int i = 0; i < 14; i++) {
            String wordId = "com.usharik.app:id/word" + (i + 1);
            WebElement word = drv.findElementById(wordId);

            Assert.assertTrue(word.isDisplayed());

            TouchAction action = new TouchAction(drv);
            action.longPress(ElementOption.element(word))
                    .waitAction(WaitOptions.waitOptions(Duration.ofSeconds(1)))
                    .moveTo(ElementOption.element(getProperCell(word.getText(), wordCases, caseSingular, casePlural)))
                    .release()
                    .perform();

            Assert.assertFalse(helper.isElementExistsById(wordId));
        }

        helper.makeScreenshot(timestamp, "scr_after.png");

        checkButton.click();

        WebElement alertTitle = drv.findElementById("com.usharik.app:id/alertTitle");
        assertEquals("All is correct!", alertTitle.getText());

        List<WebElement> alertButtons = drv.findElementsById("android:id/text1");
        assertEquals(4, alertButtons.size());

        alertButtons.get(0).click();
    }

    @Test
    public void testNavigation() throws InterruptedException {
        for(int i=0; i<5; i++) {
            goToFragmentByNum(i);

            Thread.sleep(2000);
            drv.rotate(ScreenOrientation.LANDSCAPE);
            Thread.sleep(2000);
            drv.rotate(ScreenOrientation.PORTRAIT);
        }
    }

    public void goToFragmentByNum(int i) {
        WebElement openDrawerButton = drv.findElementByXPath("//android.widget.ImageButton[@content-desc=\"Navigate up\"]");
        openDrawerButton.click();

        List<WebElement> navButtons = drv.findElementsById("com.usharik.app:id/design_menu_item_text");
        assertEquals(5, navButtons.size());
        navButtons.get(i).click();
    }

    public WebElement getProperCell(String word, String[][] wordCases,
                                    List<WebElement> caseSingular, List<WebElement> casePlural) {
        System.out.println(word);
        int i, j = 0;

        loop:
        for (i = 0; i < wordCases.length; i++) {
            for (j = 0; j < wordCases[i].length; j++) {
                if (wordCases[i][j] != null && wordCases[i][j].equals(word)) {
                    wordCases[i][j] = null;
                    break loop;
                }
            }
        }
        if (i == 0) {
            return caseSingular.get(j);
        } else {
            return casePlural.get(j);
        }
    }
}
