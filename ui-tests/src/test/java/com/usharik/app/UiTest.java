package com.usharik.app;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

public class UiTest {

    private static AndroidDriver drv;
    private static AppiumDriverLocalService service;

    @BeforeClass
    public static void init() throws MalformedURLException {
        String appPath = "/Users/macbook/IdeaProjects/CzechDeclensionQuiz/app/build/outputs/apk/debug/app-debug.apk";

        service = AppiumDriverLocalService.buildDefaultService();
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("testdroid_target", "Android");
        capabilities.setCapability("deviceName", "Android Device");
        capabilities.setCapability("testdroid_project", "LocalAppium");
        capabilities.setCapability("testdroid_testrun", "Android Run 1");
        capabilities.setCapability("testdroid_device", "Android Emulator");
        capabilities.setCapability("testdroid_app", appPath);
        capabilities.setCapability("app", appPath);

        service.start();
        drv = new AndroidDriver(service, capabilities);
    }

    @Test
    public void firstTest() {
        drv.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);

        File screenshotAs = ((TakesScreenshot) drv).getScreenshotAs(OutputType.FILE);
    }
}
