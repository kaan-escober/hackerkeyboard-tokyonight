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

import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.pocketworkstation.pckeyboard.R

/**
 * RecyclerView adapter for Tokyo Night theme selection.
 * Uses RadioButton with theme-specific accent colors for selected state.
 */
class ThemeAdapter(
    private val themes: List<ThemeSelectionFragment.ThemeItem>,
    private var selectedThemeId: String,
    private val listener: OnThemeSelectedListener
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    fun interface OnThemeSelectedListener {
        fun onThemeSelected(themeId: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_theme, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themes[position]
        holder.bind(theme, theme.id == selectedThemeId)
    }

    override fun getItemCount(): Int = themes.size

    inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val nameTextView: TextView = itemView.findViewById(R.id.theme_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.theme_description)
        private val themeRadio: RadioButton = itemView.findViewById(R.id.theme_radio)
        private val colorBase: View? = itemView.findViewById(R.id.color_base)
        private val colorAlpha: View? = itemView.findViewById(R.id.color_alpha)
        private val colorMod: View? = itemView.findViewById(R.id.color_mod)
        private val colorHighlight: View? = itemView.findViewById(R.id.color_highlight)
        private val colorText: View? = itemView.findViewById(R.id.color_text)
        private val colorAccent: View? = itemView.findViewById(R.id.color_accent)

        fun bind(theme: ThemeSelectionFragment.ThemeItem, isSelected: Boolean) {
            nameTextView.text = theme.name
            descriptionTextView.text = theme.description

            // Set radio button state with theme-specific accent color
            themeRadio.isChecked = isSelected

            // Get theme-specific accent color
            val accentColor = getThemeAccentColor(theme.id)

            if (isSelected) {
                // Apply theme-specific accent color to radio button
                themeRadio.buttonTintList = ColorStateList.valueOf(accentColor)
            } else {
                // Default gray for unselected
                themeRadio.buttonTintList = null
            }

            // Set 6-color preview grid
            colorBase?.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, theme.baseColorRes))
            colorAlpha?.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, theme.alphaColorRes))
            colorMod?.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, theme.modColorRes))
            colorHighlight?.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, theme.highlightColorRes))
            colorText?.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, theme.textColorRes))
            colorAccent?.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, theme.accentColorRes))

            // Set card selection state with theme-specific accent border
            if (isSelected) {
                cardView.strokeWidth = 3
                cardView.setStrokeColor(accentColor)
            } else {
                cardView.strokeWidth = 1
                // Get outline color from theme for unselected state
                val outlineValue = TypedValue()
                itemView.context.theme.resolveAttribute(
                    com.google.android.material.R.attr.colorOutline, outlineValue, true)
                cardView.setStrokeColor(outlineValue.data)
            }

            // Handle click
            itemView.setOnClickListener {
                if (theme.id != selectedThemeId) {
                    val oldThemeId = selectedThemeId
                    selectedThemeId = theme.id

                    // Notify adapter to update UI
                    notifyItemChanged(adapterPosition)
                    themes.indexOfFirst { it.id == oldThemeId }.takeIf { it >= 0 }?.let {
                        notifyItemChanged(it)
                    }

                    // Notify listener
                    listener.onThemeSelected(theme.id)
                }
            }
        }

        /**
         * Get theme-specific accent color for the radio button and border.
         * Storm = Green, Night = Blue, Moon = Pink, Day = Purple
         */
        private fun getThemeAccentColor(themeId: String): Int {
            val colorRes = when (themeId) {
                "10" -> R.color.tn_storm_green   // Storm - Green
                "11" -> R.color.tn_night_blue     // Night - Blue
                "13" -> R.color.tn_moon_purple    // Moon - Pink/Magenta
                "12" -> R.color.tn_day_magenta    // Day - Purple
                else -> R.color.tn_storm_green
            }
            return ContextCompat.getColor(itemView.context, colorRes)
        }
    }
}
