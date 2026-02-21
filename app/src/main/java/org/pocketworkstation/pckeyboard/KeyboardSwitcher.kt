/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.pocketworkstation.pckeyboard

import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.preference.PreferenceManager
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.InflateException
import android.view.LayoutInflater
import org.pocketworkstation.pckeyboard.material.settings.SettingsDefinitions
import java.lang.ref.SoftReference
import java.util.Arrays
import java.util.Locale

class KeyboardSwitcher private constructor() : SharedPreferences.OnSharedPreferenceChangeListener {

    private var mInputView: LatinKeyboardView? = null
    private lateinit var mInputMethodService: LatinIME

    private var mSymbolsId: KeyboardId? = null
    private var mSymbolsShiftedId: KeyboardId? = null

    private var mCurrentId: KeyboardId? = null
    private val mKeyboards = HashMap<KeyboardId, SoftReference<LatinKeyboard>>()

    private var mMode = MODE_NONE
    /** One of the MODE_XXX values */
    private var mImeOptions = 0
    private var mIsSymbols = false
    private var mFullMode = 0
    /**
     * mIsAutoCompletionActive indicates that auto completed word will be input
     * instead of what user actually typed.
     */
    private var mIsAutoCompletionActive = false
    private var mPreferSymbols = false

    private var mAutoModeSwitchState = AUTO_MODE_SWITCH_STATE_ALPHA

    // Indicates whether or not we have the settings key
    private var mHasSettingsKey = false

    private var mLastDisplayWidth = 0

    private var mLayoutId = 0

    /**
     * Represents the parameters necessary to construct a new LatinKeyboard,
     * which also serve as a unique identifier for each keyboard type.
     */
    private class KeyboardId(
        val mXml: Int,
        val mKeyboardMode: Int,
        val mEnableShiftLock: Boolean,
        val mHasVoice: Boolean
    ) {
        val mKeyboardHeightPercent: Float = LatinIME.sKeyboardSettings.keyboardHeightPercent
        val mUsingExtension: Boolean = LatinIME.sKeyboardSettings.useExtension

        private val mHashCode: Int = Arrays.hashCode(arrayOf(mXml, mKeyboardMode, mEnableShiftLock, mHasVoice))

        override fun equals(other: Any?): Boolean {
            if (other !is KeyboardId) return false
            return other.mXml == this.mXml &&
                    other.mKeyboardMode == this.mKeyboardMode &&
                    other.mUsingExtension == this.mUsingExtension &&
                    other.mEnableShiftLock == this.mEnableShiftLock &&
                    other.mHasVoice == this.mHasVoice
        }

        override fun hashCode(): Int = mHashCode
    }

    fun setVoiceMode(enableVoice: Boolean, voiceOnPrimary: Boolean) {
        // Voice mode removed
    }

    private fun hasVoiceButton(isSymbols: Boolean): Boolean = false

    fun setKeyboardMode(mode: Int, imeOptions: Int) {
        mAutoModeSwitchState = AUTO_MODE_SWITCH_STATE_ALPHA
        mPreferSymbols = mode == MODE_SYMBOLS
        var adjustedMode = mode
        if (mode == MODE_SYMBOLS) {
            adjustedMode = MODE_TEXT
        }
        try {
            setKeyboardMode(adjustedMode, imeOptions, mPreferSymbols)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Got exception: $adjustedMode,$imeOptions,$mPreferSymbols msg=${e.message}")
        }
    }

    private fun setKeyboardMode(mode: Int, imeOptions: Int, isSymbols: Boolean) {
        if (mInputView == null) return

        mMode = mode
        mImeOptions = imeOptions
        mIsSymbols = isSymbols

        mInputView?.setPreviewEnabled(mInputMethodService.getPopupOn())

        val id = getKeyboardId(mode, imeOptions, isSymbols)
        val keyboard = getKeyboard(id)

        if (mode == MODE_PHONE) {
            mInputView?.setPhoneKeyboard(keyboard)
        }

        mCurrentId = id
        mInputView?.setKeyboard(keyboard)
        keyboard.setShiftState(Keyboard.SHIFT_OFF)
        keyboard.setImeOptions(mInputMethodService.resources, mMode, imeOptions)
        keyboard.updateSymbolIcons(mIsAutoCompletionActive)
    }

