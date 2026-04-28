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

import org.pocketworkstation.pckeyboard.LatinIMEUtil.RingCharBuffer

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.SystemClock
import android.os.Vibrator
import androidx.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.PrintWriterPrinter
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.ArrayList
import java.util.Collections
import java.util.regex.Pattern

/**
 * Input method implementation for Qwerty'ish keyboard.
 */
class LatinIME : InputMethodService(),
        ComposeSequencing,
        LatinKeyboardBaseView.OnKeyboardActionListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val TAG = "PCKeyboardIME"
        private const val NOTIFICATION_CHANNEL_ID = "PCKeyboard"
        private const val NOTIFICATION_ONGOING_ID = 1001

        var ESC_SEQUENCES: MutableMap<Int, String>? = null
        var CTRL_SEQUENCES: MutableMap<Int, Int>? = null

        private const val PREF_VIBRATE_ON = "vibrate_on"
        val PREF_VIBRATE_LEN = "vibrate_len"
        private const val PREF_SOUND_ON = "sound_on"
        private const val PREF_POPUP_ON = "popup_on"
        private const val PREF_AUTO_CAP = "auto_cap"
        private const val PREF_QUICK_FIXES = "quick_fixes"

        val PREF_SELECTED_LANGUAGES = "selected_languages"
        val PREF_INPUT_LANGUAGE = "input_language"
        private const val PREF_RECORRECTION_ENABLED = "recorrection_enabled"
        val PREF_FULLSCREEN_OVERRIDE = "fullscreen_override"
        val PREF_FORCE_KEYBOARD_ON = "force_keyboard_on"
        val PREF_KEYBOARD_NOTIFICATION = "keyboard_notification"
        val PREF_CONNECTBOT_TAB_HACK = "connectbot_tab_hack"
        val PREF_FULL_KEYBOARD_IN_PORTRAIT = "full_keyboard_in_portrait"
        val PREF_HEIGHT_PORTRAIT = "settings_height_portrait"
        val PREF_HEIGHT_LANDSCAPE = "settings_height_landscape"
        val PREF_HINT_MODE = "pref_hint_mode"
        val PREF_LONGPRESS_TIMEOUT = "pref_long_press_duration"
        val PREF_RENDER_MODE = "pref_render_mode"
        val PREF_SWIPE_UP = "pref_swipe_up"
        val PREF_SWIPE_DOWN = "pref_swipe_down"
        val PREF_SWIPE_LEFT = "pref_swipe_left"
        val PREF_SWIPE_RIGHT = "pref_swipe_right"
        val PREF_VOL_UP = "pref_vol_up"
        val PREF_VOL_DOWN = "pref_vol_down"

        private const val MSG_START_TUTORIAL = 1
        private const val MSG_UPDATE_SHIFT_STATE = 2

        // How many continuous deletes at which to start deleting at a higher speed.
        private const val DELETE_ACCELERATE_AT = 20
        // Key events coming any faster than this are long-presses.
        private const val QUICK_PRESS = 200

        val ASCII_ENTER = '\n'.code
        val ASCII_SPACE = ' '.code
        val ASCII_PERIOD = '.'.code

        // Contextual menu positions
        private const val POS_METHOD = 0
        private const val POS_SETTINGS = 1

        val sKeyboardSettings: GlobalKeyboardSettings = GlobalKeyboardSettings
        var sInstance: LatinIME? = null

        private val asciiToKeyCode = IntArray(127)
        private const val KF_MASK = 0xffff
        private const val KF_SHIFTABLE = 0x10000
        private const val KF_UPPER = 0x20000
        private const val KF_LETTER = 0x40000

        private val CPS_BUFFER_SIZE = 16
        private val NUMBER_RE = Pattern.compile("(\\d+).*")

       
        fun getIntFromString(valStr: String, defVal: Int): Int {
            val num = NUMBER_RE.matcher(valStr)
            if (!num.matches()) return defVal
            return Integer.parseInt(num.group(1))
        }

       
        fun getPrefInt(prefs: SharedPreferences, prefName: String, defVal: Int): Int {
            val prefVal = prefs.getString(prefName, Integer.toString(defVal))
            return getIntFromString(prefVal ?: Integer.toString(defVal), defVal)
        }

       
        fun getPrefInt(prefs: SharedPreferences, prefName: String, defStr: String): Int {
            val defVal = getIntFromString(defStr, 0)
            return getPrefInt(prefs, prefName, defVal)
        }

       
        fun getHeight(prefs: SharedPreferences, prefName: String, defVal: String): Int {
            var v = getPrefInt(prefs, prefName, defVal)
            if (v < 15) v = 15
            if (v > 75) v = 75
            return v
        }

        private fun getCapsOrShiftLockState(): Int {
            return if (sKeyboardSettings.capsLock) Keyboard.SHIFT_CAPS_LOCKED else Keyboard.SHIFT_LOCKED
        }

        private fun nextShiftState(prevState: Int, allowCapsLock: Boolean): Int {
            return if (allowCapsLock) {
                when (prevState) {
                    Keyboard.SHIFT_OFF -> Keyboard.SHIFT_ON
                    Keyboard.SHIFT_ON -> getCapsOrShiftLockState()
                    else -> Keyboard.SHIFT_OFF
                }
            } else {
                if (prevState == Keyboard.SHIFT_OFF) Keyboard.SHIFT_ON else Keyboard.SHIFT_OFF
            }
        }

       
        fun <E> newArrayList(vararg elements: E): ArrayList<E> {
            val capacity = (elements.size * 110) / 100 + 5
            val list = ArrayList<E>(capacity)
            Collections.addAll(list, *elements)
            return list
        }

        init {
            // Include RETURN in this set even though it's not printable.
            asciiToKeyCode['\n'.code] = KeyEvent.KEYCODE_ENTER or KF_SHIFTABLE

            // Non-alphanumeric ASCII codes which have their own keys
            asciiToKeyCode[' '.code] = KeyEvent.KEYCODE_SPACE or KF_SHIFTABLE
            asciiToKeyCode['#'.code] = KeyEvent.KEYCODE_POUND
            asciiToKeyCode['\''.code] = KeyEvent.KEYCODE_APOSTROPHE
            asciiToKeyCode['*'.code] = KeyEvent.KEYCODE_STAR
            asciiToKeyCode['+'.code] = KeyEvent.KEYCODE_PLUS
            asciiToKeyCode[','.code] = KeyEvent.KEYCODE_COMMA
            asciiToKeyCode['-'.code] = KeyEvent.KEYCODE_MINUS
            asciiToKeyCode['.'.code] = KeyEvent.KEYCODE_PERIOD
            asciiToKeyCode['/'.code] = KeyEvent.KEYCODE_SLASH
            asciiToKeyCode[';'.code] = KeyEvent.KEYCODE_SEMICOLON
            asciiToKeyCode['='.code] = KeyEvent.KEYCODE_EQUALS
            asciiToKeyCode['@'.code] = KeyEvent.KEYCODE_AT
            asciiToKeyCode['['.code] = KeyEvent.KEYCODE_LEFT_BRACKET
            asciiToKeyCode['\\'.code] = KeyEvent.KEYCODE_BACKSLASH
            asciiToKeyCode[']'.code] = KeyEvent.KEYCODE_RIGHT_BRACKET
            asciiToKeyCode['`'.code] = KeyEvent.KEYCODE_GRAVE

            for (i in 0..25) {
                asciiToKeyCode['a'.code + i] = KeyEvent.KEYCODE_A + i or KF_LETTER
                asciiToKeyCode['A'.code + i] = KeyEvent.KEYCODE_A + i or KF_UPPER or KF_LETTER
            }

            for (i in 0..9) {
                asciiToKeyCode['0'.code + i] = KeyEvent.KEYCODE_0 + i
            }
        }
    }

    private var mOptionsDialog: AlertDialog? = null

    /* package */ var mKeyboardSwitcher: KeyboardSwitcher? = null

    private lateinit var mResources: Resources

    private var mAutoSpace: Boolean = false
    private var mJustAddedAutoSpace: Boolean = false
    // TODO move this state variable outside LatinIME
    private var mModCtrl: Boolean = false
    private var mModAlt: Boolean = false
    private var mModMeta: Boolean = false
    private var mModFn: Boolean = false
    // Saved shift state when leaving alphabet mode, or when applying multitouch shift
    private var mSavedShiftState: Int = 0
    private var mPasswordText: Boolean = false
    private var mVibrateOn: Boolean = false
    private var mVibrateLen: Int = 0
    private var mSoundOn: Boolean = false
    private var mPopupOn: Boolean = false
    private var mAutoCapPref: Boolean = false
    private var mAutoCapActive: Boolean = false
    private var mDeadKeysActive: Boolean = false
    private var mQuickFixes: Boolean = false
    private var mIsShowingHint: Boolean = false
    private var mConnectbotTabHack: Boolean = false
    private var mFullscreenOverride: Boolean = false
    private var mForceKeyboardOn: Boolean = false
    private var mKeyboardNotification: Boolean = false
    private var mSwipeUpAction: String = ""
    private var mSwipeDownAction: String = ""
    private var mSwipeLeftAction: String = ""
    private var mSwipeRightAction: String = ""
    private var mVolUpAction: String = ""
    private var mVolDownAction: String = ""

    private var mHeightPortrait: Int = 0
    private var mHeightLandscape: Int = 0
    private var mNumKeyboardModes: Int = 3
    private var mKeyboardModeOverridePortrait: Int = 0
    private var mKeyboardModeOverrideLandscape: Int = 0
    private var mOrientation: Int = 0
    // Keep track of the last selection range to decide if we need to show word alternatives
    private var mLastSelectionStart: Int = 0
    private var mLastSelectionEnd: Int = 0

    // Input type is such that we should not auto-correct
    private var mInputTypeNoAutoCorrect: Boolean = false

    private var mDeleteCount: Int = 0
    private var mLastKeyTime: Long = 0L

    // Modifier keys state
    private val mShiftKeyState = ModifierKeyState()
    private val mSymbolKeyState = ModifierKeyState()
    private val mCtrlKeyState = ModifierKeyState()
    private val mAltKeyState = ModifierKeyState()
    private val mMetaKeyState = ModifierKeyState()
    private val mFnKeyState = ModifierKeyState()

    // Compose sequence handling
    private var mComposeMode: Boolean = false
    private val mComposeBuffer: ComposeSequence by lazy { ComposeSequence(this) }
    private val mDeadAccentBuffer: ComposeSequence by lazy { DeadAccentSequence(this) }

    private var mAudioManager: AudioManager? = null
    // Align sound effect volume on music volume
    private val FX_VOLUME = -1.0f
    private val FX_VOLUME_RANGE_DB = 72.0f
    private var mSilentMode: Boolean = false

    /* package */ var mWordSeparators: String = ""
    private var mSentenceSeparators: String = ""
    private var mConfigurationChanging: Boolean = false

    // Keeps track of most recently inserted text (multi-character key) for reverting
    private var mEnteredText: CharSequence? = null
    private var mRefreshKeyboardRequired: Boolean = false

    private var mNotificationReceiver: NotificationReceiver? = null

    // Characters per second measurement
    private var mLastCpsTime: Long = 0L
    private val mCpsIntervals = LongArray(CPS_BUFFER_SIZE)
    private var mCpsIndex: Int = 0

    /* package */ val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_UPDATE_SHIFT_STATE -> updateShiftKeyState(currentInputEditorInfo)
            }
        }
    }

    override fun onCreate() {
        Log.i("PCKeyboard", "onCreate(), os.version=" + System.getProperty("os.version"))
        KeyboardSwitcher.init(this)
        super.onCreate()
        sInstance = this
        mResources = resources
        val conf = mResources.configuration
        mOrientation = conf.orientation
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        mKeyboardSwitcher = KeyboardSwitcher.getInstance()
        val res = resources
        mConnectbotTabHack = prefs.getBoolean(PREF_CONNECTBOT_TAB_HACK,
                res.getBoolean(R.bool.default_connectbot_tab_hack))
        mFullscreenOverride = prefs.getBoolean(PREF_FULLSCREEN_OVERRIDE,
                res.getBoolean(R.bool.default_fullscreen_override))
        mForceKeyboardOn = prefs.getBoolean(PREF_FORCE_KEYBOARD_ON,
                res.getBoolean(R.bool.default_force_keyboard_on))
        mKeyboardNotification = prefs.getBoolean(PREF_KEYBOARD_NOTIFICATION,
                res.getBoolean(R.bool.default_keyboard_notification))
        mHeightPortrait = getHeight(prefs, PREF_HEIGHT_PORTRAIT, res.getString(R.string.default_height_portrait))
        mHeightLandscape = getHeight(prefs, PREF_HEIGHT_LANDSCAPE, res.getString(R.string.default_height_landscape))
        sKeyboardSettings.hintMode = Integer.parseInt(prefs.getString(PREF_HINT_MODE, res.getString(R.string.default_hint_mode)) ?: "0")
        sKeyboardSettings.longpressTimeout = getPrefInt(prefs, PREF_LONGPRESS_TIMEOUT, res.getString(R.string.default_long_press_duration))
        sKeyboardSettings.renderMode = getPrefInt(prefs, PREF_RENDER_MODE, res.getString(R.string.default_render_mode))
        mSwipeUpAction = prefs.getString(PREF_SWIPE_UP, res.getString(R.string.default_swipe_up)) ?: ""
        mSwipeDownAction = prefs.getString(PREF_SWIPE_DOWN, res.getString(R.string.default_swipe_down)) ?: ""
        mSwipeLeftAction = prefs.getString(PREF_SWIPE_LEFT, res.getString(R.string.default_swipe_left)) ?: ""
        mSwipeRightAction = prefs.getString(PREF_SWIPE_RIGHT, res.getString(R.string.default_swipe_right)) ?: ""
        mVolUpAction = prefs.getString(PREF_VOL_UP, res.getString(R.string.default_vol_up)) ?: ""
        mVolDownAction = prefs.getString(PREF_VOL_DOWN, res.getString(R.string.default_vol_down)) ?: ""
        sKeyboardSettings.initPrefs(prefs, res)

        updateKeyboardOptions()
        ComposeSequence.loadFromAssets(this)

        mWordSeparators = mResources.getString(R.string.word_separators)
        mSentenceSeparators = mResources.getString(R.string.sentence_separators)

        mOrientation = conf.orientation

        // register to receive ringer mode changes for silent mode
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(mReceiver, filter)
        prefs.registerOnSharedPreferenceChangeListener(this)
        setNotification(mKeyboardNotification)
    }

    private fun getKeyboardModeNum(origMode: Int, override: Int): Int {
        var mode = origMode
        if (mNumKeyboardModes == 2 && mode == 2) mode = 1 // skip "compact". FIXME!
        var num = (mode + override) % mNumKeyboardModes
        if (mNumKeyboardModes == 2 && num == 1) num = 2 // skip "compact". FIXME!
        return num
    }

    private fun updateKeyboardOptions() {
        val isPortrait = isPortrait()
        val kbMode: Int
        mNumKeyboardModes = if (sKeyboardSettings.compactModeEnabled) 3 else 2 // FIXME!
        kbMode = if (isPortrait) {
            getKeyboardModeNum(sKeyboardSettings.keyboardModePortrait, mKeyboardModeOverridePortrait)
        } else {
            getKeyboardModeNum(sKeyboardSettings.keyboardModeLandscape, mKeyboardModeOverrideLandscape)
        }
        // Convert overall keyboard height to per-row percentage
        val screenHeightPercent = if (isPortrait) mHeightPortrait else mHeightLandscape
        sKeyboardSettings.keyboardMode = kbMode
        sKeyboardSettings.keyboardHeightPercent = screenHeightPercent.toFloat()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.notification_channel_name)
            val description = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setNotification(visible: Boolean) {
        val ns = Context.NOTIFICATION_SERVICE
        val mNotificationManager = getSystemService(ns) as NotificationManager

        if (visible && mNotificationReceiver == null) {
            createNotificationChannel()
            val text: CharSequence = "Keyboard notification enabled."

            mNotificationReceiver = NotificationReceiver(this)
            val pFilter = IntentFilter(NotificationReceiver.ACTION_SHOW)
            pFilter.addAction(NotificationReceiver.ACTION_SETTINGS)
            registerReceiver(mNotificationReceiver, pFilter)

            val notificationIntent = Intent(NotificationReceiver.ACTION_SHOW)
            val contentIntent = PendingIntent.getBroadcast(applicationContext, 1, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

            val configIntent = Intent(NotificationReceiver.ACTION_SETTINGS)
            val configPendingIntent = PendingIntent.getBroadcast(applicationContext, 2, configIntent, PendingIntent.FLAG_IMMUTABLE)

            val title = "Show Hacker's Keyboard"
            val body = "Select this to open the keyboard. Disable in settings."

            val mBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon_hk_notification)
                    .setColor(0xff220044.toInt())
                    .setAutoCancel(false)
                    .setTicker(text)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(contentIntent)
                    .setOngoing(true)
                    .addAction(R.drawable.icon_hk_notification, getString(R.string.notification_action_settings),
                            configPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(NOTIFICATION_ONGOING_ID, mBuilder.build())
        } else if (mNotificationReceiver != null) {
            mNotificationManager.cancel(NOTIFICATION_ONGOING_ID)
            unregisterReceiver(mNotificationReceiver)
            mNotificationReceiver = null
        }
    }

    private fun isPortrait(): Boolean {
        return mOrientation == Configuration.ORIENTATION_PORTRAIT
    }

    override fun onDestroy() {
        unregisterReceiver(mReceiver)
        if (mNotificationReceiver != null) {
            unregisterReceiver(mNotificationReceiver)
            mNotificationReceiver = null
        }
        super.onDestroy()
    }

    override fun onConfigurationChanged(conf: Configuration) {
        Log.i("PCKeyboard", "onConfigurationChanged()")
        if (conf.orientation != mOrientation) {
            val ic = currentInputConnection
            ic?.finishComposingText() // For voice input
            mOrientation = conf.orientation
            reloadKeyboards()
        }
        mConfigurationChanging = true
        super.onConfigurationChanged(conf)
        mConfigurationChanging = false
    }

    override fun onCreateInputView(): View? {
        mKeyboardSwitcher!!.recreateInputView()
        mKeyboardSwitcher!!.makeKeyboards(true)
        mKeyboardSwitcher!!.setKeyboardMode(KeyboardSwitcher.MODE_TEXT, 0)
        return mKeyboardSwitcher!!.getInputView()
    }

    override fun onCreateInputMethodInterface(): AbstractInputMethodImpl {
        return MyInputMethodImpl()
    }

    var mToken: IBinder? = null

    inner class MyInputMethodImpl : InputMethodImpl() {
        override fun attachToken(token: IBinder) {
            super.attachToken(token)
            Log.i(TAG, "attachToken $token")
            if (mToken == null) {
                mToken = token
            }
        }
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        sKeyboardSettings.editorPackageName = attribute.packageName
        sKeyboardSettings.editorFieldName = attribute.fieldName
        sKeyboardSettings.editorFieldId = attribute.fieldId
        sKeyboardSettings.editorInputType = attribute.inputType

        val inputView = mKeyboardSwitcher!!.getInputView()
        // In landscape mode, this method gets called without the input view being created.
        if (inputView == null) {
            return
        }

        if (mRefreshKeyboardRequired) {
            mRefreshKeyboardRequired = false
            toggleLanguage(true, true)
        }

        mKeyboardSwitcher!!.makeKeyboards(false)

        TextEntryState.newSession(this)

        mPasswordText = false
        val variation = attribute.inputType and EditorInfo.TYPE_MASK_VARIATION
        if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                || variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == 0xe0 /* EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD */
        ) {
            if ((attribute.inputType and EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT) {
                mPasswordText = true
            }
        }

        mInputTypeNoAutoCorrect = false
        mModCtrl = false
        mModAlt = false
        mModMeta = false
        mModFn = false
        mEnteredText = null
        mKeyboardModeOverridePortrait = 0
        mKeyboardModeOverrideLandscape = 0
        sKeyboardSettings.useExtension = false

        when (attribute.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER,
            EditorInfo.TYPE_CLASS_DATETIME,
            EditorInfo.TYPE_CLASS_PHONE ->
                mKeyboardSwitcher!!.setKeyboardMode(KeyboardSwitcher.MODE_PHONE, attribute.imeOptions)
            EditorInfo.TYPE_CLASS_TEXT -> {
                mKeyboardSwitcher!!.setKeyboardMode(KeyboardSwitcher.MODE_TEXT, attribute.imeOptions)
                mAutoSpace = if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME) {
                    false
                } else {
                    true
                }
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
                    mKeyboardSwitcher!!.setKeyboardMode(KeyboardSwitcher.MODE_EMAIL, attribute.imeOptions)
                } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                    mKeyboardSwitcher!!.setKeyboardMode(KeyboardSwitcher.MODE_URL, attribute.imeOptions)
                } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
                    mKeyboardSwitcher!!.setKeyboardMode(KeyboardSwitcher.MODE_IM, attribute.imeOptions)
                } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // do nothing
                } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT) {
                    mKeyboardSwitcher!!.setKeyboardMode(KeyboardSwitcher.MODE_WEB, attribute.imeOptions)
                    if ((attribute.inputType and EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT) == 0) {
                        mInputTypeNoAutoCorrect = true
                    }
                }

                if ((attribute.inputType and EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
                    mInputTypeNoAutoCorrect = true
                }
                if ((attribute.inputType and EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT) == 0
                        && (attribute.inputType and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) == 0) {
                    mInputTypeNoAutoCorrect = true
                }
            }
            else -> mKeyboardSwitcher!!.setKeyboardMode(KeyboardSwitcher.MODE_TEXT, attribute.imeOptions)
        }
        inputView.closing()
        loadSettings()
        updateShiftKeyState(attribute)

        inputView.setPreviewEnabled(mPopupOn)
        inputView.setProximityCorrectionEnabled(true)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun shouldShowVoiceButton(attribute: EditorInfo): Boolean {
        return true
    }

    override fun onFinishInput() {
        super.onFinishInput()
        onAutoCompletionStateChanged(false)
        mKeyboardSwitcher?.getInputView()?.closing()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateExtractedText(token: Int, text: ExtractedText) {
        super.onUpdateExtractedText(token, text)
        val ic = currentInputConnection
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int,
                                   newSelStart: Int, newSelEnd: Int, candidatesStart: Int,
                                   candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd)

        mJustAddedAutoSpace = false
        postUpdateShiftKeyState()

        mLastSelectionStart = newSelStart
        mLastSelectionEnd = newSelEnd
    }

    override fun hideWindow() {
        onAutoCompletionStateChanged(false)

        if (mOptionsDialog != null && mOptionsDialog!!.isShowing) {
            mOptionsDialog!!.dismiss()
            mOptionsDialog = null
        }
        super.hideWindow()
        TextEntryState.endSession()
    }

    override fun onDisplayCompletions(completions: Array<CompletionInfo>?) {
    }

    override fun onEvaluateInputViewShown(): Boolean {
        val parent = super.onEvaluateInputViewShown()
        val wanted = mForceKeyboardOn || parent
        return wanted
    }

    override fun setCandidatesViewShown(shown: Boolean) {
        super.setCandidatesViewShown(false)
    }

    override fun onComputeInsets(outInsets: InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        if (!isFullscreenMode) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        val dm = resources.displayMetrics
        val displayHeight = dm.heightPixels.toFloat()
        val dimen = resources.getDimension(R.dimen.max_height_for_fullscreen)
        return if (displayHeight > dimen || mFullscreenOverride || isConnectbot()) {
            false
        } else {
            super.onEvaluateFullscreenMode()
        }
    }

    fun isKeyboardVisible(): Boolean {
        return mKeyboardSwitcher != null
                && mKeyboardSwitcher!!.getInputView() != null
                && mKeyboardSwitcher!!.getInputView()!!.isShown
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (event.repeatCount == 0 && mKeyboardSwitcher!!.getInputView() != null) {
                    if (mKeyboardSwitcher!!.getInputView()!!.handleBack()) {
                        return true
                    }
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (mVolUpAction != "none" && isKeyboardVisible()) {
                    return true
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (mVolDownAction != "none" && isKeyboardVisible()) {
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        var ev = event
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val inputView = mKeyboardSwitcher!!.getInputView()
                if (inputView != null && inputView.isShown
                        && inputView.getShiftState() == Keyboard.SHIFT_ON) {
                    ev = KeyEvent(ev.downTime, ev.eventTime,
                            ev.action, ev.keyCode, ev.repeatCount, ev.deviceId, ev.scanCode,
                            KeyEvent.META_SHIFT_LEFT_ON or KeyEvent.META_SHIFT_ON)
                    val ic = currentInputConnection
                    ic?.sendKeyEvent(ev)
                    return true
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (mVolUpAction != "none" && isKeyboardVisible()) {
                    return doSwipeAction(mVolUpAction)
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (mVolDownAction != "none" && isKeyboardVisible()) {
                    return doSwipeAction(mVolDownAction)
                }
            }
        }
        return super.onKeyUp(keyCode, ev)
    }

    private fun reloadKeyboards() {
        updateKeyboardOptions()
        mKeyboardSwitcher!!.makeKeyboards(true)
    }

    private fun postUpdateShiftKeyState() {
        // TODO(klausw): disabling, I have no idea what this is supposed to accomplish.
    }

    override fun updateShiftKeyState(attr: EditorInfo) {
        updateShiftKeyStateImpl(attr)
    }

    @JvmName("updateShiftKeyStateNullable")
    fun updateShiftKeyState(attr: EditorInfo?) = updateShiftKeyStateImpl(attr)

    private fun updateShiftKeyStateImpl(attr: EditorInfo?) {
        val ic = currentInputConnection
        if (ic != null && attr != null && mKeyboardSwitcher!!.isAlphabetMode()) {
            val oldState = getShiftState()
            val isShifted = mShiftKeyState.isChording()
            val isCapsLock = (oldState == Keyboard.SHIFT_CAPS_LOCKED || oldState == Keyboard.SHIFT_LOCKED)
            val isCaps = isCapsLock || getCursorCapsMode(ic, attr) != 0
            var newState = Keyboard.SHIFT_OFF
            if (isShifted) {
                newState = if (mSavedShiftState == Keyboard.SHIFT_LOCKED) Keyboard.SHIFT_CAPS else Keyboard.SHIFT_ON
            } else if (isCaps) {
                newState = if (isCapsLock) getCapsOrShiftLockState() else Keyboard.SHIFT_CAPS
            }
            mKeyboardSwitcher!!.setShiftState(newState)
        }
        if (ic != null) {
            val states = (KeyEvent.META_FUNCTION_ON
                    or KeyEvent.META_ALT_MASK
                    or KeyEvent.META_CTRL_MASK
                    or KeyEvent.META_META_MASK
                    or KeyEvent.META_SYM_ON)
            ic.clearMetaKeyStates(states)
        }
    }

    private fun getShiftState(): Int {
        if (mKeyboardSwitcher != null) {
            val view = mKeyboardSwitcher!!.getInputView()
            if (view != null) {
                return view.getShiftState()
            }
        }
        return Keyboard.SHIFT_OFF
    }

    private fun isShiftCapsMode(): Boolean {
        if (mKeyboardSwitcher != null) {
            val view = mKeyboardSwitcher!!.getInputView()
            if (view != null) {
                return view.isShiftCaps
            }
        }
        return false
    }

    private fun getCursorCapsMode(ic: InputConnection, attr: EditorInfo): Int {
        var caps = 0
        val ei = currentInputEditorInfo
        if (mAutoCapActive && ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
            caps = ic.getCursorCapsMode(attr.inputType)
        }
        return caps
    }

    private fun doubleSpace() {
        if (!mQuickFixes) return
        val ic = currentInputConnection ?: return
        val lastThree = ic.getTextBeforeCursor(3, 0)
        if (lastThree != null && lastThree.length == 3
                && Character.isLetterOrDigit(lastThree[0])
                && lastThree[1] == ASCII_SPACE.toChar()
                && lastThree[2] == ASCII_SPACE.toChar()) {
            ic.beginBatchEdit()
            ic.deleteSurroundingText(2, 0)
            ic.commitText(". ", 1)
            ic.endBatchEdit()
            updateShiftKeyState(currentInputEditorInfo)
            mJustAddedAutoSpace = true
        }
    }

    private fun maybeRemovePreviousPeriod(text: CharSequence) {
        val ic = currentInputConnection
        if (ic == null || text.isEmpty()) return

        val lastOne = ic.getTextBeforeCursor(1, 0)
        if (lastOne != null && lastOne.length == 1
                && lastOne[0] == ASCII_PERIOD.toChar()
                && text[0] == ASCII_PERIOD.toChar()) {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun removeTrailingSpace() {
        val ic = currentInputConnection ?: return

        val lastOne = ic.getTextBeforeCursor(1, 0)
        if (lastOne != null && lastOne.length == 1
                && lastOne[0] == ASCII_SPACE.toChar()) {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun isAlphabet(code: Int): Boolean {
        return Character.isLetter(code)
    }

    private fun showInputMethodPicker() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    private fun onOptionKeyPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            launchSettings()
        } else {
            if (!isShowingOptionDialog()) {
                showOptionsMenu()
            }
        }
    }

    private fun onOptionKeyLongPressed() {
        if (!isShowingOptionDialog()) {
            showInputMethodPicker()
        }
    }

    private fun isShowingOptionDialog(): Boolean {
        return mOptionsDialog != null && mOptionsDialog!!.isShowing
    }

    private fun isConnectbot(): Boolean {
        val ei = currentInputEditorInfo
        val pkg = ei?.packageName
        if (ei == null || pkg == null) return false
        return ((pkg.equals("org.connectbot", ignoreCase = true)
                || pkg.equals("org.woltage.irssiconnectbot", ignoreCase = true)
                || pkg.equals("com.pslib.connectbot", ignoreCase = true)
                || pkg.equals("sk.vx.connectbot", ignoreCase = true))
                && ei.inputType == 0) // FIXME
    }

    private fun getMetaState(shifted: Boolean): Int {
        var meta = 0
        if (shifted) meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (mModCtrl) meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (mModAlt) meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (mModMeta) meta = meta or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        return meta
    }

    private fun sendKeyDown(ic: InputConnection?, key: Int, meta: Int) {
        val now = System.currentTimeMillis()
        ic?.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0, meta))
    }

    private fun sendKeyUp(ic: InputConnection?, key: Int, meta: Int) {
        val now = System.currentTimeMillis()
        ic?.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, key, 0, meta))
    }

    private fun sendModifiedKeyDownUp(key: Int, shifted: Boolean) {
        val ic = currentInputConnection
        val meta = getMetaState(shifted)
        sendModifierKeysDown(shifted)
        sendKeyDown(ic, key, meta)
        sendKeyUp(ic, key, meta)
        sendModifierKeysUp(shifted)
    }

    private fun isShiftMod(): Boolean {
        if (mShiftKeyState.isChording()) return true
        if (mKeyboardSwitcher != null) {
            val kb = mKeyboardSwitcher!!.getInputView()
            if (kb != null) return kb.isShiftAll()
        }
        return false
    }

    private fun delayChordingCtrlModifier(): Boolean = sKeyboardSettings.chordingCtrlKey == 0
    private fun delayChordingAltModifier(): Boolean = sKeyboardSettings.chordingAltKey == 0
    private fun delayChordingMetaModifier(): Boolean = sKeyboardSettings.chordingMetaKey == 0

    private fun sendModifiedKeyDownUp(key: Int) {
        sendModifiedKeyDownUp(key, isShiftMod())
    }

    private fun sendShiftKey(ic: InputConnection?, isDown: Boolean) {
        val key = KeyEvent.KEYCODE_SHIFT_LEFT
        val meta = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (isDown) {
            sendKeyDown(ic, key, meta)
        } else {
            sendKeyUp(ic, key, meta)
        }
    }

    private fun sendCtrlKey(ic: InputConnection?, isDown: Boolean, chording: Boolean) {
        if (chording && delayChordingCtrlModifier()) return

        var key = sKeyboardSettings.chordingCtrlKey
        if (key == 0) key = KeyEvent.KEYCODE_CTRL_LEFT
        val meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (isDown) {
            sendKeyDown(ic, key, meta)
        } else {
            sendKeyUp(ic, key, meta)
        }
    }

    private fun sendAltKey(ic: InputConnection?, isDown: Boolean, chording: Boolean) {
        if (chording && delayChordingAltModifier()) return

        var key = sKeyboardSettings.chordingAltKey
        if (key == 0) key = KeyEvent.KEYCODE_ALT_LEFT
        val meta = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (isDown) {
            sendKeyDown(ic, key, meta)
        } else {
            sendKeyUp(ic, key, meta)
        }
    }

    private fun sendMetaKey(ic: InputConnection?, isDown: Boolean, chording: Boolean) {
        if (chording && delayChordingMetaModifier()) return

        var key = sKeyboardSettings.chordingMetaKey
        if (key == 0) key = KeyEvent.KEYCODE_META_LEFT
        val meta = KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        if (isDown) {
            sendKeyDown(ic, key, meta)
        } else {
            sendKeyUp(ic, key, meta)
        }
    }

    private fun sendModifierKeysDown(shifted: Boolean) {
        val ic = currentInputConnection
        if (shifted) {
            sendShiftKey(ic, true)
        }
        if (mModCtrl && (!mCtrlKeyState.isChording() || delayChordingCtrlModifier())) {
            sendCtrlKey(ic, true, false)
        }
        if (mModAlt && (!mAltKeyState.isChording() || delayChordingAltModifier())) {
            sendAltKey(ic, true, false)
        }
        if (mModMeta && (!mMetaKeyState.isChording() || delayChordingMetaModifier())) {
            sendMetaKey(ic, true, false)
        }
    }

    private fun handleModifierKeysUp(shifted: Boolean, sendKey: Boolean) {
        val ic = currentInputConnection
        if (mModMeta && (!mMetaKeyState.isChording() || delayChordingMetaModifier())) {
            if (sendKey) sendMetaKey(ic, false, false)
            if (!mMetaKeyState.isChording()) setModMeta(false)
        }
        if (mModAlt && (!mAltKeyState.isChording() || delayChordingAltModifier())) {
            if (sendKey) sendAltKey(ic, false, false)
            if (!mAltKeyState.isChording()) setModAlt(false)
        }
        if (mModCtrl && (!mCtrlKeyState.isChording() || delayChordingCtrlModifier())) {
            if (sendKey) sendCtrlKey(ic, false, false)
            if (!mCtrlKeyState.isChording()) setModCtrl(false)
        }
        if (shifted) {
            if (sendKey) sendShiftKey(ic, false)
            val shiftState = getShiftState()
            if (!(mShiftKeyState.isChording() || shiftState == Keyboard.SHIFT_LOCKED)) {
                resetShift()
            }
        }
    }

    private fun sendModifierKeysUp(shifted: Boolean) {
        handleModifierKeysUp(shifted, true)
    }

    private fun sendSpecialKey(code: Int) {
        if (!isConnectbot()) {
            sendModifiedKeyDownUp(code)
            return
        }

        if (ESC_SEQUENCES == null) {
            ESC_SEQUENCES = HashMap()
            CTRL_SEQUENCES = HashMap()

            // VT escape sequences without leading Escape
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_HOME, "[1~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_END, "[4~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_PAGE_UP, "[5~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_PAGE_DOWN, "[6~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F1, "OP")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F2, "OQ")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F3, "OR")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F4, "OS")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F5, "[15~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F6, "[17~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F7, "[18~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F8, "[19~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F9, "[20~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F10, "[21~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F11, "[23~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F12, "[24~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FORWARD_DEL, "[3~")
            ESC_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_INSERT, "[2~")

            // Special ConnectBot hack: Ctrl-1 to Ctrl-0 for F1-F10.
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F1, KeyEvent.KEYCODE_1)
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F2, KeyEvent.KEYCODE_2)
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F3, KeyEvent.KEYCODE_3)
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F4, KeyEvent.KEYCODE_4)
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F5, KeyEvent.KEYCODE_5)
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F6, KeyEvent.KEYCODE_6)
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F7, KeyEvent.KEYCODE_7)
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F8, KeyEvent.KEYCODE_8)
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F9, KeyEvent.KEYCODE_9)
            CTRL_SEQUENCES!!.put(-LatinKeyboardView.KEYCODE_FKEY_F10, KeyEvent.KEYCODE_0)
        }
        val ic = currentInputConnection
        var ctrlseq: Int? = null
        if (mConnectbotTabHack) {
            ctrlseq = CTRL_SEQUENCES!!.get(code)
        }
        val seq = ESC_SEQUENCES!!.get(code)

        if (ctrlseq != null) {
            if (mModAlt) {
                // send ESC prefix for "Alt"
                ic!!.commitText(Character.toString(27.toChar()), 1)
            }
            ic!!.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, ctrlseq))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, ctrlseq))
        } else if (seq != null) {
            if (mModAlt) {
                // send ESC prefix for "Alt"
                ic!!.commitText(Character.toString(27.toChar()), 1)
            }
            // send ESC prefix of escape sequence
            ic!!.commitText(Character.toString(27.toChar()), 1)
            ic.commitText(seq, 1)
        } else {
            // send key code, let connectbot handle it
            sendDownUpKeyEvents(code)
        }
        handleModifierKeysUp(false, false)
    }

    fun sendModifiableKeyChar(ch: Char) {
        val modShift = isShiftMod()
        if ((modShift || mModCtrl || mModAlt || mModMeta) && ch > 0.toChar() && ch < 127.toChar()) {
            val ic = currentInputConnection
            if (isConnectbot()) {
                if (mModAlt) {
                    ic!!.commitText(Character.toString(27.toChar()), 1)
                }
                if (mModCtrl) {
                    val code = ch.code and 31
                    if (code == 9) {
                        sendTab()
                    } else {
                        ic!!.commitText(Character.toString(code.toChar()), 1)
                    }
                } else {
                    ic!!.commitText(Character.toString(ch), 1)
                }
                handleModifierKeysUp(false, false)
                return
            }

            // Non-ConnectBot
            val combinedCode = asciiToKeyCode[ch.code]
            if (combinedCode > 0) {
                val code = combinedCode and KF_MASK
                val shiftable = (combinedCode and KF_SHIFTABLE) > 0
                val upper = (combinedCode and KF_UPPER) > 0
                val letter = (combinedCode and KF_LETTER) > 0
                val shifted = modShift && (upper || shiftable)
                if (letter && !mModCtrl && !mModAlt && !mModMeta) {
                    ic!!.commitText(Character.toString(ch), 1)
                    handleModifierKeysUp(false, false)
                } else if ((ch == 'a' || ch == 'A') && mModCtrl) {
                    if (sKeyboardSettings.ctrlAOverride == 0) {
                        if (mModAlt) {
                            val isChordingAlt = mAltKeyState.isChording()
                            setModAlt(false)
                            sendModifiedKeyDownUp(code, shifted)
                            if (isChordingAlt) setModAlt(true)
                        } else {
                            Toast.makeText(applicationContext,
                                resources.getString(R.string.toast_ctrl_a_override_info), Toast.LENGTH_LONG)
                                .show()
                            sendModifierKeysDown(shifted)
                            sendModifierKeysUp(shifted)
                            return // ignore the key
                        }
                    } else if (sKeyboardSettings.ctrlAOverride == 1) {
                        sendModifierKeysDown(shifted)
                        sendModifierKeysUp(shifted)
                        return // ignore the key
                    } else {
                        sendModifiedKeyDownUp(code, shifted)
                    }
                } else {
                    sendModifiedKeyDownUp(code, shifted)
                }
                return
            }
        }

        if (ch >= '0' && ch <= '9') {
            val ic = currentInputConnection
            ic!!.clearMetaKeyStates(KeyEvent.META_SHIFT_ON or KeyEvent.META_ALT_ON or KeyEvent.META_SYM_ON)
        }

        // Default handling for anything else, including unmodified ENTER and SPACE.
        sendKeyChar(ch)
    }

    private fun sendTab() {
        val ic = currentInputConnection
        val tabHack = isConnectbot() && mConnectbotTabHack

        if (tabHack) {
            if (mModAlt) {
                ic!!.commitText(Character.toString(27.toChar()), 1)
            }
            ic!!.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_I))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_I))
        } else {
            sendModifiedKeyDownUp(KeyEvent.KEYCODE_TAB)
        }
    }

    private fun sendEscape() {
        if (isConnectbot()) {
            sendKeyChar(27.toChar())
        } else {
            sendModifiedKeyDownUp(111 /*KeyEvent.KEYCODE_ESCAPE*/)
        }
    }

    private fun processMultiKey(primaryCode: Int): Boolean {
        if (mDeadAccentBuffer.hasComposePending()) {
            mDeadAccentBuffer.execute(primaryCode)
            mDeadAccentBuffer.clear()
            return true
        }
        if (mComposeMode) {
            mComposeMode = mComposeBuffer.execute(primaryCode)
            return true
        }
        return false
    }

    // Implementation of KeyboardViewListener

    override fun onKey(primaryCode: Int, keyCodes: IntArray?, x: Int, y: Int) {
        val now = SystemClock.uptimeMillis()
        if (primaryCode != Keyboard.KEYCODE_DELETE || now > mLastKeyTime + QUICK_PRESS) {
            mDeleteCount = 0
        }
        mLastKeyTime = now
        handleKeyInternal(primaryCode, keyCodes, x, y)
        mKeyboardSwitcher!!.onKey(primaryCode)
        mEnteredText = null
    }

    private fun handleKeyInternal(primaryCode: Int, keyCodes: IntArray?, x: Int, y: Int) {
        val distinctMultiTouch = mKeyboardSwitcher!!.hasDistinctMultitouch()
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                if (processMultiKey(primaryCode)) return
                handleBackspace()
                mDeleteCount++
            }
            Keyboard.KEYCODE_SHIFT -> {
                if (!distinctMultiTouch)
                    handleShift()
            }
            Keyboard.KEYCODE_MODE_CHANGE -> {
                if (!distinctMultiTouch)
                    changeKeyboardMode()
            }
            LatinKeyboardView.KEYCODE_CTRL_LEFT -> {
                if (!distinctMultiTouch)
                    setModCtrl(!mModCtrl)
            }
            LatinKeyboardView.KEYCODE_ALT_LEFT -> {
                if (!distinctMultiTouch)
                    setModAlt(!mModAlt)
            }
            LatinKeyboardView.KEYCODE_META_LEFT -> {
                if (!distinctMultiTouch)
                    setModMeta(!mModMeta)
            }
            LatinKeyboardView.KEYCODE_FN -> {
                if (!distinctMultiTouch)
                    setModFn(!mModFn)
            }
            Keyboard.KEYCODE_CANCEL -> {
                if (!isShowingOptionDialog()) {
                    handleClose()
                }
            }
            LatinKeyboardView.KEYCODE_OPTIONS -> onOptionKeyPressed()
            LatinKeyboardView.KEYCODE_OPTIONS_LONGPRESS -> onOptionKeyLongPressed()
            LatinKeyboardView.KEYCODE_COMPOSE -> {
                mComposeMode = !mComposeMode
                mComposeBuffer.clear()
            }
            LatinKeyboardView.KEYCODE_NEXT_LANGUAGE -> toggleLanguage(false, true)
            LatinKeyboardView.KEYCODE_PREV_LANGUAGE -> toggleLanguage(false, false)
            9 /* Tab */ -> {
                if (processMultiKey(primaryCode)) return
                sendTab()
            }
            LatinKeyboardView.KEYCODE_ESCAPE -> {
                if (processMultiKey(primaryCode)) return
                sendEscape()
            }
            LatinKeyboardView.KEYCODE_DPAD_UP,
            LatinKeyboardView.KEYCODE_DPAD_DOWN,
            LatinKeyboardView.KEYCODE_DPAD_LEFT,
            LatinKeyboardView.KEYCODE_DPAD_RIGHT,
            LatinKeyboardView.KEYCODE_DPAD_CENTER,
            LatinKeyboardView.KEYCODE_HOME,
            LatinKeyboardView.KEYCODE_END,
            LatinKeyboardView.KEYCODE_PAGE_UP,
            LatinKeyboardView.KEYCODE_PAGE_DOWN,
            LatinKeyboardView.KEYCODE_FKEY_F1,
            LatinKeyboardView.KEYCODE_FKEY_F2,
            LatinKeyboardView.KEYCODE_FKEY_F3,
            LatinKeyboardView.KEYCODE_FKEY_F4,
            LatinKeyboardView.KEYCODE_FKEY_F5,
            LatinKeyboardView.KEYCODE_FKEY_F6,
            LatinKeyboardView.KEYCODE_FKEY_F7,
            LatinKeyboardView.KEYCODE_FKEY_F8,
            LatinKeyboardView.KEYCODE_FKEY_F9,
            LatinKeyboardView.KEYCODE_FKEY_F10,
            LatinKeyboardView.KEYCODE_FKEY_F11,
            LatinKeyboardView.KEYCODE_FKEY_F12,
            LatinKeyboardView.KEYCODE_FORWARD_DEL,
            LatinKeyboardView.KEYCODE_INSERT,
            LatinKeyboardView.KEYCODE_SYSRQ,
            LatinKeyboardView.KEYCODE_BREAK,
            LatinKeyboardView.KEYCODE_NUM_LOCK,
            LatinKeyboardView.KEYCODE_SCROLL_LOCK -> {
                if (processMultiKey(primaryCode)) return
                sendSpecialKey(-primaryCode)
            }
            else -> {
                if (!mComposeMode && mDeadKeysActive && Character.getType(primaryCode) == Character.NON_SPACING_MARK.toInt()) {
                    if (!mDeadAccentBuffer.execute(primaryCode)) {
                        return // pressing a dead key twice produces spacing equivalent
                    }
                    updateShiftKeyState(currentInputEditorInfo)
                    return
                }
                if (processMultiKey(primaryCode)) return
                if (primaryCode != ASCII_ENTER) {
                    mJustAddedAutoSpace = false
                }
                RingCharBuffer.getInstance().push(primaryCode.toChar(), x, y)
                if (isWordSeparator(primaryCode)) {
                    handleSeparator(primaryCode)
                } else {
                    handleCharacter(primaryCode, keyCodes)
                }
            }
        }
    }

    override fun onText(text: CharSequence) {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        maybeRemovePreviousPeriod(text)
        ic.commitText(text, 1)
        ic.endBatchEdit()
        updateShiftKeyState(currentInputEditorInfo)
        mKeyboardSwitcher!!.onKey(0) // dummy key code.
        mJustAddedAutoSpace = false
        mEnteredText = text
    }

    override fun onCancel() {
        mKeyboardSwitcher!!.onCancelInput()
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return

        ic.beginBatchEdit()
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        if (mDeleteCount > DELETE_ACCELERATE_AT) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        postUpdateShiftKeyState()
        TextEntryState.backspace()
        ic.endBatchEdit()
    }

    private fun setModCtrl(v: Boolean) {
        mKeyboardSwitcher!!.setCtrlIndicator(v)
        mModCtrl = v
    }

    private fun setModAlt(v: Boolean) {
        mKeyboardSwitcher!!.setAltIndicator(v)
        mModAlt = v
    }

    private fun setModMeta(v: Boolean) {
        mKeyboardSwitcher!!.setMetaIndicator(v)
        mModMeta = v
    }

    private fun setModFn(v: Boolean) {
        mModFn = v
        mKeyboardSwitcher!!.setFn(v)
        mKeyboardSwitcher!!.setCtrlIndicator(mModCtrl)
        mKeyboardSwitcher!!.setAltIndicator(mModAlt)
        mKeyboardSwitcher!!.setMetaIndicator(mModMeta)
    }

    private fun startMultitouchShift() {
        var newState = Keyboard.SHIFT_ON
        if (mKeyboardSwitcher!!.isAlphabetMode()) {
            mSavedShiftState = getShiftState()
            if (mSavedShiftState == Keyboard.SHIFT_LOCKED) newState = Keyboard.SHIFT_CAPS
        }
        handleShiftInternal(true, newState)
    }

    private fun commitMultitouchShift() {
        if (mKeyboardSwitcher!!.isAlphabetMode()) {
            val newState = nextShiftState(mSavedShiftState, true)
            handleShiftInternal(true, newState)
        }
        // else do nothing, keyboard is already flipped
    }

    private fun resetMultitouchShift() {
        var newState = Keyboard.SHIFT_OFF
        if (mSavedShiftState == Keyboard.SHIFT_CAPS_LOCKED || mSavedShiftState == Keyboard.SHIFT_LOCKED) {
            newState = mSavedShiftState
        }
        handleShiftInternal(true, newState)
    }

    private fun resetShift() {
        handleShiftInternal(true, Keyboard.SHIFT_OFF)
    }

    private fun handleShift() {
        handleShiftInternal(false, -1)
    }

    private fun handleShiftInternal(forceState: Boolean, newState: Int) {
        mHandler.removeMessages(MSG_UPDATE_SHIFT_STATE)
        val switcher = mKeyboardSwitcher!!
        if (switcher.isAlphabetMode()) {
            if (forceState) {
                switcher.setShiftState(newState)
            } else {
                switcher.setShiftState(nextShiftState(getShiftState(), true))
            }
        } else {
            switcher.toggleShift()
        }
    }

    private fun handleCharacter(primaryCode: Int, keyCodes: IntArray?) {
        sendModifiableKeyChar(primaryCode.toChar())
        updateShiftKeyState(currentInputEditorInfo)
        TextEntryState.typedCharacter(primaryCode.toChar(), isWordSeparator(primaryCode))
    }

    private fun handleSeparator(primaryCode: Int) {
        val ic = currentInputConnection
        ic?.beginBatchEdit()
        sendModifiableKeyChar(primaryCode.toChar())

        if (primaryCode == ASCII_SPACE) {
            doubleSpace()
        }
        updateShiftKeyState(currentInputEditorInfo)
        ic?.endBatchEdit()
    }

    private fun handleClose() {
        requestHideSelf(0)
        if (mKeyboardSwitcher != null) {
            val inputView = mKeyboardSwitcher!!.getInputView()
            inputView?.closing()
        }
        TextEntryState.endSession()
    }

    private fun isCursorTouchingWord(): Boolean {
        val ic = currentInputConnection ?: return false
        val toLeft = ic.getTextBeforeCursor(1, 0)
        val toRight = ic.getTextAfterCursor(1, 0)
        if (!TextUtils.isEmpty(toLeft) && !isWordSeparator(toLeft!![0].code)) {
            return true
        }
        if (!TextUtils.isEmpty(toRight) && !isWordSeparator(toRight!![0].code)) {
            return true
        }
        return false
    }

    private fun sameAsTextBeforeCursor(ic: InputConnection, text: CharSequence): Boolean {
        val beforeText = ic.getTextBeforeCursor(text.length, 0)
        return TextUtils.equals(text, beforeText)
    }

    protected fun getWordSeparators(): String {
        return mWordSeparators
    }

    fun isWordSeparator(code: Int): Boolean {
        val separators = getWordSeparators()
        return separators.contains(code.toChar().toString())
    }

    private fun isSentenceSeparator(code: Int): Boolean {
        return mSentenceSeparators.contains(code.toChar().toString())
    }

    private fun sendSpace() {
        sendModifiableKeyChar(ASCII_SPACE.toChar())
        updateShiftKeyState(currentInputEditorInfo)
    }

    fun preferCapitalization(): Boolean {
        return false
    }

    fun toggleLanguage(reset: Boolean, next: Boolean) {
        reloadKeyboards()
        mKeyboardSwitcher!!.makeKeyboards(true)
        updateShiftKeyState(currentInputEditorInfo)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.i("PCKeyboard", "onSharedPreferenceChanged()")
        var needReload = false
        val res = resources

        if (key != null) sKeyboardSettings.sharedPreferenceChanged(sharedPreferences, key)
        if (sKeyboardSettings.hasFlag(GlobalKeyboardSettings.FLAG_PREF_NEED_RELOAD)) {
            needReload = true
        }
        if (sKeyboardSettings.hasFlag(GlobalKeyboardSettings.FLAG_PREF_RECREATE_INPUT_VIEW)) {
            mKeyboardSwitcher!!.recreateInputView()
        }
        if (sKeyboardSettings.hasFlag(GlobalKeyboardSettings.FLAG_PREF_RESET_MODE_OVERRIDE)) {
            mKeyboardModeOverrideLandscape = 0
            mKeyboardModeOverridePortrait = 0
        }
        if (sKeyboardSettings.hasFlag(GlobalKeyboardSettings.FLAG_PREF_RESET_KEYBOARDS)) {
            toggleLanguage(true, true)
        }
        val unhandledFlags = sKeyboardSettings.unhandledFlags()
        if (unhandledFlags != GlobalKeyboardSettings.FLAG_PREF_NONE) {
            Log.w(TAG, "Not all flag settings handled, remaining=$unhandledFlags")
        }

        when (key) {
            PREF_CONNECTBOT_TAB_HACK -> mConnectbotTabHack = sharedPreferences.getBoolean(
                    PREF_CONNECTBOT_TAB_HACK, res.getBoolean(R.bool.default_connectbot_tab_hack))
            PREF_FULLSCREEN_OVERRIDE -> {
                mFullscreenOverride = sharedPreferences.getBoolean(
                        PREF_FULLSCREEN_OVERRIDE, res.getBoolean(R.bool.default_fullscreen_override))
                needReload = true
            }
            PREF_FORCE_KEYBOARD_ON -> {
                mForceKeyboardOn = sharedPreferences.getBoolean(
                        PREF_FORCE_KEYBOARD_ON, res.getBoolean(R.bool.default_force_keyboard_on))
                needReload = true
            }
            PREF_KEYBOARD_NOTIFICATION -> {
                mKeyboardNotification = sharedPreferences.getBoolean(
                        PREF_KEYBOARD_NOTIFICATION, res.getBoolean(R.bool.default_keyboard_notification))
                setNotification(mKeyboardNotification)
            }
            PREF_HEIGHT_PORTRAIT -> {
                mHeightPortrait = getHeight(sharedPreferences,
                        PREF_HEIGHT_PORTRAIT, res.getString(R.string.default_height_portrait))
                needReload = true
            }
            PREF_HEIGHT_LANDSCAPE -> {
                mHeightLandscape = getHeight(sharedPreferences,
                        PREF_HEIGHT_LANDSCAPE, res.getString(R.string.default_height_landscape))
                needReload = true
            }
            PREF_HINT_MODE -> {
                sKeyboardSettings.hintMode = Integer.parseInt(sharedPreferences.getString(PREF_HINT_MODE,
                        res.getString(R.string.default_hint_mode)) ?: "0")
                needReload = true
            }
            PREF_LONGPRESS_TIMEOUT ->
                sKeyboardSettings.longpressTimeout = getPrefInt(sharedPreferences, PREF_LONGPRESS_TIMEOUT,
                        res.getString(R.string.default_long_press_duration))
            PREF_RENDER_MODE -> {
                sKeyboardSettings.renderMode = getPrefInt(sharedPreferences, PREF_RENDER_MODE,
                        res.getString(R.string.default_render_mode))
                needReload = true
            }
            PREF_SWIPE_UP -> mSwipeUpAction = sharedPreferences.getString(PREF_SWIPE_UP, res.getString(R.string.default_swipe_up)) ?: ""
            PREF_SWIPE_DOWN -> mSwipeDownAction = sharedPreferences.getString(PREF_SWIPE_DOWN, res.getString(R.string.default_swipe_down)) ?: ""
            PREF_SWIPE_LEFT -> mSwipeLeftAction = sharedPreferences.getString(PREF_SWIPE_LEFT, res.getString(R.string.default_swipe_left)) ?: ""
            PREF_SWIPE_RIGHT -> mSwipeRightAction = sharedPreferences.getString(PREF_SWIPE_RIGHT, res.getString(R.string.default_swipe_right)) ?: ""
            PREF_VOL_UP -> mVolUpAction = sharedPreferences.getString(PREF_VOL_UP, res.getString(R.string.default_vol_up)) ?: ""
            PREF_VOL_DOWN -> mVolDownAction = sharedPreferences.getString(PREF_VOL_DOWN, res.getString(R.string.default_vol_down)) ?: ""
            PREF_VIBRATE_LEN -> mVibrateLen = getPrefInt(sharedPreferences, PREF_VIBRATE_LEN, resources.getString(R.string.vibrate_duration_ms))
        }

        updateKeyboardOptions()
        if (needReload) {
            mKeyboardSwitcher!!.makeKeyboards(true)
        }
    }

    private fun doSwipeAction(action: String?): Boolean {
        if (action == null || action == "" || action == "none") {
            return false
        } else if (action == "close") {
            handleClose()
        } else if (action == "settings") {
            launchSettings()
        } else if (action == "lang_prev") {
            toggleLanguage(false, false)
        } else if (action == "lang_next") {
            toggleLanguage(false, true)
        } else if (action == "full_mode") {
            if (isPortrait()) {
                mKeyboardModeOverridePortrait = (mKeyboardModeOverridePortrait + 1) % mNumKeyboardModes
            } else {
                mKeyboardModeOverrideLandscape = (mKeyboardModeOverrideLandscape + 1) % mNumKeyboardModes
            }
            toggleLanguage(true, true)
        } else if (action == "extension") {
            sKeyboardSettings.useExtension = !sKeyboardSettings.useExtension
            reloadKeyboards()
        } else if (action == "height_up") {
            if (isPortrait()) {
                mHeightPortrait += 5
                if (mHeightPortrait > 70) mHeightPortrait = 70
            } else {
                mHeightLandscape += 5
                if (mHeightLandscape > 70) mHeightLandscape = 70
            }
            toggleLanguage(true, true)
        } else if (action == "height_down") {
            if (isPortrait()) {
                mHeightPortrait -= 5
                if (mHeightPortrait < 15) mHeightPortrait = 15
            } else {
                mHeightLandscape -= 5
                if (mHeightLandscape < 15) mHeightLandscape = 15
            }
            toggleLanguage(true, true)
        } else {
            Log.i(TAG, "Unsupported swipe action config: $action")
        }
        return true
    }

    override fun swipeRight(): Boolean = doSwipeAction(mSwipeRightAction)
    override fun swipeLeft(): Boolean = doSwipeAction(mSwipeLeftAction)
    override fun swipeDown(): Boolean = doSwipeAction(mSwipeDownAction)
    override fun swipeUp(): Boolean = doSwipeAction(mSwipeUpAction)

    override fun onPress(primaryCode: Int) {
        val ic = currentInputConnection
        if (mKeyboardSwitcher!!.isVibrateAndSoundFeedbackRequired()) {
            vibrate()
            playKeyClick(primaryCode)
        }
        val distinctMultiTouch = mKeyboardSwitcher!!.hasDistinctMultitouch()
        if (distinctMultiTouch && primaryCode == Keyboard.KEYCODE_SHIFT) {
            mShiftKeyState.onPress()
            startMultitouchShift()
        } else if (distinctMultiTouch && primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            changeKeyboardMode()
            mSymbolKeyState.onPress()
            mKeyboardSwitcher!!.setAutoModeSwitchStateMomentary()
        } else if (distinctMultiTouch && primaryCode == LatinKeyboardView.KEYCODE_CTRL_LEFT) {
            setModCtrl(!mModCtrl)
            mCtrlKeyState.onPress()
            sendCtrlKey(ic, true, true)
        } else if (distinctMultiTouch && primaryCode == LatinKeyboardView.KEYCODE_ALT_LEFT) {
            setModAlt(!mModAlt)
            mAltKeyState.onPress()
            sendAltKey(ic, true, true)
        } else if (distinctMultiTouch && primaryCode == LatinKeyboardView.KEYCODE_META_LEFT) {
            setModMeta(!mModMeta)
            mMetaKeyState.onPress()
            sendMetaKey(ic, true, true)
        } else if (distinctMultiTouch && primaryCode == LatinKeyboardView.KEYCODE_FN) {
            setModFn(!mModFn)
            mFnKeyState.onPress()
        } else {
            mShiftKeyState.onOtherKeyPressed()
            mSymbolKeyState.onOtherKeyPressed()
            mCtrlKeyState.onOtherKeyPressed()
            mAltKeyState.onOtherKeyPressed()
            mMetaKeyState.onOtherKeyPressed()
            mFnKeyState.onOtherKeyPressed()
        }
    }

    override fun onRelease(primaryCode: Int) {
        // Reset any drag flags in the keyboard
        (mKeyboardSwitcher!!.getInputView()!!.getKeyboard() as LatinKeyboard).keyReleased()
        val distinctMultiTouch = mKeyboardSwitcher!!.hasDistinctMultitouch()
        val ic = currentInputConnection
        if (distinctMultiTouch && primaryCode == Keyboard.KEYCODE_SHIFT) {
            if (mShiftKeyState.isChording()) {
                resetMultitouchShift()
            } else {
                commitMultitouchShift()
            }
            mShiftKeyState.onRelease()
        } else if (distinctMultiTouch && primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            if (mKeyboardSwitcher!!.isInChordingAutoModeSwitchState())
                changeKeyboardMode()
            mSymbolKeyState.onRelease()
        } else if (distinctMultiTouch && primaryCode == LatinKeyboardView.KEYCODE_CTRL_LEFT) {
            if (mCtrlKeyState.isChording()) {
                setModCtrl(false)
            }
            sendCtrlKey(ic, false, true)
            mCtrlKeyState.onRelease()
        } else if (distinctMultiTouch && primaryCode == LatinKeyboardView.KEYCODE_ALT_LEFT) {
            if (mAltKeyState.isChording()) {
                setModAlt(false)
            }
            sendAltKey(ic, false, true)
            mAltKeyState.onRelease()
        } else if (distinctMultiTouch && primaryCode == LatinKeyboardView.KEYCODE_META_LEFT) {
            if (mMetaKeyState.isChording()) {
                setModMeta(false)
            }
            sendMetaKey(ic, false, true)
            mMetaKeyState.onRelease()
        } else if (distinctMultiTouch && primaryCode == LatinKeyboardView.KEYCODE_FN) {
            if (mFnKeyState.isChording()) {
                setModFn(false)
            }
            mFnKeyState.onRelease()
        }
        // WARNING: Adding a chording modifier key? Make sure you also
        // edit PointerTracker.isModifierInternal(), otherwise it will
        // force a release event instead of chording.
    }

    // receive ringer mode changes to detect silent mode
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateRingerMode()
        }
    }

    // update flags for silent mode
    private fun updateRingerMode() {
        if (mAudioManager == null) {
            mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        mAudioManager?.let {
            mSilentMode = it.ringerMode != AudioManager.RINGER_MODE_NORMAL
        }
    }

    private fun getKeyClickVolume(): Float {
        if (mAudioManager == null) return 0.0f

        val method = sKeyboardSettings.keyClickMethod
        if (method == 0) return FX_VOLUME

        var targetVol = sKeyboardSettings.keyClickVolume

        if (method > 1) {
            val mediaMax = mAudioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val mediaVol = mAudioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
            val channelVol = mediaVol.toFloat() / mediaMax
            if (method == 2) {
                targetVol *= channelVol
            } else if (method == 3) {
                if (channelVol == 0f) return 0.0f
                targetVol = Math.min(targetVol / channelVol, 1.0f)
            }
        }
        val vol = Math.pow(10.0, (FX_VOLUME_RANGE_DB * (targetVol - 1) / 20).toDouble()).toFloat()
        return vol
    }

    private fun playKeyClick(primaryCode: Int) {
        if (mAudioManager == null) {
            if (mKeyboardSwitcher!!.getInputView() != null) {
                updateRingerMode()
            }
        }
        if (mSoundOn && !mSilentMode) {
            var sound = AudioManager.FX_KEYPRESS_STANDARD
            when (primaryCode) {
                Keyboard.KEYCODE_DELETE -> sound = AudioManager.FX_KEYPRESS_DELETE
                ASCII_ENTER -> sound = AudioManager.FX_KEYPRESS_RETURN
                ASCII_SPACE -> sound = AudioManager.FX_KEYPRESS_SPACEBAR
            }
            mAudioManager!!.playSoundEffect(sound, getKeyClickVolume())
        }
    }

    private fun vibrate() {
        if (!mVibrateOn) return
        vibrate(mVibrateLen)
    }

    fun vibrate(len: Int) {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (v != null) {
            v.vibrate(len.toLong())
            return
        }

        mKeyboardSwitcher?.getInputView()?.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
    }

    /* package */ fun getPopupOn(): Boolean = mPopupOn

    private fun updateAutoTextEnabled(systemLocale: java.util.Locale) {
    }

    protected open fun launchSettings() {
        launchSettings(MaterialSettingsActivity::class.java)
    }

    protected fun launchSettings(settingsClass: Class<out android.app.Activity>) {
        val intent = Intent()
        intent.setClass(this@LatinIME, settingsClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun loadSettings() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        mVibrateOn = sp.getBoolean(PREF_VIBRATE_ON, false)
        mVibrateLen = getPrefInt(sp, PREF_VIBRATE_LEN, resources.getString(R.string.vibrate_duration_ms))
        mSoundOn = sp.getBoolean(PREF_SOUND_ON, false)
        mPopupOn = sp.getBoolean(PREF_POPUP_ON, mResources.getBoolean(R.bool.default_popup_preview))
        mAutoCapPref = sp.getBoolean(PREF_AUTO_CAP, resources.getBoolean(R.bool.default_auto_cap))
        mQuickFixes = sp.getBoolean(PREF_QUICK_FIXES, true)

        mAutoCapActive = mAutoCapPref
        mDeadKeysActive = true
    }

    private fun showOptionsMenu() {
        val context = android.view.ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Light_Dialog)
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        builder.setIcon(R.drawable.ic_dialog_keyboard)
        builder.setNegativeButton(android.R.string.cancel, null)
        val itemSettings: CharSequence = getString(R.string.english_ime_settings)
        val itemInputMethod: CharSequence = getString(R.string.selectInputMethod)
        builder.setItems(arrayOf<CharSequence>(itemInputMethod, itemSettings),
                DialogInterface.OnClickListener { di, position ->
                    di.dismiss()
                    when (position) {
                        POS_SETTINGS -> launchSettings()
                        POS_METHOD -> (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                    }
                })
        builder.setTitle(mResources.getString(R.string.english_ime_input_options))
        mOptionsDialog = builder.create()
        val window = mOptionsDialog!!.window
        val lp = window!!.attributes
        lp.token = mKeyboardSwitcher!!.getInputView()!!.windowToken
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        window.attributes = lp
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        mOptionsDialog!!.show()
    }

    fun changeKeyboardMode() {
        val switcher = mKeyboardSwitcher!!
        if (switcher.isAlphabetMode()) {
            mSavedShiftState = getShiftState()
        }
        switcher.toggleSymbols()
        if (switcher.isAlphabetMode()) {
            switcher.setShiftState(mSavedShiftState)
        }
        updateShiftKeyState(currentInputEditorInfo)
    }

    override fun dump(fd: FileDescriptor, fout: PrintWriter, args: Array<String>) {
        super.dump(fd, fout, args)
        val p = PrintWriterPrinter(fout)
        p.println("LatinIME state :")
        p.println("  mSoundOn=$mSoundOn")
        p.println("  mVibrateOn=$mVibrateOn")
        p.println("  mPopupOn=$mPopupOn")
    }

    private fun measureCps() {
        val now = System.currentTimeMillis()
        if (mLastCpsTime == 0L) mLastCpsTime = now - 100 // Initial
        mCpsIntervals[mCpsIndex] = now - mLastCpsTime
        mLastCpsTime = now
        mCpsIndex = (mCpsIndex + 1) % CPS_BUFFER_SIZE
        var total = 0L
        for (i in 0 until CPS_BUFFER_SIZE) total += mCpsIntervals[i]
        System.out.println("CPS = " + (CPS_BUFFER_SIZE * 1000f) / total)
    }

    fun onAutoCompletionStateChanged(isAutoCompletion: Boolean) {
        mKeyboardSwitcher!!.onAutoCompletionStateChanged(isAutoCompletion)
    }
}
