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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.pocketworkstation.pckeyboard.material.FeedbackFragment
import org.pocketworkstation.pckeyboard.material.GesturesFragment
import org.pocketworkstation.pckeyboard.material.InputBehaviorFragment
import org.pocketworkstation.pckeyboard.material.ThemeSelectionFragment
import org.pocketworkstation.pckeyboard.material.VisualAppearanceFragment

/**
 * Material 3 Settings Activity with Tokyo Night theme support.
 * Provides a modern, tabbed interface for keyboard configuration.
 * Optimized for smooth scrolling and instant theme changes.
 */
class MaterialSettingsActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: SettingsPagerAdapter
    private var currentThemeId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply Tokyo Night theme based on user preference
        currentThemeId = getThemeIdFromPrefs()
        setTheme(currentThemeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_material_settings)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.title = "Keyboard Settings"
        }

        // Initialize ViewPager2 and TabLayout
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)

        // Setup adapter
        pagerAdapter = SettingsPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // Performance optimization: Keep all fragments in memory to prevent flickering
        viewPager.offscreenPageLimit = NUM_PAGES

        // Add smooth fade crossfade transformer for professional transitions
        viewPager.setPageTransformer(FadeCrossfadeTransformer())

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Theme"
                    tab.setIcon(R.drawable.ic_palette)
                }
                1 -> {
                    tab.text = "Input"
                    tab.setIcon(R.drawable.ic_keyboard)
                }
                2 -> {
                    tab.text = "Visual"
                    tab.setIcon(R.drawable.ic_visibility)
                }
                3 -> {
                    tab.text = "Feedback"
                    tab.setIcon(R.drawable.ic_vibration)
                }
                4 -> {
                    tab.text = "Gestures"
                    tab.setIcon(R.drawable.ic_gesture)
                }
            }
        }.attach()

        // Register preference change listener for instant theme updates
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister preference change listener
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Instant theme change when user selects a new theme
        if (KeyboardSwitcher.PREF_KEYBOARD_LAYOUT == key) {
            val newThemeId = getThemeIdFromPrefs()
            if (newThemeId != currentThemeId) {
                currentThemeId = newThemeId
                // Recreate activity to apply new theme instantly
                recreate()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Get the theme resource ID based on user preference.
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
     * Smooth fade crossfade page transformer for ViewPager2.
     * Creates a professional fade transition between pages.
     */
    private class FadeCrossfadeTransformer : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            when {
                position < -1 || position > 1 -> {
                    // Page is off-screen
                    page.alpha = 0f
                }
                position <= 0 -> {
                    // Page is moving out to the left or is the current page
                    page.alpha = 1 + position
                    page.translationX = 0f
                }
                else -> {
                    // Page is moving in from the right
                    page.alpha = 1 - position
                    page.translationX = 0f
                }
            }
        }
    }

    /**
     * FragmentStateAdapter for managing the 5 settings screens.
     */
    private class SettingsPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ThemeSelectionFragment()
                1 -> InputBehaviorFragment()
                2 -> VisualAppearanceFragment()
                3 -> FeedbackFragment()
                4 -> GesturesFragment()
                else -> ThemeSelectionFragment()
            }
        }

        override fun getItemCount(): Int = NUM_PAGES
    }

    companion object {
        private const val TAG = "MaterialSettings"
        private const val NUM_PAGES = 5
    }
}
