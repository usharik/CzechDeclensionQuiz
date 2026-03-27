package com.usharik.app.subscription;

public enum SubscriptionSource {
    NONE("none"),
    DEBUG("debug"),
    PLAY_BILLING("play_billing");

    private final String preferenceValue;

    SubscriptionSource(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public String preferenceValue() {
        return preferenceValue;
    }

    public static SubscriptionSource fromPreference(String value) {
        if (value != null) {
            for (SubscriptionSource source : values()) {
                if (source.preferenceValue.equalsIgnoreCase(value)) {
                    return source;
                }
            }
        }
        return NONE;
    }
}