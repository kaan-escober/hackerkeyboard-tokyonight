package org.pocketworkstation.pckeyboard

import android.content.Context
import androidx.preference.EditTextPreference
import android.util.AttributeSet

/**
 * Custom preference that extends [EditTextPreference] to automatically
 * update the summary text to match the entered value.
 *
 * This preference synchronizes the display summary with the text input value,
 * providing real-time visual feedback of the currently stored preference value.
 * When [setText] is called, the summary is automatically updated
 * to display the new text.
 */
class AutoSummaryEditTextPreference : EditTextPreference {

    /**
     * Constructs an `AutoSummaryEditTextPreference` with the given context.
     *
     * @param context the [Context] used to access resources
     */
    constructor(context: Context) : super(context)

    /**
     * Constructs an `AutoSummaryEditTextPreference` with the given context and attributes.
     *
     * @param context the [Context] used to access resources
     * @param attrs   the attributes of the XML tag that is inflating the preference
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * Constructs an `AutoSummaryEditTextPreference` with the given context, attributes,
     * and default style attribute.
     *
     * @param context the [Context] used to access resources
     * @param attrs   the attributes of the XML tag that is inflating the preference
     * @param defStyle the default style attribute resource ID
     */
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /**
     * Constructs an `AutoSummaryEditTextPreference` with the given context, attributes,
     * default style attribute, and default style resource ID.
     *
     * @param context the [Context] used to access resources
     * @param attrs   the attributes of the XML tag that is inflating the preference
     * @param defStyleAttr the default style attribute resource ID
     * @param defStyleRes  the default style resource ID
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    /**
     * Sets the text value for this preference and automatically updates the summary.
     *
     * This method overrides the parent [EditTextPreference.setText] to
     * ensure that whenever the text value changes, the preference's summary is also
     * updated to display the new text. This provides visual feedback of the current
     * preference value in the preference screen.
     *
     * @param text the new text value to set for this preference
     */
    override fun setText(text: String?) {
        super.setText(text)
        summary = text
    }
}
