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
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.pocketworkstation.pckeyboard.material.MainHomeFragment
import org.pocketworkstation.pckeyboard.material.MainAboutFragment
import org.pocketworkstation.pckeyboard.material.SettingsTabFragment

/**
 * Modern Material 3 Main Activity with bottom navigation.
 * Uses ViewPager2 for smooth, buttery transitions between Home, Settings, and About.
 */
class MaterialMainActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private var currentThemeId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val layoutPref = prefs.getString(KeyboardSwitcher.PREF_KEYBOARD_LAYOUT, "10") ?: "10"
        currentThemeId = getThemeIdForLayout(layoutPref)
        setTheme(currentThemeId)

        super.onCreate(savedInstanceState)
        
        // Fix status bar icons for light theme
        if (layoutPref == "12") { // Day theme
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or 
                                               View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        
        setContentView(R.layout.activity_material_main_new)
        setupUI()
        
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    private fun getThemeIdForLayout(layoutPref: String): Int {
        return try {
            when (layoutPref.toInt()) {
                11 -> R.style.Theme_HackerKeyboard_Material3_Night
                12 -> R.style.Theme_HackerKeyboard_Material3_Day
                13 -> R.style.Theme_HackerKeyboard_Material3_Moon
                10 -> R.style.Theme_HackerKeyboard_Material3_Storm
                else -> R.style.Theme_HackerKeyboard_Material3_Storm
            }
        } catch (e: Exception) {
            R.style.Theme_HackerKeyboard_Material3_Storm
        }
    }

    private fun setupUI() {
        viewPager = findViewById(R.id.main_view_pager)
        bottomNav = findViewById(R.id.bottom_navigation)

        viewPager.adapter = MainPagerAdapter(this)
        viewPager.isUserInputEnabled = false // Only nav via bottom bar for "app" feel
        viewPager.offscreenPageLimit = 3

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> viewPager.setCurrentItem(0, true)
                R.id.nav_settings -> viewPager.setCurrentItem(1, true)
                R.id.nav_about -> viewPager.setCurrentItem(2, true)
            }
            true
        }

        // Setup toolbar title sync
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val title = when (position) {
                    0 -> "Hacker's Keyboard"
                    1 -> "Settings"
                    2 -> "About"
                    else -> ""
                }
                supportActionBar?.title = title
                bottomNav.menu.getItem(position).isChecked = true
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (KeyboardSwitcher.PREF_KEYBOARD_LAYOUT == key) {
            val layoutPref = sharedPreferences?.getString(key, "10") ?: "10"
            val newThemeId = getThemeIdForLayout(layoutPref)
            if (newThemeId != currentThemeId) {
                currentThemeId = newThemeId
                recreate()
            }
        }
    }

    private class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MainHomeFragment()
                1 -> SettingsTabFragment()
                2 -> MainAboutFragment()
                else -> MainHomeFragment()
            }
        }
    }
}