    private fun getKeyboard(id: KeyboardId): LatinKeyboard {
        val ref = mKeyboards[id]
        var keyboard = ref?.get()
        if (keyboard == null) {
            val orig = mInputMethodService.resources
            val conf = orig.configuration
            val saveLocale = conf.locale
            conf.locale = LatinIME.sKeyboardSettings.inputLocale
            orig.updateConfiguration(conf, null)
            keyboard = LatinKeyboard(
                mInputMethodService,
                id.mXml,
                id.mKeyboardMode,
                id.mKeyboardHeightPercent
            )
            keyboard.setVoiceMode(false, false)

            if (id.mEnableShiftLock) {
                keyboard.enableShiftLock()
            }
            mKeyboards[id] = SoftReference(keyboard)

            conf.locale = saveLocale
            orig.updateConfiguration(conf, null)
        }
        return keyboard
    }

    fun isFullMode(): Boolean = mFullMode > 0

    private fun getKeyboardId(mode: Int, imeOptions: Int, isSymbols: Boolean): KeyboardId {
        val hasVoice = hasVoiceButton(isSymbols)
        if (mFullMode > 0) {
            when (mode) {
                MODE_TEXT, MODE_URL, MODE_EMAIL, MODE_IM, MODE_WEB ->
                    return KeyboardId(
                        if (mFullMode == 1) KBD_COMPACT else KBD_FULL,
                        KEYBOARDMODE_NORMAL,
                        true,
                        hasVoice
                    )
            }
        }

        val keyboardRowsResId = KBD_QWERTY
        if (isSymbols) {
            return if (mode == MODE_PHONE) {
                KeyboardId(KBD_PHONE_SYMBOLS, 0, false, hasVoice)
            } else {
                KeyboardId(
                    KBD_SYMBOLS,
                    if (mHasSettingsKey) KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY else KEYBOARDMODE_SYMBOLS,
                    false,
                    hasVoice
                )
            }
        }

        return when (mode) {
            MODE_NONE, MODE_TEXT ->
                KeyboardId(
                    keyboardRowsResId,
                    if (mHasSettingsKey) KEYBOARDMODE_NORMAL_WITH_SETTINGS_KEY else KEYBOARDMODE_NORMAL,
                    true,
                    hasVoice
                )
            MODE_SYMBOLS ->
                KeyboardId(
                    KBD_SYMBOLS,
                    if (mHasSettingsKey) KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY else KEYBOARDMODE_SYMBOLS,
                    false,
                    hasVoice
                )
            MODE_PHONE ->
                KeyboardId(KBD_PHONE, 0, false, hasVoice)
            MODE_URL ->
                KeyboardId(
                    keyboardRowsResId,
                    if (mHasSettingsKey) KEYBOARDMODE_URL_WITH_SETTINGS_KEY else KEYBOARDMODE_URL,
                    true,
                    hasVoice
                )
            MODE_EMAIL ->
                KeyboardId(
                    keyboardRowsResId,
                    if (mHasSettingsKey) KEYBOARDMODE_EMAIL_WITH_SETTINGS_KEY else KEYBOARDMODE_EMAIL,
                    true,
                    hasVoice
                )
            MODE_IM ->
                KeyboardId(
                    keyboardRowsResId,
                    if (mHasSettingsKey) KEYBOARDMODE_IM_WITH_SETTINGS_KEY else KEYBOARDMODE_IM,
                    true,
                    hasVoice
                )
            MODE_WEB ->
                KeyboardId(
                    keyboardRowsResId,
                    if (mHasSettingsKey) KEYBOARDMODE_WEB_WITH_SETTINGS_KEY else KEYBOARDMODE_WEB,
                    true,
                    hasVoice
                )
            else -> KeyboardId(keyboardRowsResId, KEYBOARDMODE_NORMAL, true, hasVoice)
        }
    }

    fun getKeyboardMode(): Int = mMode

