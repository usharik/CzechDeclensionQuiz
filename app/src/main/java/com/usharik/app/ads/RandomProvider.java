package com.usharik.app.ads;

/**
 * Abstraction over random-number generation.
 * Allows deterministic values to be injected during unit testing.
 */
public interface RandomProvider {
    double nextDouble();
}
