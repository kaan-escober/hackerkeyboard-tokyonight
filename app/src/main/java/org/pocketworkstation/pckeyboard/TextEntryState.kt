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
import android.text.format.DateFormat
import android.util.Log
import org.pocketworkstation.pckeyboard.Keyboard.Key
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.Calendar

/**
 * Tracks text input state and provides debug logging support for keyboard interactions.
 *
 * This class implements a state machine to track the current text entry state, which is used
 * for autocorrection and diagnostic logging. The state transitions follow these rules:
 * - START: Initial state or after a separator/space. Transitions to IN_WORD when
 *   a non-separator character is typed.
 * - IN_WORD: Inside a word (non-space, non-separator characters). Transitions to
 *   START when a space or separator is typed.
 * - PUNCTUATION_AFTER_WORD: After punctuation following a word. Transitions based
 *   on next character type.
 * - UNKNOWN: Undefined state, treated similarly to START.
 *
 * The class provides optional debug logging to files (key.txt and action.txt) when
 * the LOGGING flag is enabled.
 */
class TextEntryState {

    enum class State {
        UNKNOWN,
        START,
        IN_WORD,
        PUNCTUATION_AFTER_WORD,
    }

    companion object {
        private const val DBG = false
        private const val TAG = "TextEntryState"

        private var LOGGING = false

        private var sBackspaceCount = 0
        private var sSessionCount = 0
        private var sTypedChars = 0
        private var sActualChars = 0

        private var sState: State = State.UNKNOWN

        private var sKeyLocationFile: FileOutputStream? = null
        private var sUserActionFile: FileOutputStream? = null

        /**
         * Initializes a new text entry session and resets state counters.
         *
         * @param context The Android application context, used to open log files if LOGGING is enabled
         */
       
        fun newSession(context: Context) {
            sSessionCount++
            sBackspaceCount = 0
            sTypedChars = 0
            sActualChars = 0
            sState = State.START

            if (LOGGING) {
                try {
                    sKeyLocationFile = context.openFileOutput("key.txt", Context.MODE_APPEND)
                    sUserActionFile = context.openFileOutput("action.txt", Context.MODE_APPEND)
                } catch (ioe: IOException) {
                    Log.e(TAG, "Couldn't open file for output: $ioe")
                }
            }
        }

        /**
         * Closes the current text entry session and writes session statistics to the debug log.
         */
       
        fun endSession() {
            if (sKeyLocationFile == null) return

            try {
                sKeyLocationFile!!.close()

                val out =
                    DateFormat.format("MM:dd hh:mm:ss", Calendar.getInstance().time).toString() +
                        " BS: " + sBackspaceCount +
                        " saved: " + ((sActualChars - sTypedChars).toFloat() / sActualChars) +
                        "\n"

                sUserActionFile!!.write(out.toByteArray(Charset.defaultCharset()))
                sUserActionFile!!.close()

                sKeyLocationFile = null
                sUserActionFile = null
            } catch (_: IOException) {
                // intentionally swallowed
            }
        }

       
        fun acceptedDefault(typedWord: CharSequence, actualWord: CharSequence) {
            // no-op
        }

       
        fun backToAcceptedDefault(typedWord: CharSequence) {
            // no-op
        }

       
        fun manualTyped(typedWord: CharSequence) {
            sState = State.START
            displayState()
        }

       
        fun acceptedTyped(typedWord: CharSequence) {
            // no-op
        }

       
        fun selectedForCorrection() {
            // no-op
        }

        /**
         * Updates the text entry state based on the typed character.
         *
         * @param c The character that was typed
         * @param isSeparator true if the character is a word separator, false otherwise
         */
       
        fun typedCharacter(c: Char, isSeparator: Boolean) {
            val isSpace = c == ' '
            when (sState) {
                State.IN_WORD -> {
                    if (isSpace || isSeparator) {
                        sState = State.START
                    }
                }
                State.START,
                State.UNKNOWN,
                State.PUNCTUATION_AFTER_WORD -> {
                    sState = if (!isSpace && !isSeparator) {
                        State.IN_WORD
                    } else {
                        State.START
                    }
                }
            }
            displayState()
        }

        /**
         * Records a backspace key press and updates debug logging.
         */
       
        fun backspace() {
            sBackspaceCount++
            displayState()
        }

       
        fun reset() {
            sState = State.START
            displayState()
        }

        /**
         * Returns the current text entry state.
         *
         * @return The current State of the text entry state machine
         */
       
        fun getState(): State {
            if (DBG) {
                Log.d(TAG, "Returning state = $sState")
            }
            return sState
        }

       
        fun isCorrecting(): Boolean = false

        /**
         * Logs the location and coordinates of a key press for debug analysis.
         *
         * @param key The Key object that was pressed
         * @param x The X coordinate of the touch event
         * @param y The Y coordinate of the touch event
         */
       
        fun keyPressedAt(key: Key, x: Int, y: Int) {
            if (LOGGING && sKeyLocationFile != null && key.codes!![0] >= 32) {
                val out =
                    "KEY: " + key.codes!![0].toChar() +
                        " X: " + x +
                        " Y: " + y +
                        " MX: " + (key.x + key.width / 2) +
                        " MY: " + (key.y + key.height / 2) +
                        "\n"
                try {
                    sKeyLocationFile!!.write(out.toByteArray(Charset.defaultCharset()))
                } catch (_: IOException) {
                    // TODO: May run out of space
                }
            }
        }

        private fun displayState() {
            if (DBG) {
                Log.i(TAG, "State = $sState")
            }
        }
    }
}
