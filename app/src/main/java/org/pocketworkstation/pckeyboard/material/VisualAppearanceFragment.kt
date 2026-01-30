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

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import org.pocketworkstation.pckeyboard.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Fragment for configuring keyboard visual appearance settings.
 */
class VisualAppearanceFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    // Switches
    private var popupSwitch: MaterialSwitch? = null
    private var fullscreenOverrideSwitch: MaterialSwitch? = null
    private var forceKeyboardOnSwitch: MaterialSwitch? = null
    private var keyboardNotificationSwitch: MaterialSwitch? = null

    // Sliders
    private var heightPortraitSlider: Slider? = null
    private var heightPortraitValue: TextView? = null
    private var heightLandscapeSlider: Slider? = null
    private var heightLandscapeValue: TextView? = null
    private var labelScaleSlider: Slider? = null
    private var labelScaleValue: TextView? = null
    private var candidateScaleSlider: Slider? = null
    private var candidateScaleValue: TextView? = null
    private var topRowScaleSlider: Slider? = null
    private var topRowScaleValue: TextView? = null

    // Keyboard Mode Toggle Groups
    private var keyboardModePortraitGroup: MaterialButtonToggleGroup? = null
    private var keyboardModeLandscapeGroup: MaterialButtonToggleGroup? = null

    // Hint Mode Toggle Group
    private var hintModeGroup: MaterialButtonToggleGroup? = null

    // Font Toggle Group
    private var keyboardFontGroup: MaterialButtonToggleGroup? = null
    private var customFontStatus: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                result.data?.data?.let { uri ->
                    copyCustomFont(uri)
                }
            } else {
                // User cancelled, revert to stored preference
                loadPreferences()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_visual_appearance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        try {
            // Initialize switches
            popupSwitch = view.findViewById(R.id.popup_switch)
            fullscreenOverrideSwitch = view.findViewById(R.id.fullscreen_override_switch)
            forceKeyboardOnSwitch = view.findViewById(R.id.force_keyboard_on_switch)
            keyboardNotificationSwitch = view.findViewById(R.id.keyboard_notification_switch)

            // Initialize sliders
            heightPortraitSlider = view.findViewById(R.id.height_portrait_slider)
            heightPortraitValue = view.findViewById(R.id.height_portrait_value)
            heightLandscapeSlider = view.findViewById(R.id.height_landscape_slider)
            heightLandscapeValue = view.findViewById(R.id.height_landscape_value)
            labelScaleSlider = view.findViewById(R.id.label_scale_slider)
            labelScaleValue = view.findViewById(R.id.label_scale_value)
            candidateScaleSlider = view.findViewById(R.id.candidate_scale_slider)
            candidateScaleValue = view.findViewById(R.id.candidate_scale_value)
            topRowScaleSlider = view.findViewById(R.id.top_row_scale_slider)
            topRowScaleValue = view.findViewById(R.id.top_row_scale_value)

            // Initialize keyboard mode toggle groups
            keyboardModePortraitGroup = view.findViewById(R.id.keyboard_mode_portrait_group)
            keyboardModeLandscapeGroup = view.findViewById(R.id.keyboard_mode_landscape_group)

            // Initialize hint mode toggle group
            hintModeGroup = view.findViewById(R.id.hint_mode_group)

            // Initialize font toggle group
            keyboardFontGroup = view.findViewById(R.id.keyboard_font_group)
            customFontStatus = view.findViewById(R.id.custom_font_status)

            // Load current values
            loadPreferences()

            // Setup listeners
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPreferences() {
        popupSwitch?.isChecked = prefs.getBoolean("popup_on", true)
        fullscreenOverrideSwitch?.isChecked = prefs.getBoolean("fullscreen_override", false)
        forceKeyboardOnSwitch?.isChecked = prefs.getBoolean("force_keyboard_on", false)
        keyboardNotificationSwitch?.isChecked = prefs.getBoolean("keyboard_notification", false)

        heightPortraitSlider?.let { slider ->
            heightPortraitValue?.let { valueText ->
                try {
                    val heightStr = prefs.getString("settings_height_portrait", "35") ?: "35"
                    val heightPortrait = heightStr.toInt().coerceIn(15, 75)
                    slider.value = heightPortrait.toFloat()
                    valueText.text = "$heightPortrait%"
                } catch (e: Exception) {
                    e.printStackTrace()
                    slider.value = 35f
                    valueText.text = "35%"
                }
            }
        }

        heightLandscapeSlider?.let { slider ->
            heightLandscapeValue?.let { valueText ->
                try {
                    val heightStr = prefs.getString("settings_height_landscape", "50") ?: "50"
                    val heightLandscape = heightStr.toInt().coerceIn(15, 75)
                    slider.value = heightLandscape.toFloat()
                    valueText.text = "$heightLandscape%"
                } catch (e: Exception) {
                    e.printStackTrace()
                    slider.value = 50f
                    valueText.text = "50%"
                }
            }
        }

        // Load label scale (stored as String, range 0.5-2.0)
        labelScaleSlider?.let { slider ->
            labelScaleValue?.let { valueText ->
                try {
                    val scaleStr = prefs.getString("pref_label_scale_v2", "1.0") ?: "1.0"
                    val scale = scaleStr.toFloat().coerceIn(0.5f, 2.0f)
                    slider.value = scale
                    valueText.text = "${(scale * 100).toInt()}%"
                } catch (e: Exception) {
                    e.printStackTrace()
                    slider.value = 1.0f
                    valueText.text = "100%"
                }
            }
        }

        // Load candidate scale (stored as String, range 0.5-2.0)
        candidateScaleSlider?.let { slider ->
            candidateScaleValue?.let { valueText ->
                try {
                    val scaleStr = prefs.getString("pref_candidate_scale_v2", "1.0") ?: "1.0"
                    val scale = scaleStr.toFloat().coerceIn(0.5f, 2.0f)
                    slider.value = scale
                    valueText.text = "${(scale * 100).toInt()}%"
                } catch (e: Exception) {
                    e.printStackTrace()
                    slider.value = 1.0f
                    valueText.text = "100%"
                }
            }
        }

        // Load top row scale (stored as String, range 0.5-2.0)
        topRowScaleSlider?.let { slider ->
            topRowScaleValue?.let { valueText ->
                try {
                    val scaleStr = prefs.getString("pref_top_row_scale", "1.0") ?: "1.0"
                    val scale = scaleStr.toFloat().coerceIn(0.5f, 2.0f)
                    slider.value = scale
                    valueText.text = "${(scale * 100).toInt()}%"
                } catch (e: Exception) {
                    e.printStackTrace()
                    slider.value = 1.0f
                    valueText.text = "100%"
                }
            }
        }

        // Load keyboard mode portrait (0=Auto, 1=4-row, 2=5-row)
        keyboardModePortraitGroup?.let { group ->
            try {
                val modeStr = prefs.getString("pref_keyboard_mode_portrait", "0") ?: "0"
                selectKeyboardModePortrait(modeStr.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
                selectKeyboardModePortrait(0) // Default to Auto
            }
        }

        // Load keyboard mode landscape (0=Auto, 1=4-row, 2=5-row)
        keyboardModeLandscapeGroup?.let { group ->
            try {
                val modeStr = prefs.getString("pref_keyboard_mode_landscape", "0") ?: "0"
                selectKeyboardModeLandscape(modeStr.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
                selectKeyboardModeLandscape(0) // Default to Auto
            }
        }

        // Load hint mode (0=Off, 1=On, 2=Preview)
        hintModeGroup?.let { group ->
            try {
                val hintStr = prefs.getString("pref_hint_mode", "1") ?: "1"
                selectHintMode(hintStr.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
                selectHintMode(1) // Default to On
            }
        }

        // Load font mode (0=Code, 4=Custom)
        keyboardFontGroup?.let { group ->
            try {
                val fontStr = prefs.getString("pref_keyboard_font", "0") ?: "0"
                selectFontMode(fontStr.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
                selectFontMode(0) // Default to Code
            }
        }
    }

    private fun selectKeyboardModePortrait(mode: Int) {
        keyboardModePortraitGroup?.clearChecked()
        val buttonId = when (mode) {
            0 -> R.id.keyboard_mode_portrait_auto
            1 -> R.id.keyboard_mode_portrait_4row
            2 -> R.id.keyboard_mode_portrait_5row
            else -> R.id.keyboard_mode_portrait_auto
        }
        keyboardModePortraitGroup?.check(buttonId)
    }

    private fun selectKeyboardModeLandscape(mode: Int) {
        keyboardModeLandscapeGroup?.clearChecked()
        val buttonId = when (mode) {
            0 -> R.id.keyboard_mode_landscape_auto
            1 -> R.id.keyboard_mode_landscape_4row
            2 -> R.id.keyboard_mode_landscape_5row
            else -> R.id.keyboard_mode_landscape_auto
        }
        keyboardModeLandscapeGroup?.check(buttonId)
    }

    private fun selectHintMode(mode: Int) {
        hintModeGroup?.clearChecked()
        val buttonId = when (mode) {
            0 -> R.id.hint_mode_off
            1 -> R.id.hint_mode_on
            2 -> R.id.hint_mode_preview
            else -> R.id.hint_mode_on
        }
        hintModeGroup?.check(buttonId)
    }

    private fun selectFontMode(mode: Int) {
        keyboardFontGroup?.clearChecked()
        val buttonId = when (mode) {
            0 -> R.id.font_code
            4 -> R.id.font_custom
            else -> R.id.font_code // Fallback for removed options or errors
        }
        keyboardFontGroup?.check(buttonId)

        if (mode == 4) {
            updateCustomFontStatus(true)
        } else {
            customFontStatus?.visibility = View.GONE
        }
    }

    private fun copyCustomFont(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val outFile = File(requireContext().filesDir, "custom_font.ttf")
            val outputStream = FileOutputStream(outFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Persist the custom selection only after successful copy
            prefs.edit().putString("pref_keyboard_font", "4").apply()
            updateCustomFontStatus(true)

        } catch (e: IOException) {
            e.printStackTrace()
            updateCustomFontStatus(false)
            prefs.edit().putString("pref_keyboard_font", "0").apply() // Revert to Code
            loadPreferences()
        }
    }

    private fun updateCustomFontStatus(success: Boolean) {
        val status = customFontStatus ?: return
        status.visibility = View.VISIBLE
        
        if (success) {
            val fontFile = File(requireContext().filesDir, "custom_font.ttf")
            if (fontFile.exists()) {
                status.text = "Custom font loaded (${fontFile.length() / 1024} KB)"
                status.setTextColor(status.textColors.defaultColor)
            } else {
                status.text = "Custom font selected but file missing"
            }
        } else {
            status.text = "Failed to load font file"
            status.setTextColor(0xFFFF0000.toInt()) // Red
        }
    }

    private fun setupListeners() {
        popupSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("popup_on", isChecked).apply()
        }

        fullscreenOverrideSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("fullscreen_override", isChecked).apply()
        }

        forceKeyboardOnSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("force_keyboard_on", isChecked).apply()
        }

        keyboardNotificationSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("keyboard_notification", isChecked).apply()
        }

        heightPortraitSlider?.let { slider ->
            heightPortraitValue?.let { valueText ->
                slider.addOnChangeListener { _, value, fromUser ->
                    val height = value.toInt()
                    valueText.text = "$height%"
                    if (fromUser) {
                        prefs.edit().putString("settings_height_portrait", height.toString()).apply()
                    }
                }
            }
        }

        heightLandscapeSlider?.let { slider ->
            heightLandscapeValue?.let { valueText ->
                slider.addOnChangeListener { _, value, fromUser ->
                    val height = value.toInt()
                    valueText.text = "$height%"
                    if (fromUser) {
                        prefs.edit().putString("settings_height_landscape", height.toString()).apply()
                    }
                }
            }
        }

        // Label scale slider listener
        labelScaleSlider?.let { slider ->
            labelScaleValue?.let { valueText ->
                slider.addOnChangeListener { _, value, fromUser ->
                    valueText.text = "${(value * 100).toInt()}%"
                    if (fromUser) {
                        prefs.edit().putString("pref_label_scale_v2", String.format("%.1f", value)).apply()
                    }
                }
            }
        }

        // Candidate scale slider listener
        candidateScaleSlider?.let { slider ->
            candidateScaleValue?.let { valueText ->
                slider.addOnChangeListener { _, value, fromUser ->
                    valueText.text = "${(value * 100).toInt()}%"
                    if (fromUser) {
                        prefs.edit().putString("pref_candidate_scale_v2", String.format("%.1f", value)).apply()
                    }
                }
            }
        }

        // Top row scale slider listener
        topRowScaleSlider?.let { slider ->
            topRowScaleValue?.let { valueText ->
                slider.addOnChangeListener { _, value, fromUser ->
                    valueText.text = "${(value * 100).toInt()}%"
                    if (fromUser) {
                        prefs.edit().putString("pref_top_row_scale", String.format("%.1f", value)).apply()
                    }
                }
            }
        }

        // Keyboard mode portrait toggle group listener
        keyboardModePortraitGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.keyboard_mode_portrait_auto -> 0
                    R.id.keyboard_mode_portrait_4row -> 1
                    R.id.keyboard_mode_portrait_5row -> 2
                    else -> 0
                }
                prefs.edit().putString("pref_keyboard_mode_portrait", mode.toString()).apply()
            }
        }

        // Keyboard mode landscape toggle group listener
        keyboardModeLandscapeGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.keyboard_mode_landscape_auto -> 0
                    R.id.keyboard_mode_landscape_4row -> 1
                    R.id.keyboard_mode_landscape_5row -> 2
                    else -> 0
                }
                prefs.edit().putString("pref_keyboard_mode_landscape", mode.toString()).apply()
            }
        }

        // Hint mode toggle group listener
        hintModeGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.hint_mode_off -> 0
                    R.id.hint_mode_on -> 1
                    R.id.hint_mode_preview -> 2
                    else -> 1
                }
                prefs.edit().putString("pref_hint_mode", mode.toString()).apply()
            }
        }

        // Font mode toggle group listener
        keyboardFontGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.font_code -> 0
                    R.id.font_custom -> 4
                    else -> 0
                }
                
                if (mode == 4) {
                    // Launch file picker
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*" // Accept all, user must pick ttf/otf
                    }
                    filePickerLauncher.launch(intent)
                    return@addOnButtonCheckedListener // Don't save pref yet, wait for result
                }
                
                prefs.edit().putString("pref_keyboard_font", mode.toString()).apply()
                customFontStatus?.visibility = View.GONE
            }
        }
    }
}
