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

import android.app.Dialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.pocketworkstation.pckeyboard.KeyboardSwitcher
import org.pocketworkstation.pckeyboard.R

/**
 * Fragment for selecting Tokyo Night theme variants.
 * Shows a loading spinner during theme switch for smooth UX.
 */
class ThemeSelectionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ThemeAdapter
    private lateinit var prefs: SharedPreferences
    private var loadingDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_theme_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        recyclerView = view.findViewById(R.id.theme_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Get current theme
        val currentTheme = prefs.getString(KeyboardSwitcher.PREF_KEYBOARD_LAYOUT, "10") ?: "10"

        // Create theme list with prominent Tokyo Night colors for each variant
        val themes = listOf(
            ThemeItem("10", "Tokyo Night Storm",
                "Deep blue with vibrant accents",
                R.color.tn_storm_bg, R.color.tn_storm_bg_dark, R.color.tn_storm_terminal_black,
                R.color.tn_storm_blue, R.color.tn_storm_fg, R.color.tn_storm_green),
            ThemeItem("11", "Tokyo Night Night",
                "Pure dark with neon highlights",
                R.color.tn_night_bg, R.color.tn_night_bg_dark, R.color.tn_night_terminal_black,
                R.color.tn_night_cyan, R.color.tn_night_fg, R.color.tn_night_magenta),
            ThemeItem("13", "Tokyo Night Moon",
                "Moonlit blue with soft contrast",
                R.color.tn_moon_bg, R.color.tn_moon_bg_dark, R.color.tn_moon_terminal_black,
                R.color.tn_moon_purple, R.color.tn_moon_fg, R.color.tn_moon_green),
            ThemeItem("12", "Tokyo Night Day",
                "Light mode with vibrant colors",
                R.color.tn_day_bg, R.color.tn_day_bg_dark, R.color.tn_day_terminal_black,
                R.color.tn_day_blue, R.color.tn_day_fg, R.color.tn_day_green)
        )

        adapter = ThemeAdapter(themes, currentTheme, ::onThemeSelected)
        recyclerView.adapter = adapter
    }

    private fun onThemeSelected(themeId: String) {
        // Show loading spinner - it will stay visible until activity recreates
        showLoadingDialog()

        // Save theme preference
        prefs.edit()
            .putString(KeyboardSwitcher.PREF_KEYBOARD_LAYOUT, themeId)
            .apply()

        // Post recreate to next frame to ensure spinner is visible
        // The dialog will be dismissed automatically when activity is destroyed
        Handler(Looper.getMainLooper()).post {
            activity?.recreate()
        }
    }

    /**
     * Show a modern circular loading spinner during theme switch.
     */
    private fun showLoadingDialog() {
        val context = context ?: return

        loadingDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)

            // Create a circular progress indicator
            val progressIndicator = CircularProgressIndicator(requireContext()).apply {
                isIndeterminate = true
                // Use theme-aware colorPrimary instead of hardcoded color
                val typedValue = TypedValue()
                requireContext().theme.resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary, typedValue, true)
                setIndicatorColor(typedValue.data)
                trackThickness = 8
                indicatorSize = 64
            }

            // Wrap in a card for better appearance
            val cardView = MaterialCardView(requireContext()).apply {
                cardElevation = 16f
                radius = 24f
                setContentPadding(48, 48, 48, 48)
                addView(progressIndicator)
            }

            setContentView(cardView)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }
    }

    /**
     * Dismiss the loading dialog if showing.
     */
    private fun dismissLoadingDialog() {
        loadingDialog?.takeIf { it.isShowing }?.dismiss()
        loadingDialog = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissLoadingDialog()
    }

    /**
     * Data class for theme items with 6-color preview.
     */
    data class ThemeItem(
        val id: String,
        val name: String,
        val description: String,
        val baseColorRes: Int,
        val alphaColorRes: Int,
        val modColorRes: Int,
        val highlightColorRes: Int,
        val textColorRes: Int,
        val accentColorRes: Int
    )
}
