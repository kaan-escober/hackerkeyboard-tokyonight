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
 * Represents a section/category of settings in the Material Settings UI.
 * Groups related settings items together with a title.
 */
class SettingsSection(
    val title: String,
    items: List<SettingsItem> = emptyList()
) {
    private val _items = items.toMutableList()
    
    val items: List<SettingsItem> get() = _items

    fun addItem(item: SettingsItem) {
        _items.add(item)
    }

    val itemCount: Int get() = _items.size

    /**
     * Builder for creating SettingsSection instances.
     */
    class Builder(private val title: String) {
        private val items = mutableListOf<SettingsItem>()

        fun addBoolean(
            key: String,
            title: String,
            summary: String,
            defaultValue: Boolean
        ) = apply {
            items.add(BooleanSettingsItem(key, title, summary, defaultValue))
        }

        fun addBoolean(
            key: String,
            title: String,
            summary: String,
            defaultValue: Boolean,
            summaryOn: String,
            summaryOff: String
        ) = apply {
            items.add(BooleanSettingsItem(key, title, summary, defaultValue, summaryOn, summaryOff))
        }

        fun addBoolean(
            key: String,
            title: String,
            summary: String,
            defaultValue: Boolean,
            summaryOn: String,
            summaryOff: String,
            dependencyKey: String
        ) = apply {
            items.add(BooleanSettingsItem(key, title, summary, defaultValue, summaryOn, summaryOff, dependencyKey))
        }

        fun addSlider(
            key: String,
            title: String,
            summary: String,
            min: Float,
            max: Float,
            step: Float,
            defaultValue: Float,
            storeAsString: Boolean,
            displayFormat: String
        ) = apply {
            items.add(SliderSettingsItem(key, title, summary, min, max, step, defaultValue,
                storeAsString, displayFormat = displayFormat))
        }

        fun addSlider(
            key: String,
            title: String,
            summary: String,
            min: Float,
            max: Float,
            step: Float,
            defaultValue: Float,
            storeAsString: Boolean,
            displayAsPercent: Boolean,
            displayFormat: String?,
            dependencyKey: String?
        ) = apply {
            items.add(SliderSettingsItem(key, title, summary, min, max, step, defaultValue,
                storeAsString, displayAsPercent, displayFormat, dependencyKey))
        }

        fun addList(
            key: String,
            title: String,
            summary: String,
            entries: Array<String>,
            entryValues: Array<String>,
            defaultValue: String
        ) = apply {
            items.add(ListSettingsItem(key, title, summary, entries, entryValues, defaultValue))
        }

        fun addItem(item: SettingsItem) = apply {
            items.add(item)
        }

        fun build(): SettingsSection = SettingsSection(title, items)
    }
}
