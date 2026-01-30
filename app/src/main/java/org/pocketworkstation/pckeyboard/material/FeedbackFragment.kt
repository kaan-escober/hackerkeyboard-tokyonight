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

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import org.pocketworkstation.pckeyboard.R

/**
 * Fragment for configuring keyboard feedback settings (vibration and sound).
 */
class FeedbackFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    // Switches
    private var vibrateSwitch: MaterialSwitch? = null
    private var soundSwitch: MaterialSwitch? = null

    // Sliders
    private var vibrateLengthSlider: Slider? = null
    private var vibrateLengthValue: TextView? = null
    private var clickVolumeSlider: Slider? = null
    private var clickVolumeValue: TextView? = null

    // Click method buttons
    private var clickMethodGroup: MaterialButtonToggleGroup? = null
    private var clickMethodStandard: MaterialButton? = null
    private var clickMethodAndroid: MaterialButton? = null
    private var clickMethodNone: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Initialize vibrator and audio manager for feedback preview
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        try {
            // Initialize switches
            vibrateSwitch = view.findViewById(R.id.vibrate_switch)
            soundSwitch = view.findViewById(R.id.sound_switch)

            // Initialize sliders
            vibrateLengthSlider = view.findViewById(R.id.vibrate_length_slider)
            vibrateLengthValue = view.findViewById(R.id.vibrate_length_value)
            clickVolumeSlider = view.findViewById(R.id.click_volume_slider)
            clickVolumeValue = view.findViewById(R.id.click_volume_value)

            // Initialize click method buttons
            clickMethodGroup = view.findViewById(R.id.click_method_group)
            clickMethodStandard = view.findViewById(R.id.click_method_standard)
            clickMethodAndroid = view.findViewById(R.id.click_method_android)
            clickMethodNone = view.findViewById(R.id.click_method_none)

            // Load current values
            loadPreferences()

            // Setup listeners
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPreferences() {
        vibrateSwitch?.isChecked = prefs.getBoolean("vibrate_on", true)
        soundSwitch?.isChecked = prefs.getBoolean("sound_on", false)

        vibrateLengthSlider?.let { slider ->
            vibrateLengthValue?.let { valueText ->
                try {
                    var vibrateLength = 40 // default

                    // Try to read as String first (VibratePreference stores as String)
                    try {
                        val stringValue = prefs.getString("vibrate_len", null)
                        if (!stringValue.isNullOrEmpty()) {
                            // Parse float and convert to int, handling suffixes like " ms"
                            val numericPart = stringValue.replace(Regex("[^0-9.]"), "")
                            vibrateLength = numericPart.toFloat().toInt()
                        }
                    } catch (e: NumberFormatException) {
                        // Fall back to reading as int for backward compatibility
                        try {
                            vibrateLength = prefs.getInt("vibrate_len", 40)
                        } catch (ce: ClassCastException) {
                            // If both fail, use default
                            vibrateLength = 40
                        }
                    }

                    // Clamp value to slider range (5-200)
                    vibrateLength = vibrateLength.coerceIn(5, 200)
                    slider.value = vibrateLength.toFloat()
                    valueText.text = "$vibrateLength ms"
                } catch (e: Exception) {
                    e.printStackTrace()
                    slider.value = 40f
                    valueText.text = "40 ms"
                }
            }
        }

        // Load click method preference
        clickMethodGroup?.let { group ->
            try {
                when (prefs.getString("pref_click_method", "0")) {
                    "0" -> group.check(R.id.click_method_standard)
                    "1" -> group.check(R.id.click_method_android)
                    "2" -> group.check(R.id.click_method_none)
                    else -> group.check(R.id.click_method_standard)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                group.check(R.id.click_method_standard)
            }
        }

        // Load click volume preference (stored as float 0.0-1.0 in legacy)
        clickVolumeSlider?.let { slider ->
            clickVolumeValue?.let { valueText ->
                try {
                    var clickVolume = 0.2f // default 20%

                    // Try to read as String first (legacy stores as float string like "0.2")
                    try {
                        val stringValue = prefs.getString("pref_click_volume", null)
                        if (!stringValue.isNullOrEmpty()) {
                            clickVolume = stringValue.toFloat()
                        }
                    } catch (e: NumberFormatException) {
                        clickVolume = 0.2f
                    }

                    // Convert to percentage for display (0-100)
                    val displayPercent = (clickVolume * 100).toInt().coerceIn(0, 100)
                    slider.value = displayPercent.toFloat()
                    valueText.text = "$displayPercent%"
                } catch (e: Exception) {
                    e.printStackTrace()
                    slider.value = 20f
                    valueText.text = "20%"
                }
            }
        }
    }

    private fun setupListeners() {
        vibrateSwitch?.let { switch ->
            vibrateLengthSlider?.let { slider ->
                switch.setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean("vibrate_on", isChecked).apply()
                    slider.isEnabled = isChecked
                    // Apply visual dimming when disabled
                    slider.alpha = if (isChecked) 1.0f else 0.4f
                    vibrateLengthValue?.alpha = if (isChecked) 1.0f else 0.4f
                }
            }
        }

        soundSwitch?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_on", isChecked).apply()
            // Apply visual dimming to click method and volume controls when sound is disabled
            clickMethodGroup?.let {
                it.isEnabled = isChecked
                it.alpha = if (isChecked) 1.0f else 0.4f
            }
            clickVolumeSlider?.let {
                it.isEnabled = isChecked
                it.alpha = if (isChecked) 1.0f else 0.4f
            }
            clickVolumeValue?.alpha = if (isChecked) 1.0f else 0.4f
        }

        vibrateLengthSlider?.let { slider ->
            vibrateLengthValue?.let { valueText ->
                slider.addOnChangeListener { _, value, fromUser ->
                    val length = value.toInt()
                    valueText.text = "$length ms"
                    if (fromUser) {
                        prefs.edit().putString("vibrate_len", length.toString()).apply()
                        // Trigger vibration preview while sliding
                        triggerVibration(length)
                    }
                }

                // Set initial enabled state and alpha for vibrate length slider
                vibrateSwitch?.let { switch ->
                    val isEnabled = switch.isChecked
                    slider.isEnabled = isEnabled
                    slider.alpha = if (isEnabled) 1.0f else 0.4f
                    valueText.alpha = if (isEnabled) 1.0f else 0.4f
                }
            }
        }

        // Setup click method button group listener
        clickMethodGroup?.let { group ->
            group.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    val value = when (checkedId) {
                        R.id.click_method_standard -> "0"
                        R.id.click_method_android -> "1"
                        R.id.click_method_none -> "2"
                        else -> "0"
                    }
                    prefs.edit().putString("pref_click_method", value).apply()
                }
            }

            // Set initial enabled state and alpha based on sound switch
            soundSwitch?.let { switch ->
                val isEnabled = switch.isChecked
                group.isEnabled = isEnabled
                group.alpha = if (isEnabled) 1.0f else 0.4f
            }
        }

        // Setup click volume slider listener
        clickVolumeSlider?.let { slider ->
            clickVolumeValue?.let { valueText ->
                slider.addOnChangeListener { _, value, fromUser ->
                    val volume = value.toInt()
                    valueText.text = "$volume%"
                    if (fromUser) {
                        // Store as float 0.0-1.0 to match legacy format
                        val floatValue = volume / 100.0f
                        prefs.edit().putString("pref_click_volume", floatValue.toString()).apply()
                        // Play click sound preview at the new volume
                        playClickSound(floatValue)
                    }
                }

                // Set initial enabled state and alpha based on sound switch
                soundSwitch?.let { switch ->
                    val isEnabled = switch.isChecked
                    slider.isEnabled = isEnabled
                    slider.alpha = if (isEnabled) 1.0f else 0.4f
                    valueText.alpha = if (isEnabled) 1.0f else 0.4f
                }
            }
        }
    }

    /**
     * Trigger a vibration with the specified duration for preview.
     */
    private fun triggerVibration(durationMs: Int) {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs.toLong())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Play a click sound at the specified volume for preview.
     */
    private fun playClickSound(volume: Float) {
        val audio = audioManager ?: return

        try {
            // Use system click sound effect
            audio.playSoundEffect(AudioManager.FX_KEY_CLICK, volume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
