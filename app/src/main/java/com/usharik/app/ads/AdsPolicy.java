package com.usharik.app.ads;

/**
 * Single point to decide whether ads should be displayed.
 *
 * <p>Implement this interface and bind it in the DI graph to disable ads after a
 * purchase, in tests, or for any other reason — without touching fragment code.
 */
public interface AdsPolicy {

    /** @return {@code true} if ads are allowed to be shown. */
    boolean areAdsEnabled();
}
