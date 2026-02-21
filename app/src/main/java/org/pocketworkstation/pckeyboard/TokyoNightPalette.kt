/*
 * Copyright (C) 2024 Tokyo Night Theme Contributors
 * Licensed under the Apache License, Version 2.0
 *
 * Tokyo Night Color Palette for Hacker's Keyboard
 * Extracted from: https://github.com/folke/tokyonight.nvim
 *
 * This class provides a centralized, data-oriented color palette system
 * for all four Tokyo Night variants: Storm, Night, Moon, and Day.
 *
 * Usage:
 *   TokyoNightPalette.Variant storm = TokyoNightPalette.STORM;
 *   int bgColor = storm.bg;
 *   int textColor = storm.fg;
 */
package org.pocketworkstation.pckeyboard

import androidx.annotation.ColorInt
import androidx.annotation.IntDef

class TokyoNightPalette {

    /**
     * Base Variant class containing all color definitions for a Tokyo Night theme.
     * Organized by semantic purpose: backgrounds, foregrounds, accents, UI, and terminal colors.
     */
    class Variant(
        // ========== BACKGROUND COLORS ==========
        @ColorInt val bg: Int,
        @ColorInt val bgDark: Int,
        @ColorInt val bgDark1: Int,
        @ColorInt val bgHighlight: Int,

        // ========== FOREGROUND COLORS ==========
        @ColorInt val fg: Int,
        @ColorInt val fgDark: Int,
        @ColorInt val fgGutter: Int,

        // ========== PRIMARY ACCENT COLORS ==========
        @ColorInt val blue: Int,
        @ColorInt val blue0: Int,
        @ColorInt val blue1: Int,
        @ColorInt val blue2: Int,
        @ColorInt val blue5: Int,
        @ColorInt val blue6: Int,
        @ColorInt val blue7: Int,

        // ========== SECONDARY ACCENT COLORS ==========
        @ColorInt val cyan: Int,
        @ColorInt val green: Int,
        @ColorInt val green1: Int,
        @ColorInt val green2: Int,
        @ColorInt val magenta: Int,
        @ColorInt val magenta2: Int,
        @ColorInt val orange: Int,
        @ColorInt val purple: Int,
        @ColorInt val red: Int,
        @ColorInt val red1: Int,
        @ColorInt val teal: Int,
        @ColorInt val yellow: Int,

        // ========== UI COLORS ==========
        @ColorInt val comment: Int,
        @ColorInt val dark3: Int,
        @ColorInt val dark5: Int,
        @ColorInt val terminalBlack: Int,

        // ========== GIT COLORS ==========
        @ColorInt val gitAdd: Int,
        @ColorInt val gitChange: Int,
        @ColorInt val gitDelete: Int,

        // ========== SEMANTIC KEYBOARD ATTRIBUTES ==========
        @ColorInt val kbdColorBase: Int,
        @ColorInt val kbdColorAlpha: Int,
        @ColorInt val kbdColorMod: Int,
        @ColorInt val kbdColorHighlight: Int,
        @ColorInt val kbdColorText: Int,
        @ColorInt val kbdColorAccent: Int,
        @ColorInt val kbdColorPopup: Int,

        // ========== TERMINAL COLORS (ANSI) ==========
        @ColorInt val terminalRed: Int,
        @ColorInt val terminalGreen: Int,
        @ColorInt val terminalYellow: Int,
        @ColorInt val terminalBlue: Int,
        @ColorInt val terminalMagenta: Int,
        @ColorInt val terminalCyan: Int,
        @ColorInt val terminalWhite: Int,
        @ColorInt val terminalBrightRed: Int,
        @ColorInt val terminalBrightGreen: Int,
        @ColorInt val terminalBrightYellow: Int,
        @ColorInt val terminalBrightBlue: Int,
        @ColorInt val terminalBrightMagenta: Int,
        @ColorInt val terminalBrightCyan: Int,
        @ColorInt val terminalBrightWhite: Int,
    )

