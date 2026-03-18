package com.usharik.app;

import java.time.Duration;

public final class Parameters {

    // Configuration - Paths
    static final String APP_PATH = System.getProperty("app.path", "app/release/app-release.apk");
    static final String DATA_JSON_PATH = System.getProperty("data.jsonl.path", "database/src/main/assets/data.jsonl");
    static final String APPIUM_URL = System.getProperty("appium.url", "").trim();

    // Configuration - Timeouts (in seconds)
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(
            Long.parseLong(System.getProperty("test.timeout.default", "10")));
    static final Duration IMPLICIT_WAIT = Duration.ofSeconds(
            Long.parseLong(System.getProperty("test.timeout.implicit", "2")));

    // Configuration - Delays (in milliseconds)
    static final long SCREEN_STABILITY_DELAY = Long.parseLong(
            System.getProperty("test.delay.screen.stability", "30"));
    static final long UI_UPDATE_DELAY = Long.parseLong(
            System.getProperty("test.delay.ui.update", "30"));
    static final long DRAG_DURATION = Long.parseLong(
            System.getProperty("test.delay.drag.duration", "10"));
}
