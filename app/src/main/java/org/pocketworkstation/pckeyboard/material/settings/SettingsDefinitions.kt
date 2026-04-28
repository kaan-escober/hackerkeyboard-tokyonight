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
            .build(),

        // Keyboard Mode Section
        SettingsSection.Builder("Keyboard Mode")
            .addList(KEY_KEYBOARD_MODE_PORTRAIT, "Portrait mode",
                "Layout to use in portrait orientation",
                getKeyboardModeEntries(), getKeyboardModeValues(), "0")
            .addList(KEY_KEYBOARD_MODE_LANDSCAPE, "Landscape mode",
                "Layout to use in landscape orientation",
                getKeyboardModeEntries(), getKeyboardModeValues(), "0")
            .build(),

        // Layout Scaling Section
        SettingsSection.Builder("Layout Scaling")
            .addSlider(KEY_LABEL_SCALE, "Label scale",
                "Adjust size of key labels",
                0.5f, 2.0f, 0.1f, 1.0f, false, "%.1f")
            .addSlider(KEY_TOP_ROW_SCALE, "Top row scale",
                "Adjust size of top row keys",
                0.5f, 2.0f, 0.1f, 1.0f, false, "%.1f")
            .build(),

        // Advanced Visuals Section
        SettingsSection.Builder("Advanced Rendering")
            .addList(KEY_RENDER_MODE, "Render mode",
                "Graphic rendering engine to use",
                arrayOf("Smooth (Recommended)", "Retro", "Simple"),
                arrayOf("0", "1", "2"), "0")
            .addList(KEY_HINT_MODE, "Hint mode",
                "How to display key hints",
                arrayOf("None", "Small", "Full"),
                arrayOf("0", "1", "2"), "1")
            .addList(KEY_KEYBOARD_FONT, "Keyboard font",
                "Font used for key labels",
                arrayOf("System Default", "Monospace", "Serif", "Sans Serif"),
                arrayOf("0", "1", "2", "3"), "0")
            .build()
    )

    // ==================== FEEDBACK SETTINGS ====================
   
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
            .addSlider(KEY_CLICK_VOLUME, "Sound volume",
                "Adjust volume of keypress sounds",
                0f, 1f, 0.05f, 1f, false, false, "%.2f", KEY_SOUND_ON)
            .build()
    )

    /**
     * Advanced key behavior settings.
     */
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
            .addList(KEY_CTRL_A_OVERRIDE, "Ctrl-A override",
                "Configure Ctrl-A (select all) behavior",
                arrayOf("Disable Ctrl-A, use Ctrl-Alt-A instead", "Disable Ctrl-A completely", "Use Ctrl-A (no override)"),
                arrayOf("0", "1", "2"),
                "0")
            .addList(KEY_SLIDE_KEYS, "Sliding key events",
                "Action for sliding across keys",
                arrayOf("Ignore keys during sliding (Recommended)", "Send first key touched", "Send last key touched", "Send first and last key", "Send all keys touched"),
                arrayOf("0", "1", "2", "3", "4"),
                "0")
            .build(),

        // Gestures Section
        SettingsSection.Builder("Gestures")
            .addList(KEY_SWIPE_UP, "Swipe up",
                "Action when swiping up on keyboard",
                getGestureEntries(),
                getGestureValues(),
                "extension")
            .addList(KEY_SWIPE_DOWN, "Swipe down",
                "Action when swiping down on keyboard",
                getGestureEntries(),
                getGestureValues(),
                "close")
            .build(),

        // Debugging Section
        SettingsSection.Builder("Debugging")
            .addBoolean(KEY_TOUCH_POS, "Show touch position",
                "Display touch coordinates on screen", false,
                "Touch position displayed",
                "Touch position hidden")
            .build()
    )

    // ==================== GESTURE OPTIONS ====================
   
    fun getGestureEntries(): Array<String> = arrayOf(
        "(no action)", "Close keyboard", "Toggle extension row", "Launch Settings",
        "Toggle suggestions", "Voice input", "Switch keyboard layout",
        "Increase height", "Decrease height", "Previous language", "Next language"
    )

    fun getGestureValues(): Array<String> = arrayOf(
        "none", "close", "extension", "settings",
        "suggestions", "voice_input", "full_mode",
        "height_up", "height_down", "lang_prev", "lang_next"
    )

    // ==================== CHORDING KEY OPTIONS ====================
   
    fun getChordingKeyEntries(): Array<String> = arrayOf(
        "Disabled",
        "Bottom left",
        "Bottom right",
        "Both"
    )

   
    fun getChordingKeyValues(): Array<String> = arrayOf("0", "1", "2", "3")

    // ==================== KEYBOARD MODE OPTIONS ====================
   
    fun getKeyboardModeEntries(): Array<String> = arrayOf(
        "Auto",
        "4-row",
        "5-row"
    )

   
    fun getKeyboardModeValues(): Array<String> = arrayOf("0", "1", "2")
}