    companion object {
        // Theme variant IDs
        const val THEME_STORM: Int = 10
        const val THEME_NIGHT: Int = 11
        const val THEME_DAY: Int = 12
        const val THEME_MOON: Int = 13

        @IntDef(THEME_STORM, THEME_NIGHT, THEME_DAY, THEME_MOON)
        @Retention(AnnotationRetention.SOURCE)
        @Target(
            AnnotationTarget.VALUE_PARAMETER,
            AnnotationTarget.FIELD,
            AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY,
        )
        annotation class ThemeVariant

        // ========== STORM VARIANT (ID: 10) ==========
       
        val STORM: Variant = Variant(
            // Background colors
            0xFF24283b.toInt(), 0xFF1f2335.toInt(), 0xFF1b1e2d.toInt(), 0xFF292e42.toInt(),
            // Foreground colors
            0xFFC0caf5.toInt(), 0xFFa9b1d6.toInt(), 0xFF3b4261.toInt(),
            // Primary accents
            0xFF7aa2f7.toInt(), 0xFF3d59a1.toInt(), 0xFF2ac3de.toInt(), 0xFF0db9d7.toInt(),
            0xFF89ddff.toInt(), 0xFFb4f9f8.toInt(), 0xFF394b70.toInt(),
            // Secondary accents
            0xFF7dcfff.toInt(), 0xFF9ece6a.toInt(), 0xFF73daca.toInt(), 0xFF41a6b5.toInt(),
            0xFFbb9af7.toInt(), 0xFFff007c.toInt(),
            0xFFff9e64.toInt(), 0xFF9d7cd8.toInt(), 0xFFf7768e.toInt(), 0xFFdb4b4b.toInt(),
            0xFF1abc9c.toInt(), 0xFFe0af68.toInt(),
            // UI colors
            0xFF565f89.toInt(), 0xFF545c7e.toInt(), 0xFF737aa2.toInt(), 0xFF414868.toInt(),
            // Git colors
            0xFF449dab.toInt(), 0xFF6183bb.toInt(), 0xFF914c54.toInt(),
            // Semantic keyboard attributes
            0xFF24283b.toInt(), 0xFF24283b.toInt(), 0xFF292e42.toInt(),
            0xFF7aa2f7.toInt(), 0xFFC0caf5.toInt(), 0xFF7dcfff.toInt(), 0xFF565f89.toInt(),
            // Terminal colors
            0xFFf7768e.toInt(), 0xFF9ece6a.toInt(), 0xFFe0af68.toInt(), 0xFF7aa2f7.toInt(),
            0xFFbb9af7.toInt(), 0xFF7dcfff.toInt(), 0xFFa9b1d6.toInt(),
            0xFFf7768e.toInt(), 0xFF9ece6a.toInt(), 0xFFe0af68.toInt(), 0xFF7aa2f7.toInt(),
            0xFFbb9af7.toInt(), 0xFF7dcfff.toInt(), 0xFFC0caf5.toInt(),
        )

        // ========== NIGHT VARIANT (ID: 11) ==========
       
        val NIGHT: Variant = Variant(
            // Background colors (darker than Storm)
            0xFF1a1b26.toInt(), 0xFF16161e.toInt(), 0xFF0c0e14.toInt(), 0xFF292e42.toInt(),
            // Foreground colors
            0xFFC0caf5.toInt(), 0xFFa9b1d6.toInt(), 0xFF3b4261.toInt(),
            // Primary accents
            0xFF7aa2f7.toInt(), 0xFF3d59a1.toInt(), 0xFF2ac3de.toInt(), 0xFF0db9d7.toInt(),
            0xFF89ddff.toInt(), 0xFFb4f9f8.toInt(), 0xFF394b70.toInt(),
            // Secondary accents
            0xFF7dcfff.toInt(), 0xFF9ece6a.toInt(), 0xFF73daca.toInt(), 0xFF41a6b5.toInt(),
            0xFFbb9af7.toInt(), 0xFFff007c.toInt(),
            0xFFff9e64.toInt(), 0xFF9d7cd8.toInt(), 0xFFf7768e.toInt(), 0xFFdb4b4b.toInt(),
            0xFF1abc9c.toInt(), 0xFFe0af68.toInt(),
            // UI colors
            0xFF565f89.toInt(), 0xFF545c7e.toInt(), 0xFF737aa2.toInt(), 0xFF414868.toInt(),
            // Git colors
            0xFF449dab.toInt(), 0xFF6183bb.toInt(), 0xFF914c54.toInt(),
            // Semantic keyboard attributes
            0xFF1a1b26.toInt(), 0xFF1a1b26.toInt(), 0xFF16161e.toInt(),
            0xFF7aa2f7.toInt(), 0xFFC0caf5.toInt(), 0xFF7dcfff.toInt(), 0xFF565f89.toInt(),
            // Terminal colors
            0xFFf7768e.toInt(), 0xFF9ece6a.toInt(), 0xFFe0af68.toInt(), 0xFF7aa2f7.toInt(),
            0xFFbb9af7.toInt(), 0xFF7dcfff.toInt(), 0xFFa9b1d6.toInt(),
            0xFFf7768e.toInt(), 0xFF9ece6a.toInt(), 0xFFe0af68.toInt(), 0xFF7aa2f7.toInt(),
            0xFFbb9af7.toInt(), 0xFF7dcfff.toInt(), 0xFFC0caf5.toInt(),
        )

        // ========== MOON VARIANT (ID: 13) ==========
       
        val MOON: Variant = Variant(
            // Background colors
            0xFF222436.toInt(), 0xFF1e2030.toInt(), 0xFF191b29.toInt(), 0xFF2f334d.toInt(),
            // Foreground colors
            0xFFc8d3f5.toInt(), 0xFF828bb8.toInt(), 0xFF3b4261.toInt(),
            // Primary accents
            0xFF82aaff.toInt(), 0xFF3e68d7.toInt(), 0xFF65bcff.toInt(), 0xFF0db9d7.toInt(),
            0xFF89ddff.toInt(), 0xFFb4f9f8.toInt(), 0xFF394b70.toInt(),
            // Secondary accents
            0xFF86e1fc.toInt(), 0xFFc3e88d.toInt(), 0xFF4fd6be.toInt(), 0xFF41a6b5.toInt(),
            0xFFc099ff.toInt(), 0xFFff007c.toInt(),
            0xFFff966c.toInt(), 0xFFfca7ea.toInt(), 0xFFff757f.toInt(), 0xFFc53b53.toInt(),
            0xFF4fd6be.toInt(), 0xFFffc777.toInt(),
            // UI colors
            0xFF636da6.toInt(), 0xFF545c7e.toInt(), 0xFF737aa2.toInt(), 0xFF444a73.toInt(),
            // Git colors
            0xFFb8db87.toInt(), 0xFF7ca1f2.toInt(), 0xFFe26a75.toInt(),
            // Semantic keyboard attributes
            0xFF222436.toInt(), 0xFF222436.toInt(), 0xFF2f334d.toInt(),
            0xFF82aaff.toInt(), 0xFFc8d3f5.toInt(), 0xFF86e1fc.toInt(), 0xFF636da6.toInt(),
            // Terminal colors
            0xFFff757f.toInt(), 0xFFc3e88d.toInt(), 0xFFffc777.toInt(), 0xFF82aaff.toInt(),
            0xFFc099ff.toInt(), 0xFF86e1fc.toInt(), 0xFF828bb8.toInt(),
            0xFFff757f.toInt(), 0xFFc3e88d.toInt(), 0xFFffc777.toInt(), 0xFF82aaff.toInt(),
            0xFFc099ff.toInt(), 0xFF86e1fc.toInt(), 0xFFc8d3f5.toInt(),
        )

        // ========== DAY VARIANT (ID: 12) ==========
       
        val DAY: Variant = Variant(
            // Background colors (inverted)
            0xFFf5f5f5.toInt(), 0xFFe8e8e8.toInt(), 0xFFf3f3f3.toInt(), 0xFFd6d6d6.toInt(),
            // Foreground colors (inverted)
            0xFF3f3f3f.toInt(), 0xFF565656.toInt(), 0xFFc4c4c4.toInt(),
            // Primary accents (inverted)
            0xFF858eff.toInt(), 0xFFc2a65e.toInt(), 0xFFd53c21.toInt(), 0xFFf24628.toInt(),
            0xFF762200.toInt(), 0xFF4b0607.toInt(), 0xFFc6b48f.toInt(),
            // Secondary accents (inverted)
            0xFF823000.toInt(), 0xFF613197.toInt(), 0xFF8c2535.toInt(), 0xFFbe594a.toInt(),
            0xFF44609b.toInt(), 0xFF00f283.toInt(),
            0xFF006199.toInt(), 0xFF628227.toInt(), 0xFF088791.toInt(), 0xFF24b4b4.toInt(),
            0xFFe54363.toInt(), 0xFF1f509f.toInt(),
            // UI colors (inverted)
            0xFFa9a09f.toInt(), 0xFFaba383.toInt(), 0xFF8c859d.toInt(), 0xFFbebf97.toInt(),
            // Git colors (inverted)
            0xFFbb6254.toInt(), 0xFF9e7c44.toInt(), 0xFF6eb3ab.toInt(),
            // Semantic keyboard attributes
            0xFFf5f5f5.toInt(), 0xFFf5f5f5.toInt(), 0xFFe8e8e8.toInt(),
            0xFF858eff.toInt(), 0xFF3f3f3f.toInt(), 0xFF823000.toInt(), 0xFFa9a09f.toInt(),
            // Terminal colors (inverted)
            0xFF088791.toInt(), 0xFF613197.toInt(), 0xFF1f509f.toInt(), 0xFF858eff.toInt(),
            0xFF44609b.toInt(), 0xFF823000.toInt(), 0xFF565656.toInt(),
            0xFF088791.toInt(), 0xFF613197.toInt(), 0xFF1f509f.toInt(), 0xFF858eff.toInt(),
            0xFF44609b.toInt(), 0xFF823000.toInt(), 0xFF3f3f3f.toInt(),
        )

        /**
         * Get a variant by its theme ID.
         *
         * @param themeId The theme variant ID (THEME_STORM, THEME_NIGHT, THEME_DAY, THEME_MOON)
         * @return The corresponding Variant, or STORM if ID is not recognized
         */
       
        fun getVariant(@ThemeVariant themeId: Int): Variant =
            when (themeId) {
                THEME_NIGHT -> NIGHT
                THEME_DAY -> DAY
                THEME_MOON -> MOON
                THEME_STORM -> STORM
                else -> STORM
            }

        /**
         * Get a variant by its name.
         *
         * @param name The variant name ("storm", "night", "day", "moon")
         * @return The corresponding Variant, or STORM if name is not recognized
         */
       
        fun getVariantByName(name: String?): Variant =
            when (name?.lowercase()) {
                "night" -> NIGHT
                "day" -> DAY
                "moon" -> MOON
                "storm" -> STORM
                else -> STORM
            }

        /**
         * Get the theme ID for a variant name.
         *
         * @param name The variant name ("storm", "night", "day", "moon")
         * @return The corresponding theme ID
         */
       
        @ThemeVariant
        fun getThemeIdByName(name: String?): Int =
            when (name?.lowercase()) {
                "night" -> THEME_NIGHT
                "day" -> THEME_DAY
                "moon" -> THEME_MOON
                "storm" -> THEME_STORM
                else -> THEME_STORM
            }

        /**
         * Get the name of a theme variant.
         *
         * @param themeId The theme variant ID
         * @return The variant name
         */
       
        fun getThemeName(@ThemeVariant themeId: Int): String =
            when (themeId) {
                THEME_NIGHT -> "night"
                THEME_DAY -> "day"
                THEME_MOON -> "moon"
                THEME_STORM -> "storm"
                else -> "storm"
            }

        /**
         * Check if a theme is a light variant.
         *
         * @param themeId The theme variant ID
         * @return true if the theme is light (DAY), false otherwise
         */
       
        fun isLightTheme(@ThemeVariant themeId: Int): Boolean = themeId == THEME_DAY

        /**
         * Check if a theme is a dark variant.
         *
         * @param themeId The theme variant ID
         * @return true if the theme is dark, false otherwise
         */
       
        fun isDarkTheme(@ThemeVariant themeId: Int): Boolean = themeId != THEME_DAY
    }
}
