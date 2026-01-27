package org.pocketworkstation.pckeyboard

import android.content.Context
import androidx.preference.ListPreference
import android.util.AttributeSet
import android.util.Log

/**
 * Custom ListPreference that automatically updates its summary to reflect the selected entry.
 *
 * This preference extends [ListPreference] to provide automatic synchronization between
 * the selected value and the displayed summary. The summary shows the human-readable entry
 * text (not the entry value), with special handling for percent signs.
 */
class AutoSummaryListPreference : ListPreference {

    companion object {
        private const val TAG = "HK/AutoSummaryListPreference"
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    /**
     * Attempts to set the summary to the currently selected entry.
     * If the entry cannot be retrieved (due to misconfiguration), logs a warning
     * and leaves the summary unchanged.
     */
    private fun trySetSummary() {
        val entry: CharSequence? = try {
            getEntry()
        } catch (e: Exception) {
            Log.i(TAG, "Malfunctioning ListPreference, can't get entry: ${e.message}")
            null
        }

        if (entry != null) {
            val percent = "percent"
            summary = entry.toString().replace("%", " $percent")
        }
    }

    override fun setEntries(entries: Array<CharSequence>) {
        super.setEntries(entries)
        trySetSummary()
    }

    override fun setEntryValues(entryValues: Array<CharSequence>) {
        super.setEntryValues(entryValues)
        trySetSummary()
    }

    override fun setValue(value: String?) {
        super.setValue(value)
        trySetSummary()
    }
}
