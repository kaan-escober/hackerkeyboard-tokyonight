/*
 * Copyright (C) 2011 Darren Salt
 *
 * Licensed under the Apache License, Version 2.0 (the "Licence"); you may
 * not use this file except in compliance with the Licence. You may obtain
 * a copy of the Licence at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * Licence for the specific language governing permissions and limitations
 * under the Licence.
 */

package org.pocketworkstation.pckeyboard

import android.content.Context
import android.util.Log
import org.json.JSONObject

open class ComposeSequence(
    protected var composeUser: ComposeSequencing
) {
   
    protected val composeBuffer = StringBuilder(10)

    init { clear() }

    fun clear() {
        composeBuffer.setLength(0)
    }

    fun hasComposePending(): Boolean = composeBuffer.isNotEmpty()

    fun bufferKey(code: Char) {
        composeBuffer.append(code)
    }

    open fun executeToString(code: Int): String? {
        var c = code
        val ks = KeyboardSwitcher.getInstance()
        val inputView = ks.getInputView()
        if (inputView != null && inputView.isShiftCaps
            && ks.isAlphabetMode()
            && Character.isLowerCase(c)
        ) {
            c = Character.toUpperCase(c)
        }
        bufferKey(c.toChar())
        composeUser.getCurrentInputEditorInfo()?.let { composeUser.updateShiftKeyState(it) }

        val seq = composeBuffer.toString()
        val composed = get(seq)
        return when {
            composed != null -> composed
            !isValid(seq) -> ""
            else -> null
        }
    }

    open fun execute(code: Int): Boolean {
        val composed = executeToString(code)
        if (composed != null) {
            clear()
            composeUser.onText(composed)
            return false
        }
        return true
    }

    open fun execute(sequence: CharSequence): Boolean {
        var result = true
        for (c in sequence) { result = execute(c.code) }
        return result
    }

    companion object {
        private const val TAG = "HK/ComposeSequence"

       
        val mMap = mutableMapOf<String, String>()

       
        val mPrefixes = mutableSetOf<String>()

        protected val UP          = LatinKeyboardView.KEYCODE_DPAD_UP.toChar()
        protected val DOWN        = LatinKeyboardView.KEYCODE_DPAD_DOWN.toChar()
        protected val LEFT        = LatinKeyboardView.KEYCODE_DPAD_LEFT.toChar()
        protected val RIGHT       = LatinKeyboardView.KEYCODE_DPAD_RIGHT.toChar()
        protected val COMPOSE     = LatinKeyboardView.KEYCODE_DPAD_CENTER.toChar()
        protected val PAGE_UP     = LatinKeyboardView.KEYCODE_PAGE_UP.toChar()
        protected val PAGE_DOWN   = LatinKeyboardView.KEYCODE_PAGE_DOWN.toChar()
        protected val ESCAPE      = LatinKeyboardView.KEYCODE_ESCAPE.toChar()
        protected val DELETE      = LatinKeyboardView.KEYCODE_FORWARD_DEL.toChar()
        protected val CAPS_LOCK   = LatinKeyboardView.KEYCODE_CAPS_LOCK.toChar()
        protected val SCROLL_LOCK = LatinKeyboardView.KEYCODE_SCROLL_LOCK.toChar()
        protected val SYSRQ       = LatinKeyboardView.KEYCODE_SYSRQ.toChar()
        protected val BREAK       = LatinKeyboardView.KEYCODE_BREAK.toChar()
        protected val HOME        = LatinKeyboardView.KEYCODE_HOME.toChar()
        protected val END         = LatinKeyboardView.KEYCODE_END.toChar()
        protected val INSERT      = LatinKeyboardView.KEYCODE_INSERT.toChar()
        protected val F1          = LatinKeyboardView.KEYCODE_FKEY_F1.toChar()
        protected val F2          = LatinKeyboardView.KEYCODE_FKEY_F2.toChar()
        protected val F3          = LatinKeyboardView.KEYCODE_FKEY_F3.toChar()
        protected val F4          = LatinKeyboardView.KEYCODE_FKEY_F4.toChar()
        protected val F5          = LatinKeyboardView.KEYCODE_FKEY_F5.toChar()
        protected val F6          = LatinKeyboardView.KEYCODE_FKEY_F6.toChar()
        protected val F7          = LatinKeyboardView.KEYCODE_FKEY_F7.toChar()
        protected val F8          = LatinKeyboardView.KEYCODE_FKEY_F8.toChar()
        protected val F9          = LatinKeyboardView.KEYCODE_FKEY_F9.toChar()
        protected val F10         = LatinKeyboardView.KEYCODE_FKEY_F10.toChar()
        protected val F11         = LatinKeyboardView.KEYCODE_FKEY_F11.toChar()
        protected val F12         = LatinKeyboardView.KEYCODE_FKEY_F12.toChar()
        protected val NUM_LOCK    = LatinKeyboardView.KEYCODE_NUM_LOCK.toChar()

        private val keyNames = mapOf(
            '"'.code to "quot",
            UP.code to "↑",
            DOWN.code to "↓",
            LEFT.code to "←",
            RIGHT.code to "→",
            COMPOSE.code to "◯",
            PAGE_UP.code to "PgUp",
            PAGE_DOWN.code to "PgDn",
            ESCAPE.code to "Esc",
            DELETE.code to "Del",
            CAPS_LOCK.code to "Caps",
            SCROLL_LOCK.code to "Scroll",
            SYSRQ.code to "SysRq",
            BREAK.code to "Break",
            HOME.code to "Home",
            END.code to "End",
            INSERT.code to "Insert",
            F1.code to "F1",
            F2.code to "F2",
            F3.code to "F3",
            F4.code to "F4",
            F5.code to "F5",
            F6.code to "F6",
            F7.code to "F7",
            F8.code to "F8",
            F9.code to "F9",
            F10.code to "F10",
            F11.code to "F11",
            F12.code to "F12",
            NUM_LOCK.code to "Num",
        )

       
        @JvmStatic
        protected fun get(key: String?): String? {
            if (key.isNullOrEmpty()) return null
            return mMap[key]
        }

        private fun isValid(partialKey: String?): Boolean {
            if (partialKey.isNullOrEmpty()) return false
            return partialKey in mPrefixes
        }

        fun loadFromAssets(context: Context) {
            if (mMap.isNotEmpty()) return
            try {
                val json = context.assets.open("compose_sequences.json")
                    .bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.getString(k)
                    if (k.isEmpty() || v.isEmpty()) continue
                    mMap[k] = v
                    for (i in 1 until k.length) mPrefixes.add(k.substring(0, i))
                }
                Log.i(TAG, "Loaded ${mMap.size} compose sequences")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load compose sequences", e)
            }
        }

    }
}
