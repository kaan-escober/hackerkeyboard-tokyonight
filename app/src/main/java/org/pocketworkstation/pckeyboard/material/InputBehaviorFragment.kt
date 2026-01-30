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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import org.pocketworkstation.pckeyboard.R

/**
 * Fragment for configuring keyboard input behavior settings.
 */
class InputBehaviorFragment : Fragment() {

    private lateinit var prefs: SharedPreferences

    // Switches
    private var autoCapSwitch: MaterialSwitch? = null
    private var connectbotTabSwitch: MaterialSwitch? = null
    private var fullKeyboardPortraitSwitch: MaterialSwitch? = null
    private var capsLockSwitch: MaterialSwitch? = null
    private var shiftLockModifiersSwitch: MaterialSwitch? = null
    private var fullscreenOverrideSwitch: MaterialSwitch? = null
    private var forceKeyboardSwitch: MaterialSwitch? = null
    private var keyboardNotificationSwitch: MaterialSwitch? = null

    // Dropdowns
    private var ctrlAOverrideDropdown: MaterialAutoCompleteTextView? = null
    private var slideKeysDropdown: MaterialAutoCompleteTextView? = null
    private var popupContentDropdown: MaterialAutoCompleteTextView? = null
    private var ctrlKeyCodeDropdown: MaterialAutoCompleteTextView? = null
    private var altKeyCodeDropdown: MaterialAutoCompleteTextView? = null
    private var metaKeyCodeDropdown: MaterialAutoCompleteTextView? = null

