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

package org.pocketworkstation.pckeyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton

/**
 * Material 3 styled main/welcome activity for Hacker's Keyboard.
 * Provides setup buttons for enabling keyboard, setting input method, and accessing settings.
 * Supports Tokyo Night theme variants with instant theme switching.
 */
class MaterialMainActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var currentThemeId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply Tokyo Night theme based on user preference BEFORE super.onCreate()
        currentThemeId = getThemeIdFromPrefs()
        setTheme(currentThemeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_material_main)

        // Register preference change listener for instant theme switching
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)

        setupUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Instant theme change when user selects a new theme
        if (KeyboardSwitcher.PREF_KEYBOARD_LAYOUT == key) {
            val newThemeId = getThemeIdFromPrefs()
            if (newThemeId != currentThemeId) {
                currentThemeId = newThemeId
                recreate()
            }
        }
    }

    /**
     * Get the Material 3 theme resource ID based on user's keyboard layout preference.
     */
    private fun getThemeIdFromPrefs(): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val layoutPref = prefs.getString(KeyboardSwitcher.PREF_KEYBOARD_LAYOUT, "10") ?: "10"

        return try {
            when (layoutPref.toInt()) {
                11 -> R.style.Theme_HackerKeyboard_Material3_Night
                12 -> R.style.Theme_HackerKeyboard_Material3_Day
                13 -> R.style.Theme_HackerKeyboard_Material3_Moon
                10 -> R.style.Theme_HackerKeyboard_Material3_Storm
                else -> R.style.Theme_HackerKeyboard_Material3_Storm
            }
        } catch (e: NumberFormatException) {
            R.style.Theme_HackerKeyboard_Material3_Storm
        }
    }

    /**
     * Setup all UI components and click listeners.
     */
    private fun setupUI() {
        // Setup version text
        findViewById<TextView>(R.id.version_text)?.let {
            it.text = "Version ${getString(R.string.auto_version)}"
        }

        // Setup Enable Keyboard button
        findViewById<MaterialButton>(R.id.btn_enable_keyboard)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        // Setup Set Input Method button
        findViewById<MaterialButton>(R.id.btn_set_input_method)?.setOnClickListener {
            val mgr = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            mgr?.showInputMethodPicker()
        }

        // Setup Settings button
        findViewById<MaterialButton>(R.id.btn_settings)?.setOnClickListener {
            startActivity(Intent(this, MaterialSettingsActivity::class.java))
        }

        // Setup GitHub link
        findViewById<LinearLayout>(R.id.github_link)?.setOnClickListener {
            val url = getString(R.string.about_github_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }
}
