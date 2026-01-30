package org.pocketworkstation.pckeyboard

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round

/**
 * SeekBarPreference provides a dialog for editing float-valued preferences with a slider.
 */
open class SeekBarPreference : Preference {

    private var mMin: Float = 0f
    private var mMax: Float = 0f
    protected var mVal: Float = 0f
    protected var mPrevVal: Float = 0f
    private var mStep: Float = 0f
    private var mAsPercent: Boolean = false
    private var mLogScale: Boolean = false
    private var mDisplayFormat: String? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    protected open fun init(context: Context, attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference)
        mMin = a.getFloat(R.styleable.SeekBarPreference_minValue, 0.0f)
        mMax = a.getFloat(R.styleable.SeekBarPreference_maxValue, 100.0f)
        mStep = a.getFloat(R.styleable.SeekBarPreference_step, 0.0f)
        mAsPercent = a.getBoolean(R.styleable.SeekBarPreference_asPercent, false)
        mLogScale = a.getBoolean(R.styleable.SeekBarPreference_logScale, false)
        mDisplayFormat = a.getString(R.styleable.SeekBarPreference_displayFormat)
        a.recycle()
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getFloat(index, 0.0f)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        if (getPersistedFloat(-1.234f) != -1.234f) {
            mVal = getPersistedFloat(0.0f)
        } else if (defaultValue != null) {
            mVal = when (defaultValue) {
                is Float -> defaultValue
                else -> try {
                    defaultValue.toString().toFloat()
                } catch (e: NumberFormatException) {
                    0.0f
                }
            }
        } else {
            mVal = 0.0f
        }
        mPrevVal = mVal
    }

    private fun formatFloatDisplay(value: Float): String {
        return when {
            mAsPercent -> String.format("%d%%", (value * 100).toInt())
            mDisplayFormat != null -> String.format(mDisplayFormat!!, value)
            else -> value.toString()
        }
    }

    protected fun setVal(value: Float) {
        mVal = value
    }

    protected fun savePrevVal() {
        mPrevVal = mVal
    }

    protected fun restoreVal() {
        mVal = mPrevVal
    }

    protected fun getValString(): String {
        return mVal.toString()
    }

    private fun percentToSteppedVal(percent: Int, min: Float, max: Float, step: Float, logScale: Boolean): Float {
        var value = if (logScale) {
            exp(percentToSteppedVal(percent, ln(min), ln(max), step, false))
        } else {
            var delta = percent * (max - min) / 100
            if (step != 0.0f) {
                delta = round(delta / step) * step
            }
            min + delta
        }
        value = String.format(Locale.US, "%.2g", value).toFloat()
        return value
    }

    private fun getPercent(value: Float, min: Float, max: Float): Int {
        if (max == min) return 0
        return (100 * (value - min) / (max - min)).toInt()
    }

    private fun getProgressVal(): Int {
        return if (mLogScale) {
            getPercent(ln(mVal), ln(mMin), ln(mMax))
        } else {
            getPercent(mVal, mMin, mMax)
        }
    }

    override fun onClick() {
        showDialog()
    }

    private fun showDialog() {
        val context = context
        val view = LayoutInflater.from(context).inflate(R.layout.seek_bar_dialog, null)
        val seek = view.findViewById<SeekBar>(R.id.seekBarPref)
        val minText = view.findViewById<TextView>(R.id.seekMin)
        val maxText = view.findViewById<TextView>(R.id.seekMax)
        val valText = view.findViewById<TextView>(R.id.seekVal)

        valText.text = formatFloatDisplay(mVal)
        minText.text = formatFloatDisplay(mMin)
        maxText.text = formatFloatDisplay(mMax)
        seek.progress = getProgressVal()

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newVal = percentToSteppedVal(progress, mMin, mMax, mStep, mLogScale)
                    if (newVal != mVal) {
                        onChange(newVal)
                    }
                    mVal = newVal
                    seek.progress = getProgressVal()
                }
                valText.text = formatFloatDisplay(mVal)
            }
        })

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (callChangeListener(mVal)) {
                    persistValue(mVal)
                    mPrevVal = mVal
                    notifyChanged()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                mVal = mPrevVal
            }
            .show()
    }

    open fun onChange(value: Float) {
        // override in subclasses
    }

    protected open fun persistValue(value: Float) {
        persistFloat(value)
    }

    override fun getSummary(): CharSequence {
        return formatFloatDisplay(mVal)
    }
}
