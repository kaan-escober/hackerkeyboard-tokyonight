/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.PopupWindow
import android.widget.TextView
import androidx.preference.PreferenceManager
import org.pocketworkstation.pckeyboard.Keyboard.Key
import org.pocketworkstation.pckeyboard.graphics.SeamlessPopupDrawable
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.LinkedList
import java.util.WeakHashMap

open class LatinKeyboardBaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.keyboardViewStyle
) : View(context, attrs, defStyle), PointerTracker.UIProxy {

    companion object {
        private const val TAG = "HK/LatinKbdBaseView"
        private const val DEBUG = false

        const val NOT_A_TOUCH_COORDINATE = -1
        const val NOT_A_KEY = -1
        private const val NUMBER_HINT_VERTICAL_ADJUSTMENT_PIXEL = -1

        private var sCustomTypeface: Typeface? = null
        private var sCurrentFontMode: Int = -1

       
        var sSetRenderMode: Method? = null
        private var sPrevRenderMode: Int = -1

        private val INVERTING_MATRIX = floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )

       
        fun isNumberAtLeftmostPopupChar(key: Key): Boolean {
            val pc = key.popupCharacters ?: return false
            return pc.isNotEmpty() && isAsciiDigit(pc[0])
        }

       
        fun isNumberAtRightmostPopupChar(key: Key): Boolean {
            val pc = key.popupCharacters ?: return false
            return pc.isNotEmpty() && isAsciiDigit(pc[pc.length - 1])
        }

        private fun isAsciiDigit(c: Char): Boolean = c.code < 0x80 && Character.isDigit(c)

        private fun isNumberAtEdgeOfPopupChars(key: Key): Boolean =
            isNumberAtLeftmostPopupChar(key) || isNumberAtRightmostPopupChar(key)

       
        fun initCompatibility() {
            try {
                sSetRenderMode = View::class.java.getMethod("setLayerType", Int::class.java, Paint::class.java)
                Log.i(TAG, "setRenderMode is supported")
            } catch (e: SecurityException) {
                Log.w(TAG, "unexpected SecurityException", e)
            } catch (e: NoSuchMethodException) {
                Log.i(TAG, "ignoring render mode, not supported")
            }
        }

        init {
            initCompatibility()
        }
    }

    interface OnKeyboardActionListener {
        fun onPress(primaryCode: Int)
        fun onRelease(primaryCode: Int)
        fun onKey(primaryCode: Int, keyCodes: IntArray?, x: Int, y: Int)
        fun onText(text: CharSequence)
        fun onCancel()
        fun swipeLeft(): Boolean
        fun swipeRight(): Boolean
        fun swipeDown(): Boolean
        fun swipeUp(): Boolean
    }

    // Timing constants
    private val mKeyRepeatInterval: Int

    // XML attributes
    private var mKeyTextSize: Float = 0f
    private var mLabelScale: Float = 1.0f
    private var mKeyTextColor: Int = 0
    private var mKeyHintColor: Int = 0
    private var mKeyCursorColor: Int = 0
    private var mKeyActiveColor: Int = 0
    private var mInvertSymbols: Boolean = false
    private var mRecolorSymbols: Boolean = false
    private var mKeyTextStyle: Typeface? = null
    private var mLabelTextSize: Float = 0f
    private var mSymbolColorScheme: Int = 0
    private var mShadowColor: Int = 0
    private var mShadowRadius: Float = 0f
    private var mKeyBackground: Drawable? = null
    private var mBackgroundAlpha: Int = 255
    private var mBackgroundDimAmount: Float = 0.5f
    private var mKeyHysteresisDistance: Float = 0f
    private var mVerticalCorrection: Float = 0f
    protected var mPreviewOffset: Int = 0
    protected var mPreviewHeight: Int = 0
    protected var mPopupLayout: Int = 0

    // Main keyboard
    private var mKeyboard: Keyboard? = null
    private var mKeys: Array<Key>? = null
    private var mKeyboardVerticalGap: Int = 0

    // Key preview popup
    protected var mPreviewText: TextView? = null
    protected var mPreviewPopup: PopupWindow? = null
    protected var mPreviewTextSizeLarge: Int = 0
    protected var mOffsetInWindow: IntArray? = null
    protected var mOldPreviewKeyIndex: Int = NOT_A_KEY
    protected var mShowPreview: Boolean = true
    protected var mShowTouchPoints: Boolean = true
    protected var mPopupPreviewOffsetX: Int = 0
    protected var mPopupPreviewOffsetY: Int = 0
    protected var mWindowY: Int = 0
    protected var mPopupPreviewDisplayedY: Int = 0
    protected val mDelayBeforePreview: Int
    protected val mDelayBeforeSpacePreview: Int
    protected val mDelayAfterPreview: Int

    // Popup mini keyboard
    protected var mMiniKeyboardPopup: PopupWindow? = null
    protected var mMiniKeyboard: LatinKeyboardBaseView? = null
    protected var mMiniKeyboardContainer: View? = null
    protected var mMiniKeyboardParent: View? = null
    protected var mMiniKeyboardVisible: Boolean = false
    protected var mIsMiniKeyboard: Boolean = false
    protected var mSeamlessPopupDrawable: SeamlessPopupDrawable? = null
    protected var mPreviewPopupDrawable: SeamlessPopupDrawable? = null
    protected val mMiniKeyboardCacheMain: WeakHashMap<Key, Keyboard> = WeakHashMap()
    protected val mMiniKeyboardCacheShift: WeakHashMap<Key, Keyboard> = WeakHashMap()
    protected val mMiniKeyboardCacheCaps: WeakHashMap<Key, Keyboard> = WeakHashMap()
    protected var mMiniKeyboardOriginX: Int = 0
    protected var mMiniKeyboardOriginY: Int = 0
    protected var mMiniKeyboardPopupTime: Long = 0L
    protected var mWindowOffset: IntArray? = null
    protected val mMiniKeyboardSlideAllowance: Float
    protected var mMiniKeyboardTrackerId: Int = 0

    private var mKeyboardActionListener: OnKeyboardActionListener? = null

    @get:JvmName("getOnKeyboardActionListenerProp")
    val onKeyboardActionListener: OnKeyboardActionListener?
        get() = mKeyboardActionListener

    private val mPointerTrackers: ArrayList<PointerTracker> = ArrayList()
    private var mIgnoreMove: Boolean = false

    private val mPointerQueue: PointerQueue = PointerQueue()

    private val mHasDistinctMultitouch: Boolean
    private var mOldPointerCount: Int = 1

    protected var mKeyDetector: KeyDetector = ProximityKeyDetector()

    // Swipe gesture detector
    private var mGestureDetector: GestureDetector? = null
    private val mSwipeTracker: SwipeTracker = SwipeTracker()
    private val mSwipeThreshold: Int
    private val mDisambiguateSwipe: Boolean

    // Drawing
    private val mDirtyRect: Rect = Rect()
    private var mInvalidatedKey: Key? = null
    private val mPaint: Paint
    private val mPaintHint: Paint
    private val mPadding: Rect
    private val mClipRegion: Rect = Rect(0, 0, 0, 0)
    private var mViewWidth: Int = 0
    private val mTextHeightCache: HashMap<Int, Int> = HashMap()
    private val KEY_LABEL_VERTICAL_ADJUSTMENT_FACTOR = 0.55f
    private val KEY_LABEL_HEIGHT_REFERENCE_CHAR = "H"

    private val mInvertingColorFilter: ColorMatrixColorFilter = ColorMatrixColorFilter(INVERTING_MATRIX)

    val mHandler: UIHandler = UIHandler()

    inner class UIHandler : Handler() {
        private val MSG_POPUP_PREVIEW = 1
        private val MSG_DISMISS_PREVIEW = 2
        private val MSG_REPEAT_KEY = 3
        private val MSG_LONGPRESS_KEY = 4

        private var mInKeyRepeat: Boolean = false

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_POPUP_PREVIEW -> showKey(msg.arg1, msg.obj as PointerTracker)
                MSG_DISMISS_PREVIEW -> mPreviewPopup?.dismiss()
                MSG_REPEAT_KEY -> {
                    val tracker = msg.obj as PointerTracker
                    tracker.repeatKey(msg.arg1)
                    startKeyRepeatTimer(mKeyRepeatInterval.toLong(), msg.arg1, tracker)
                }
                MSG_LONGPRESS_KEY -> {
                    val tracker = msg.obj as PointerTracker
                    openPopupIfRequired(msg.arg1, tracker)
                }
            }
        }

        fun popupPreview(delay: Long, keyIndex: Int, tracker: PointerTracker) {
            removeMessages(MSG_POPUP_PREVIEW)
            val previewPopup = mPreviewPopup
            if (previewPopup != null && previewPopup.isShowing && mPreviewText?.visibility == VISIBLE) {
                showKey(keyIndex, tracker)
            } else {
                sendMessageDelayed(obtainMessage(MSG_POPUP_PREVIEW, keyIndex, 0, tracker), delay)
            }
        }

        fun cancelPopupPreview() {
            removeMessages(MSG_POPUP_PREVIEW)
        }

        fun dismissPreview(delay: Long) {
            if (mPreviewPopup?.isShowing == true) {
                sendMessageDelayed(obtainMessage(MSG_DISMISS_PREVIEW), delay)
            }
        }

        fun cancelDismissPreview() {
            removeMessages(MSG_DISMISS_PREVIEW)
        }

        fun startKeyRepeatTimer(delay: Long, keyIndex: Int, tracker: PointerTracker) {
            mInKeyRepeat = true
            sendMessageDelayed(obtainMessage(MSG_REPEAT_KEY, keyIndex, 0, tracker), delay)
        }

        fun cancelKeyRepeatTimer() {
            mInKeyRepeat = false
            removeMessages(MSG_REPEAT_KEY)
        }

        fun isInKeyRepeat(): Boolean = mInKeyRepeat

        fun startLongPressTimer(delay: Long, keyIndex: Int, tracker: PointerTracker) {
            removeMessages(MSG_LONGPRESS_KEY)
            sendMessageDelayed(obtainMessage(MSG_LONGPRESS_KEY, keyIndex, 0, tracker), delay)
        }

        fun cancelLongPressTimer() {
            removeMessages(MSG_LONGPRESS_KEY)
        }

        fun cancelKeyTimers() {
            cancelKeyRepeatTimer()
            cancelLongPressTimer()
        }

        fun cancelAllMessages() {
            cancelKeyTimers()
            cancelPopupPreview()
            cancelDismissPreview()
        }
    }

    class PointerQueue {
        private val mQueue: LinkedList<PointerTracker> = LinkedList()

        fun add(tracker: PointerTracker) {
            mQueue.add(tracker)
        }

        fun lastIndexOf(tracker: PointerTracker): Int {
            val queue = mQueue
            for (index in queue.size - 1 downTo 0) {
                if (queue[index] === tracker) return index
            }
            return -1
        }

        fun releaseAllPointersOlderThan(tracker: PointerTracker, eventTime: Long) {
            val queue = mQueue
            var oldestPos = 0
            while (oldestPos < queue.size) {
                val t = queue[oldestPos]
                if (t === tracker) break
                if (t.isModifier()) {
                    oldestPos++
                } else {
                    t.onUpEvent(t.getLastX(), t.getLastY(), eventTime)
                    t.setAlreadyProcessed()
                    queue.removeAt(oldestPos)
                }
                if (queue.isEmpty()) return
            }
        }

        fun releaseAllPointersExcept(tracker: PointerTracker?, eventTime: Long) {
            for (t in mQueue) {
                if (t === tracker) continue
                t.onUpEvent(t.getLastX(), t.getLastY(), eventTime)
                t.setAlreadyProcessed()
            }
            mQueue.clear()
            if (tracker != null) mQueue.add(tracker)
        }

        fun remove(tracker: PointerTracker) {
            mQueue.remove(tracker)
        }

        fun isInSlidingKeyInput(): Boolean {
            for (tracker in mQueue) {
                if (tracker.isInSlidingKeyInput()) return true
            }
            return false
        }
    }

    private fun setRenderModeIfPossible(mode: Int) {
        val setter = sSetRenderMode ?: return
        if (mode == sPrevRenderMode) return
        try {
            setter.invoke(this, mode, null)
            sPrevRenderMode = mode
            Log.i(TAG, "render mode set to ${LatinIME.sKeyboardSettings.renderMode}")
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }

    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val fontPref = prefs.getString("pref_keyboard_font", "0") ?: "0"
        val fontMode = fontPref.toIntOrNull() ?: 0

        if (sCustomTypeface == null || sCurrentFontMode != fontMode) {
            try {
                sCustomTypeface = when (fontMode) {
                    4 -> {
                        val customFont = File(context.filesDir, "custom_font.ttf")
                        if (customFont.exists()) Typeface.createFromFile(customFont)
                        else Typeface.MONOSPACE
                    }
                    else -> Typeface.createFromAsset(context.assets, "fonts/GoogleSansCode-Regular.ttf")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not load custom font", e)
                sCustomTypeface = Typeface.MONOSPACE
            }
            sCurrentFontMode = fontMode
        }

        if (!isInEditMode) Log.i(TAG, "Creating new LatinKeyboardBaseView $this")
        setRenderModeIfPossible(LatinIME.sKeyboardSettings.renderMode)

        val a: TypedArray = context.obtainStyledAttributes(
            attrs, R.styleable.LatinKeyboardBaseView, defStyle, R.style.LatinKeyboardBaseView)

        val n = a.indexCount
        for (i in 0 until n) {
            val attr = a.getIndex(i)
            when (attr) {
                R.styleable.LatinKeyboardBaseView_keyBackground ->
                    mKeyBackground = a.getDrawable(attr)
                R.styleable.LatinKeyboardBaseView_keyHysteresisDistance ->
                    mKeyHysteresisDistance = a.getDimensionPixelOffset(attr, 0).toFloat()
                R.styleable.LatinKeyboardBaseView_verticalCorrection ->
                    mVerticalCorrection = a.getDimensionPixelOffset(attr, 0).toFloat()
                R.styleable.LatinKeyboardBaseView_keyTextSize ->
                    mKeyTextSize = a.getDimensionPixelSize(attr, 18).toFloat()
                R.styleable.LatinKeyboardBaseView_keyTextColor ->
                    mKeyTextColor = a.getColor(attr, 0xFF000000.toInt())
                R.styleable.LatinKeyboardBaseView_keyHintColor ->
                    mKeyHintColor = a.getColor(attr, 0xFFBBBBBB.toInt())
                R.styleable.LatinKeyboardBaseView_keyCursorColor ->
                    mKeyCursorColor = a.getColor(attr, 0xFF000000.toInt())
                R.styleable.LatinKeyboardBaseView_kbdColorActive ->
                    mKeyActiveColor = a.getColor(attr, 0xFFFF9E64.toInt())
                R.styleable.LatinKeyboardBaseView_invertSymbols ->
                    mInvertSymbols = a.getBoolean(attr, false)
                R.styleable.LatinKeyboardBaseView_recolorSymbols ->
                    mRecolorSymbols = a.getBoolean(attr, false)
                R.styleable.LatinKeyboardBaseView_labelTextSize ->
                    mLabelTextSize = a.getDimensionPixelSize(attr, 14).toFloat()
                R.styleable.LatinKeyboardBaseView_shadowColor ->
                    mShadowColor = a.getColor(attr, 0)
                R.styleable.LatinKeyboardBaseView_shadowRadius ->
                    mShadowRadius = a.getFloat(attr, 0f)
                R.styleable.LatinKeyboardBaseView_backgroundDimAmount ->
                    mBackgroundDimAmount = a.getFloat(attr, 0.5f)
                R.styleable.LatinKeyboardBaseView_backgroundAlpha ->
                    mBackgroundAlpha = a.getInteger(attr, 255)
                R.styleable.LatinKeyboardBaseView_keyTextStyle -> {
                    val textStyle = a.getInt(attr, 0)
                    mKeyTextStyle = when (textStyle) {
                        0 -> sCustomTypeface
                        1 -> Typeface.create(sCustomTypeface, Typeface.BOLD)
                        else -> Typeface.create(sCustomTypeface, textStyle)
                    }
                }
                R.styleable.LatinKeyboardBaseView_symbolColorScheme ->
                    mSymbolColorScheme = a.getInt(attr, 0)
            }
        }
        a.recycle()

        if (mKeyTextStyle == null) {
            mKeyTextStyle = Typeface.create(sCustomTypeface, Typeface.BOLD)
        }

        val res: Resources = resources

        mShowPreview = false
        mDelayBeforePreview = 0
        mDelayBeforeSpacePreview = res.getInteger(R.integer.config_delay_before_space_preview)
        mDelayAfterPreview = res.getInteger(R.integer.config_delay_after_preview)

        mPopupLayout = 0

        mPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            alpha = 255
        }

        mPaintHint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            alpha = 255
            typeface = Typeface.create(sCustomTypeface, Typeface.BOLD)
        }

        mPadding = Rect(0, 0, 0, 0)
        mKeyBackground!!.getPadding(mPadding)

        mSwipeThreshold = (300 * res.displayMetrics.density).toInt()
        mDisambiguateSwipe = res.getBoolean(R.bool.config_swipeDisambiguation)
        mMiniKeyboardSlideAllowance = res.getDimension(R.dimen.mini_keyboard_slide_allowance)

        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                me1: MotionEvent?,
                me2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (me1 == null) return false
                val absX = Math.abs(velocityX)
                val absY = Math.abs(velocityY)
                val deltaX = me2.x - me1.x
                val deltaY = me2.y - me1.y
                mSwipeTracker.computeCurrentVelocity(1000)
                val endingVelocityX = mSwipeTracker.getXVelocity()
                val endingVelocityY = mSwipeTracker.getYVelocity()
                val travelX = width / 3
                val travelY = height / 3
                val travelMin = Math.min(travelX, travelY)
                if (velocityX > mSwipeThreshold && absY < absX && deltaX > travelMin) {
                    if (mDisambiguateSwipe && endingVelocityX >= velocityX / 4) {
                        if (swipeRight()) return true
                    }
                } else if (velocityX < -mSwipeThreshold && absY < absX && deltaX < -travelMin) {
                    if (mDisambiguateSwipe && endingVelocityX <= velocityX / 4) {
                        if (swipeLeft()) return true
                    }
                } else if (velocityY < -mSwipeThreshold && absX < absY && deltaY < -travelMin) {
                    if (mDisambiguateSwipe && endingVelocityY <= velocityY / 4) {
                        if (swipeUp()) return true
                    }
                } else if (velocityY > mSwipeThreshold && absX < absY / 2 && deltaY > travelMin) {
                    if (mDisambiguateSwipe && endingVelocityY >= velocityY / 4) {
                        if (swipeDown()) return true
                    }
                }
                return false
            }
        }

        mGestureDetector = GestureDetector(context, gestureListener, null, true)
        mGestureDetector!!.setIsLongpressEnabled(false)

        mHasDistinctMultitouch = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)
        mKeyRepeatInterval = res.getInteger(R.integer.config_key_repeat_interval)
    }

    private fun showHints7Bit(): Boolean = LatinIME.sKeyboardSettings.hintMode >= 1
    private fun showHintsAll(): Boolean = LatinIME.sKeyboardSettings.hintMode >= 2

    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener) {
        mKeyboardActionListener = listener
        for (tracker in mPointerTrackers) {
            tracker.setOnKeyboardActionListener(listener)
        }
    }

    protected fun getOnKeyboardActionListener(): OnKeyboardActionListener? = mKeyboardActionListener

    @get:JvmName("getKeyboardProp")
    @set:JvmName("setKeyboardProp")
    var keyboard: Keyboard?
        get() = mKeyboard
        set(value) { if (value != null) setKeyboard(value) }

    fun getKeyboard(): Keyboard? = mKeyboard

    @get:JvmName("getShiftStateProp")
    @set:JvmName("setShiftStateProp")
    var shiftState: Int
        get() = getShiftState()
        set(value) { setShiftState(value) }

    val isShiftCaps: Boolean
        get() = getShiftState() != Keyboard.SHIFT_OFF

    open fun setKeyboard(keyboard: Keyboard) {
        if (mKeyboard != null) dismissKeyPreview()
        mHandler.cancelKeyTimers()
        mHandler.cancelPopupPreview()
        mKeyboard = keyboard
        mKeys = mKeyDetector.setKeyboard(keyboard, 0f, 0f)
        mKeyboardVerticalGap = resources.getDimension(R.dimen.key_bottom_gap).toInt()
        val keys = mKeys!!
        for (tracker in mPointerTrackers) {
            tracker.setKeyboard(keys, mKeyHysteresisDistance)
        }
        mLabelScale = LatinIME.sKeyboardSettings.labelScalePref
        requestLayout()
        mKeyboard?.setKeyboardWidth(mViewWidth)
        invalidateAllKeys()
        computeProximityThreshold(keyboard)
        mMiniKeyboardCacheMain.clear()
        mMiniKeyboardCacheShift.clear()
        mMiniKeyboardCacheCaps.clear()
        setRenderModeIfPossible(LatinIME.sKeyboardSettings.renderMode)
        mIgnoreMove = true
    }

    override fun hasDistinctMultitouch(): Boolean = mHasDistinctMultitouch

    fun setShiftState(shiftState: Int): Boolean {
        val kbd = mKeyboard ?: return false
        if (kbd.setShiftState(shiftState)) {
            invalidateAllKeys()
            return true
        }
        return false
    }

    fun setCtrlIndicator(active: Boolean) {
        mKeyboard?.setCtrlIndicator(active)?.let { invalidateKey(it) }
    }

    fun setAltIndicator(active: Boolean) {
        mKeyboard?.setAltIndicator(active)?.let { invalidateKey(it) }
    }

    fun setMetaIndicator(active: Boolean) {
        mKeyboard?.setMetaIndicator(active)?.let { invalidateKey(it) }
    }

    fun getShiftState(): Int = mKeyboard?.getShiftState() ?: Keyboard.SHIFT_OFF

    fun isShiftAll(): Boolean {
        val state = getShiftState()
        return if (LatinIME.sKeyboardSettings.shiftLockModifiers)
            state == Keyboard.SHIFT_ON || state == Keyboard.SHIFT_LOCKED
        else state == Keyboard.SHIFT_ON
    }

    open fun setPreviewEnabled(previewEnabled: Boolean) {
        mShowPreview = previewEnabled
    }

    fun isPreviewEnabled(): Boolean = mShowPreview

    private fun isBlackSym(): Boolean = mSymbolColorScheme == 1

    fun setPopupParent(v: View) { mMiniKeyboardParent = v }

    fun setPopupOffset(x: Int, y: Int) {
        mPopupPreviewOffsetX = x
        mPopupPreviewOffsetY = y
        mPreviewPopup?.dismiss()
    }

    fun setProximityCorrectionEnabled(enabled: Boolean) {
        mKeyDetector.setProximityCorrectionEnabled(enabled)
    }

    fun isProximityCorrectionEnabled(): Boolean = mKeyDetector.isProximityCorrectionEnabled()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val kbd = mKeyboard
        if (kbd == null) {
            setMeasuredDimension(paddingLeft + paddingRight, paddingTop + paddingBottom)
        } else {
            val width = kbd.getMinWidth() + paddingLeft + paddingRight
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                val badWidth = MeasureSpec.getSize(widthMeasureSpec)
                if (badWidth != width) Log.i(TAG, "ignoring unexpected width=$badWidth")
            }
            Log.i(TAG, "onMeasure width=$width")
            setMeasuredDimension(width, kbd.getHeight() + paddingTop + paddingBottom)
        }
    }

    private fun computeProximityThreshold(keyboard: Keyboard?) {
        if (keyboard == null) return
        val keys = mKeys ?: return
        val length = keys.size
        var dimensionSum = 0
        for (i in 0 until length) {
            val key = keys[i]
            dimensionSum += Math.min(key.width, key.height + mKeyboardVerticalGap) + key.gap
        }
        if (dimensionSum < 0 || length == 0) return
        mKeyDetector.setProximityThreshold((dimensionSum * 1.4f / length).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.i(TAG, "onSizeChanged, w=$w, h=$h")
        mViewWidth = w
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        onBufferDraw(canvas)
    }

    private fun drawDeadKeyLabel(canvas: Canvas, hint: String, x: Int, baseline: Float, paint: Paint) {
        val c = hint[0]
        val accent = DeadAccentSequence.getSpacing(c)
        canvas.drawText(Keyboard.DEAD_KEY_PLACEHOLDER_STRING, x.toFloat(), baseline, paint)
        canvas.drawText(accent, x.toFloat(), baseline, paint)
    }

    private fun getLabelHeight(paint: Paint, labelSize: Int): Int {
        val cached = mTextHeightCache[labelSize]
        if (cached != null) return cached
        val textBounds = Rect()
        paint.getTextBounds(KEY_LABEL_HEIGHT_REFERENCE_CHAR, 0, 1, textBounds)
        val labelHeight = textBounds.height()
        mTextHeightCache[labelSize] = labelHeight
        return labelHeight
    }

    private fun onBufferDraw(canvas: Canvas) {
        canvas.getClipBounds(mDirtyRect)
        if (mKeyboard == null) return

        val paint = mPaint
        val paintHint = mPaintHint
        paintHint.color = mKeyHintColor
        val keyBackground = mKeyBackground!!
        val clipRegion = mClipRegion
        val padding = mPadding
        val kbdPaddingLeft = paddingLeft
        val kbdPaddingTop = paddingTop
        val keys = mKeys ?: return
        val invalidKey = mInvalidatedKey

        var iconColorFilter: android.graphics.ColorFilter? = null
        var shadowColorFilter: android.graphics.ColorFilter? = null
        if (mInvertSymbols) {
            iconColorFilter = mInvertingColorFilter
        } else if (mRecolorSymbols) {
            iconColorFilter = PorterDuffColorFilter(mKeyTextColor, PorterDuff.Mode.SRC_ATOP)
            shadowColorFilter = PorterDuffColorFilter(mShadowColor, PorterDuff.Mode.SRC_ATOP)
        }

        var drawSingleKey = false
        if (invalidKey != null && canvas.getClipBounds(clipRegion)) {
            if (invalidKey.x + kbdPaddingLeft - 1 <= clipRegion.left &&
                invalidKey.y + kbdPaddingTop - 1 <= clipRegion.top &&
                invalidKey.x + invalidKey.width + kbdPaddingLeft + 1 >= clipRegion.right &&
                invalidKey.y + invalidKey.height + kbdPaddingTop + 1 >= clipRegion.bottom) {
                drawSingleKey = true
            }
        }

        val keyCount = keys.size

        val keyWidths = ArrayList<Int>()
        val keyHeights = ArrayList<Int>()
        for (i in 0 until keyCount) {
            val key = keys[i]
            keyWidths.add(key.width)
            keyHeights.add(key.height)
        }
        keyWidths.sort()
        keyHeights.sort()
        val medianKeyWidth = if (keyWidths.isNotEmpty()) keyWidths[keyCount / 2] else 0
        val medianKeyHeight = if (keyHeights.isNotEmpty()) keyHeights[keyCount / 2] else 0
        mKeyTextSize = Math.min(medianKeyHeight * 6 / 10, medianKeyWidth * 6 / 10).toFloat()
        mLabelTextSize = mKeyTextSize * 3 / 4

        for (i in 0 until keyCount) {
            val key = keys[i]
            if (drawSingleKey && invalidKey != key) continue
            if (!mDirtyRect.intersects(
                key.x + kbdPaddingLeft,
                key.y + kbdPaddingTop,
                key.x + key.width + kbdPaddingLeft,
                key.y + key.height + kbdPaddingTop)) continue

            val isPreviewed = (i == mOldPreviewKeyIndex && mPreviewPopup != null && mPreviewPopup!!.isShowing)

            paint.color = if (key.isCursor) mKeyCursorColor else mKeyTextColor
            paint.alpha = if (isPreviewed) 60 else 255

            val drawableState = key.getCurrentDrawableState()
            keyBackground.state = drawableState

            val label = key.getCaseLabel()

            var yscale = 1.0f
            val bounds = keyBackground.bounds
            if (key.width != bounds.right || key.height != bounds.bottom) {
                val minHeight = keyBackground.minimumHeight
                if (minHeight > key.height) {
                    yscale = key.height.toFloat() / minHeight
                    keyBackground.setBounds(0, 0, key.width, minHeight)
                } else {
                    keyBackground.setBounds(0, 0, key.width, key.height)
                }
            }
            canvas.translate((key.x + kbdPaddingLeft).toFloat(), (key.y + kbdPaddingTop).toFloat())
            if (yscale != 1.0f) {
                canvas.save()
                canvas.scale(1.0f, yscale)
            }

            val originalAlpha = mBackgroundAlpha
            when {
                isPreviewed -> keyBackground.alpha = 170
                mBackgroundAlpha != 255 -> keyBackground.alpha = mBackgroundAlpha
                else -> keyBackground.alpha = 255
            }

            if (!mIsMiniKeyboard || key.pressed) {
                if (mIsMiniKeyboard) {
                    val origBounds = keyBackground.bounds
                    keyBackground.setBounds(2, 2, key.width - 2, key.height - 2)
                    keyBackground.draw(canvas)
                    keyBackground.bounds = origBounds
                } else {
                    keyBackground.draw(canvas)
                }
            }
            keyBackground.alpha = originalAlpha

            if (yscale != 1.0f) canvas.restore()

            var shouldDrawIcon = true
            if (label != null) {
                val labelSize: Int = if (label.length > 1 && key.codes!!.size < 2) {
                    (mLabelTextSize * mLabelScale).toInt()
                } else {
                    (mKeyTextSize * mLabelScale).toInt()
                }
                paint.typeface = mKeyTextStyle
                paint.isFakeBoldText = key.isCursor
                paint.textSize = labelSize.toFloat()

                val labelHeight = getLabelHeight(paint, labelSize)

                paint.setShadowLayer(mShadowRadius, 0f, 0f, mShadowColor)

                val hint = key.getHintLabel(showHints7Bit(), showHintsAll())
                if (hint.isNotEmpty() && !(key.isShifted() && key.shiftLabel != null && hint[0] == key.shiftLabel!![0])) {
                    val hintTextSize = (mKeyTextSize * 0.6 * mLabelScale).toInt()
                    paintHint.textSize = hintTextSize.toFloat()
                    paintHint.alpha = 100

                    val hintLabelHeight = getLabelHeight(paintHint, hintTextSize)
                    val x = key.width - padding.right
                    val baseline = (padding.top + hintLabelHeight * 12 / 10).toFloat()
                    if (Character.getType(hint[0]) == Character.NON_SPACING_MARK.toInt()) {
                        drawDeadKeyLabel(canvas, hint, x, baseline, paintHint)
                    } else {
                        canvas.drawText(hint, x.toFloat(), baseline, paintHint)
                    }
                }

                val altHint = key.getAltHintLabel(showHints7Bit(), showHintsAll())
                if (altHint.isNotEmpty()) {
                    val hintTextSize = (mKeyTextSize * 0.6 * mLabelScale).toInt()
                    paintHint.textSize = hintTextSize.toFloat()
                    paintHint.alpha = 100

                    val hintLabelHeight = getLabelHeight(paintHint, hintTextSize)
                    val x = key.width - padding.right
                    val baseline = (padding.top + hintLabelHeight * (if (hint.isEmpty()) 12 else 26) / 10).toFloat()
                    if (Character.getType(altHint[0]) == Character.NON_SPACING_MARK.toInt()) {
                        drawDeadKeyLabel(canvas, altHint, x, baseline, paintHint)
                    } else {
                        canvas.drawText(altHint, x.toFloat(), baseline, paintHint)
                    }
                }

                val centerX = (key.width + padding.left - padding.right) / 2
                val centerY = (key.height + padding.top - padding.bottom) / 2
                val baseline = centerY + labelHeight * KEY_LABEL_VERTICAL_ADJUSTMENT_FACTOR
                if (key.isDeadKey()) {
                    drawDeadKeyLabel(canvas, label, centerX, baseline, paint)
                } else {
                    canvas.drawText(label, centerX.toFloat(), baseline, paint)
                }
                if (key.isCursor) {
                    paint.setShadowLayer(0f, 0f, 0f, 0)
                    canvas.drawText(label, centerX + 0.5f, baseline, paint)
                    canvas.drawText(label, centerX - 0.5f, baseline, paint)
                    canvas.drawText(label, centerX.toFloat(), baseline + 0.5f, paint)
                    canvas.drawText(label, centerX.toFloat(), baseline - 0.5f, paint)
                }

                paint.setShadowLayer(0f, 0f, 0f, 0)

                shouldDrawIcon = shouldDrawLabelAndIcon(key)
            }

            val icon: Drawable? = key.icon
            if (icon != null && shouldDrawIcon) {
                val drawableWidth: Int
                val drawableHeight: Int
                val drawableX: Int
                val drawableY: Int
                if (shouldDrawIconFully(key)) {
                    drawableWidth = key.width
                    drawableHeight = key.height
                    drawableX = 0
                    drawableY = NUMBER_HINT_VERTICAL_ADJUSTMENT_PIXEL
                } else {
                    drawableWidth = icon.intrinsicWidth
                    drawableHeight = icon.intrinsicHeight
                    drawableX = (key.width + padding.left - padding.right - drawableWidth) / 2
                    drawableY = (key.height + padding.top - padding.bottom - drawableHeight) / 2
                }
                canvas.translate(drawableX.toFloat(), drawableY.toFloat())
                icon.setBounds(0, 0, drawableWidth, drawableHeight)

                val isModifierActive = key.modifier && (key.on || key.locked || key.pressed)
                if (isModifierActive) {
                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    glowPaint.setShadowLayer(12f, 0f, 0f, mKeyActiveColor)
                    icon.colorFilter = PorterDuffColorFilter(mKeyActiveColor, PorterDuff.Mode.SRC_ATOP)
                    val b = Bitmap.createBitmap(Math.max(1, drawableWidth), Math.max(1, drawableHeight), Bitmap.Config.ARGB_8888)
                    val c = Canvas(b)
                    icon.draw(c)
                    canvas.drawBitmap(b, 0f, 0f, glowPaint)
                    icon.colorFilter = null
                } else if (iconColorFilter != null) {
                    if (shadowColorFilter != null && mShadowRadius > 0) {
                        val shadowBlur = BlurMaskFilter(mShadowRadius, BlurMaskFilter.Blur.OUTER)
                        val blurPaint = Paint()
                        blurPaint.maskFilter = shadowBlur
                        val tmpIcon = Bitmap.createBitmap(key.width, key.height, Bitmap.Config.ARGB_8888)
                        val tmpCanvas = Canvas(tmpIcon)
                        icon.draw(tmpCanvas)
                        val offsets = IntArray(2)
                        val shadowBitmap = tmpIcon.extractAlpha(blurPaint, offsets)
                        val shadowPaint = Paint()
                        shadowPaint.colorFilter = shadowColorFilter
                        canvas.drawBitmap(shadowBitmap, offsets[0].toFloat(), offsets[1].toFloat(), shadowPaint)
                    }
                    icon.colorFilter = iconColorFilter
                    icon.draw(canvas)
                    icon.colorFilter = null
                } else {
                    icon.draw(canvas)
                }
                canvas.translate(-drawableX.toFloat(), -drawableY.toFloat())
            }
            canvas.translate(-(key.x + kbdPaddingLeft).toFloat(), -(key.y + kbdPaddingTop).toFloat())
        }

        mInvalidatedKey = null
        mDirtyRect.setEmpty()

        if (mMiniKeyboardVisible) {
            paint.color = ((mBackgroundDimAmount * 0xFF).toInt()) shl 24
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        if (LatinIME.sKeyboardSettings.showTouchPos || DEBUG) {
            if (LatinIME.sKeyboardSettings.showTouchPos || mShowTouchPoints) {
                for (tracker in mPointerTrackers) {
                    val startX = tracker.getStartX()
                    val startY = tracker.getStartY()
                    val lastX = tracker.getLastX()
                    val lastY = tracker.getLastY()
                    paint.alpha = 128
                    paint.color = 0xFFFF0000.toInt()
                    canvas.drawCircle(startX.toFloat(), startY.toFloat(), 3f, paint)
                    canvas.drawLine(startX.toFloat(), startY.toFloat(), lastX.toFloat(), lastY.toFloat(), paint)
                    paint.color = 0xFF0000FF.toInt()
                    canvas.drawCircle(lastX.toFloat(), lastY.toFloat(), 3f, paint)
                    paint.color = 0xFF00FF00.toInt()
                    canvas.drawCircle(((startX + lastX) / 2).toFloat(), ((startY + lastY) / 2).toFloat(), 2f, paint)
                }
            }
        }

    }

    private fun dismissKeyPreview() {
        for (tracker in mPointerTrackers) tracker.updateKey(NOT_A_KEY)
        showPreviewNullable(NOT_A_KEY, null)
    }

    override fun showPreview(keyIndex: Int, tracker: PointerTracker) {
        showPreviewNullable(keyIndex, tracker)
    }

    private fun showPreviewNullable(keyIndex: Int, tracker: PointerTracker?) {
        val oldKeyIndex = mOldPreviewKeyIndex
        mOldPreviewKeyIndex = keyIndex

        var effectiveKeyIndex = keyIndex
        if (tracker != null && effectiveKeyIndex != NOT_A_KEY) {
            val key = tracker.getKey(effectiveKeyIndex)
            if (key != null && (key.modifier || (key.label != null && key.label!!.length > 1))) {
                effectiveKeyIndex = NOT_A_KEY
            }
        }

        val isLanguageSwitchEnabled = (mKeyboard is LatinKeyboard) &&
            (mKeyboard as LatinKeyboard).isLanguageSwitchEnabled()
        val hidePreviewOrShowSpaceKeyPreview = (tracker == null) ||
            tracker.isSpaceKey(effectiveKeyIndex) || tracker.isSpaceKey(oldKeyIndex)
        if (oldKeyIndex != effectiveKeyIndex &&
            (mShowPreview || (hidePreviewOrShowSpaceKeyPreview && isLanguageSwitchEnabled))) {
            if (effectiveKeyIndex == NOT_A_KEY) {
                val keys = mKeys
                if (keys != null && oldKeyIndex != NOT_A_KEY && oldKeyIndex < keys.size) {
                    invalidateKey(keys[oldKeyIndex])
                }
                mHandler.cancelPopupPreview()
                mHandler.dismissPreview(mDelayAfterPreview.toLong())
            } else if (tracker != null) {
                val delay = if (mShowPreview) mDelayBeforePreview else mDelayBeforeSpacePreview
                mHandler.popupPreview(delay.toLong(), effectiveKeyIndex, tracker)
            }
        }
    }

    private fun showKey(keyIndex: Int, tracker: PointerTracker) {
        val key = tracker.getKey(keyIndex) ?: return

        val keys = mKeys
        if (keys != null && mOldPreviewKeyIndex != NOT_A_KEY && mOldPreviewKeyIndex < keys.size) {
            invalidateKey(keys[mOldPreviewKeyIndex])
        }
        invalidateKey(key)

        if (mPreviewPopupDrawable == null) {
            mPreviewPopupDrawable = SeamlessPopupDrawable(context)
            val density = resources.displayMetrics.density
            mPreviewPopupDrawable!!.setStrokeWidth(2.0f * density)
            mPreviewPopupDrawable!!.setCornerRadius(4.0f * density)
            mPreviewPopupDrawable!!.setKeyCornerRadius(4.0f * density)
            mPreviewText!!.setBackgroundDrawable(mPreviewPopupDrawable)
        }

        val typedValue = TypedValue()
        val theme = context.theme

        var keyColor = 0xFF1a1b26.toInt()
        val attrId = if (key.modifier) R.attr.kbdColorMod else R.attr.kbdColorAlpha
        if (theme.resolveAttribute(attrId, typedValue, true)) {
            keyColor = typedValue.data
        } else if (theme.resolveAttribute(R.attr.kbdColorBase, typedValue, true)) {
            keyColor = typedValue.data
        }

        var strokeColor = 0xFF414868.toInt()
        if (theme.resolveAttribute(R.attr.kbdColorPopup, typedValue, true)) {
            strokeColor = typedValue.data
        }
        mPreviewPopupDrawable!!.setColors(keyColor, strokeColor)

        val density = resources.displayMetrics.density
        mPreviewPopupDrawable!!.setStrokeWidth(if (shouldDrawStroke(key)) 2.0f * density else 0f)

        val padding = (6 * density).toInt()
        mPreviewText!!.setPadding(padding, padding, padding, padding + key.height)

        val icon = key.icon
        if (icon != null && TextUtils.isEmpty(key.label) && !shouldDrawLabelAndIcon(key)) {
            mPreviewText!!.setCompoundDrawables(null, null, null,
                if (key.iconPreview != null) key.iconPreview else icon)
            mPreviewText!!.text = null
        } else {
            mPreviewText!!.setCompoundDrawables(null, null, null, null)
            mPreviewText!!.text = key.getCaseLabel()
            if (key.label != null && key.label!!.length > 1 && key.codes!!.size < 2) {
                mPreviewText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize)
            } else {
                mPreviewText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPreviewTextSizeLarge.toFloat())
            }
            mPreviewText!!.typeface = mKeyTextStyle
        }
        mPreviewText!!.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))

        val popupWidth = key.width
        val popupHeight = Math.max(mPreviewText!!.measuredHeight, mPreviewHeight)

        val lp = mPreviewText!!.layoutParams
        if (lp != null) {
            lp.width = popupWidth
            lp.height = popupHeight
        }

        var popupPreviewX = key.x
        var popupPreviewY = key.y + key.height - popupHeight

        mHandler.cancelDismissPreview()
        if (mOffsetInWindow == null) mOffsetInWindow = IntArray(2)
        getLocationInWindow(mOffsetInWindow)
        mOffsetInWindow!![0] += mPopupPreviewOffsetX
        mOffsetInWindow!![1] += mPopupPreviewOffsetY
        val windowLocation = IntArray(2)
        getLocationOnScreen(windowLocation)
        mWindowY = windowLocation[1]

        popupPreviewX += mOffsetInWindow!![0]
        popupPreviewY += mOffsetInWindow!![1]

        if (popupPreviewY + mWindowY < 0) {
            if (key.x + key.width <= width / 2) {
                popupPreviewX += (key.width * 2.5).toInt()
            } else {
                popupPreviewX -= (key.width * 2.5).toInt()
            }
            popupPreviewY += popupHeight
        }

        if (mPreviewPopupDrawable != null) {
            var contentHeight = popupHeight - key.height
            if (contentHeight < 0) contentHeight = 0

            val popupRect = Rect(0, 0, popupWidth, contentHeight)
            val keyRect = Rect(0, contentHeight, popupWidth, popupHeight)

            mPreviewPopupDrawable!!.setGeometry(
                keyRect,
                popupRect,
                key.height.toFloat(),
                contentHeight.toFloat(),
                10f * density,
                keyRect.top.toFloat(),
                popupRect.top.toFloat()
            )
        }

        val previewPopup = mPreviewPopup!!
        if (previewPopup.isShowing) {
            previewPopup.update(popupPreviewX, popupPreviewY, popupWidth, popupHeight)
        } else {
            previewPopup.width = popupWidth
            previewPopup.height = popupHeight
            previewPopup.showAtLocation(mMiniKeyboardParent, Gravity.NO_GRAVITY,
                popupPreviewX, popupPreviewY)
        }
        mPopupPreviewDisplayedY = popupPreviewY
        mPreviewText!!.visibility = VISIBLE
    }

    fun invalidateAllKeys() {
        mDirtyRect.union(0, 0, width, height)
        invalidate()
    }

    override fun invalidateKey(key: Key) {
        if (key == null) return
        mInvalidatedKey = key
        mDirtyRect.union(key.x + paddingLeft, key.y + paddingTop,
            key.x + key.width + paddingLeft, key.y + key.height + paddingTop)
        invalidate(key.x + paddingLeft, key.y + paddingTop,
            key.x + key.width + paddingLeft, key.y + key.height + paddingTop)
    }

    private fun openPopupIfRequired(keyIndex: Int, tracker: PointerTracker): Boolean {
        if (mPopupLayout == 0) return false
        val popupKey = tracker.getKey(keyIndex) ?: return false
        if (tracker.isInSlidingKeyInput()) return false
        val result = onLongPress(popupKey)
        if (result) {
            dismissKeyPreview()
            mMiniKeyboardTrackerId = tracker.mPointerId
            tracker.setAlreadyProcessed()
            mPointerQueue.remove(tracker)
        }
        return result
    }

    private fun inflateMiniKeyboardContainer() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val container = inflater.inflate(mPopupLayout, null)

        mMiniKeyboard = container.findViewById<View>(R.id.LatinKeyboardBaseView) as LatinKeyboardBaseView

        mSeamlessPopupDrawable = SeamlessPopupDrawable(context)

        val typedValue = TypedValue()
        val theme = context.theme
        var backgroundColor = 0xFF1a1b26.toInt()
        if (theme.resolveAttribute(R.attr.kbdColorBase, typedValue, true)) {
            backgroundColor = typedValue.data
        }
        var strokeColor = 0xFF414868.toInt()
        if (theme.resolveAttribute(R.attr.kbdColorPopup, typedValue, true)) {
            strokeColor = typedValue.data
        }
        mSeamlessPopupDrawable!!.setColors(backgroundColor, strokeColor)
        val density = resources.displayMetrics.density
        mSeamlessPopupDrawable!!.setCornerRadius(4.0f * density)
        mSeamlessPopupDrawable!!.setKeyCornerRadius(4.0f * density)

        container.setBackgroundDrawable(mSeamlessPopupDrawable)

        mMiniKeyboard!!.mIsMiniKeyboard = true
        mMiniKeyboard!!.setOnKeyboardActionListener(object : OnKeyboardActionListener {
            override fun onKey(primaryCode: Int, keyCodes: IntArray?, x: Int, y: Int) {
                mKeyboardActionListener!!.onKey(primaryCode, keyCodes, x, y)
                dismissPopupKeyboard()
            }
            override fun onText(text: CharSequence) {
                mKeyboardActionListener!!.onText(text)
                dismissPopupKeyboard()
            }
            override fun onCancel() {
                mKeyboardActionListener!!.onCancel()
                dismissPopupKeyboard()
            }
            override fun swipeLeft(): Boolean = false
            override fun swipeRight(): Boolean = false
            override fun swipeUp(): Boolean = false
            override fun swipeDown(): Boolean = false
            override fun onPress(primaryCode: Int) { mKeyboardActionListener!!.onPress(primaryCode) }
            override fun onRelease(primaryCode: Int) { mKeyboardActionListener!!.onRelease(primaryCode) }
        })

        mMiniKeyboard!!.mKeyDetector = MiniKeyboardKeyDetector(mMiniKeyboardSlideAllowance)
        mMiniKeyboard!!.mGestureDetector = null

        mMiniKeyboard!!.setPopupParent(this)

        mMiniKeyboardContainer = container
    }

    private fun isOneRowKeys(keys: List<Key>): Boolean {
        if (keys.isEmpty()) return false
        val edgeFlags = keys[0].edgeFlags
        return (edgeFlags and Keyboard.EDGE_TOP) != 0 && (edgeFlags and Keyboard.EDGE_BOTTOM) != 0
    }

    private fun getLongPressKeyboard(popupKey: Key): Keyboard? {
        val cache: WeakHashMap<Key, Keyboard> = when {
            popupKey.isDistinctCaps() -> mMiniKeyboardCacheCaps
            popupKey.isShifted() -> mMiniKeyboardCacheShift
            else -> mMiniKeyboardCacheMain
        }
        var kbd = cache[popupKey]
        if (kbd == null) {
            kbd = popupKey.getPopupKeyboard(context, paddingLeft + paddingRight)
            if (kbd != null) cache[popupKey] = kbd
        }
        return kbd
    }

    protected open fun onLongPress(popupKey: Key): Boolean {
        if (mPopupLayout == 0) return false

        val kbd = getLongPressKeyboard(popupKey) ?: return false

        if (mMiniKeyboardContainer == null) inflateMiniKeyboardContainer()
        val miniKeyboard = mMiniKeyboard ?: return false
        miniKeyboard.setKeyboard(kbd)

        mMiniKeyboardContainer!!.setPadding(0, 0, 0, popupKey.height)
        mMiniKeyboardContainer!!.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))

        if (mWindowOffset == null) {
            mWindowOffset = IntArray(2)
            getLocationInWindow(mWindowOffset)
        }

        val miniKeys = miniKeyboard.keyboard!!.getKeys()
        val miniKeyWidth = if (miniKeys.size > 0) miniKeys[0].width else 0

        var popupX = popupKey.x + mWindowOffset!![0]
        popupX += paddingLeft
        if (shouldAlignLeftmost(popupKey)) {
            popupX += popupKey.width - miniKeyWidth
            popupX -= mMiniKeyboardContainer!!.paddingLeft
        } else {
            popupX += miniKeyWidth
            popupX -= mMiniKeyboardContainer!!.measuredWidth
            popupX += mMiniKeyboardContainer!!.paddingRight
        }

        val contentHeight = mMiniKeyboardContainer!!.measuredHeight - popupKey.height
        var popupY = popupKey.y + mWindowOffset!![1] + paddingTop
        popupY -= contentHeight

        val x = popupX
        val y = if (mShowPreview && isOneRowKeys(miniKeys)) mPopupPreviewDisplayedY else popupY

        var adjustedX = x
        if (x < 0) {
            adjustedX = 0
        } else if (x > measuredWidth - mMiniKeyboardContainer!!.measuredWidth) {
            adjustedX = measuredWidth - mMiniKeyboardContainer!!.measuredWidth
        }

        if (mSeamlessPopupDrawable != null) {
            val typedValue = TypedValue()
            val theme = context.theme

            var keyColor = 0xFF1a1b26.toInt()
            val attrId = if (popupKey.modifier) R.attr.kbdColorMod else R.attr.kbdColorAlpha
            if (theme.resolveAttribute(attrId, typedValue, true)) {
                keyColor = typedValue.data
            } else if (theme.resolveAttribute(R.attr.kbdColorBase, typedValue, true)) {
                keyColor = typedValue.data
            }

            var strokeColor = 0xFF414868.toInt()
            if (theme.resolveAttribute(R.attr.kbdColorPopup, typedValue, true)) {
                strokeColor = typedValue.data
            }
            mSeamlessPopupDrawable!!.setColors(keyColor, strokeColor)

            val density = resources.displayMetrics.density
            mSeamlessPopupDrawable!!.setStrokeWidth(if (popupKey.modifier) 0f else 2.0f * density)

            val popupRect = Rect(0, 0, mMiniKeyboardContainer!!.measuredWidth, contentHeight)

            val keyGlobalX = popupKey.x + mWindowOffset!![0] + paddingLeft
            val relativeKeyX = keyGlobalX - adjustedX

            val keyRect = Rect(relativeKeyX, contentHeight,
                relativeKeyX + popupKey.width, contentHeight + popupKey.height)

            mSeamlessPopupDrawable!!.setGeometry(
                keyRect,
                popupRect,
                popupKey.height.toFloat(),
                contentHeight.toFloat(),
                10f * density,
                keyRect.top.toFloat(),
                popupRect.top.toFloat()
            )
        }

        mMiniKeyboardOriginX = adjustedX + mMiniKeyboardContainer!!.paddingLeft - mWindowOffset!![0]
        mMiniKeyboardOriginY = y + mMiniKeyboardContainer!!.paddingTop - mWindowOffset!![1]
        miniKeyboard.setPopupOffset(adjustedX, y)
        miniKeyboard.setShiftState(getShiftState())
        miniKeyboard.setPreviewEnabled(false)
        mMiniKeyboardPopup!!.contentView = mMiniKeyboardContainer
        mMiniKeyboardPopup!!.width = mMiniKeyboardContainer!!.measuredWidth
        mMiniKeyboardPopup!!.height = mMiniKeyboardContainer!!.measuredHeight
        mMiniKeyboardPopup!!.showAtLocation(this, Gravity.NO_GRAVITY, adjustedX, y)
        mMiniKeyboardVisible = true

        val eventTime = SystemClock.uptimeMillis()
        mMiniKeyboardPopupTime = eventTime
        val downEvent = generateMiniKeyboardMotionEvent(MotionEvent.ACTION_DOWN,
            popupKey.x + popupKey.width / 2, popupKey.y + popupKey.height / 2, eventTime)
        miniKeyboard.onTouchEvent(downEvent)
        downEvent.recycle()

        invalidateAllKeys()
        return true
    }

    private fun shouldDrawIconFully(key: Key): Boolean =
        isNumberAtEdgeOfPopupChars(key) || isLatinF1Key(key) || LatinKeyboard.hasPuncOrSmileysPopup(key)

    private fun shouldDrawLabelAndIcon(key: Key): Boolean =
        isNonMicLatinF1Key(key) || LatinKeyboard.hasPuncOrSmileysPopup(key)

    private fun shouldDrawStroke(key: Key): Boolean {
        if (key.modifier) return false
        val codes = key.codes
        if (codes == null || codes.isEmpty()) return true
        val code = codes[0]
        if (code == Keyboard.KEYCODE_SHIFT || code == Keyboard.KEYCODE_MODE_CHANGE ||
            code == Keyboard.KEYCODE_ALT_SYM) return false
        if (code == -113 || code == -57 || code == -117) return false
        return true
    }

    private fun shouldAlignLeftmost(key: Key): Boolean = !key.popupReversed

    private fun isLatinF1Key(key: Key): Boolean =
        (mKeyboard is LatinKeyboard) && (mKeyboard as LatinKeyboard).isF1Key(key)

    private fun isNonMicLatinF1Key(key: Key): Boolean =
        isLatinF1Key(key) && key.label != null

    private fun generateMiniKeyboardMotionEvent(action: Int, x: Int, y: Int, eventTime: Long): MotionEvent =
        MotionEvent.obtain(mMiniKeyboardPopupTime, eventTime, action,
            (x - mMiniKeyboardOriginX).toFloat(), (y - mMiniKeyboardOriginY).toFloat(), 0)

    open fun enableSlideKeyHack(): Boolean = false

    private fun getPointerTracker(id: Int): PointerTracker {
        val pointers = mPointerTrackers
        val keys = mKeys
        val listener = mKeyboardActionListener

        for (i in pointers.size..id) {
            val tracker = PointerTracker(i, mHandler, mKeyDetector, this, resources, enableSlideKeyHack())
            if (keys != null) tracker.setKeyboard(keys, mKeyHysteresisDistance)
            if (listener != null) tracker.setOnKeyboardActionListener(listener)
            pointers.add(tracker)
        }

        return pointers[id]
    }

    fun isInSlidingKeyInput(): Boolean {
        val mini = mMiniKeyboard
        return if (mMiniKeyboardVisible && mini != null) {
            mini.isInSlidingKeyInput()
        } else {
            mPointerQueue.isInSlidingKeyInput()
        }
    }

    fun getPointerCount(): Int = mOldPointerCount

    override fun onTouchEvent(me: MotionEvent): Boolean = onTouchEvent(me, false)

    fun onTouchEvent(me: MotionEvent, continuing: Boolean): Boolean {
        val action = me.actionMasked
        val pointerCount = me.pointerCount
        val oldPointerCount = mOldPointerCount
        mOldPointerCount = pointerCount

        if (!mHasDistinctMultitouch && pointerCount > 1 && oldPointerCount > 1) return true

        mSwipeTracker.addMovement(me)

        if (!mMiniKeyboardVisible && mGestureDetector != null && mGestureDetector!!.onTouchEvent(me)) {
            dismissKeyPreview()
            mHandler.cancelKeyTimers()
            return true
        }

        val eventTime = me.eventTime
        val index = me.actionIndex
        val id = me.getPointerId(index)
        val x = me.getX(index).toInt()
        val y = me.getY(index).toInt()

        val mini = mMiniKeyboard
        if (mMiniKeyboardVisible && mini != null) {
            val miniKeyboardPointerIndex = me.findPointerIndex(mMiniKeyboardTrackerId)
            if (miniKeyboardPointerIndex >= 0 && miniKeyboardPointerIndex < pointerCount) {
                val miniKeyboardX = me.getX(miniKeyboardPointerIndex).toInt()
                val miniKeyboardY = me.getY(miniKeyboardPointerIndex).toInt()
                val translated = generateMiniKeyboardMotionEvent(action, miniKeyboardX, miniKeyboardY, eventTime)
                mini.onTouchEvent(translated)
                translated.recycle()
            }
            return true
        }

        if (mHandler.isInKeyRepeat()) {
            if (action == MotionEvent.ACTION_MOVE) return true
            val tracker = getPointerTracker(id)
            if (pointerCount > 1 && !tracker.isModifier()) {
                mHandler.cancelKeyRepeatTimer()
            }
        }

        if (!mHasDistinctMultitouch) {
            val tracker = getPointerTracker(0)
            if (pointerCount == 1 && oldPointerCount == 2) {
                tracker.onDownEvent(x, y, eventTime)
            } else if (pointerCount == 2 && oldPointerCount == 1) {
                tracker.onUpEvent(tracker.getLastX(), tracker.getLastY(), eventTime)
            } else if (pointerCount == 1 && oldPointerCount == 1) {
                tracker.onTouchEvent(action, x, y, eventTime)
            } else {
                Log.w(TAG, "Unknown touch panel behavior: pointer count is $pointerCount (old $oldPointerCount)")
            }
            if (continuing) tracker.setSlidingKeyInputState(true)
            return true
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (!mIgnoreMove) {
                for (i in 0 until pointerCount) {
                    val tracker = getPointerTracker(me.getPointerId(i))
                    tracker.onMoveEvent(me.getX(i).toInt(), me.getY(i).toInt(), eventTime)
                }
            }
        } else {
            val tracker = getPointerTracker(id)
            when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    mIgnoreMove = false
                    onDownEvent(tracker, x, y, eventTime)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mIgnoreMove = false
                    onUpEvent(tracker, x, y, eventTime)
                }
                MotionEvent.ACTION_CANCEL -> onCancelEvent(tracker, x, y, eventTime)
            }
            if (continuing) tracker.setSlidingKeyInputState(true)
        }

        return true
    }

    private fun onDownEvent(tracker: PointerTracker, x: Int, y: Int, eventTime: Long) {
        if (tracker.isOnModifierKey(x, y)) {
            mPointerQueue.releaseAllPointersExcept(null, eventTime)
        }
        tracker.onDownEvent(x, y, eventTime)
        mPointerQueue.add(tracker)
    }

    private fun onUpEvent(tracker: PointerTracker, x: Int, y: Int, eventTime: Long) {
        if (tracker.isModifier()) {
            mPointerQueue.releaseAllPointersExcept(tracker, eventTime)
        } else {
            val index = mPointerQueue.lastIndexOf(tracker)
            if (index >= 0) {
                mPointerQueue.releaseAllPointersOlderThan(tracker, eventTime)
            } else {
                Log.w(TAG, "onUpEvent: corresponding down event not found for pointer ${tracker.mPointerId}")
            }
        }
        tracker.onUpEvent(x, y, eventTime)
        mPointerQueue.remove(tracker)
    }

    private fun onCancelEvent(tracker: PointerTracker, x: Int, y: Int, eventTime: Long) {
        tracker.onCancelEvent(x, y, eventTime)
        mPointerQueue.remove(tracker)
    }

    protected open fun swipeRight(): Boolean = mKeyboardActionListener!!.swipeRight()
    protected open fun swipeLeft(): Boolean = mKeyboardActionListener!!.swipeLeft()
    open fun swipeUp(): Boolean = mKeyboardActionListener!!.swipeUp()
    protected open fun swipeDown(): Boolean = mKeyboardActionListener!!.swipeDown()

    open fun closing() {
        Log.i(TAG, "closing $this")
        mPreviewPopup?.dismiss()
        mHandler.cancelAllMessages()
        dismissPopupKeyboard()
        mMiniKeyboardCacheMain.clear()
        mMiniKeyboardCacheShift.clear()
        mMiniKeyboardCacheCaps.clear()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
    }

    protected fun popupKeyboardIsShowing(): Boolean =
        mMiniKeyboardPopup != null && mMiniKeyboardPopup!!.isShowing

    protected fun dismissPopupKeyboard() {
        val popup = mMiniKeyboardPopup ?: return
        if (popup.isShowing) popup.dismiss()
        mMiniKeyboardVisible = false
        mPointerQueue.releaseAllPointersExcept(null, 0)
        invalidateAllKeys()
    }

    fun handleBack(): Boolean {
        val popup = mMiniKeyboardPopup
        if (popup != null && popup.isShowing) {
            dismissPopupKeyboard()
            return true
        }
        return false
    }
}
