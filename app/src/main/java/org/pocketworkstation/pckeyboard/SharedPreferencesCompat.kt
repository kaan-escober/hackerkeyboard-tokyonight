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

import android.content.SharedPreferences

/**
 * Compat helper to call SharedPreferences.Editor.apply(), falling back to commit()
 * if something goes wrong.
 *
 * With minSdk 21+, apply() is always available on the platform; reflection is unnecessary.
 */
object SharedPreferencesCompat {

   
    fun apply(editor: SharedPreferences.Editor) {
        try {
            editor.apply()
            return
        } catch (_: Throwable) {
            // fall through
        }
        editor.commit()
    }
}
