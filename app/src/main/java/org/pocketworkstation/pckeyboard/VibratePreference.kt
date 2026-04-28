package org.pocketworkstation.pckeyboard

import android.content.Context
import android.util.AttributeSet

/**
 * Preference class for configuring vibration intensity settings.
 *
 * Extends [SeekBarPreferenceString] to allow users to adjust vibration intensity levels
 * through a slider dialog. Triggers haptic feedback on the keyboard input method when
 * the vibration value changes.
 */
class VibratePreference : SeekBarPreferenceString {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    /**
     * Called when the vibration preference value changes.
     *
     * Retrieves the current IME instance and triggers haptic feedback with the new
     * vibration intensity value. This allows users to preview the vibration effect
     * as they adjust the slider.
     *
     * @param val the new vibration intensity value as a float
     */
    override fun onChange(value: Float) {
        val ime = LatinIME.sInstance
        ime?.vibrate(value.toInt())
    }
}