    fun isAlphabetMode(): Boolean {
        val currentId = mCurrentId ?: return false
        val currentMode = currentId.mKeyboardMode
        if (mFullMode > 0 && currentMode == KEYBOARDMODE_NORMAL) return true

        for (mode in ALPHABET_MODES) {
            if (currentMode == mode) {
                return true
            }
        }
        return false
    }

    fun setShiftState(shiftState: Int) {
        mInputView?.setShiftState(shiftState)
    }

    fun setFn(useFn: Boolean) {
        if (mInputView == null) return
        val oldShiftState = mInputView!!.getShiftState()
        if (useFn) {
            val kbd = getKeyboard(mSymbolsId!!)
            kbd.enableShiftLock()
            mCurrentId = mSymbolsId
            mInputView?.setKeyboard(kbd)
            mInputView?.setShiftState(oldShiftState)
        } else {
            // Return to default keyboard state
            setKeyboardMode(mMode, mImeOptions, false)
            mInputView?.setShiftState(oldShiftState)
        }
    }

    fun setCtrlIndicator(active: Boolean) {
        mInputView?.setCtrlIndicator(active)
    }

    fun setAltIndicator(active: Boolean) {
        mInputView?.setAltIndicator(active)
    }

    fun setMetaIndicator(active: Boolean) {
        mInputView?.setMetaIndicator(active)
    }

    fun toggleShift() {
        if (isAlphabetMode()) return

        if (mFullMode > 0) {
            val shifted = mInputView?.isShiftAll() ?: false
            mInputView?.setShiftState(if (shifted) Keyboard.SHIFT_OFF else Keyboard.SHIFT_ON)
            return
        }

        val currentId = mCurrentId
        val symbolsId = mSymbolsId
        val symbolsShiftedId = mSymbolsShiftedId

        if (currentId != null && symbolsId != null && symbolsShiftedId != null) {
            if (currentId == symbolsId || currentId != symbolsShiftedId) {
                val symbolsShiftedKeyboard = getKeyboard(symbolsShiftedId)
                mCurrentId = symbolsShiftedId
                mInputView?.setKeyboard(symbolsShiftedKeyboard)
                // Symbol shifted keyboard has a ALT_SYM key that has a caps lock style indicator.
                // To enable the indicator, we need to set the shift state appropriately.
                symbolsShiftedKeyboard.enableShiftLock()
                symbolsShiftedKeyboard.setShiftState(Keyboard.SHIFT_LOCKED)
                symbolsShiftedKeyboard.setImeOptions(mInputMethodService.resources, mMode, mImeOptions)
            } else {
                val symbolsKeyboard = getKeyboard(symbolsId)
                mCurrentId = symbolsId
                mInputView?.setKeyboard(symbolsKeyboard)
                symbolsKeyboard.enableShiftLock()
                symbolsKeyboard.setShiftState(Keyboard.SHIFT_OFF)
                symbolsKeyboard.setImeOptions(mInputMethodService.resources, mMode, mImeOptions)
            }
        }
    }

    fun onCancelInput() {
        // Snap back to the previous keyboard mode if the user cancels sliding input.
        if (mAutoModeSwitchState == AUTO_MODE_SWITCH_STATE_MOMENTARY && getPointerCount() == 1) {
            mInputMethodService.changeKeyboardMode()
        }
    }

    fun toggleSymbols() {
        setKeyboardMode(mMode, mImeOptions, !mIsSymbols)
        if (mIsSymbols && !mPreferSymbols) {
            mAutoModeSwitchState = AUTO_MODE_SWITCH_STATE_SYMBOL_BEGIN
        } else {
            mAutoModeSwitchState = AUTO_MODE_SWITCH_STATE_ALPHA
        }
    }

    fun hasDistinctMultitouch(): Boolean = mInputView?.hasDistinctMultitouch() ?: false

    fun setAutoModeSwitchStateMomentary() {
        mAutoModeSwitchState = AUTO_MODE_SWITCH_STATE_MOMENTARY
    }

    fun isInMomentaryAutoModeSwitchState(): Boolean =
        mAutoModeSwitchState == AUTO_MODE_SWITCH_STATE_MOMENTARY

