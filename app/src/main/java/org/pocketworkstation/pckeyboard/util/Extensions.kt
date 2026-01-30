package org.pocketworkstation.pckeyboard.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.view.inputmethod.InputConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// ═══════════════════════════════════════════════════════════════
// COROUTINE SCOPES
// ═══════════════════════════════════════════════════════════════

/**
 * Create a coroutine scope suitable for IME service lifecycle.
 * Uses SupervisorJob to prevent child failures from canceling siblings.
 */
fun imeServiceScope(): CoroutineScope = CoroutineScope(
    SupervisorJob() + Dispatchers.Main.immediate
)

// ═══════════════════════════════════════════════════════════════
// INPUT CONNECTION EXTENSIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Execute operations within a batch edit block.
 * Automatically handles null connections and batch edit lifecycle.
 */
inline fun InputConnection?.withBatchEdit(block: InputConnection.() -> Unit) {
    this ?: return
    beginBatchEdit()
    try {
        block()
    } finally {
        endBatchEdit()
    }
}

/**
 * Safely commit text, handling null connections.
 */
fun InputConnection?.safeCommitText(text: CharSequence, newCursorPosition: Int = 1) {
    this?.commitText(text, newCursorPosition)
}

/**
 * Safely delete surrounding text, handling null connections.
 */
fun InputConnection?.safeDeleteSurroundingText(beforeLength: Int, afterLength: Int) {
    this?.deleteSurroundingText(beforeLength, afterLength)
}

// ═══════════════════════════════════════════════════════════════
// VIEW EXTENSIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Set view visibility to VISIBLE.
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Set view visibility to GONE.
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Set view visibility to INVISIBLE.
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Toggle view visibility between VISIBLE and GONE.
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

// ═══════════════════════════════════════════════════════════════
// SHARED PREFERENCES EXTENSIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Apply changes to SharedPreferences (always available since API 9).
 */
inline fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
    edit().apply {
        block()
        apply()
    }
}

// ═══════════════════════════════════════════════════════════════
// LOGGING EXTENSIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Log debug message with tag.
 */
fun Any.logd(message: String) {
    Log.d(this::class.java.simpleName, message)
}

/**
 * Log error message with tag.
 */
fun Any.loge(message: String, throwable: Throwable? = null) {
    Log.e(this::class.java.simpleName, message, throwable)
}

/**
 * Log warning message with tag.
 */
fun Any.logw(message: String) {
    Log.w(this::class.java.simpleName, message)
}

/**
 * Log info message with tag.
 */
fun Any.logi(message: String) {
    Log.i(this::class.java.simpleName, message)
}

// ═══════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Return value if predicate is true, null otherwise.
 */
inline fun <T> T.takeIfTrue(predicate: Boolean): T? = if (predicate) this else null

/**
 * Return value if predicate is false, null otherwise.
 */
inline fun <T> T.takeIfFalse(predicate: Boolean): T? = if (!predicate) this else null
