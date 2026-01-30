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

package org.pocketworkstation.pckeyboard.material.settings

/**
 * Central definition of all keyboard settings.
 * Maps to the original prefs.xml settings with correct keys, types, and defaults.
 */
object SettingsDefinitions {

    // ==================== PREFERENCE KEYS ====================
    // These must match the keys used in GlobalKeyboardSettings and LatinIME

    // Input Behavior
    const val KEY_AUTO_CAP = "auto_cap"
    const val KEY_CONNECTBOT_TAB = "connectbot_tab_hack"
    const val KEY_LONG_PRESS_DURATION = "pref_long_press_duration"
    const val KEY_POPUP_CONTENT = "pref_popup_content"

    // Visual Appearance
    const val KEY_HEIGHT_PORTRAIT = "settings_height_portrait"
    const val KEY_HEIGHT_LANDSCAPE = "settings_height_landscape"
    const val KEY_POPUP_ON = "popup_on"
    const val KEY_FULLSCREEN_OVERRIDE = "fullscreen_override"
    const val KEY_FORCE_KEYBOARD_ON = "force_keyboard_on"
    const val KEY_KEYBOARD_NOTIFICATION = "keyboard_notification"
    const val KEY_KEYBOARD_MODE_PORTRAIT = "pref_keyboard_mode_portrait"
    const val KEY_KEYBOARD_MODE_LANDSCAPE = "pref_keyboard_mode_landscape"
    const val KEY_HINT_MODE = "pref_hint_mode"
    const val KEY_LABEL_SCALE = "pref_label_scale_v2"
    const val KEY_TOP_ROW_SCALE = "pref_top_row_scale"
    const val KEY_RENDER_MODE = "pref_render_mode"
    const val KEY_KEYBOARD_FONT = "pref_keyboard_font"

    // Feedback
    const val KEY_VIBRATE_ON = "vibrate_on"
    const val KEY_VIBRATE_LEN = "vibrate_len"
    const val KEY_SOUND_ON = "sound_on"
    const val KEY_CLICK_METHOD = "pref_click_method"
    const val KEY_CLICK_VOLUME = "pref_click_volume"

    // Key Behavior
    const val KEY_CAPS_LOCK = "pref_caps_lock"
    const val KEY_SHIFT_LOCK_MODIFIERS = "pref_shift_lock_modifiers"
    const val KEY_CTRL_A_OVERRIDE = "pref_ctrl_a_override"
    const val KEY_CHORDING_CTRL = "pref_chording_ctrl_key"
    const val KEY_CHORDING_ALT = "pref_chording_alt_key"
    const val KEY_CHORDING_META = "pref_chording_meta_key"
    const val KEY_SLIDE_KEYS = "pref_slide_keys_int"

    // Gestures
    const val KEY_SWIPE_UP = "pref_swipe_up"
    const val KEY_SWIPE_DOWN = "pref_swipe_down"
    const val KEY_SWIPE_LEFT = "pref_swipe_left"
    const val KEY_SWIPE_RIGHT = "pref_swipe_right"
    const val KEY_VOL_UP = "pref_vol_up"
    const val KEY_VOL_DOWN = "pref_vol_down"

    // Theme
    const val KEY_KEYBOARD_LAYOUT = "pref_keyboard_layout"

    // Debug
    const val KEY_TOUCH_POS = "pref_touch_pos"

    // ==================== INPUT BEHAVIOR SETTINGS ====================
    @JvmStatic
    fun getInputBehaviorSections(): List<SettingsSection> = listOf(
        // Text Correction Section
        SettingsSection.Builder("Text Correction")
            .addBoolean(KEY_AUTO_CAP, "Auto-capitalization",
                "Capitalize first letter of sentences", true)
            .build(),

        // Advanced Section
        SettingsSection.Builder("Advanced")
            .addBoolean(KEY_CONNECTBOT_TAB, "ConnectBot tab hack",
                "Enable special tab handling for ConnectBot", true,
                "Tab key sends ESC+TAB for ConnectBot",
                "Tab key sends normal tab character")
            .addSlider(KEY_LONG_PRESS_DURATION, "Long press duration",
                "Time to hold key for alternate characters",
                100f, 1000f, 50f, 400f, true, "%.0f ms")
            .build()
    )