    fun isInChordingAutoModeSwitchState(): Boolean =
        mAutoModeSwitchState == AUTO_MODE_SWITCH_STATE_CHORDING

    fun isVibrateAndSoundFeedbackRequired(): Boolean =
        mInputView?.let { !it.isInSlidingKeyInput() } ?: false

    private fun getPointerCount(): Int = mInputView?.getPointerCount() ?: 0

    /**
     * Updates state machine to figure out when to automatically snap back to
     * the previous mode.
     */
    fun onKey(key: Int) {
        // Switch back to alpha mode if user types one or more non-space/enter characters
        // followed by a space/enter
        when (mAutoModeSwitchState) {
            AUTO_MODE_SWITCH_STATE_MOMENTARY -> {
                // Only distinct multi touch devices can be in this state.
                if (key == Keyboard.KEYCODE_MODE_CHANGE) {
                    // Detected only the mode change key has been pressed, and then released.
                    mAutoModeSwitchState = if (mIsSymbols) {
                        AUTO_MODE_SWITCH_STATE_SYMBOL_BEGIN
                    } else {
                        AUTO_MODE_SWITCH_STATE_ALPHA
                    }
                } else if (getPointerCount() == 1) {
                    // Snap back to the previous keyboard mode if the user pressed the mode change key
                    // and slid to other key, then released the finger.
                    mInputMethodService.changeKeyboardMode()
                } else {
                    // Chording input is being started. The keyboard mode will be snapped back to the
                    // previous mode in onReleaseSymbol when the mode change key is released.
                    mAutoModeSwitchState = AUTO_MODE_SWITCH_STATE_CHORDING
                }
            }
            AUTO_MODE_SWITCH_STATE_SYMBOL_BEGIN -> {
                if (key != LatinIME.ASCII_SPACE && key != LatinIME.ASCII_ENTER && key >= 0) {
                    mAutoModeSwitchState = AUTO_MODE_SWITCH_STATE_SYMBOL
                }
            }
            AUTO_MODE_SWITCH_STATE_SYMBOL -> {
                // Snap back to alpha keyboard mode if user types one or more non-space/enter
                // characters followed by a space/enter.
                if (key == LatinIME.ASCII_ENTER || key == LatinIME.ASCII_SPACE) {
                    mInputMethodService.changeKeyboardMode()
                }
            }
        }
    }

    fun getInputView(): LatinKeyboardView? = mInputView

    fun recreateInputView() {
        changeLatinKeyboardView(mLayoutId, true)
    }

    private fun makeSymbolsId(hasVoice: Boolean): KeyboardId {
        return when (mFullMode) {
            1 -> KeyboardId(KBD_COMPACT_FN, KEYBOARDMODE_SYMBOLS, true, false)
            2 -> KeyboardId(KBD_FULL_FN, KEYBOARDMODE_SYMBOLS, true, false)
            else -> KeyboardId(
                KBD_SYMBOLS,
                if (mHasSettingsKey) KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY else KEYBOARDMODE_SYMBOLS,
                false,
                false
            )
        }
    }

    private fun makeSymbolsShiftedId(hasVoice: Boolean): KeyboardId? {
        if (mFullMode > 0) return null
        return KeyboardId(
            KBD_SYMBOLS_SHIFT,
            if (mHasSettingsKey) KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY else KEYBOARDMODE_SYMBOLS,
            false,
            false
        )
    }

    fun makeKeyboards(forceCreate: Boolean) {
        mFullMode = LatinIME.sKeyboardSettings.keyboardMode
        mSymbolsId = makeSymbolsId(false)
        mSymbolsShiftedId = makeSymbolsShiftedId(false)

        if (forceCreate) {
            mKeyboards.clear()
        }

        // Configuration change is coming after the keyboard gets recreated. So don't rely on that.
        // If keyboards have already been made, check if we have a screen width change and
        // create the keyboard layouts again at the correct orientation
        val displayWidth = mInputMethodService.getMaxWidth()
        if (displayWidth == mLastDisplayWidth) return

        mLastDisplayWidth = displayWidth
        if (!forceCreate) {
            mKeyboards.clear()
        }
    }

