package com.usharik.app.subscription;

import androidx.annotation.NonNull;

public final class SubscriptionStatus {
    private final SubscriptionPlan plan;
    private final SubscriptionSource source;
    private final String productId;

    private SubscriptionStatus(SubscriptionPlan plan, SubscriptionSource source, String productId) {
        this.plan = plan;
        this.source = source == null ? SubscriptionSource.NONE : source;
        this.productId = productId == null ? "" : productId;
    }

    public static SubscriptionStatus free() {
        return new SubscriptionStatus(SubscriptionPlan.FREE, SubscriptionSource.NONE, "");
    }

    public static SubscriptionStatus premium(SubscriptionSource source, String productId) {
        return new SubscriptionStatus(SubscriptionPlan.PREMIUM, source, productId);
    }

    public boolean hasPremiumAccess() {
        return plan == SubscriptionPlan.PREMIUM;
    }

    @NonNull
    public SubscriptionPlan getPlan() {
        return plan;
    }

    @NonNull
    public SubscriptionSource getSource() {
        return source;
    }

    @NonNull
    public String getProductId() {
        return productId;
    }
}