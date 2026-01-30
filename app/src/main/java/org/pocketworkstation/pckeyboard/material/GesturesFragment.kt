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

package org.pocketworkstation.pckeyboard.material

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import org.pocketworkstation.pckeyboard.R

/**
 * Fragment for configuring gesture and hardware key actions.
 * Handles swipe gestures (up, down, left, right) and hardware key actions (volume up/down).
 */
class GesturesFragment : Fragment() {

    private lateinit var prefs: SharedPreferences

    // Swipe gesture dropdowns
    private var swipeUpDropdown: MaterialAutoCompleteTextView? = null
    private var swipeDownDropdown: MaterialAutoCompleteTextView? = null
    private var swipeLeftDropdown: MaterialAutoCompleteTextView? = null
    private var swipeRightDropdown: MaterialAutoCompleteTextView? = null

    // Hardware key dropdowns
    private var volUpDropdown: MaterialAutoCompleteTextView? = null
    private var volDownDropdown: MaterialAutoCompleteTextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gestures, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        try {
            // Initialize swipe gesture dropdowns
            swipeUpDropdown = view.findViewById(R.id.swipe_up_dropdown)
            swipeDownDropdown = view.findViewById(R.id.swipe_down_dropdown)
            swipeLeftDropdown = view.findViewById(R.id.swipe_left_dropdown)
            swipeRightDropdown = view.findViewById(R.id.swipe_right_dropdown)

            // Initialize hardware key dropdowns
            volUpDropdown = view.findViewById(R.id.vol_up_dropdown)
            volDownDropdown = view.findViewById(R.id.vol_down_dropdown)

            // Setup adapter for all dropdowns
            setupDropdownAdapters()

            // Load current values
            loadPreferences()

            // Setup listeners
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Setup ArrayAdapter for all dropdown menus with gesture options.
     * Each dropdown needs its own adapter instance.
     */
    private fun setupDropdownAdapters() {
        try {
            val adapter = { 
                ArrayAdapter(
                    requireContext(),
                    R.layout.item_dropdown,
                    GESTURE_ENTRIES
                )
            }

            swipeUpDropdown?.setAdapter(adapter())
            swipeDownDropdown?.setAdapter(adapter())
            swipeLeftDropdown?.setAdapter(adapter())
            swipeRightDropdown?.setAdapter(adapter())
            volUpDropdown?.setAdapter(adapter())
            volDownDropdown?.setAdapter(adapter())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load gesture and hardware key preferences from SharedPreferences.
     */
    private fun loadPreferences() {
        try {
            // Load swipe up (default: "extension" - matches legacy)
            swipeUpDropdown?.let {
                val value = prefs.getString("pref_swipe_up", "extension") ?: "extension"
                it.setText(valueToEntry(value), false)
            }

            // Load swipe down (default: "close" - matches legacy)
            swipeDownDropdown?.let {
                val value = prefs.getString("pref_swipe_down", "close") ?: "close"
                it.setText(valueToEntry(value), false)
            }

            // Load swipe left (default: "none" - matches legacy)
            swipeLeftDropdown?.let {
                val value = prefs.getString("pref_swipe_left", "none") ?: "none"
                it.setText(valueToEntry(value), false)
            }

            // Load swipe right (default: "none" - matches legacy)
            swipeRightDropdown?.let {
                val value = prefs.getString("pref_swipe_right", "none") ?: "none"
                it.setText(valueToEntry(value), false)
            }

            // Load volume up (default: "none")
            volUpDropdown?.let {
                val value = prefs.getString("pref_vol_up", "none") ?: "none"
                it.setText(valueToEntry(value), false)
            }

            // Load volume down (default: "none")
            volDownDropdown?.let {
                val value = prefs.getString("pref_vol_down", "none") ?: "none"
                it.setText(valueToEntry(value), false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Convert a preference value to its corresponding display entry.
     *
     * @param value The preference value (e.g., "shift", "close")
     * @return The display entry (e.g., "Shift", "Close keyboard")
     */
    private fun valueToEntry(value: String): String {
        val index = GESTURE_VALUES.indexOf(value)
        return if (index >= 0) GESTURE_ENTRIES[index] else GESTURE_ENTRIES[0]
    }

    /**
     * Convert a display entry to its corresponding preference value.
     *
     * @param entry The display entry (e.g., "Shift", "Close keyboard")
     * @return The preference value (e.g., "shift", "close")
     */
    private fun entryToValue(entry: String): String {
        val index = GESTURE_ENTRIES.indexOf(entry)
        return if (index >= 0) GESTURE_VALUES[index] else GESTURE_VALUES[0]
    }

    /**
     * Setup listeners for all dropdown menus to save preferences when changed.
     */
    private fun setupListeners() {
        try {
            // Swipe up listener
            swipeUpDropdown?.setOnItemClickListener { parent, _, position, _ ->
                val entry = parent.getItemAtPosition(position) as String
                prefs.edit().putString("pref_swipe_up", entryToValue(entry)).apply()
            }

            // Swipe down listener
            swipeDownDropdown?.setOnItemClickListener { parent, _, position, _ ->
                val entry = parent.getItemAtPosition(position) as String
                prefs.edit().putString("pref_swipe_down", entryToValue(entry)).apply()
            }

            // Swipe left listener
            swipeLeftDropdown?.setOnItemClickListener { parent, _, position, _ ->
                val entry = parent.getItemAtPosition(position) as String
                prefs.edit().putString("pref_swipe_left", entryToValue(entry)).apply()
            }

            // Swipe right listener
            swipeRightDropdown?.setOnItemClickListener { parent, _, position, _ ->
                val entry = parent.getItemAtPosition(position) as String
                prefs.edit().putString("pref_swipe_right", entryToValue(entry)).apply()
            }

            // Volume up listener
            volUpDropdown?.setOnItemClickListener { parent, _, position, _ ->
                val entry = parent.getItemAtPosition(position) as String
                prefs.edit().putString("pref_vol_up", entryToValue(entry)).apply()
            }

            // Volume down listener
            volDownDropdown?.setOnItemClickListener { parent, _, position, _ ->
                val entry = parent.getItemAtPosition(position) as String
                prefs.edit().putString("pref_vol_down", entryToValue(entry)).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        // Gesture action entries and values (matching legacy prefs_actions.xml)
        private val GESTURE_ENTRIES = arrayOf(
            "(no action)", "Close keyboard", "Toggle extension row", "Launch Settings",
            "Toggle suggestions", "Voice input", "Switch keyboard layout",
            "Increase height", "Decrease height", "Previous language", "Next language"
        )

        private val GESTURE_VALUES = arrayOf(
            "none", "close", "extension", "settings",
            "suggestions", "voice_input", "full_mode",
            "height_up", "height_down", "lang_prev", "lang_next"
        )
    }
}
