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
 * Settings item for boolean (switch/toggle) preferences.
 */
class BooleanSettingsItem @JvmOverloads constructor(
    key: String,
    title: String,
    summary: String,
    val defaultValue: Boolean,
    val summaryOn: String? = null,
    val summaryOff: String? = null,
    dependencyKey: String? = null
) : SettingsItem(key, title, summary, Type.BOOLEAN, dependencyKey) {

    fun hasDynamicSummary(): Boolean = summaryOn != null || summaryOff != null

    fun getSummaryForState(isChecked: Boolean): String {
        return when {
            isChecked && summaryOn != null -> summaryOn
            !isChecked && summaryOff != null -> summaryOff
            else -> summary
        }
    }
}
