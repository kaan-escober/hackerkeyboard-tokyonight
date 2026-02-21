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
 * Settings item for list/dropdown preferences.
 */
class ListSettingsItem constructor(
    key: String,
    title: String,
    summary: String,
    val entries: Array<String>,      // Display labels
    val entryValues: Array<String>,  // Stored values
    val defaultValue: String,
    dependencyKey: String? = null
) : SettingsItem(key, title, summary, Type.LIST, dependencyKey) {

    /**
     * Get the display label for a stored value.
     */
    fun getEntryForValue(value: String): String {
        val index = entryValues.indexOf(value)
        return if (index >= 0) entries[index] else value
    }

    /**
     * Get the index of a stored value.
     */
    fun getIndexForValue(value: String): Int = entryValues.indexOf(value)
}