    // ==================== VISUAL APPEARANCE SETTINGS ====================
    @JvmStatic
    fun getVisualAppearanceSections(): List<SettingsSection> = listOf(
        // Display Options Section
        SettingsSection.Builder("Display Options")
            .addBoolean(KEY_POPUP_ON, "Show key popups",
                "Display popup when key is pressed", true)
            .addBoolean(KEY_FULLSCREEN_OVERRIDE, "Fullscreen override",
                "Force fullscreen mode in landscape", false,
                "Fullscreen mode enabled in landscape",
                "Using app's fullscreen setting")
            .addBoolean(KEY_FORCE_KEYBOARD_ON, "Force keyboard on",
                "Keep keyboard visible at all times", false,
                "Keyboard always visible",
                "Keyboard hides when not needed")
            .addBoolean(KEY_KEYBOARD_NOTIFICATION, "Keyboard notification",
                "Show persistent notification when keyboard is active", false,
                "Notification shown when keyboard is active",
                "No notification shown")
            .build(),

        // Keyboard Size Section
        SettingsSection.Builder("Keyboard Size")
            .addSlider(KEY_HEIGHT_PORTRAIT, "Height in portrait mode",
                "Adjust keyboard height when device is vertical",
                15f, 75f, 1f, 50f, true, true, "%.0f%%", null)
            .addSlider(KEY_HEIGHT_LANDSCAPE, "Height in landscape mode",
                "Adjust keyboard height when device is horizontal",
                15f, 75f, 1f, 50f, true, true, "%.0f%%", null)
            .build()
    )

    // ==================== FEEDBACK SETTINGS ====================
    @JvmStatic
    fun getFeedbackSections(): List<SettingsSection> = listOf(
        // Haptic Feedback Section
        SettingsSection.Builder("Haptic Feedback")
            .addBoolean(KEY_VIBRATE_ON, "Vibrate on keypress",
                "Provide haptic feedback when typing", true)
            .addSlider(KEY_VIBRATE_LEN, "Vibration duration",
                "Adjust the length of haptic feedback",
                5f, 200f, 5f, 40f, false, "%.0f ms")
            .build(),

        // Audio Feedback Section
        SettingsSection.Builder("Audio Feedback")
            .addBoolean(KEY_SOUND_ON, "Sound on keypress",
                "Play sound effect when typing", false)
            .build()
    )

    // ==================== ADVANCED/KEY BEHAVIOR SETTINGS ====================
    @JvmStatic
    fun getAdvancedSections(): List<SettingsSection> = listOf(
        // Key Behavior Section
        SettingsSection.Builder("Key Behavior")
            .addBoolean(KEY_CAPS_LOCK, "Caps lock",
                "Double-tap shift for caps lock", true,
                "Double-tap shift enables caps lock",
                "Caps lock disabled")
            .addBoolean(KEY_SHIFT_LOCK_MODIFIERS, "Shift lock modifiers",
                "Shift key locks modifier keys", false,
                "Shift locks Ctrl/Alt/Meta",
                "Modifiers work independently")
            .build(),

        // Debugging Section
        SettingsSection.Builder("Debugging")
            .addBoolean(KEY_TOUCH_POS, "Show touch position",
                "Display touch coordinates on screen", false,
                "Touch position displayed",
                "Touch position hidden")
            .build()
    )

    // ==================== SWIPE ACTION OPTIONS ====================
    @JvmStatic
    fun getSwipeActionEntries(): Array<String> = arrayOf(
        "None",
        "Close keyboard",
        "Shift",
        "Caps lock",
        "Ctrl",
        "Alt",
        "Meta",
        "Fn",
        "Compose",
        "Cursor left",
        "Cursor right",
        "Cursor up",
        "Cursor down",
        "Extension keyboard",
        "Full keyboard"
    )

    @JvmStatic
    fun getSwipeActionValues(): Array<String> = arrayOf(
        "none",
        "close",
        "shift",
        "caps",
        "ctrl",
        "alt",
        "meta",
        "fn",
        "compose",
        "cursor_left",
        "cursor_right",
        "cursor_up",
        "cursor_down",
        "extension",
        "full"
    )

    // ==================== CHORDING KEY OPTIONS ====================
    @JvmStatic
    fun getChordingKeyEntries(): Array<String> = arrayOf(
        "Disabled",
        "Bottom left",
        "Bottom right",
        "Both"
    )

    @JvmStatic
    fun getChordingKeyValues(): Array<String> = arrayOf("0", "1", "2", "3")

    // ==================== KEYBOARD MODE OPTIONS ====================
    @JvmStatic
    fun getKeyboardModeEntries(): Array<String> = arrayOf(
        "Auto",
        "4-row",
        "5-row"
    )

    @JvmStatic
    fun getKeyboardModeValues(): Array<String> = arrayOf("0", "1", "2")
}
