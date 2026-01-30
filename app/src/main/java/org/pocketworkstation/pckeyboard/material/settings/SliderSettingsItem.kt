/*
 * Copyright (C) 2025 Hacker's Keyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pocketworkstation.pckeyboard.material.settings

/**
 * Settings item for slider/seekbar preferences.
 * Supports both integer and float values, with optional percentage display.
 */
class SliderSettingsItem @JvmOverloads constructor(
    key: String,
    title: String,
    summary: String,
    val minValue: Float,
    val maxValue: Float,
    val stepSize: Float,
    val defaultValue: Float,
    val storeAsString: Boolean,  // Original prefs use SeekBarPreferenceString
    val displayAsPercent: Boolean = false,
    val displayFormat: String? = null,  // e.g., "%.0f ms" or "%.0f%%"
    dependencyKey: String? = null
) : SettingsItem(key, title, summary, Type.SLIDER, dependencyKey) {

    /**
     * Clamp a value to the valid range.
     */
    fun clamp(value: Float): Float = value.coerceIn(minValue, maxValue)

    /**
     * Format a value for display.
     */
    fun formatValue(value: Float): String {
        return when {
            displayFormat != null -> String.format(displayFormat, value)
            displayAsPercent -> String.format("%.0f%%", value)
            else -> value.toInt().toString()
        }
    }
}
