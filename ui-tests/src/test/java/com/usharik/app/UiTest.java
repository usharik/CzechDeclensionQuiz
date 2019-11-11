package com.usharik.app;

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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UiTest {

    private static final String APP_PATH = "/Users/macbook/IdeaProjects/CzechDeclensionQuiz/app/release/app-release.apk";

    private static AndroidDriver drv;
    private static AppiumDriverLocalService service;
    private static TestHelper helper;

    @BeforeClass
    public static void init() {
        service = AppiumDriverLocalService.buildDefaultService();
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Android Device");
        capabilities.setCapability(MobileCapabilityType.APP, APP_PATH);
        capabilities.setCapability(MobileCapabilityType.NO_RESET, true);
        capabilities.setCapability(AndroidMobileCapabilityType.NO_SIGN, true);
        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UiAutomator2");

        service.start();
        drv = new AndroidDriver(service, capabilities);
        helper = new TestHelper(drv);
    }


    @AfterClass
    public static void release() {
        if (service.isRunning()) {
            service.stop();
        }
    }

    @Test
    public void firstTest() {
        drv.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);

        List caseSingular = drv.findElementsById("com.usharik.app:id/caseSingular");
        List casePlural = drv.findElementsById("com.usharik.app:id/casePlural");

        String timestamp = Long.toString(System.currentTimeMillis());

        helper.makeScreenshot(timestamp, "scr_before.png");

        for (int i=0; i<7; i++) {
            String wordId = "com.usharik.app:id/word" + (i + 1);
            WebElement word = drv.findElementById(wordId);

            Assert.assertTrue(word.isDisplayed());

            TouchAction action = new TouchAction(drv);
            action.longPress(ElementOption.element(word))
                    .waitAction(WaitOptions.waitOptions(Duration.ofSeconds(3)))
                    .moveTo(ElementOption.element((WebElement) caseSingular.get(i)))
                    .release()
                    .perform();

            Assert.assertFalse(helper.isElementExistsById(wordId));
        }

        for (int i=7; i<14; i++) {
            String wordId = "com.usharik.app:id/word" + (i + 1);
            WebElement word = drv.findElementById(wordId);

            Assert.assertTrue(word.isDisplayed());

            TouchAction action = new TouchAction(drv);
            action.longPress(ElementOption.element(word))
                    .waitAction(WaitOptions.waitOptions(Duration.ofSeconds(3)))
                    .moveTo(ElementOption.element((WebElement) casePlural.get(i - 7)))
                    .release()
                    .perform();

            Assert.assertFalse(helper.isElementExistsById(wordId));
        }

        helper.makeScreenshot(timestamp, "scr_after.png");
    }
}
