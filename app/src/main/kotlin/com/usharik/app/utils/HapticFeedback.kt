package com.usharik.app.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

object HapticFeedback {
    fun light(context: Context) = vibrate(context, 10)
    fun medium(context: Context) = vibrate(context, 20)
    fun success(context: Context) = vibrate(context, 50)
    fun error(context: Context) { vibrator(context)?.takeIf { it.hasVibrator() }?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100, 100, 100), -1)) }
    private fun vibrate(context: Context, milliseconds: Long) { vibrator(context)?.takeIf { it.hasVibrator() }?.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)) }
    @Suppress("DEPRECATION") private fun vibrator(context: Context): Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
}
