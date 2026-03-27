package com.usharik.app.subscription;

public enum SubscriptionPlan {
    FREE("free"),
    PREMIUM("premium");

    private final String preferenceValue;

    SubscriptionPlan(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public String preferenceValue() {
        return preferenceValue;
    }

    public static SubscriptionPlan fromPreference(String value) {
        if (value != null) {
            for (SubscriptionPlan plan : values()) {
                if (plan.preferenceValue.equalsIgnoreCase(value)) {
                    return plan;
                }
            }
        }
        return FREE;
    }
}