    private fun changeLatinKeyboardView(newLayout: Int, forceReset: Boolean) {
        var layoutId = newLayout
        if (mLayoutId != layoutId || mInputView == null || forceReset) {
            mInputView?.closing()

            if (THEMES.size <= layoutId) {
                layoutId = DEFAULT_LAYOUT_ID.toInt()
            }

            LatinIMEUtil.GCUtils.getInstance().reset()
            var tryGC = true
            for (i in 0 until LatinIMEUtil.GCUtils.GC_TRY_LOOP_MAX) {
                if (!tryGC) break
                try {
                    val themeContext = ContextThemeWrapper(mInputMethodService, STYLES[layoutId])
                    val inflater = LayoutInflater.from(themeContext)
                    mInputView = inflater.inflate(THEMES[layoutId], null) as LatinKeyboardView
                    tryGC = false
                } catch (e: OutOfMemoryError) {
                    tryGC = LatinIMEUtil.GCUtils.getInstance().tryGCOrWait("$mLayoutId,$layoutId", e)
                } catch (e: InflateException) {
                    tryGC = LatinIMEUtil.GCUtils.getInstance().tryGCOrWait("$mLayoutId,$layoutId", e)
                }
            }

            mInputView?.setExtensionLayoutResId(THEMES[layoutId])
            mInputView?.setOnKeyboardActionListener(mInputMethodService)

            // Calculate nav bar height for edge-to-edge padding
            var navBarHeight = 0
            // Only apply this on Oreo+ where we enabled the edge-to-edge flags
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val resourceId = mInputMethodService.resources.getIdentifier(
                    "navigation_bar_height",
                    "dimen",
                    "android"
                )
                if (resourceId > 0) {
                    navBarHeight = mInputMethodService.resources.getDimensionPixelSize(resourceId)
                    // User requested 2.7x the system height for optimal clearance
                    navBarHeight = (navBarHeight * 2.5f).toInt()
                }
            }

            mInputView?.setPadding(0, 0, 0, navBarHeight)
            mLayoutId = layoutId

            // Update Navigation Bar Color to match the keyboard theme
            updateNavigationBarColor(layoutId)
        }

