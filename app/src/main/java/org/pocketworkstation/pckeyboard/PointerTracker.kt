/*
 * Copyright (C) 2010 Google Inc.
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

import android.content.res.Resources
import android.util.Log
import android.view.MotionEvent
import org.pocketworkstation.pckeyboard.Keyboard.Key
import org.pocketworkstation.pckeyboard.LatinKeyboardBaseView.OnKeyboardActionListener
import org.pocketworkstation.pckeyboard.LatinKeyboardBaseView.UIHandler

class PointerTracker internal constructor(
    @JvmField val mPointerId: Int,
    internal val mHandler: UIHandler,
    private val mKeyDetector: KeyDetector,
    private val mProxy: UIProxy,
    res: Resources,
    slideKeyHack: Boolean
) {
    interface UIProxy {
        fun invalidateKey(key: Key)
        fun showPreview(keyIndex: Int, tracker: PointerTracker)
        fun hasDistinctMultitouch(): Boolean
    }

    // Timing constants
    private val mDelayBeforeKeyRepeatStart: Int
    private val mMultiTapKeyTimeout: Int

    private var mListener: OnKeyboardActionListener? = null
    private val mKeyboardSwitcher: KeyboardSwitcher
    private val mHasDistinctMultitouch: Boolean

    private var mKeys: Array<Key>? = null
    private var mKeyHysteresisDistanceSquared = -1

    private val mKeyState: KeyState

    // true if keyboard layout has been changed.
    private var mKeyboardLayoutHasBeenChanged = false

    // true if event is already translated to a key action (long press or mini-keyboard)
    private var mKeyAlreadyProcessed = false

    // true if this pointer is repeatable key
    private var mIsRepeatableKey = false

    // true if this pointer is in sliding key input
    private var mIsInSlidingKeyInput = false

    // For multi-tap
    private var mLastSentIndex = NOT_A_KEY
    private var mTapCount = 0
    private var mLastTapTime = -1L
    private var mInMultiTap = false
    private val mPreviewLabel = StringBuilder(1)

    // pressed key
    private var mPreviousKey = NOT_A_KEY

    init {
        mKeyboardSwitcher = KeyboardSwitcher.getInstance()
        mKeyState = KeyState(mKeyDetector)
        mHasDistinctMultitouch = mProxy.hasDistinctMultitouch()
        mDelayBeforeKeyRepeatStart = res.getInteger(R.integer.config_delay_before_key_repeat_start)
        mMultiTapKeyTimeout = res.getInteger(R.integer.config_multi_tap_key_timeout)
        sSlideKeyHack = slideKeyHack
        resetMultiTap()
    }

    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener) {
        mListener = listener
    }

    fun setKeyboard(keys: Array<Key>, keyHysteresisDistance: Float) {
        require(keyHysteresisDistance >= 0) { "keyHysteresisDistance must be >= 0" }
        mKeys = keys
        mKeyHysteresisDistanceSquared = (keyHysteresisDistance * keyHysteresisDistance).toInt()
        // Mark that keyboard layout has been changed.
        mKeyboardLayoutHasBeenChanged = true
    }

    fun isInSlidingKeyInput(): Boolean = mIsInSlidingKeyInput

    fun setSlidingKeyInputState(state: Boolean) {
        mIsInSlidingKeyInput = state
    }

    private fun isValidKeyIndex(keyIndex: Int): Boolean {
        val keys = mKeys ?: return false
        return keyIndex >= 0 && keyIndex < keys.size
    }

    fun getKey(keyIndex: Int): Key? {
        val keys = mKeys ?: return null
        return if (isValidKeyIndex(keyIndex)) keys[keyIndex] else null
    }

    private fun isModifierInternal(keyIndex: Int): Boolean {
        val key = getKey(keyIndex) ?: return false
        if (key.codes == null) return false
        val primaryCode = key.codes!![0]
        return primaryCode == Keyboard.KEYCODE_SHIFT ||
                primaryCode == Keyboard.KEYCODE_MODE_CHANGE ||
                primaryCode == LatinKeyboardView.KEYCODE_CTRL_LEFT ||
                primaryCode == LatinKeyboardView.KEYCODE_ALT_LEFT ||
                primaryCode == LatinKeyboardView.KEYCODE_META_LEFT ||
                primaryCode == LatinKeyboardView.KEYCODE_FN
    }

    fun isModifier(): Boolean {
        return isModifierInternal(mKeyState.keyIndex)
    }

    fun isOnModifierKey(x: Int, y: Int): Boolean {
        return isModifierInternal(mKeyDetector.getKeyIndexAndNearbyCodes(x, y, null))
    }

    fun isSpaceKey(keyIndex: Int): Boolean {
        val key = getKey(keyIndex) ?: return false
        return key.codes != null && key.codes!![0] == LatinIME.ASCII_SPACE
    }

    fun updateKey(keyIndex: Int) {
        if (mKeyAlreadyProcessed)
            return
        val oldKeyIndex = mPreviousKey
        mPreviousKey = keyIndex
        if (keyIndex != oldKeyIndex) {
            val keys = mKeys ?: return
            if (isValidKeyIndex(oldKeyIndex)) {
                // if new key index is not a key, old key was just released inside of the key.
                val inside = (keyIndex == NOT_A_KEY)
                keys[oldKeyIndex].onReleased(inside)
                mProxy.invalidateKey(keys[oldKeyIndex])
            }
            if (isValidKeyIndex(keyIndex)) {
                keys[keyIndex].onPressed()
                mProxy.invalidateKey(keys[keyIndex])
            }
        }
    }

    fun setAlreadyProcessed() {
        mKeyAlreadyProcessed = true
    }

    fun onTouchEvent(action: Int, x: Int, y: Int, eventTime: Long) {
        when (action) {
            MotionEvent.ACTION_MOVE -> onMoveEvent(x, y, eventTime)
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> onDownEvent(x, y, eventTime)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> onUpEvent(x, y, eventTime)
            MotionEvent.ACTION_CANCEL -> onCancelEvent(x, y, eventTime)
        }
    }

    fun onDownEvent(x: Int, y: Int, eventTime: Long) {
        if (DEBUG)
            debugLog("onDownEvent:", x, y)
        var keyIndex = mKeyState.onDownKey(x, y, eventTime)
        mKeyboardLayoutHasBeenChanged = false
        mKeyAlreadyProcessed = false
        mIsRepeatableKey = false
        mIsInSlidingKeyInput = false
        checkMultiTap(eventTime, keyIndex)
        val listener = mListener
        if (listener != null) {
            if (isValidKeyIndex(keyIndex)) {
                val key = mKeys!![keyIndex]
                if (key.codes != null) listener.onPress(key.getPrimaryCode())
                // This onPress call may have changed keyboard layout. Those cases are detected at
                // setKeyboard(). In those cases, we should update keyIndex according to the
                // new keyboard layout.
                if (mKeyboardLayoutHasBeenChanged) {
                    mKeyboardLayoutHasBeenChanged = false
                    keyIndex = mKeyState.onDownKey(x, y, eventTime)
                }
            }
        }
        if (isValidKeyIndex(keyIndex)) {
            val keys = mKeys!!
            if (keys[keyIndex].repeatable) {
                repeatKey(keyIndex)
                mHandler.startKeyRepeatTimer(mDelayBeforeKeyRepeatStart.toLong(), keyIndex, this)
                mIsRepeatableKey = true
            }
            startLongPressTimer(keyIndex)
        }
        showKeyPreviewAndUpdateKey(keyIndex)
    }

    fun onMoveEvent(x: Int, y: Int, eventTime: Long) {
        if (DEBUG_MOVE)
            debugLog("onMoveEvent:", x, y)
        if (mKeyAlreadyProcessed)
            return
        val keyState = mKeyState
        var keyIndex = keyState.onMoveKey(x, y)
        val oldKey = getKey(keyState.keyIndex)
        if (isValidKeyIndex(keyIndex)) {
            val isMinorMoveBounce = isMinorMoveBounce(x, y, keyIndex)
            if (DEBUG_MOVE) Log.i(TAG, "isMinorMoveBounce=$isMinorMoveBounce oldKey=$oldKey")
            if (oldKey == null) {
                // The pointer has been slid in to the new key, but the finger was not on any keys.
                // In this case, we must call onPress() to notify that the new key is being pressed.
                val listener = mListener
                if (listener != null) {
                    val key = getKey(keyIndex)!!
                    if (key.codes != null) listener.onPress(key.getPrimaryCode())
                    // This onPress call may have changed keyboard layout. Those cases are detected
                    // at setKeyboard(). In those cases, we should update keyIndex according
                    // to the new keyboard layout.
                    if (mKeyboardLayoutHasBeenChanged) {
                        mKeyboardLayoutHasBeenChanged = false
                        keyIndex = keyState.onMoveKey(x, y)
                    }
                }
                keyState.onMoveToNewKey(keyIndex, x, y)
                startLongPressTimer(keyIndex)
            } else if (!isMinorMoveBounce) {
                // The pointer has been slid in to the new key from the previous key, we must call
                // onRelease() first to notify that the previous key has been released, then call
                // onPress() to notify that the new key is being pressed.
                mIsInSlidingKeyInput = true
                val listener = mListener
                if (listener != null && oldKey.codes != null)
                    listener.onRelease(oldKey.getPrimaryCode())
                resetMultiTap()
                if (listener != null) {
                    val key = getKey(keyIndex)!!
                    if (key.codes != null) listener.onPress(key.getPrimaryCode())
                    // This onPress call may have changed keyboard layout. Those cases are detected
                    // at setKeyboard(). In those cases, we should update keyIndex according
                    // to the new keyboard layout.
                    if (mKeyboardLayoutHasBeenChanged) {
                        mKeyboardLayoutHasBeenChanged = false
                        keyIndex = keyState.onMoveKey(x, y)
                    }
                    addSlideKey(oldKey)
                }
                keyState.onMoveToNewKey(keyIndex, x, y)
                startLongPressTimer(keyIndex)
            }
        } else {
            if (oldKey != null && !isMinorMoveBounce(x, y, keyIndex)) {
                // The pointer has been slid out from the previous key, we must call onRelease() to
                // notify that the previous key has been released.
                mIsInSlidingKeyInput = true
                val listener = mListener
                if (listener != null && oldKey.codes != null)
                    listener.onRelease(oldKey.getPrimaryCode())
                resetMultiTap()
                keyState.onMoveToNewKey(keyIndex, x, y)
                mHandler.cancelLongPressTimer()
            }
        }
        showKeyPreviewAndUpdateKey(keyState.keyIndex)
    }

    fun onUpEvent(x: Int, y: Int, eventTime: Long) {
        if (DEBUG)
            debugLog("onUpEvent  :", x, y)
        mHandler.cancelKeyTimers()
        mHandler.cancelPopupPreview()
        showKeyPreviewAndUpdateKey(NOT_A_KEY)
        mIsInSlidingKeyInput = false
        sendSlideKeys()
        if (mKeyAlreadyProcessed)
            return
        var keyIndex = mKeyState.onUpKey(x, y)
        var finalX = x
        var finalY = y
        if (isMinorMoveBounce(x, y, keyIndex)) {
            // Use previous fixed key index and coordinates.
            keyIndex = mKeyState.keyIndex
            finalX = mKeyState.keyX
            finalY = mKeyState.keyY
        }
        if (!mIsRepeatableKey) {
            detectAndSendKey(keyIndex, finalX, finalY, eventTime)
        }

        if (isValidKeyIndex(keyIndex))
            mProxy.invalidateKey(mKeys!![keyIndex])
    }

    fun onCancelEvent(x: Int, y: Int, eventTime: Long) {
        if (DEBUG)
            debugLog("onCancelEvt:", x, y)
        mHandler.cancelKeyTimers()
        mHandler.cancelPopupPreview()
        showKeyPreviewAndUpdateKey(NOT_A_KEY)
        mIsInSlidingKeyInput = false
        val keyIndex = mKeyState.keyIndex
        if (isValidKeyIndex(keyIndex))
            mProxy.invalidateKey(mKeys!![keyIndex])
    }

    fun repeatKey(keyIndex: Int) {
        val key = getKey(keyIndex)
        if (key != null) {
            // While key is repeating, because there is no need to handle multi-tap key, we can
            // pass -1 as eventTime argument.
            detectAndSendKey(keyIndex, key.x, key.y, -1)
        }
    }

    fun getLastX(): Int = mKeyState.lastX

    fun getLastY(): Int = mKeyState.lastY

    fun getDownTime(): Long = mKeyState.downTime

    // These package scope methods are only for debugging purpose.
    @JvmName("getStartX")
    fun getStartX(): Int = mKeyState.startX

    @JvmName("getStartY")
    fun getStartY(): Int = mKeyState.startY

    private fun isMinorMoveBounce(x: Int, y: Int, newKey: Int): Boolean {
        val keys = mKeys
        if (keys == null || mKeyHysteresisDistanceSquared < 0)
            throw IllegalStateException("keyboard and/or hysteresis not set")
        val curKey = mKeyState.keyIndex
        return if (newKey == curKey) {
            true
        } else if (isValidKeyIndex(curKey)) {
            getSquareDistanceToKeyEdge(x, y, keys[curKey]) < mKeyHysteresisDistanceSquared
        } else {
            false
        }
    }

    private fun showKeyPreviewAndUpdateKey(keyIndex: Int) {
        updateKey(keyIndex)
        // The modifier key, such as shift key, should not be shown as preview when multi-touch is
        // supported. On the other hand, if multi-touch is not supported, the modifier key should
        // be shown as preview.
        if (mHasDistinctMultitouch && isModifier()) {
            mProxy.showPreview(NOT_A_KEY, this)
        } else {
            mProxy.showPreview(keyIndex, this)
        }
    }

    private fun startLongPressTimer(keyIndex: Int) {
        if (mKeyboardSwitcher.isInMomentaryAutoModeSwitchState()) {
            // We use longer timeout for sliding finger input started from the symbols mode key.
            mHandler.startLongPressTimer((LatinIME.sKeyboardSettings.longpressTimeout * 3).toLong(), keyIndex, this)
        } else {
            mHandler.startLongPressTimer(LatinIME.sKeyboardSettings.longpressTimeout.toLong(), keyIndex, this)
        }
    }

    private fun detectAndSendKey(index: Int, x: Int, y: Int, eventTime: Long) {
        detectAndSendKey(getKey(index), x, y, eventTime)
        mLastSentIndex = index
    }

    private fun detectAndSendKey(key: Key?, x: Int, y: Int, eventTime: Long) {
        val listener = mListener

        if (key == null) {
            listener?.onCancel()
        } else {
            val keyText = key.text
            if (keyText != null) {
                listener?.let {
                    it.onText(keyText)
                    it.onRelease(0) // dummy key code
                }
            } else {
                if (key.codes == null) return
                var code = key.getPrimaryCode()
                val codes = mKeyDetector.newCodeArray()
                mKeyDetector.getKeyIndexAndNearbyCodes(x, y, codes)
                // Multi-tap
                if (mInMultiTap) {
                    if (mTapCount != -1) {
                        mListener?.onKey(Keyboard.KEYCODE_DELETE, KEY_DELETE, x, y)
                    } else {
                        mTapCount = 0
                    }
                    code = key.codes!![mTapCount]
                }
                /*
                 * Swap the first and second values in the codes array if the primary code is not
                 * the first value but the second value in the array. This happens when key
                 * debouncing is in effect.
                 */
                if (codes.size >= 2 && codes[0] != code && codes[1] == code) {
                    codes[1] = codes[0]
                    codes[0] = code
                }
                listener?.let {
                    it.onKey(code, codes, x, y)
                    it.onRelease(code)
                }
            }
            mLastTapTime = eventTime
        }
    }

    /**
     * Handle multi-tap keys by producing the key label for the current multi-tap state.
     */
    fun getPreviewText(key: Key): CharSequence? {
        return if (mInMultiTap) {
            // Multi-tap
            mPreviewLabel.setLength(0)
            mPreviewLabel.append(key.codes!![if (mTapCount < 0) 0 else mTapCount].toChar())
            mPreviewLabel
        } else {
            if (key.isDeadKey()) {
                DeadAccentSequence.normalize(" " + key.label)
            } else {
                key.label
            }
        }
    }

    private fun resetMultiTap() {
        mLastSentIndex = NOT_A_KEY
        mTapCount = 0
        mLastTapTime = -1
        mInMultiTap = false
    }

    private fun checkMultiTap(eventTime: Long, keyIndex: Int) {
        val key = getKey(keyIndex)
        if (key == null || key.codes == null)
            return

        val keyCodes = key.codes!!
        val isMultiTap = (eventTime < mLastTapTime + mMultiTapKeyTimeout && keyIndex == mLastSentIndex)
        if (keyCodes.size > 1) {
            mInMultiTap = true
            if (isMultiTap) {
                mTapCount = (mTapCount + 1) % keyCodes.size
                return
            } else {
                mTapCount = -1
                return
            }
        }
        if (!isMultiTap) {
            resetMultiTap()
        }
    }

    private fun debugLog(title: String, x: Int, y: Int) {
        val keyIndex = mKeyDetector.getKeyIndexAndNearbyCodes(x, y, null)
        val key = getKey(keyIndex)
        val code: String = if (key == null || key.codes == null) {
            "----"
        } else {
            val primaryCode = key.codes!![0]
            String.format(if (primaryCode < 0) "%4d" else "0x%02x", primaryCode)
        }
        Log.d(TAG, String.format("%s%s[%d] %3d,%3d %3d(%s) %s", title,
            if (mKeyAlreadyProcessed) "-" else " ", mPointerId, x, y, keyIndex, code,
            if (isModifier()) "modifier" else ""))
    }

    // This class keeps track of a key index and a position where this pointer is.
    private class KeyState(private val mKeyDetector: KeyDetector) {
        // The position and time at which first down event occurred.
        var startX = 0
        var startY = 0
        var downTime = 0L

        // The current key index where this pointer is.
        var keyIndex = NOT_A_KEY
        // The position where keyIndex was recognized for the first time.
        var keyX = 0
        var keyY = 0

        // Last pointer position.
        var lastX = 0
        var lastY = 0

        fun onDownKey(x: Int, y: Int, eventTime: Long): Int {
            startX = x
            startY = y
            downTime = eventTime

            return onMoveToNewKey(onMoveKeyInternal(x, y), x, y)
        }

        private fun onMoveKeyInternal(x: Int, y: Int): Int {
            lastX = x
            lastY = y
            return mKeyDetector.getKeyIndexAndNearbyCodes(x, y, null)
        }

        fun onMoveKey(x: Int, y: Int): Int {
            return onMoveKeyInternal(x, y)
        }

        fun onMoveToNewKey(newKeyIndex: Int, x: Int, y: Int): Int {
            keyIndex = newKeyIndex
            keyX = x
            keyY = y
            return newKeyIndex
        }

        fun onUpKey(x: Int, y: Int): Int {
            return onMoveKeyInternal(x, y)
        }
    }

    companion object {
        private const val TAG = "PointerTracker"
        private const val DEBUG = false
        private const val DEBUG_MOVE = false

        // Miscellaneous constants
        private const val NOT_A_KEY = LatinKeyboardBaseView.NOT_A_KEY
        private val KEY_DELETE = intArrayOf(Keyboard.KEYCODE_DELETE)

        private var sSlideKeyHack = false
        private val sSlideKeys = ArrayList<Key>(10)

        private fun addSlideKey(key: Key?) {
            if (!sSlideKeyHack || LatinIME.sKeyboardSettings.sendSlideKeys == 0) return
            if (key == null) return
            if (key.modifier) {
                clearSlideKeys()
            } else {
                sSlideKeys.add(key)
            }
        }

        @JvmStatic
        fun clearSlideKeys() {
            sSlideKeys.clear()
        }

        private fun getSquareDistanceToKeyEdge(x: Int, y: Int, key: Key): Int {
            val left = key.x
            val right = key.x + key.width
            val top = key.y
            val bottom = key.y + key.height
            val edgeX = if (x < left) left else if (x > right) right else x
            val edgeY = if (y < top) top else if (y > bottom) bottom else y
            val dx = x - edgeX
            val dy = y - edgeY
            return dx * dx + dy * dy
        }
    }

    private fun sendSlideKeys() {
        if (!sSlideKeyHack) return
        val slideMode = LatinIME.sKeyboardSettings.sendSlideKeys
        if ((slideMode and 4) > 0) {
            // send all
            for (key in sSlideKeys) {
                detectAndSendKey(key, key.x, key.y, -1)
            }
        } else {
            // Send first and/or last key only.
            val n = sSlideKeys.size
            if (n > 0 && (slideMode and 1) > 0) {
                val key = sSlideKeys[0]
                detectAndSendKey(key, key.x, key.y, -1)
            }
            if (n > 1 && (slideMode and 2) > 0) {
                val key = sSlideKeys[n - 1]
                detectAndSendKey(key, key.x, key.y, -1)
            }
        }
        clearSlideKeys()
    }
}
