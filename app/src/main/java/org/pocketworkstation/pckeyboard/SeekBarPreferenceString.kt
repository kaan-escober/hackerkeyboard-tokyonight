package org.pocketworkstation.pckeyboard

import java.util.regex.Pattern
import android.content.Context
import android.util.AttributeSet

/**
 * Variant of [SeekBarPreference] that stores values as string preferences.
 *
 * This is for compatibility with existing preferences, switching types
 * leads to runtime errors when upgrading or downgrading.
 *
 * Handles legacy preference values that may include suffixes like " ms" or "%"
 * by extracting the numeric portion during deserialization.
 */
open class SeekBarPreferenceString : SeekBarPreference {

    companion object {
        private val FLOAT_RE: Pattern = Pattern.compile("(\\d+\\.?\\d*).*")
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    /**
     * Extracts a float value from a string that may contain non-numeric suffixes.
     * Handles legacy preferences with " ms" or "%" suffixes by parsing only the numeric portion.
     *
     * @param pref The preference string to parse
     * @return The parsed float value, or 0.0f if parsing fails
     */
    private fun floatFromString(pref: String?): Float {
        if (pref == null) return 0.0f
        val num = FLOAT_RE.matcher(pref)
        if (!num.matches()) return 0.0f
        return num.group(1)?.toFloat() ?: 0.0f
    }

    override fun onGetDefaultValue(a: android.content.res.TypedArray, index: Int): Any {
        return a.getString(index) ?: ""
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val persisted = getPersistedString(null)
        mVal = if (persisted != null) {
            try {
                floatFromString(persisted)
            } catch (e: NumberFormatException) {
                0.0f
            }
        } else if (defaultValue != null) {
            when (defaultValue) {
                is Float -> defaultValue
                else -> try {
                    floatFromString(defaultValue.toString())
                } catch (e: NumberFormatException) {
                    0.0f
                }
            }
        } else {
            0.0f
        }
        mPrevVal = mVal
    }

    override fun persistValue(value: Float) {
        persistString(value.toString())
    }
}