        mInputMethodService.mHandler.post {
            mInputView?.let {
                mInputMethodService.setInputView(it)
            }
            mInputMethodService.updateInputViewShown()
        }
    }

    private fun updateNavigationBarColor(layoutId: Int) {
        val dialog = mInputMethodService.window ?: return
        val window = dialog.window ?: return

        // Map layout ID to Tokyo Night theme ID
        val themeId = when (layoutId) {
            11 -> TokyoNightPalette.THEME_NIGHT
            12 -> TokyoNightPalette.THEME_DAY
            13 -> TokyoNightPalette.THEME_MOON
            else -> TokyoNightPalette.THEME_STORM // Default for 10 and legacy
        }

        val variant = TokyoNightPalette.getVariant(themeId)
        val isLight = TokyoNightPalette.isLightTheme(themeId)

        // Edge-to-Edge: Make navigation bar transparent and layout behind it
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Handle light/dark navigation bar icons and edge-to-edge flags
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            var flags = window.decorView.systemUiVisibility

            // Enable edge-to-edge
            flags = flags or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            flags = flags or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

            flags = if (isLight) {
                flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                flags and android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            PREF_KEYBOARD_LAYOUT -> {
                changeLatinKeyboardView(
                    sharedPreferences.getString(key, DEFAULT_LAYOUT_ID)?.toInt() ?: DEFAULT_LAYOUT_ID.toInt(),
                    true
                )
            }
            PREF_SETTINGS_KEY -> {
                updateSettingsKeyState(sharedPreferences)
                recreateInputView()
            }
            SettingsDefinitions.KEY_KEYBOARD_FONT -> {
                recreateInputView()
            }
        }
    }

    fun onAutoCompletionStateChanged(isAutoCompletion: Boolean) {
        if (isAutoCompletion != mIsAutoCompletionActive) {
            val keyboardView = getInputView()
            mIsAutoCompletionActive = isAutoCompletion
            keyboardView?.let {
                val keyboard = it.keyboard as? LatinKeyboard
                keyboard?.let { kbd ->
                    val key = kbd.onAutoCompletionStateChanged(isAutoCompletion)
                    if (key != null) {
                        it.invalidateKey(key)
                    }
                }
            }
        }
    }

    private fun updateSettingsKeyState(prefs: SharedPreferences) {
        val resources = mInputMethodService.resources
        val settingsKeyMode = prefs.getString(
            PREF_SETTINGS_KEY,
            resources.getString(DEFAULT_SETTINGS_KEY_MODE)
        )

        // We show the settings key when 1) SETTINGS_KEY_MODE_ALWAYS_SHOW or
        // 2) SETTINGS_KEY_MODE_AUTO and there are two or more enabled IMEs on the system
        mHasSettingsKey = settingsKeyMode == resources.getString(SETTINGS_KEY_MODE_ALWAYS_SHOW) ||
                settingsKeyMode == resources.getString(SETTINGS_KEY_MODE_AUTO)
    }

    companion object {
        private const val TAG = "PCKeyboardKbSw"

        const val MODE_NONE = 0
        const val MODE_TEXT = 1
        const val MODE_SYMBOLS = 2
        const val MODE_PHONE = 3
        const val MODE_URL = 4
        const val MODE_EMAIL = 5
        const val MODE_IM = 6
        const val MODE_WEB = 7

        // Main keyboard layouts without the settings key
        @JvmField val KEYBOARDMODE_NORMAL = R.id.mode_normal
        @JvmField val KEYBOARDMODE_URL = R.id.mode_url
        @JvmField val KEYBOARDMODE_EMAIL = R.id.mode_email
        @JvmField val KEYBOARDMODE_IM = R.id.mode_im
        @JvmField val KEYBOARDMODE_WEB = R.id.mode_webentry

        // Main keyboard layouts with the settings key
        @JvmField val KEYBOARDMODE_NORMAL_WITH_SETTINGS_KEY = R.id.mode_normal_with_settings_key
        @JvmField val KEYBOARDMODE_URL_WITH_SETTINGS_KEY = R.id.mode_url_with_settings_key
        @JvmField val KEYBOARDMODE_EMAIL_WITH_SETTINGS_KEY = R.id.mode_email_with_settings_key
        @JvmField val KEYBOARDMODE_IM_WITH_SETTINGS_KEY = R.id.mode_im_with_settings_key
        @JvmField val KEYBOARDMODE_WEB_WITH_SETTINGS_KEY = R.id.mode_webentry_with_settings_key

        // Symbols keyboard layout without the settings key
        @JvmField val KEYBOARDMODE_SYMBOLS = R.id.mode_symbols

        // Symbols keyboard layout with the settings key
        @JvmField val KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY = R.id.mode_symbols_with_settings_key

        const val DEFAULT_LAYOUT_ID = "10"
        const val PREF_KEYBOARD_LAYOUT = "pref_keyboard_layout"
        const val PREF_SETTINGS_KEY = "settings_key"

        private val THEMES = intArrayOf(
            R.layout.input_tokyonight_dynamic, // 0 -> Mapped to Storm as fallback
            R.layout.input_tokyonight_dynamic, // 1 -> Legacy Stone Bold
            R.layout.input_tokyonight_dynamic, // 2 -> Legacy Trans Neon
            R.layout.input_tokyonight_dynamic, // 3 -> Legacy Material Dark
            R.layout.input_tokyonight_dynamic, // 4 -> Legacy Material Light
            R.layout.input_tokyonight_dynamic, // 5 -> Legacy ICS Darker
            R.layout.input_tokyonight_dynamic, // 6 -> Legacy Material Black
            R.layout.input_tokyonight_dynamic, // 7 -> Legacy Gingerbread
            R.layout.input_tokyonight_dynamic, // 8
            R.layout.input_tokyonight_dynamic, // 9
            R.layout.input_tokyonight_dynamic, // 10 - Storm
            R.layout.input_tokyonight_dynamic, // 11 - Night
            R.layout.input_tokyonight_dynamic, // 12 - Day
            R.layout.input_tokyonight_dynamic  // 13 - Moon
        )

        private val STYLES = intArrayOf(
            R.style.Theme_TokyoNight_Storm, // 0
            R.style.Theme_TokyoNight_Storm, // 1
            R.style.Theme_TokyoNight_Storm, // 2
            R.style.Theme_TokyoNight_Storm, // 3
            R.style.Theme_TokyoNight_Storm, // 4
            R.style.Theme_TokyoNight_Storm, // 5
            R.style.Theme_TokyoNight_Storm, // 6
            R.style.Theme_TokyoNight_Storm, // 7
            R.style.Theme_TokyoNight_Storm, // 8
            R.style.Theme_TokyoNight_Storm, // 9
            R.style.Theme_TokyoNight_Storm, // 10
            R.style.Theme_TokyoNight_Night, // 11
            R.style.Theme_TokyoNight_Day,   // 12
            R.style.Theme_TokyoNight_Moon   // 13
        )

        // Keyboard resource IDs
        private val KBD_PHONE = R.xml.kbd_phone
        private val KBD_PHONE_SYMBOLS = R.xml.kbd_phone_symbols
        private val KBD_SYMBOLS = R.xml.kbd_symbols
        private val KBD_SYMBOLS_SHIFT = R.xml.kbd_symbols_shift
        private val KBD_QWERTY = R.xml.kbd_qwerty
        private val KBD_FULL = R.xml.kbd_full
        private val KBD_FULL_FN = R.xml.kbd_full_fn
        private val KBD_COMPACT = R.xml.kbd_compact
        private val KBD_COMPACT_FN = R.xml.kbd_compact_fn

        private val ALPHABET_MODES = intArrayOf(
            KEYBOARDMODE_NORMAL,
            KEYBOARDMODE_URL,
            KEYBOARDMODE_EMAIL,
            KEYBOARDMODE_IM,
            KEYBOARDMODE_WEB,
            KEYBOARDMODE_NORMAL_WITH_SETTINGS_KEY,
            KEYBOARDMODE_URL_WITH_SETTINGS_KEY,
            KEYBOARDMODE_EMAIL_WITH_SETTINGS_KEY,
            KEYBOARDMODE_IM_WITH_SETTINGS_KEY,
            KEYBOARDMODE_WEB_WITH_SETTINGS_KEY
        )

        private const val AUTO_MODE_SWITCH_STATE_ALPHA = 0
        private const val AUTO_MODE_SWITCH_STATE_SYMBOL_BEGIN = 1
        private const val AUTO_MODE_SWITCH_STATE_SYMBOL = 2
        private const val AUTO_MODE_SWITCH_STATE_MOMENTARY = 3
        private const val AUTO_MODE_SWITCH_STATE_CHORDING = 4

        private val SETTINGS_KEY_MODE_AUTO = R.string.settings_key_mode_auto
        private val SETTINGS_KEY_MODE_ALWAYS_SHOW = R.string.settings_key_mode_always_show
        private val DEFAULT_SETTINGS_KEY_MODE = SETTINGS_KEY_MODE_AUTO

        private val sInstance = KeyboardSwitcher()

        @JvmStatic
        fun getInstance(): KeyboardSwitcher = sInstance

        @JvmStatic
        fun init(ims: LatinIME) {
            sInstance.mInputMethodService = ims

            val prefs = PreferenceManager.getDefaultSharedPreferences(ims)
            sInstance.mLayoutId = prefs.getString(PREF_KEYBOARD_LAYOUT, DEFAULT_LAYOUT_ID)?.toInt()
                ?: DEFAULT_LAYOUT_ID.toInt()

            sInstance.updateSettingsKeyState(prefs)
            prefs.registerOnSharedPreferenceChangeListener(sInstance)

            sInstance.mSymbolsId = sInstance.makeSymbolsId(false)
            sInstance.mSymbolsShiftedId = sInstance.makeSymbolsShiftedId(false)
        }
    }
}
