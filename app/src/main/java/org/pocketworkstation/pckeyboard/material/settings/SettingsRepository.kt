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

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Repository for reading and writing keyboard settings.
 * Handles type conversion between Material UI and original preference system.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // Boolean settings
    fun getBoolean(item: BooleanSettingsItem): Boolean =
        prefs.getBoolean(item.key, item.defaultValue)

    fun setBoolean(item: BooleanSettingsItem, value: Boolean) {
        prefs.edit().putBoolean(item.key, value).apply()
    }

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    // Slider settings (stored as String to match SeekBarPreferenceString)
    fun getSliderValue(item: SliderSettingsItem): Float {
        return try {
            if (item.storeAsString) {
                val value = prefs.getString(item.key, item.defaultValue.toInt().toString())
                item.clamp(value?.toFloat() ?: item.defaultValue)
            } else {
                item.clamp(prefs.getFloat(item.key, item.defaultValue))
            }
        } catch (e: Exception) {
            item.defaultValue
        }
    }

    fun setSliderValue(item: SliderSettingsItem, value: Float) {
        val clampedValue = item.clamp(value)
        if (item.storeAsString) {
            prefs.edit().putString(item.key, clampedValue.toInt().toString()).apply()
        } else {
            prefs.edit().putFloat(item.key, clampedValue).apply()
        }
    }

    // List settings
    fun getListValue(item: ListSettingsItem): String =
        prefs.getString(item.key, item.defaultValue) ?: item.defaultValue

    fun setListValue(item: ListSettingsItem, value: String) {
        prefs.edit().putString(item.key, value).apply()
    }

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String): String =
        prefs.getString(key, defaultValue) ?: defaultValue

    // Generic preference access
    val preferences: SharedPreferences get() = prefs

    /**
     * Check if a dependency is satisfied (for dependent settings).
     */
    fun isDependencySatisfied(item: SettingsItem): Boolean {
        if (!item.hasDependency()) {
            return true
        }
        return prefs.getBoolean(item.dependencyKey, false)
    }
}
