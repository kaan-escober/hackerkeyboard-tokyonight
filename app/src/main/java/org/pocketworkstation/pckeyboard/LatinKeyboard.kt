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

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.inputmethod.EditorInfo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class LatinKeyboard @JvmOverloads constructor(
    context: Context,
    xmlLayoutResId: Int,
    mode: Int = 0,
    kbHeightPercent: Float = 0f
) : Keyboard(context, 0, xmlLayoutResId, mode, kbHeightPercent) {

    private var mShiftLockIcon: Drawable? = null
    private var mShiftLockPreviewIcon: Drawable? = null
    private var mOldShiftIcon: Drawable? = null
    private var mSpaceIcon: Drawable? = null
    private var mSpaceAutoCompletionIndicator: Drawable? = null
    private var mSpacePreviewIcon: Drawable? = null
    private var mMicIcon: Drawable? = null
    private var mMicPreviewIcon: Drawable? = null
    private var mSettingsIcon: Drawable? = null
    private var mSettingsPreviewIcon: Drawable? = null
    private var m123MicIcon: Drawable? = null
    private var m123MicPreviewIcon: Drawable? = null
    private val mButtonArrowLeftIcon: Drawable
    private val mButtonArrowRightIcon: Drawable
    private var mShiftKey: Key? = null
    private var mEnterKey: Key? = null
    private var mF1Key: Key? = null
    private val mHintIcon: Drawable
    private var mSpaceKey: Key? = null
    private var m123Key: Key? = null
    private val mSpaceKeyIndexArray: IntArray
    private var mSpaceDragStartX: Int = 0
    private var mSpaceDragLastDiff: Int = 0
    private val mRes: Resources
    private val mContext: Context
    private var mMode: Int = 0
    private val mIsAlphaKeyboard: Boolean
    private val mIsAlphaFullKeyboard: Boolean
    private val mIsFnFullKeyboard: Boolean
    private var m123Label: CharSequence? = null
    private var mCurrentlyInSpace: Boolean = false
    private var mPrefLetterFrequencies: IntArray? = null
    private var mPrefLetter: Int = 0
    private var mPrefLetterX: Int = 0
    private var mPrefLetterY: Int = 0
    private var mPrefDistance: Int = 0

    private var mExtensionResId: Int = 0

    // TODO: remove this attribute when either Keyboard.mDefaultVerticalGap or Key.parent becomes
    // non-private.
    private val mVerticalGap: Int

    private var mExtensionKeyboard: LatinKeyboard? = null

    companion object {
        private const val DEBUG_PREFERRED_LETTER = true
        private const val TAG = "PCKeyboardLK"
        private const val OPACITY_FULLY_OPAQUE = 255
        private const val SPACE_LED_LENGTH_PERCENT = 80
        private const val SPACEBAR_DRAG_THRESHOLD = 0.51f
        private const val OVERLAP_PERCENTAGE_LOW_PROB = 0.70f
        private const val OVERLAP_PERCENTAGE_HIGH_PROB = 0.85f
        private const val SPACEBAR_POPUP_MIN_RATIO = 0.4f
        private const val SPACEBAR_POPUP_MAX_RATIO = 0.4f
        private const val SPACEBAR_LANGUAGE_BASELINE = 0.6f
        private const val MINIMUM_SCALE_OF_LANGUAGE_NAME = 0.8f

        @JvmField
        var sSpacebarVerticalCorrection: Int = 0

        @JvmStatic
        fun hasPuncOrSmileysPopup(key: Key): Boolean =
            key.popupResId == R.xml.popup_punctuation || key.popupResId == R.xml.popup_smileys
    }

    init {
        val res = context.getResources()
        mContext = context
        mMode = mode
        mRes = res
        mShiftLockIcon = res.getDrawable(R.drawable.sym_keyboard_shift_locked)
        mShiftLockPreviewIcon = res.getDrawable(R.drawable.sym_keyboard_feedback_shift_locked)
        mShiftLockPreviewIcon?.setDefaultBounds()
        mSpaceIcon = res.getDrawable(R.drawable.sym_keyboard_space)
        mSpaceAutoCompletionIndicator = res.getDrawable(R.drawable.sym_keyboard_space_led)
        mSpacePreviewIcon = res.getDrawable(R.drawable.sym_keyboard_feedback_space)
        mMicIcon = res.getDrawable(R.drawable.sym_keyboard_mic)
        mMicPreviewIcon = res.getDrawable(R.drawable.sym_keyboard_feedback_mic)
        mSettingsIcon = res.getDrawable(R.drawable.sym_keyboard_settings)
        mSettingsPreviewIcon = res.getDrawable(R.drawable.sym_keyboard_feedback_settings)
        mMicPreviewIcon?.setDefaultBounds()
        mButtonArrowLeftIcon = res.getDrawable(R.drawable.sym_keyboard_language_arrows_left)
        mButtonArrowRightIcon = res.getDrawable(R.drawable.sym_keyboard_language_arrows_right)
        m123MicIcon = res.getDrawable(R.drawable.sym_keyboard_123_mic)
        m123MicPreviewIcon = res.getDrawable(R.drawable.sym_keyboard_feedback_123_mic)
        mHintIcon = res.getDrawable(R.drawable.hint_popup)
        m123MicPreviewIcon?.setDefaultBounds()
        sSpacebarVerticalCorrection = res.getDimensionPixelOffset(R.dimen.spacebar_vertical_correction)
        mIsAlphaKeyboard = xmlLayoutResId == R.xml.kbd_qwerty
        mIsAlphaFullKeyboard = xmlLayoutResId == R.xml.kbd_full
        mIsFnFullKeyboard = xmlLayoutResId == R.xml.kbd_full_fn || xmlLayoutResId == R.xml.kbd_compact_fn
        mSpaceKeyIndexArray = intArrayOf(indexOf(LatinIME.ASCII_SPACE))
        mVerticalGap = super.getVerticalGap()
    }

    private fun Drawable.setDefaultBounds() {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }

    override fun createKeyFromXml(res: Resources, parent: Row, x: Int, y: Int,
            parser: XmlResourceParser): Key {
        val key = LatinKey(res, parent, x, y, parser)
        val codes = key.codes ?: return key
        when (codes[0]) {
            LatinIME.ASCII_ENTER -> mEnterKey = key
            LatinKeyboardView.KEYCODE_F1 -> mF1Key = key
            LatinIME.ASCII_SPACE -> mSpaceKey = key
            KEYCODE_MODE_CHANGE -> {
                m123Key = key
                m123Label = key.label
            }
        }
        return key
    }

    fun setImeOptions(res: Resources, mode: Int, options: Int) {
        mMode = mode
        val enterKey = mEnterKey ?: return
        // Reset some of the rarely used attributes.
        enterKey.popupCharacters = null
        enterKey.popupResId = 0
        enterKey.text = null
        when (options and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            EditorInfo.IME_ACTION_GO -> {
                enterKey.iconPreview = null
                enterKey.icon = null
                enterKey.label = res.getText(R.string.label_go_key)
            }
            EditorInfo.IME_ACTION_NEXT -> {
                enterKey.iconPreview = null
                enterKey.icon = null
                enterKey.label = res.getText(R.string.label_next_key)
            }
            EditorInfo.IME_ACTION_DONE -> {
                enterKey.iconPreview = null
                enterKey.icon = null
                enterKey.label = res.getText(R.string.label_done_key)
            }
            EditorInfo.IME_ACTION_SEARCH -> {
                enterKey.iconPreview = res.getDrawable(R.drawable.sym_keyboard_feedback_search)
                enterKey.icon = res.getDrawable(R.drawable.sym_keyboard_search)
                enterKey.label = null
            }
            EditorInfo.IME_ACTION_SEND -> {
                enterKey.iconPreview = null
                enterKey.icon = null
                enterKey.label = res.getText(R.string.label_send_key)
            }
            else -> {
                // Keep Return key in IM mode, we have a dedicated smiley key.
                enterKey.iconPreview = res.getDrawable(R.drawable.sym_keyboard_feedback_return)
                enterKey.icon = res.getDrawable(R.drawable.sym_keyboard_return)
                enterKey.label = null
            }
        }
        // Set the initial size of the preview icon
        enterKey.iconPreview?.setDefaultBounds()
    }

    fun enableShiftLock() {
        val index = getShiftKeyIndex()
        if (index >= 0) {
            mShiftKey = getKeys().get(index)
            mOldShiftIcon = mShiftKey?.icon
        }
    }

    override fun setShiftState(shiftState: Int, updateKey: Boolean): Boolean {
        val shiftKey = mShiftKey
        if (shiftKey != null) {
            // Tri-state LED tracks "on" and "lock" states, icon shows Caps state.
            shiftKey.on = shiftState == SHIFT_ON || shiftState == SHIFT_LOCKED
            shiftKey.locked = shiftState == SHIFT_LOCKED || shiftState == SHIFT_CAPS_LOCKED
            shiftKey.icon = if (shiftState == SHIFT_OFF || shiftState == SHIFT_ON || shiftState == SHIFT_LOCKED)
                mOldShiftIcon else mShiftLockIcon
            return super.setShiftState(shiftState, false)
        } else {
            return super.setShiftState(shiftState, true)
        }
    }

    /* package */ fun isAlphaKeyboard(): Boolean = mIsAlphaKeyboard

    fun setExtension(extKeyboard: LatinKeyboard) {
        mExtensionKeyboard = extKeyboard
    }

    fun getExtension(): LatinKeyboard? = mExtensionKeyboard

    fun updateSymbolIcons(isAutoCompletion: Boolean) {
        updateDynamicKeys()
        updateSpaceBarForLocale(isAutoCompletion)
    }

    fun setVoiceMode(hasVoiceButton: Boolean, hasVoice: Boolean) {
        // Voice mode removed
        updateDynamicKeys()
    }

    private fun updateDynamicKeys() {
        update123Key()
        updateF1Key()
    }

    private fun update123Key() {
        // Update KEYCODE_MODE_CHANGE key only on alphabet mode, not on symbol mode.
        val key = m123Key
        if (key != null && mIsAlphaKeyboard) {
            key.icon = null
            key.iconPreview = null
            key.label = m123Label
        }
    }

    private fun updateF1Key() {
        // Update KEYCODE_F1 key. Please note that some keyboard layouts have no F1 key.
        val key = mF1Key ?: return

        if (mIsAlphaKeyboard) {
            when (mMode) {
                KeyboardSwitcher.MODE_URL -> setNonMicF1Key(key, "/", R.xml.popup_slash)
                KeyboardSwitcher.MODE_EMAIL -> setNonMicF1Key(key, "@", R.xml.popup_at)
                else -> setNonMicF1Key(key, ",", R.xml.popup_comma)
            }
        } else if (mIsAlphaFullKeyboard) {
            setSettingsF1Key(key)
        } else if (mIsFnFullKeyboard) {
            // No mic key support
        } else {  // Symbols keyboard
            setNonMicF1Key(key, ",", R.xml.popup_comma)
        }
    }

    private fun setMicF1Key(key: Key) {
        // Voice support removed
    }

    private fun setSettingsF1Key(key: Key) {
        if (key.shiftLabel != null && key.label != null) {
            key.codes = intArrayOf(key.label!![0].code)
            return // leave key otherwise unmodified
        }
        val settingsHintDrawable = BitmapDrawable(mRes,
            drawSynthesizedSettingsHintImage(key.width, key.height, mSettingsIcon, mHintIcon))
        key.label = null
        key.icon = settingsHintDrawable
        key.codes = intArrayOf(LatinKeyboardView.KEYCODE_OPTIONS)
        key.popupResId = R.xml.popup_mic
        key.iconPreview = mSettingsPreviewIcon
    }

    private fun setNonMicF1Key(key: Key, label: String, popupResId: Int) {
        if (key.shiftLabel != null) {
            key.codes = intArrayOf(key.label!![0].code)
            return // leave key unmodified
        }
        key.label = label
        key.codes = intArrayOf(label[0].code)
        key.popupResId = popupResId
        key.icon = mHintIcon
        key.iconPreview = null
    }

    fun isF1Key(key: Key): Boolean = key == mF1Key

    /**
     * @return a key which should be invalidated.
     */
    fun onAutoCompletionStateChanged(isAutoCompletion: Boolean): Key? {
        updateSpaceBarForLocale(isAutoCompletion)
        return mSpaceKey
    }

    fun isLanguageSwitchEnabled(): Boolean = false

    private fun toTitleCase(s: String): String {
        if (s.isEmpty()) return s
        return s[0].uppercaseChar() + s.substring(1)
    }

    private fun updateSpaceBarForLocale(isAutoCompletion: Boolean) {
        val spaceKey = mSpaceKey ?: return
        spaceKey.icon = BitmapDrawable(mRes, drawSpaceBar(OPACITY_FULLY_OPAQUE, isAutoCompletion))
    }

    // Compute width of text with specified text size using paint.
    private fun getTextWidth(paint: Paint, text: String, textSize: Float, bounds: Rect): Int {
        paint.textSize = textSize
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.width()
    }

    // Overlay two images: mainIcon and hintIcon.
    private fun drawSynthesizedSettingsHintImage(
            width: Int, height: Int, mainIcon: Drawable?, hintIcon: Drawable?): Bitmap? {
        if (mainIcon == null || hintIcon == null) return null
        val hintIconPadding = Rect(0, 0, 0, 0)
        hintIcon.getPadding(hintIconPadding)
        val buffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(buffer)
        canvas.drawColor(mRes.getColor(R.color.latinkeyboard_transparent), PorterDuff.Mode.CLEAR)

        // Draw main icon at the center of the key visual
        val drawableX = (width + hintIconPadding.left - hintIconPadding.right
                - mainIcon.intrinsicWidth) / 2
        val drawableY = (height + hintIconPadding.top - hintIconPadding.bottom
                - mainIcon.intrinsicHeight) / 2
        mainIcon.setDefaultBounds()
        canvas.translate(drawableX.toFloat(), drawableY.toFloat())
        mainIcon.draw(canvas)
        canvas.translate(-drawableX.toFloat(), -drawableY.toFloat())

        // Draw hint icon fully in the key
        hintIcon.setBounds(0, 0, width, height)
        hintIcon.draw(canvas)
        return buffer
    }

    private fun drawSpaceBar(opacity: Int, isAutoCompletion: Boolean): Bitmap {
        val spaceKey = mSpaceKey!!
        val spaceIcon = mSpaceIcon!!
        val width = spaceKey.width
        val height = spaceIcon.intrinsicHeight
        val buffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(buffer)
        canvas.drawColor(mRes.getColor(R.color.latinkeyboard_transparent), PorterDuff.Mode.CLEAR)

        // Draw the spacebar icon at the bottom
        if (isAutoCompletion) {
            val autoIcon = mSpaceAutoCompletionIndicator!!
            val iconWidth = width * SPACE_LED_LENGTH_PERCENT / 100
            val iconHeight = autoIcon.intrinsicHeight
            val x = (width - iconWidth) / 2
            val y = height - iconHeight
            autoIcon.setBounds(x, y, x + iconWidth, y + iconHeight)
            autoIcon.draw(canvas)
        } else {
            val iconWidth = spaceIcon.intrinsicWidth
            val iconHeight = spaceIcon.intrinsicHeight
            val x = (width - iconWidth) / 2
            val y = height - iconHeight
            spaceIcon.setBounds(x, y, x + iconWidth, y + iconHeight)
            spaceIcon.draw(canvas)
        }
        return buffer
    }

    private fun getSpacePreviewWidth(): Int {
        return min(
            max(mSpaceKey!!.width, (getMinWidth() * SPACEBAR_POPUP_MIN_RATIO).toInt()),
            (getScreenHeight() * SPACEBAR_POPUP_MAX_RATIO).toInt())
    }

    private fun updateLocaleDrag(diff: Int) {
        // Language switching is removed, so this method does nothing.
    }

    fun getLanguageChangeDirection(): Int = 0 // Language switching is removed

    fun isCurrentlyInSpace(): Boolean = mCurrentlyInSpace

    fun setPreferredLetters(frequencies: IntArray) {
        mPrefLetterFrequencies = frequencies
        mPrefLetter = 0
    }

    fun keyReleased() {
        mCurrentlyInSpace = false
        mSpaceDragLastDiff = 0
        mPrefLetter = 0
        mPrefLetterX = 0
        mPrefLetterY = 0
        mPrefDistance = Integer.MAX_VALUE
        // No language switcher, so no need to update locale drag
    }

    /**
     * Does the magic of locking the touch gesture into the spacebar when
     * switching input languages.
     */
    fun isInside(key: LatinKey, x: Int, y: Int): Boolean {
        var xCoord = x
        var yCoord = y
        val code = key.codes!![0]
        if (code == KEYCODE_SHIFT || code == KEYCODE_DELETE) {
            // Adjust target area for these keys
            yCoord -= key.height / 10
            if (code == KEYCODE_SHIFT) {
                if (key.x == 0) {
                    xCoord += key.width / 6  // left shift
                } else {
                    xCoord -= key.width / 6  // right shift
                }
            }
            if (code == KEYCODE_DELETE) xCoord -= key.width / 6
        } else if (code == LatinIME.ASCII_SPACE) {
            yCoord += sSpacebarVerticalCorrection
        } else if (mPrefLetterFrequencies != null) {
            val pref = mPrefLetterFrequencies!!
            // New coordinate? Reset
            if (mPrefLetterX != xCoord || mPrefLetterY != yCoord) {
                mPrefLetter = 0
                mPrefDistance = Integer.MAX_VALUE
            }
            // Handle preferred next letter
            if (mPrefLetter > 0) {
                if (DEBUG_PREFERRED_LETTER) {
                    if (mPrefLetter == code && !key.isInsideSuper(xCoord, yCoord)) {
                        Log.d(TAG, "CORRECTED !!!!!!")
                    }
                }
                return mPrefLetter == code
            } else {
                val inside = key.isInsideSuper(xCoord, yCoord)
                val nearby = getNearestKeys(xCoord, yCoord)
                val nearbyKeys = getKeys()
                if (inside) {
                    // If it's a preferred letter
                    if (inPrefList(code, pref)) {
                        // Check if its frequency is much lower than a nearby key
                        mPrefLetter = code
                        mPrefLetterX = xCoord
                        mPrefLetterY = yCoord
                        for (i in nearby.indices) {
                            val k = nearbyKeys.get(nearby[i])
                            val kCode = k.codes!![0]
                            if (k != key && inPrefList(kCode, pref)) {
                                val dist = distanceFrom(k, xCoord, yCoord)
                                if (dist < (k.width * OVERLAP_PERCENTAGE_LOW_PROB).toInt() &&
                                        pref[kCode] > pref[mPrefLetter] * 3) {
                                    mPrefLetter = kCode
                                    mPrefDistance = dist
                                    if (DEBUG_PREFERRED_LETTER) {
                                        Log.d(TAG, "CORRECTED ALTHOUGH PREFERRED !!!!!!")
                                    }
                                    break
                                }
                            }
                        }
                        return mPrefLetter == code
                    }
                }

                // Get the surrounding keys and intersect with the preferred list
                for (i in nearby.indices) {
                    val k = nearbyKeys.get(nearby[i])
                    val kCode2 = k.codes!![0]
                    if (inPrefList(kCode2, pref)) {
                        val dist = distanceFrom(k, xCoord, yCoord)
                        if (dist < (k.width * OVERLAP_PERCENTAGE_HIGH_PROB).toInt()
                                && dist < mPrefDistance) {
                            mPrefLetter = kCode2
                            mPrefLetterX = xCoord
                            mPrefLetterY = yCoord
                            mPrefDistance = dist
                        }
                    }
                }
                // Didn't find any
                return if (mPrefLetter == 0) inside else mPrefLetter == code
            }
        }

        return key.isInsideSuper(xCoord, yCoord)
    }

    private fun inPrefList(code: Int, pref: IntArray): Boolean {
        if (code < pref.size && code >= 0) return pref[code] > 0
        return false
    }

    private fun distanceFrom(k: Key, x: Int, y: Int): Int {
        return if (y > k.y && y < k.y + k.height) {
            abs(k.x + k.width / 2 - x)
        } else {
            Integer.MAX_VALUE
        }
    }

    override fun getNearestKeys(x: Int, y: Int): IntArray {
        return if (mCurrentlyInSpace) {
            mSpaceKeyIndexArray
        } else {
            // Avoid dead pixels at edges of the keyboard
            super.getNearestKeys(
                max(0, min(x, getMinWidth() - 1)),
                max(0, min(y, getHeight() - 1)))
        }
    }

    private fun indexOf(code: Int): Int {
        val keys = getKeys()
        val count = keys.size
        for (i in 0 until count) {
            if (keys.get(i).codes!![0] == code) return i
        }
        return -1
    }

    private fun getTextSizeFromTheme(style: Int, defValue: Int): Int {
        val array: TypedArray = mContext.theme.obtainStyledAttributes(
            style, intArrayOf(android.R.attr.textSize))
        val resId = array.getResourceId(0, 0)
        if (resId >= array.length()) {
            Log.i(TAG, "getTextSizeFromTheme error: resId $resId > ${array.length()}")
            return defValue
        }
        val textSize = array.getDimensionPixelSize(resId, defValue)
        return textSize
    }

    // TODO LatinKey could be static class
    inner class LatinKey(res: Resources, parent: Keyboard.Row, x: Int, y: Int,
            parser: XmlResourceParser) : Key(res, parent, x, y, parser) {

        // functional normal state (with properties)
        private val KEY_STATE_FUNCTIONAL_NORMAL = intArrayOf(
            android.R.attr.state_single
        )

        // functional pressed state (with properties)
        private val KEY_STATE_FUNCTIONAL_PRESSED = intArrayOf(
            android.R.attr.state_single,
            android.R.attr.state_pressed
        )

        // functional checked state (for sticky modifiers like Shift/Alt/Ctrl)
        private val KEY_STATE_FUNCTIONAL_CHECKED = intArrayOf(
            android.R.attr.state_single,
            android.R.attr.state_checked
        )

        // functional pressed + checked state
        private val KEY_STATE_FUNCTIONAL_PRESSED_CHECKED = intArrayOf(
            android.R.attr.state_single,
            android.R.attr.state_pressed,
            android.R.attr.state_checked
        )

        // functional is used for styling.
        private fun isFunctionalKey(): Boolean = modifier

        /**
         * Overriding this method so that we can reduce the target area for certain keys.
         */
        override fun isInside(x: Int, y: Int): Boolean {
            // TODO This should be done by parent.isInside(this, x, y)
            // if Key.parent were protected.
            return this@LatinKeyboard.isInside(this, x, y)
        }

        fun isInsideSuper(x: Int, y: Int): Boolean = super.isInside(x, y)

        override fun getCurrentDrawableState(): IntArray {
            if (isFunctionalKey()) {
                return if (pressed) {
                    if (sticky && (on || locked)) KEY_STATE_FUNCTIONAL_PRESSED_CHECKED
                    else KEY_STATE_FUNCTIONAL_PRESSED
                } else {
                    if (sticky && (on || locked)) KEY_STATE_FUNCTIONAL_CHECKED
                    else KEY_STATE_FUNCTIONAL_NORMAL
                }
            }
            return super.getCurrentDrawableState()
        }

        override fun squaredDistanceFrom(x: Int, y: Int): Int {
            // We should count vertical gap between rows to calculate the center of this Key.
            val verticalGap = this@LatinKeyboard.mVerticalGap
            val xDist = this.x + width / 2 - x
            val yDist = this.y + (height + verticalGap) / 2 - y
            return xDist * xDist + yDist * yDist
        }
    }
}
