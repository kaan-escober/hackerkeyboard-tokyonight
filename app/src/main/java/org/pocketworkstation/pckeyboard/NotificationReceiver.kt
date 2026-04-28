/*
 * Broadcast receiver that handles keyboard notification actions.
 *
 * This class extends BroadcastReceiver to receive and process intents from
 * keyboard notifications. It supports two primary actions:
 * - ACTION_SHOW: Displays the soft input keyboard
 * - ACTION_SETTINGS: Opens the keyboard settings activity
 *
 * The receiver is instantiated with a reference to the LatinIME service
 * to enable control of the keyboard input method.
 */
package org.pocketworkstation.pckeyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.inputmethod.InputMethodManager

/**
 * Broadcast receiver for handling keyboard notification actions.
 *
 * Processes intents from the keyboard's notification panel to either show the
 * soft input keyboard or open the settings activity.
 */
class NotificationReceiver(private val ime: LatinIME) : BroadcastReceiver() {

    companion object {
        const val TAG: String = "PCKeyboard/Notification"

       
        val ACTION_SHOW: String = "org.pocketworkstation.pckeyboard.SHOW"

       
        val ACTION_SETTINGS: String = "org.pocketworkstation.pckeyboard.SETTINGS"
    }

    init {
        Log.i(TAG, "NotificationReceiver created, ime=$ime")
    }

    /**
     * Handles broadcast intents from keyboard notifications.
     *
     * Processes two action types:
     * - ACTION_SHOW: Calls InputMethodManager.showSoftInputFromInputMethod
     *   to display the keyboard with the SHOW_FORCED flag
     * - ACTION_SETTINGS: Starts the MaterialSettingsActivity to allow
     *   the user to configure keyboard settings
     *
     * @param context The context in which the receiver is running
     * @param intent The intent being received, containing the action to perform
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "NotificationReceiver.onReceive called, action=$action")

        when (action) {
            ACTION_SHOW -> {
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInputFromInputMethod(ime.mToken, InputMethodManager.SHOW_FORCED)
            }
            ACTION_SETTINGS -> {
                context.startActivity(Intent(ime, MaterialSettingsActivity::class.java))
            }
        }
    }
}