    // Slider
    private var longPressSlider: Slider? = null
    private var longPressValue: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_input_behavior, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        try {
            // Initialize switches
            autoCapSwitch = view.findViewById(R.id.auto_cap_switch)
            connectbotTabSwitch = view.findViewById(R.id.connectbot_tab_switch)
            fullKeyboardPortraitSwitch = view.findViewById(R.id.full_keyboard_portrait_switch)
            capsLockSwitch = view.findViewById(R.id.caps_lock_switch)
            shiftLockModifiersSwitch = view.findViewById(R.id.shift_lock_modifiers_switch)
            fullscreenOverrideSwitch = view.findViewById(R.id.fullscreen_override_switch)
            forceKeyboardSwitch = view.findViewById(R.id.force_keyboard_switch)
            keyboardNotificationSwitch = view.findViewById(R.id.keyboard_notification_switch)

            // Initialize dropdowns
            ctrlAOverrideDropdown = view.findViewById(R.id.ctrl_a_override_dropdown)
            slideKeysDropdown = view.findViewById(R.id.slide_keys_dropdown)
            popupContentDropdown = view.findViewById(R.id.popup_content_dropdown)
            ctrlKeyCodeDropdown = view.findViewById(R.id.ctrl_key_code_dropdown)
            altKeyCodeDropdown = view.findViewById(R.id.alt_key_code_dropdown)
            metaKeyCodeDropdown = view.findViewById(R.id.meta_key_code_dropdown)

            // Initialize slider
            longPressSlider = view.findViewById(R.id.long_press_slider)
            longPressValue = view.findViewById(R.id.long_press_value)

            // Setup dropdown adapters
            setupDropdownAdapters()

            // Load current values
            loadPreferences()

            // Setup listeners
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPreferences() {
        autoCapSwitch?.isChecked = prefs.getBoolean("auto_cap", true)
        connectbotTabSwitch?.isChecked = prefs.getBoolean("connectbot_tab_hack", false)
        fullKeyboardPortraitSwitch?.isChecked = prefs.getBoolean("full_keyboard_in_portrait", false)
        capsLockSwitch?.isChecked = prefs.getBoolean("pref_caps_lock", true)
        shiftLockModifiersSwitch?.isChecked = prefs.getBoolean("pref_shift_lock_modifiers", false)
        fullscreenOverrideSwitch?.isChecked = prefs.getBoolean("fullscreen_override", false)
        forceKeyboardSwitch?.isChecked = prefs.getBoolean("force_keyboard_on", false)
        keyboardNotificationSwitch?.isChecked = prefs.getBoolean("keyboard_notification", false)

        // Load dropdown values
        ctrlAOverrideDropdown?.let {
            val value = prefs.getString("pref_ctrl_a_override", "0") ?: "0"
            it.setText(valueToEntry(value, CTRL_A_VALUES, CTRL_A_ENTRIES), false)
        }
        slideKeysDropdown?.let {
            val value = prefs.getString("pref_slide_keys_int", "0") ?: "0"
            it.setText(valueToEntry(value, SLIDE_KEYS_VALUES, SLIDE_KEYS_ENTRIES), false)
        }
        popupContentDropdown?.let {
            val value = prefs.getString("pref_popup_content", "1") ?: "1"
            it.setText(valueToEntry(value, POPUP_CONTENT_VALUES, POPUP_CONTENT_ENTRIES), false)
        }

        // Load key code dropdown values
        ctrlKeyCodeDropdown?.let {
            val value = prefs.getString("pref_chording_ctrl_key", "0") ?: "0"
            it.setText(valueToEntry(value, CTRL_KEY_VALUES, CTRL_KEY_ENTRIES), false)
        }
        altKeyCodeDropdown?.let {
            val value = prefs.getString("pref_chording_alt_key", "0") ?: "0"
            it.setText(valueToEntry(value, ALT_KEY_VALUES, ALT_KEY_ENTRIES), false)
        }
        metaKeyCodeDropdown?.let {
            val value = prefs.getString("pref_chording_meta_key", "0") ?: "0"
            it.setText(valueToEntry(value, META_KEY_VALUES, META_KEY_ENTRIES), false)
        }

        longPressSlider?.let { slider ->
            longPressValue?.let { valueText ->
                try {
                    // pref_long_press_duration is stored as String in original prefs
                    val longPressStr = prefs.getString("pref_long_press_duration", "400") ?: "400"
                    val longPressDuration = longPressStr.toInt().coerceIn(100, 1000)
                    slider.value = longPressDuration.toFloat()
                    valueText.text = "$longPressDuration ms"
                } catch (e: Exception) {
                    e.printStackTrace()
                    slider.value = 400f
                    valueText.text = "400 ms"
                }
            }
        }
    }

    /**
     * Setup ArrayAdapter for all dropdown menus.
     */
    private fun setupDropdownAdapters() {
        val createAdapter = { entries: Array<String> ->
            ArrayAdapter(requireContext(), R.layout.item_dropdown, entries)
        }

        ctrlAOverrideDropdown?.setAdapter(createAdapter(CTRL_A_ENTRIES))
        slideKeysDropdown?.setAdapter(createAdapter(SLIDE_KEYS_ENTRIES))
        popupContentDropdown?.setAdapter(createAdapter(POPUP_CONTENT_ENTRIES))
        ctrlKeyCodeDropdown?.setAdapter(createAdapter(CTRL_KEY_ENTRIES))
        altKeyCodeDropdown?.setAdapter(createAdapter(ALT_KEY_ENTRIES))
        metaKeyCodeDropdown?.setAdapter(createAdapter(META_KEY_ENTRIES))
    }

    /**
     * Convert a preference value to its corresponding display entry.
     */
    private fun valueToEntry(value: String, values: Array<String>, entries: Array<String>): String {
        val index = values.indexOf(value)
        return if (index >= 0) entries[index] else entries[0]
    }

    /**
     * Convert a display entry to its corresponding preference value.
     */
    private fun entryToValue(entry: String, entries: Array<String>, values: Array<String>): String {
        val index = entries.indexOf(entry)
        return if (index >= 0) values[index] else values[0]
    }

    private fun setupListeners() {
        autoCapSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_cap", isChecked).apply()
        }

        connectbotTabSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("connectbot_tab_hack", isChecked).apply()
        }

        fullKeyboardPortraitSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("full_keyboard_in_portrait", isChecked).apply()
        }

        capsLockSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_caps_lock", isChecked).apply()
        }

        shiftLockModifiersSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_shift_lock_modifiers", isChecked).apply()
        }

        fullscreenOverrideSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("fullscreen_override", isChecked).apply()
        }

        forceKeyboardSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("force_keyboard_on", isChecked).apply()
        }

        keyboardNotificationSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("keyboard_notification", isChecked).apply()
        }

        // Dropdown listeners
        ctrlAOverrideDropdown?.setOnItemClickListener { parent, _, position, _ ->
            val entry = parent.getItemAtPosition(position) as String
            val value = entryToValue(entry, CTRL_A_ENTRIES, CTRL_A_VALUES)
            prefs.edit().putString("pref_ctrl_a_override", value).apply()
        }

        slideKeysDropdown?.setOnItemClickListener { parent, _, position, _ ->
            val entry = parent.getItemAtPosition(position) as String
            val value = entryToValue(entry, SLIDE_KEYS_ENTRIES, SLIDE_KEYS_VALUES)
            prefs.edit().putString("pref_slide_keys_int", value).apply()
        }

        popupContentDropdown?.setOnItemClickListener { parent, _, position, _ ->
            val entry = parent.getItemAtPosition(position) as String
            val value = entryToValue(entry, POPUP_CONTENT_ENTRIES, POPUP_CONTENT_VALUES)
            prefs.edit().putString("pref_popup_content", value).apply()
        }

        // Key code dropdown listeners
        ctrlKeyCodeDropdown?.setOnItemClickListener { parent, _, position, _ ->
            val entry = parent.getItemAtPosition(position) as String
            val value = entryToValue(entry, CTRL_KEY_ENTRIES, CTRL_KEY_VALUES)
            prefs.edit().putString("pref_chording_ctrl_key", value).apply()
        }

        altKeyCodeDropdown?.setOnItemClickListener { parent, _, position, _ ->
            val entry = parent.getItemAtPosition(position) as String
            val value = entryToValue(entry, ALT_KEY_ENTRIES, ALT_KEY_VALUES)
            prefs.edit().putString("pref_chording_alt_key", value).apply()
        }

        metaKeyCodeDropdown?.setOnItemClickListener { parent, _, position, _ ->
            val entry = parent.getItemAtPosition(position) as String
            val value = entryToValue(entry, META_KEY_ENTRIES, META_KEY_VALUES)
            prefs.edit().putString("pref_chording_meta_key", value).apply()
        }

        longPressSlider?.let { slider ->
            longPressValue?.let { valueText ->
                slider.addOnChangeListener { _, value, fromUser ->
                    val duration = value.toInt()
                    valueText.text = "$duration ms"
                    if (fromUser) {
                        // Store as String to match original SeekBarPreferenceString
                        prefs.edit().putString("pref_long_press_duration", duration.toString()).apply()
                    }
                }
            }
        }
    }

    companion object {
        // Ctrl-A Override options
        private val CTRL_A_ENTRIES = arrayOf(
            "Disable Ctrl-A, use Ctrl-Alt-A instead",
            "Disable Ctrl-A completely",
            "Use Ctrl-A (no override)"
        )
        private val CTRL_A_VALUES = arrayOf("0", "1", "2")

        // Sliding key events options
        private val SLIDE_KEYS_ENTRIES = arrayOf(
            "Ignore keys during sliding (Recommended)",
            "Send first key touched",
            "Send last key touched",
            "Send first and last key",
            "Send all keys touched"
        )
        private val SLIDE_KEYS_VALUES = arrayOf("0", "1", "2", "3", "4")

        // Popup content options
        private val POPUP_CONTENT_ENTRIES = arrayOf(
            "No popups",
            "No popups, use auto-repeat",
            "Unique only: 3 → ³, e → é",
            "Add shifted: 3 → #³, e → é",
            "Add upper: 3 → #³, e → Eé",
            "Add self: 3 → 3#³, e → eEé"
        )
        private val POPUP_CONTENT_VALUES = arrayOf("256", "768", "0", "1", "3", "7")

        // Ctrl key code options
        private val CTRL_KEY_ENTRIES = arrayOf(
            "None (ignored when not modifying)",
            "Left Ctrl",
            "Right Ctrl"
        )
        private val CTRL_KEY_VALUES = arrayOf("0", "113", "114")

        // Alt key code options
        private val ALT_KEY_ENTRIES = arrayOf(
            "None (ignored when not modifying)",
            "Left Alt",
            "Right Alt"
        )
        private val ALT_KEY_VALUES = arrayOf("0", "57", "58")

        // Meta key code options
        private val META_KEY_ENTRIES = arrayOf(
            "None (ignored when not modifying)",
            "Left Meta",
            "Right Meta"
        )
        private val META_KEY_VALUES = arrayOf("0", "117", "118")
    }
}
