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

import android.os.Build
import java.text.Normalizer

/**
 * Handles dead key accents and diacritical marks with Unicode normalization support.
 *
 * Extends [ComposeSequence] to provide specialized handling for combining diacritical marks
 * (dead keys) such as grave accents, acute accents, circumflex, tilde, and other spacing/non-spacing
 * variants. This class maintains a mapping of compose sequences to their normalized Unicode
 * equivalents.
 *
 * Key features:
 * - Automatic Unicode NFC normalization for composed characters on Android 2.3+
 * - Support for dead key sequences with spacing and non-spacing variants
 * - Fallback composition handling for unrecognized sequences
 * - Greek dialytika support with tone marks
 *
 * The class maintains static compose mappings initialized in a companion object, covering:
 * - Standard combining diacritical marks (U+0300–U+0314)
 * - Greek dialytika combinations with tonos
 *
 * @see ComposeSequence
 * @see java.text.Normalizer
 */
class DeadAccentSequence(user: ComposeSequencing) : ComposeSequence(user) {

    /**
     * Executes the composition of a dead key with the next character.
     *
     * Processes the next character code in the dead key composition sequence. Handles composition
     * in the following order:
     * 1. Attempts to find a pre-registered compose mapping via [executeToString]
     * 2. If mapping returns empty string (unrecognized), attempts Unicode NFC normalization
     *    by reversing the compose buffer and applying [doNormalise]
     * 3. Handles multiple combining accents by returning incomplete status
     *
     * On successful composition, clears the compose buffer and sends the composed string to the
     * UI via [composeUser]. Unrecognized or incomplete sequences return true to indicate
     * the sequence should be continued.
     *
     * @param code the character code to compose with the current dead key
     * @return true if the sequence is incomplete or unrecognized (continue composing);
     *         false if the composition was successful and output sent to UI
     */
    override fun execute(code: Int): Boolean {
        val composed = executeToString(code)
        if (composed != null) {
            var result = composed
            if (result.isEmpty()) {
                // Unrecognised - try to use the built-in Java text normalisation
                val c = composeBuffer.codePointAt(composeBuffer.length - 1)
                if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) {
                    val buildComposed = StringBuilder(10)
                    buildComposed.append(composeBuffer)
                    // FIXME? Put the combining character(s) temporarily at the end, else this won't work
                    result = doNormalise(buildComposed.reverse().toString())
                    if (result.isEmpty()) {
                        return true // incomplete :-)
                    }
                } else {
                    return true // there may be multiple combining accents
                }
            }

            clear()
            composeUser.onText(result)
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "HK/DeadAccent"

        /**
         * Registers a dead key accent mapping with spacing and ASCII variants.
         *
         * Stores multiple composition rules for a single diacritical mark:
         * - Combines the non-spacing mark with space to produce ASCII representation
         * - Combines the non-spacing mark with itself to produce spacing variant
         * - Combines the dead key placeholder with non-spacing mark to produce spacing variant
         *
         * @param nonSpacing the combining (non-spacing) diacritical mark character
         * @param spacing the spacing variant of the diacritical mark (standalone representation)
         * @param ascii optional ASCII fallback representation (uses spacing if null)
         */
        private fun putAccent(nonSpacing: String, spacing: String, ascii: String?) {
            val asciiValue = ascii ?: spacing
            put("$nonSpacing ", asciiValue)
            put(nonSpacing + nonSpacing, spacing)
            put(Keyboard.DEAD_KEY_PLACEHOLDER.toString() + nonSpacing, spacing)
        }

        /**
         * Retrieves the spacing variant for a combining diacritical mark.
         *
         * Attempts to find a registered spacing representation for a non-spacing combining mark.
         * If no explicit mapping exists, attempts Unicode normalization. Falls back to the
         * non-spacing character itself if normalization produces no result.
         *
         * @param nonSpacing the combining (non-spacing) diacritical mark character
         * @return the spacing variant representation, or the original character if no mapping exists
         */
        @JvmStatic
        fun getSpacing(nonSpacing: Char): String {
            var spacing = get("" + Keyboard.DEAD_KEY_PLACEHOLDER + nonSpacing)
            if (spacing == null) spacing = normalize(" $nonSpacing")
            return spacing ?: nonSpacing.toString()
        }

        /**
         * Applies Unicode NFC normalization to the input string.
         *
         * Uses Java's [Normalizer] for Unicode NFC (Canonical Decomposition, followed by
         * Canonical Composition) on Android 2.3 and later. Returns the input unchanged on
         * earlier Android versions.
         *
         * @param input the string to normalize
         * @return the NFC-normalized string (or the input unchanged on older Android versions)
         */
        private fun doNormalise(input: String): String {
            return if (Build.VERSION.SDK_INT >= 9) {
                Normalizer.normalize(input, Normalizer.Form.NFC)
            } else {
                input
            }
        }

        /**
         * Normalizes a string using registered compose mappings or Unicode normalization.
         *
         * Performs two-level normalization:
         * 1. Checks for an explicit registered mapping in the compose table
         * 2. Falls back to Unicode NFC normalization via [doNormalise]
         *
         * This ensures that both pre-composed dead key sequences and standard Unicode
         * composable strings are properly handled.
         *
         * @param input the string to normalize
         * @return the normalized string from the compose table, or the NFC-normalized result
         */
        @JvmStatic
        fun normalize(input: String): String {
            val lookup = mMap[input]
            return lookup ?: doNormalise(input)
        }

        init {
            // space + combining diacritical
            // cf. http://unicode.org/charts/PDF/U0300.pdf
            putAccent("\u0300", "\u02cb", "`")  // grave
            putAccent("\u0301", "\u02ca", "´")  // acute
            putAccent("\u0302", "\u02c6", "^")  // circumflex
            putAccent("\u0303", "\u02dc", "~")  // small tilde
            putAccent("\u0304", "\u02c9", "¯")  // macron
            putAccent("\u0305", "\u00af", "¯")  // overline
            putAccent("\u0306", "\u02d8", null)  // breve
            putAccent("\u0307", "\u02d9", null)  // dot above
            putAccent("\u0308", "\u00a8", "¨")  // diaeresis
            putAccent("\u0309", "\u02c0", null)  // hook above
            putAccent("\u030a", "\u02da", "°")  // ring above
            putAccent("\u030b", "\u02dd", "\"")  // double acute
            putAccent("\u030c", "\u02c7", null)  // caron
            putAccent("\u030d", "\u02c8", null)  // vertical line above
            putAccent("\u030e", "\"", "\"")  // double vertical line above
            putAccent("\u0313", "\u02bc", null)  // comma above
            putAccent("\u0314", "\u02bd", null)  // reversed comma above

            put("\u0308\u0301\u03b9", "\u0390")  // Greek Dialytika+Tonos, iota
            put("\u0301\u0308\u03b9", "\u0390")  // Greek Dialytika+Tonos, iota
            put("\u0301\u03ca", "\u0390")        // Greek Dialytika+Tonos, iota
            put("\u0308\u0301\u03c5", "\u03b0")  // Greek Dialytika+Tonos, upsilon
            put("\u0301\u0308\u03c5", "\u03b0")  // Greek Dialytika+Tonos, upsilon
            put("\u0301\u03cb", "\u03b0")        // Greek Dialytika+Tonos, upsilon
        }
    }
}
