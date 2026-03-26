package com.usharik.app.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Utility class for providing haptic feedback throughout the app
 */
public class HapticFeedback {

    /**
     * Light haptic feedback for button clicks and touch events
     * Duration: 10ms
     */
    public static void light(Context context) {
        vibrate(context, 10);
    }

    /**
     * Medium haptic feedback for drag start
     * Duration: 20ms
     */
    public static void medium(Context context) {
        vibrate(context, 20);
    }

    /**
     * Success haptic feedback (single short vibration)
     * Duration: 50ms
     */
    public static void success(Context context) {
        vibrate(context, 50);
    }

    /**
     * Error haptic feedback (triple vibration)
     */
    public static void error(Context context) {
        Vibrator vibrator = getVibrator(context);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] pattern = {0, 100, 100, 100, 100, 100};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                long[] pattern = {0, 100, 100, 100, 100, 100};
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    /**
     * Internal method to perform vibration
     */
    private static void vibrate(Context context, long duration) {
        Vibrator vibrator = getVibrator(context);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    /**
     * Get vibrator service
     */
    private static Vibrator getVibrator(Context context) {
        if (context == null) return null;
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}

