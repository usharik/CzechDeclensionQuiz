package com.usharik.app.ads;

import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;

/**
 * Production implementation of {@link RandomProvider} backed by {@link ThreadLocalRandom}.
 */
public class ThreadLocalRandomProvider implements RandomProvider {

    @Inject
    public ThreadLocalRandomProvider() {
    }

    @Override
    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }
}
