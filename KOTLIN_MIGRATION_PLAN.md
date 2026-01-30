# Hacker's Keyboard - Kotlin Migration Plan

> **Goal:** Migrate all Java code to idiomatic Kotlin while preserving 100% functionality  
> **Estimated LOC Reduction:** 10,857 → ~5,500 (49% reduction)  
> **Estimated Time:** 3-4 weeks for complete migration  
> **New Dependencies Download:** ~0.5 MB

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Prerequisites & Setup](#prerequisites--setup)
3. [Dependency Changes](#dependency-changes)
4. [Migration Phases](#migration-phases)
   - [Phase 0: Infrastructure](#phase-0-infrastructure)
   - [Phase 1: Data Models & Utilities](#phase-1-data-models--utilities)
   - [Phase 2: Detection & Tracking](#phase-2-detection--tracking)
   - [Phase 3: Keyboard Core](#phase-3-keyboard-core)
   - [Phase 4: View Layer](#phase-4-view-layer)
   - [Phase 5: Service Layer](#phase-5-service-layer)
   - [Phase 6: Settings & Activities](#phase-6-settings--activities)
5. [Hilt Analysis (Optional)](#hilt-analysis-optional)
6. [Testing Strategy](#testing-strategy)
7. [Rollback Plan](#rollback-plan)
8. [Appendix: Pattern Transformations](#appendix-pattern-transformations)

---

## Executive Summary

### Current State
| Metric | Value |
|--------|-------|
| Total Files | 45 |
| Java Files | 35 |
| Kotlin Files | 10 (already migrated) |
| Total LOC | ~13,025 |
| Code LOC (no comments) | ~10,857 |

### Target State
| Metric | Value |
|--------|-------|
| Java Files | 0 |
| Kotlin Files | 45 |
| Estimated LOC | ~5,500 |
| Code Reduction | ~49% |

### Key Modernizations
- ✅ Handler/Message → Kotlin Coroutines
- ✅ Anonymous classes → Lambdas/SAM conversions
- ✅ Nullable checks → Kotlin null safety
- ✅ Static singletons → Kotlin objects
- ✅ Boilerplate getters/setters → Properties
- ✅ Switch statements → When expressions
- ✅ Manual state flags → Sealed classes/Data classes
- ✅ SharedPreferences → DataStore
- ❌ Compose (not using)
- ⚠️ Hilt (optional, analysis below)

---

## Prerequisites & Setup

### 1. Create Migration Branch
```bash
git checkout -b kotlin-migration
git push -u origin kotlin-migration
```

### 2. Update Kotlin Version
Current: `1.9.24` ✅ (already good)

### 3. Enable Additional Kotlin Features

Update `app/build.gradle`:

```groovy
android {
    // ... existing config ...
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
        freeCompilerArgs += [
            '-opt-in=kotlin.RequiresOptIn',
            '-Xjvm-default=all'
        ]
    }
}
```

### 4. Install IDE Plugins (if using Android Studio)
- Kotlin plugin (bundled)
- "Convert Java File to Kotlin" available via Code menu

---

## Dependency Changes

### Updated `app/build.gradle`

```groovy
plugins {
    id 'com.android.application'
    id 'kotlin-android'
}

android {
    namespace 'org.pocketworkstation.pckeyboard'
    compileSdk 34
    buildToolsVersion "34.0.4"

    defaultConfig {
        applicationId 'org.pocketworkstation.pckeyboard.modern'
        minSdk 23
        targetSdk 34
        versionCode 1041002
        versionName "v1.42.0-kotlin"
        
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
        freeCompilerArgs += [
            '-opt-in=kotlin.RequiresOptIn',
            '-Xjvm-default=all'
        ]
    }

    lint {
        checkReleaseBuilds false
        abortOnError false
    }
}

dependencies {
    // ═══════════════════════════════════════════
    // CORE ANDROID (existing)
    // ═══════════════════════════════════════════
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.preference:preference-ktx:1.2.1'  // Changed to ktx
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.coordinatorlayout:coordinatorlayout:1.2.0'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'

    // ═══════════════════════════════════════════
    // KOTLIN EXTENSIONS (NEW - mostly cached)
    // ═══════════════════════════════════════════
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.activity:activity-ktx:1.8.2'
    implementation 'androidx.fragment:fragment-ktx:1.6.2'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-service:2.7.0'

    // ═══════════════════════════════════════════
    // COROUTINES (cached)
    // ═══════════════════════════════════════════
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // ═══════════════════════════════════════════
    // DATASTORE (NEW - ~300KB download)
    // ═══════════════════════════════════════════
    implementation 'androidx.datastore:datastore-preferences:1.0.0'

    // ═══════════════════════════════════════════
    // TESTING
    // ═══════════════════════════════════════════
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:5.7.0'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:5.2.1'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
    testImplementation 'app.cash.turbine:turbine:1.0.0'  // StateFlow testing
    
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test:rules:1.5.0'
}
```

### Download Impact
| Dependency | Size | Already Cached |
|------------|------|----------------|
| core-ktx | 200 KB | ✅ Yes |
| lifecycle-runtime-ktx | 50 KB | ✅ Yes |
| coroutines-android | 300 KB | ✅ Yes |
| datastore-preferences | 300 KB | ❌ New |
| turbine (test only) | 100 KB | ❌ New |
| mockito-kotlin | 50 KB | ❌ New |
| **Total New Download** | **~500 KB** | |

---

## Migration Phases

### Phase 0: Infrastructure

**Duration:** 1 day  
**Risk:** Low  
**Files:** Build configuration only

#### Tasks

1. **Update build.gradle** with new dependencies (as above)
2. **Run `gradle assembleDebug`** to verify build works
3. **Create base Kotlin utilities file**

Create `app/src/main/java/org/pocketworkstation/pckeyboard/util/Extensions.kt`:

```kotlin
package org.pocketworkstation.pckeyboard.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.view.inputmethod.InputConnection
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// ═══════════════════════════════════════════════════════════════
// DATASTORE
// ═══════════════════════════════════════════════════════════════
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keyboard_settings")

// ═══════════════════════════════════════════════════════════════
// COROUTINE SCOPES
// ═══════════════════════════════════════════════════════════════
val KeyboardScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

// ═══════════════════════════════════════════════════════════════
// VIEW EXTENSIONS
// ═══════════════════════════════════════════════════════════════
inline fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

inline fun View.invisibleIf(condition: Boolean) {
    visibility = if (condition) View.INVISIBLE else View.VISIBLE
}

// ═══════════════════════════════════════════════════════════════
// INPUT CONNECTION EXTENSIONS
// ═══════════════════════════════════════════════════════════════
inline fun InputConnection?.withBatchEdit(block: InputConnection.() -> Unit) {
    this ?: return
    beginBatchEdit()
    try {
        block()
    } finally {
        endBatchEdit()
    }
}

// ═══════════════════════════════════════════════════════════════
// LOGGING
// ═══════════════════════════════════════════════════════════════
inline fun <reified T> T.logd(message: String) {
    Log.d(T::class.java.simpleName, message)
}

inline fun <reified T> T.logi(message: String) {
    Log.i(T::class.java.simpleName, message)
}

inline fun <reified T> T.loge(message: String, throwable: Throwable? = null) {
    Log.e(T::class.java.simpleName, message, throwable)
}

// ═══════════════════════════════════════════════════════════════
// MATH
// ═══════════════════════════════════════════════════════════════
fun Int.squared(): Int = this * this

fun squaredDistance(x1: Int, y1: Int, x2: Int, y2: Int): Int {
    val dx = x1 - x2
    val dy = y1 - y2
    return dx * dx + dy * dy
}
```

Create `app/src/main/java/org/pocketworkstation/pckeyboard/util/CoroutineUtils.kt`:

```kotlin
package org.pocketworkstation.pckeyboard.util

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Replacement for Handler.postDelayed
 */
fun CoroutineScope.postDelayed(
    delayMs: Long,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> Unit
): Job = launch(context) {
    delay(delayMs)
    block()
}

/**
 * Debounced job execution - cancels previous if still running
 */
class DebouncedRunner(private val scope: CoroutineScope) {
    private var job: Job? = null
    
    fun runDebounced(delayMs: Long, block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(delayMs)
            block()
        }
    }
    
    fun cancel() {
        job?.cancel()
        job = null
    }
}

/**
 * Repeating job - like Handler's repeating messages
 */
fun CoroutineScope.launchRepeating(
    intervalMs: Long,
    initialDelayMs: Long = 0,
    block: suspend () -> Unit
): Job = launch {
    delay(initialDelayMs)
    while (isActive) {
        block()
        delay(intervalMs)
    }
}
```

#### Verification
```bash
gradle assembleDebug
gradle test
```

---

### Phase 1: Data Models & Utilities

**Duration:** 2 days  
**Risk:** Low  
**Dependencies:** None (leaf nodes in dependency graph)

#### File Migration Order

| Order | File | Current LOC | Target LOC | Notes |
|-------|------|-------------|------------|-------|
| 1.1 | `WordComposer.java` | 209 | ~80 | Data class + extension functions |
| 1.2 | `SwipeTracker.java` | 207 | ~100 | Sealed state, data class |
| 1.3 | `EditingUtil.java` | 337 | ~120 | Extension functions, remove reflection |
| 1.4 | `LatinIMEUtil.java` | 164 | ~60 | Delete GCUtils, simplify RingBuffer |
| 1.5 | `ComposeSequence.java` | 1077 | ~400 | Data class, when expressions |
| 1.6 | `DeadAccentSequence.java` | ~150 | ~60 | Extend ComposeSequence |
| 1.7 | `GlobalKeyboardSettings.java` | 256 | ~80 | DataStore migration |

---

#### 1.1 WordComposer.java → WordComposer.kt

**Current Pattern (Java):**
```java
public class WordComposer {
    private final ArrayList<int[]> mCodes;
    private String mPreferredWord;
    private final StringBuilder mTypedWord;
    private int mCapsCount;
    private boolean mAutoCapitalized;
    private boolean mIsFirstCharCapitalized;
    
    // 15+ methods with boilerplate
}
```

**Target Pattern (Kotlin):**
```kotlin
package org.pocketworkstation.pckeyboard

/**
 * Stores the currently composing word with adjacent key codes for autocorrection.
 */
class WordComposer(
    private val codes: MutableList<IntArray> = mutableListOf(),
    private val typedWord: StringBuilder = StringBuilder(20)
) {
    var preferredWord: String? = null
        private set
    
    var capsCount: Int = 0
        private set
    
    var isAutoCapitalized: Boolean = false
    
    val isFirstCharCapitalized: Boolean
        get() = typedWord.isNotEmpty() && typedWord[0].isUpperCase()
    
    val size: Int get() = codes.size
    
    val isAllUpperCase: Boolean 
        get() = capsCount > 0 && capsCount == size
    
    val isMostlyCaps: Boolean 
        get() = capsCount > 1
    
    fun getCodesAt(index: Int): IntArray = codes[index]
    
    fun getTypedWord(): CharSequence? = typedWord.takeIf { codes.isNotEmpty() }
    
    fun getPreferredWord(): CharSequence? = preferredWord ?: getTypedWord()
    
    fun add(primaryCode: Int, keyCodes: IntArray) {
        typedWord.append(primaryCode.toChar())
        
        // Swap primary code to front if needed
        val correctedCodes = keyCodes.correctPrimaryPosition(primaryCode).lowercased()
        codes.add(correctedCodes)
        
        if (primaryCode.toChar().isUpperCase()) capsCount++
    }
    
    fun deleteLast() {
        if (codes.isEmpty()) return
        
        codes.removeAt(codes.lastIndex)
        val lastChar = typedWord.last()
        typedWord.deleteAt(typedWord.lastIndex)
        
        if (lastChar.isUpperCase()) capsCount--
    }
    
    fun setPreferredWord(word: String?) {
        preferredWord = word
    }
    
    fun reset() {
        codes.clear()
        typedWord.clear()
        preferredWord = null
        capsCount = 0
        isAutoCapitalized = false
    }
    
    // Create a copy
    fun copy(): WordComposer = WordComposer(
        codes = codes.map { it.copyOf() }.toMutableList(),
        typedWord = StringBuilder(typedWord)
    ).also {
        it.preferredWord = preferredWord
        it.capsCount = capsCount
        it.isAutoCapitalized = isAutoCapitalized
    }
    
    private fun IntArray.correctPrimaryPosition(primary: Int): IntArray {
        if (size >= 2 && this[0] != primary && this[1] == primary) {
            this[1] = this[0]
            this[0] = primary
        }
        return this
    }
    
    private fun IntArray.lowercased(): IntArray {
        for (i in indices) {
            if (this[i] > 0) this[i] = this[i].toChar().lowercaseChar().code
        }
        return this
    }
}
```

**LOC Reduction:** 209 → 85 (59%)

---

#### 1.2 SwipeTracker.java → SwipeTracker.kt

**Target Pattern:**
```kotlin
package org.pocketworkstation.pckeyboard

import android.view.MotionEvent
import kotlin.math.max
import kotlin.math.min

/**
 * Tracks finger swipe motion using a ring buffer of recent touch events.
 */
class SwipeTracker {
    
    private val buffer = EventRingBuffer(NUM_PAST)
    
    var xVelocity: Float = 0f
        private set
    var yVelocity: Float = 0f
        private set
    
    fun addMovement(ev: MotionEvent) {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            buffer.clear()
            return
        }
        
        val time = ev.eventTime
        // Add historical points
        repeat(ev.historySize) { i ->
            addPoint(ev.getHistoricalX(i), ev.getHistoricalY(i), ev.getHistoricalEventTime(i))
        }
        addPoint(ev.x, ev.y, time)
    }
    
    private fun addPoint(x: Float, y: Float, time: Long) {
        // Remove old events outside time window
        while (buffer.size > 0 && buffer.getTime(0) < time - LONGEST_PAST_TIME) {
            buffer.dropOldest()
        }
        buffer.add(x, y, time)
    }
    
    fun computeCurrentVelocity(units: Int, maxVelocity: Float = Float.MAX_VALUE) {
        if (buffer.size < 2) {
            xVelocity = 0f
            yVelocity = 0f
            return
        }
        
        val oldestX = buffer.getX(0)
        val oldestY = buffer.getY(0)
        val oldestTime = buffer.getTime(0)
        
        var accumX = 0f
        var accumY = 0f
        
        for (pos in 1 until buffer.size) {
            val dur = (buffer.getTime(pos) - oldestTime).toInt()
            if (dur == 0) continue
            
            val velX = (buffer.getX(pos) - oldestX) / dur * units
            val velY = (buffer.getY(pos) - oldestY) / dur * units
            
            accumX = if (accumX == 0f) velX else (accumX + velX) * 0.5f
            accumY = if (accumY == 0f) velY else (accumY + velY) * 0.5f
        }
        
        xVelocity = accumX.coerceIn(-maxVelocity, maxVelocity)
        yVelocity = accumY.coerceIn(-maxVelocity, maxVelocity)
    }
    
    private class EventRingBuffer(private val maxSize: Int) {
        private val xBuf = FloatArray(maxSize)
        private val yBuf = FloatArray(maxSize)
        private val timeBuf = LongArray(maxSize)
        private var head = 0
        private var tail = 0
        var size = 0
            private set
        
        fun clear() {
            head = 0
            tail = 0
            size = 0
        }
        
        fun add(x: Float, y: Float, time: Long) {
            xBuf[head] = x
            yBuf[head] = y
            timeBuf[head] = time
            head = (head + 1) % maxSize
            
            if (size < maxSize) {
                size++
            } else {
                tail = (tail + 1) % maxSize
            }
        }
        
        fun dropOldest() {
            if (size > 0) {
                size--
                tail = (tail + 1) % maxSize
            }
        }
        
        private fun index(pos: Int) = (tail + pos) % maxSize
        
        fun getX(pos: Int) = xBuf[index(pos)]
        fun getY(pos: Int) = yBuf[index(pos)]
        fun getTime(pos: Int) = timeBuf[index(pos)]
    }
    
    companion object {
        private const val NUM_PAST = 4
        private const val LONGEST_PAST_TIME = 200L
    }
}
```

**LOC Reduction:** 207 → 95 (54%)

---

#### 1.3 EditingUtil.java → EditingUtil.kt

**Key Changes:**
- Remove reflection (minSdk 23 has all APIs)
- Convert to extension functions
- Use Kotlin null safety

```kotlin
package org.pocketworkstation.pckeyboard

import android.text.TextUtils
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

/**
 * Extension functions for InputConnection text editing.
 */

data class WordRange(
    val charsBefore: Int,
    val charsAfter: Int,
    val word: String
)

data class SelectedWord(
    val start: Int,
    val end: Int,
    val word: CharSequence
)

private const val LOOKBACK_CHARACTER_NUM = 15
private val SPACE_REGEX = "\\s+".toRegex()

/**
 * Appends text to the input field, adding a space if needed.
 */
fun InputConnection.appendText(newText: String) {
    finishComposingText()
    
    val prefix = getTextBeforeCursor(1, 0)?.let { 
        if (it.isNotEmpty() && it != " ") " " else "" 
    } ?: ""
    
    setComposingText(prefix + newText, 1)
}

/**
 * Gets the cursor position in the text field.
 */
fun InputConnection.getCursorPosition(): Int {
    val extracted = getExtractedText(ExtractedTextRequest(), 0) ?: return -1
    return extracted.startOffset + extracted.selectionStart
}

/**
 * Gets the word at the cursor position.
 */
fun InputConnection.getWordAtCursor(separators: String): WordRange? {
    val before = getTextBeforeCursor(1000, 0) ?: return null
    val after = getTextAfterCursor(1000, 0) ?: return null
    
    // Find word boundaries
    var start = before.length
    while (start > 0 && !separators.contains(before[start - 1])) start--
    
    var end = 0
    while (end < after.length && !separators.contains(after[end])) end++
    
    val word = before.substring(start) + after.substring(0, end)
    
    return WordRange(
        charsBefore = before.length - start,
        charsAfter = end,
        word = word
    )
}

/**
 * Deletes the word at the cursor.
 */
fun InputConnection.deleteWordAtCursor(separators: String) {
    val range = getWordAtCursor(separators) ?: return
    
    finishComposingText()
    val newCursor = getCursorPosition() - range.charsBefore
    setSelection(newCursor, newCursor)
    deleteSurroundingText(0, range.charsBefore + range.charsAfter)
}

/**
 * Gets the previous word before cursor.
 */
fun InputConnection.getPreviousWord(separators: String): CharSequence? {
    val prev = getTextBeforeCursor(LOOKBACK_CHARACTER_NUM, 0) ?: return null
    val words = prev.split(SPACE_REGEX)
    
    if (words.size >= 2) {
        val word = words[words.size - 2]
        if (word.isNotEmpty() && !separators.contains(word.last())) {
            return word
        }
    }
    return null
}

/**
 * Gets the selected word or word at cursor.
 */
fun InputConnection.getWordAtCursorOrSelection(
    selStart: Int,
    selEnd: Int,
    separators: String
): SelectedWord? {
    if (selStart == selEnd) {
        // Just a cursor
        val range = getWordAtCursor(separators) ?: return null
        if (range.word.isEmpty()) return null
        
        return SelectedWord(
            start = selStart - range.charsBefore,
            end = selEnd + range.charsAfter,
            word = range.word
        )
    }
    
    // Check boundaries
    val charBefore = getTextBeforeCursor(1, 0)
    if (!charBefore.isNullOrEmpty() && !separators.contains(charBefore)) return null
    
    val charAfter = getTextAfterCursor(1, 0)
    if (!charAfter.isNullOrEmpty() && !separators.contains(charAfter)) return null
    
    // Get selection
    val selection = getSelectedText(0) ?: return null
    if (selection.isEmpty()) return null
    
    // Check if selection contains separators
    if (selection.any { separators.contains(it) }) return null
    
    return SelectedWord(start = selStart, end = selEnd, word = selection)
}

/**
 * Sets the selected word into composition mode for underlining.
 */
fun InputConnection.underlineWord(word: SelectedWord) {
    setComposingRegion(word.start, word.end)
}
```

**LOC Reduction:** 337 → 110 (67%)  
**Key Improvement:** Removed all reflection code - direct API calls since minSdk 23

---

#### 1.4 LatinIMEUtil.java → LatinIMEUtil.kt

**Key Changes:**
- DELETE `GCUtils` entirely (obsolete)
- Simplify `RingCharBuffer`

```kotlin
package org.pocketworkstation.pckeyboard

import android.content.Context
import android.os.AsyncTask

object LatinIMEUtil {
    
    /**
     * Cancels an AsyncTask safely.
     */
    fun cancelTask(task: AsyncTask<*, *, *>?, mayInterruptIfRunning: Boolean) {
        if (task != null && task.status != AsyncTask.Status.FINISHED) {
            task.cancel(mayInterruptIfRunning)
        }
    }
}

/**
 * Ring buffer for tracking recently typed characters.
 */
class RingCharBuffer private constructor() {
    
    private var context: Context? = null
    private var enabled = false
    private var end = 0
    var length = 0
        private set
    
    private val charBuf = CharArray(BUFSIZE)
    private val xBuf = IntArray(BUFSIZE)
    private val yBuf = IntArray(BUFSIZE)
    
    private fun normalize(value: Int): Int {
        val result = value % BUFSIZE
        return if (result < 0) result + BUFSIZE else result
    }
    
    fun push(c: Char, x: Int, y: Int) {
        if (!enabled) return
        
        charBuf[end] = c
        xBuf[end] = x
        yBuf[end] = y
        end = normalize(end + 1)
        if (length < BUFSIZE) length++
    }
    
    fun pop(): Char {
        if (length < 1) return PLACEHOLDER
        
        end = normalize(end - 1)
        length--
        return charBuf[end]
    }
    
    fun getLastChar(): Char = 
        if (length < 1) PLACEHOLDER else charBuf[normalize(end - 1)]
    
    fun getLastString(): String {
        val ime = context as? LatinIME ?: return ""
        
        return buildString {
            for (i in 0 until length) {
                val c = charBuf[normalize(end - 1 - i)]
                if (!ime.isWordSeparator(c.code)) {
                    append(c)
                } else {
                    break
                }
            }
        }.reversed()
    }
    
    fun reset() {
        length = 0
    }
    
    companion object {
        private const val PLACEHOLDER = '\uFFFC'
        private const val INVALID_COORDINATE = -2
        const val BUFSIZE = 20
        
        private val instance = RingCharBuffer()
        
        @JvmStatic
        fun getInstance(): RingCharBuffer = instance
        
        @JvmStatic
        fun init(context: Context, enabled: Boolean): RingCharBuffer {
            instance.context = context
            instance.enabled = enabled
            return instance
        }
    }
}
```

**LOC Reduction:** 164 → 70 (57%)  
**Key Improvement:** Deleted ~50 LOC of useless GCUtils

---

#### 1.5 - 1.6 ComposeSequence Files

Migrate `ComposeSequence.java` and `DeadAccentSequence.java`:
- Convert Map initialization to Kotlin maps
- Use `when` expressions
- Sealed class for compose states

*(Detailed implementation similar to above patterns)*

---

#### 1.7 GlobalKeyboardSettings.java → KeyboardSettings.kt

**Major Refactor:** Replace with DataStore

```kotlin
package org.pocketworkstation.pckeyboard

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.pocketworkstation.pckeyboard.util.dataStore
import java.util.Locale

/**
 * Type-safe keyboard settings backed by DataStore.
 */
class KeyboardSettings(private val context: Context) {
    
    private val dataStore = context.dataStore
    
    // ═══════════════════════════════════════════════════════════════
    // PREFERENCE KEYS
    // ═══════════════════════════════════════════════════════════════
    
    private object Keys {
        val POPUP_FLAGS = intPreferencesKey("popup_keyboard_flags")
        val TOP_ROW_SCALE = floatPreferencesKey("top_row_scale")
        val SHOW_TOUCH_POS = booleanPreferencesKey("show_touch_pos")
        val KEYBOARD_MODE_PORTRAIT = intPreferencesKey("keyboard_mode_portrait")
        val KEYBOARD_MODE_LANDSCAPE = intPreferencesKey("keyboard_mode_landscape")
        val CTRL_A_OVERRIDE = intPreferencesKey("ctrl_a_override")
        val CHORDING_CTRL_KEY = intPreferencesKey("chording_ctrl_key")
        val CHORDING_ALT_KEY = intPreferencesKey("chording_alt_key")
        val CHORDING_META_KEY = intPreferencesKey("chording_meta_key")
        val KEY_CLICK_VOLUME = floatPreferencesKey("key_click_volume")
        val KEY_CLICK_METHOD = intPreferencesKey("key_click_method")
        val CAPS_LOCK = booleanPreferencesKey("caps_lock")
        val SHIFT_LOCK_MODIFIERS = booleanPreferencesKey("shift_lock_modifiers")
        val LABEL_SCALE = floatPreferencesKey("label_scale")
        val LONGPRESS_TIMEOUT = intPreferencesKey("longpress_timeout")
        val HINT_MODE = intPreferencesKey("hint_mode")
        val RENDER_MODE = intPreferencesKey("render_mode")
        val KEYBOARD_HEIGHT_PERCENT = floatPreferencesKey("keyboard_height_percent")
        val SLIDE_KEYS = intPreferencesKey("slide_keys")
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CACHED VALUES (for synchronous access in View code)
    // ═══════════════════════════════════════════════════════════════
    
    @Volatile var popupKeyboardFlags: Int = 0x1
    @Volatile var topRowScale: Float = 1.0f
    @Volatile var showTouchPos: Boolean = false
    @Volatile var keyboardModePortrait: Int = 0
    @Volatile var keyboardModeLandscape: Int = 2
    @Volatile var ctrlAOverride: Int = 0
    @Volatile var chordingCtrlKey: Int = 0
    @Volatile var chordingAltKey: Int = 0
    @Volatile var chordingMetaKey: Int = 0
    @Volatile var keyClickVolume: Float = 0.0f
    @Volatile var keyClickMethod: Int = 0
    @Volatile var capsLock: Boolean = true
    @Volatile var shiftLockModifiers: Boolean = false
    @Volatile var labelScalePref: Float = 1.0f
    @Volatile var longpressTimeout: Int = 400
    @Volatile var hintMode: Int = 0
    @Volatile var renderMode: Int = 1
    @Volatile var keyboardHeightPercent: Float = 40.0f
    @Volatile var sendSlideKeys: Int = 0
    
    // Runtime state (not persisted)
    var keyboardMode: Int = 0
    var useExtension: Boolean = false
    var inputLocale: Locale = Locale.getDefault()
    var editorPackageName: String? = null
    var editorFieldName: String? = null
    var editorFieldId: Int = 0
    var editorInputType: Int = 0
    
    // ═══════════════════════════════════════════════════════════════
    // FLOWS (for reactive observation)
    // ═══════════════════════════════════════════════════════════════
    
    val popupFlagsFlow: Flow<Int> = dataStore.data.map { it[Keys.POPUP_FLAGS] ?: 0x1 }
    val capsLockFlow: Flow<Boolean> = dataStore.data.map { it[Keys.CAPS_LOCK] ?: true }
    val longpressTimeoutFlow: Flow<Int> = dataStore.data.map { it[Keys.LONGPRESS_TIMEOUT] ?: 400 }
    
    // ═══════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════
    
    suspend fun loadFromDataStore() {
        dataStore.data.collect { prefs ->
            popupKeyboardFlags = prefs[Keys.POPUP_FLAGS] ?: 0x1
            topRowScale = prefs[Keys.TOP_ROW_SCALE] ?: 1.0f
            showTouchPos = prefs[Keys.SHOW_TOUCH_POS] ?: false
            keyboardModePortrait = prefs[Keys.KEYBOARD_MODE_PORTRAIT] ?: 0
            keyboardModeLandscape = prefs[Keys.KEYBOARD_MODE_LANDSCAPE] ?: 2
            ctrlAOverride = prefs[Keys.CTRL_A_OVERRIDE] ?: 0
            chordingCtrlKey = prefs[Keys.CHORDING_CTRL_KEY] ?: 0
            chordingAltKey = prefs[Keys.CHORDING_ALT_KEY] ?: 0
            chordingMetaKey = prefs[Keys.CHORDING_META_KEY] ?: 0
            keyClickVolume = prefs[Keys.KEY_CLICK_VOLUME] ?: 0.0f
            keyClickMethod = prefs[Keys.KEY_CLICK_METHOD] ?: 0
            capsLock = prefs[Keys.CAPS_LOCK] ?: true
            shiftLockModifiers = prefs[Keys.SHIFT_LOCK_MODIFIERS] ?: false
            labelScalePref = prefs[Keys.LABEL_SCALE] ?: 1.0f
            longpressTimeout = prefs[Keys.LONGPRESS_TIMEOUT] ?: 400
            hintMode = prefs[Keys.HINT_MODE] ?: 0
            renderMode = prefs[Keys.RENDER_MODE] ?: 1
            keyboardHeightPercent = prefs[Keys.KEYBOARD_HEIGHT_PERCENT] ?: 40.0f
            sendSlideKeys = prefs[Keys.SLIDE_KEYS] ?: 0
        }
    }
    
    suspend fun updateCapsLock(enabled: Boolean) {
        dataStore.edit { it[Keys.CAPS_LOCK] = enabled }
    }
    
    suspend fun updateLongpressTimeout(timeout: Int) {
        dataStore.edit { it[Keys.LONGPRESS_TIMEOUT] = timeout }
    }
    
    // Add more update functions as needed...
    
    companion object {
        // Migration from SharedPreferences (one-time)
        suspend fun migrateFromSharedPreferences(
            context: Context,
            prefs: android.content.SharedPreferences
        ) {
            context.dataStore.edit { dataStorePrefs ->
                // Migrate each preference
                prefs.all.forEach { (key, value) ->
                    when (key) {
                        "pref_caps_lock" -> dataStorePrefs[Keys.CAPS_LOCK] = value as? Boolean ?: true
                        "pref_long_press_duration" -> {
                            val intVal = (value as? String)?.toIntOrNull() ?: 400
                            dataStorePrefs[Keys.LONGPRESS_TIMEOUT] = intVal
                        }
                        // ... migrate other prefs
                    }
                }
            }
        }
    }
}
```

**LOC Reduction:** 256 → 150 (41%)  
**Key Improvement:** Type-safe, reactive, no more reflection-like patterns

---

### Phase 2: Detection & Tracking

**Duration:** 2 days  
**Risk:** Medium  
**Dependencies:** Phase 1 must be complete

#### File Migration Order

| Order | File | Current LOC | Target LOC |
|-------|------|-------------|------------|
| 2.1 | `KeyDetector.java` | 115 | ~50 |
| 2.2 | `ProximityKeyDetector.java` | 140 | ~60 |
| 2.3 | `PointerTracker.java` | 498 | ~250 |

---

#### 2.1 KeyDetector.java → KeyDetector.kt

```kotlin
package org.pocketworkstation.pckeyboard

import org.pocketworkstation.pckeyboard.Keyboard.Key

/**
 * Base class for touch-to-key detection strategies.
 */
abstract class KeyDetector {
    
    protected var keyboard: Keyboard? = null
    protected var keys: Array<Key> = emptyArray()
    protected var correctionX: Int = 0
    protected var correctionY: Int = 0
    protected var proximityCorrectOn: Boolean = false
    protected var proximityThresholdSquare: Int = 0
    
    fun setKeyboard(keyboard: Keyboard, correctionX: Float, correctionY: Float): Array<Key> {
        this.keyboard = keyboard
        this.correctionX = correctionX.toInt()
        this.correctionY = correctionY.toInt()
        this.keys = keyboard.keys.toTypedArray()
        return keys
    }
    
    fun getTouchX(x: Int): Int = x + correctionX
    fun getTouchY(y: Int): Int = y + correctionY
    
    fun getKeys(): Array<Key> {
        check(keys.isNotEmpty()) { "Keyboard not set" }
        return keys
    }
    
    fun setProximityCorrectionEnabled(enabled: Boolean) {
        proximityCorrectOn = enabled
    }
    
    fun isProximityCorrectionEnabled(): Boolean = proximityCorrectOn
    
    fun setProximityThreshold(threshold: Int) {
        proximityThresholdSquare = threshold * threshold
    }
    
    fun newCodeArray(): IntArray = IntArray(getMaxNearbyKeys()) { LatinKeyboardBaseView.NOT_A_KEY }
    
    abstract fun getMaxNearbyKeys(): Int
    
    abstract fun getKeyIndexAndNearbyCodes(x: Int, y: Int, allKeys: IntArray?): Int
}
```

**LOC Reduction:** 115 → 45 (61%)

---

#### 2.2 ProximityKeyDetector.java → ProximityKeyDetector.kt

```kotlin
package org.pocketworkstation.pckeyboard

/**
 * Key detector with proximity-based nearby key detection for the main keyboard.
 */
class ProximityKeyDetector : KeyDetector() {
    
    private val distances = IntArray(MAX_NEARBY_KEYS)
    
    override fun getMaxNearbyKeys(): Int = MAX_NEARBY_KEYS
    
    override fun getKeyIndexAndNearbyCodes(x: Int, y: Int, allKeys: IntArray?): Int {
        val keys = getKeys()
        val touchX = getTouchX(x)
        val touchY = getTouchY(y)
        
        var primaryIndex = LatinKeyboardBaseView.NOT_A_KEY
        var closestKey = LatinKeyboardBaseView.NOT_A_KEY
        var closestKeyDist = proximityThresholdSquare + 1
        
        distances.fill(Int.MAX_VALUE)
        
        val nearestKeyIndices = keyboard?.getNearestKeys(touchX, touchY) ?: return primaryIndex
        
        for (i in nearestKeyIndices.indices) {
            val keyIndex = nearestKeyIndices[i]
            val key = keys[keyIndex]
            val isInside = key.isInside(touchX, touchY)
            
            if (isInside) {
                primaryIndex = keyIndex
            }
            
            val dist = key.squaredDistanceFrom(touchX, touchY)
            val isNearby = proximityCorrectOn && dist < proximityThresholdSquare
            val isPrintable = key.codes.isNotEmpty() && key.codes[0] > 32
            
            if ((isNearby || isInside) && isPrintable) {
                if (dist < closestKeyDist) {
                    closestKeyDist = dist
                    closestKey = keyIndex
                }
                
                allKeys?.let { insertByDistance(it, key.codes, dist) }
            }
        }
        
        return if (primaryIndex == LatinKeyboardBaseView.NOT_A_KEY) closestKey else primaryIndex
    }
    
    private fun insertByDistance(allKeys: IntArray, codes: IntArray, dist: Int) {
        for (j in distances.indices) {
            if (distances[j] > dist) {
                // Shift existing entries
                val nCodes = codes.size
                System.arraycopy(distances, j, distances, j + nCodes, distances.size - j - nCodes)
                System.arraycopy(allKeys, j, allKeys, j + nCodes, allKeys.size - j - nCodes)
                
                // Insert new codes
                System.arraycopy(codes, 0, allKeys, j, nCodes)
                distances.fill(dist, j, j + nCodes)
                break
            }
        }
    }
    
    companion object {
        private const val MAX_NEARBY_KEYS = 12
    }
}
```

**LOC Reduction:** 140 → 65 (54%)

---

#### 2.3 PointerTracker.java → PointerTracker.kt

**Major refactoring with sealed classes:**

```kotlin
package org.pocketworkstation.pckeyboard

import android.content.res.Resources
import kotlinx.coroutines.*
import org.pocketworkstation.pckeyboard.Keyboard.Key
import org.pocketworkstation.pckeyboard.LatinKeyboardBaseView.OnKeyboardActionListener
import org.pocketworkstation.pckeyboard.LatinKeyboardBaseView.UIHandler
import org.pocketworkstation.pckeyboard.util.KeyboardScope

/**
 * Tracks a single pointer (finger) on the keyboard.
 */
class PointerTracker(
    val pointerId: Int,
    private val handler: UIHandler,
    private val keyDetector: KeyDetector,
    private val proxy: UIProxy,
    resources: Resources,
    slideKeyHack: Boolean
) {
    
    interface UIProxy {
        fun invalidateKey(key: Key?)
        fun showPreview(keyIndex: Int, tracker: PointerTracker)
        fun hasDistinctMultitouch(): Boolean
    }
    
    // ═══════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════
    
    private data class KeyState(
        val startX: Int = 0,
        val startY: Int = 0,
        val downTime: Long = 0,
        val keyIndex: Int = NOT_A_KEY,
        val keyX: Int = 0,
        val keyY: Int = 0,
        val lastX: Int = 0,
        val lastY: Int = 0
    )
    
    private var state = KeyState()
    private var keys: Array<Key> = emptyArray()
    private var keyHysteresisDistanceSquared = -1
    
    private var keyboardLayoutChanged = false
    private var keyAlreadyProcessed = false
    private var isRepeatableKey = false
    var isInSlidingKeyInput = false
        private set
    
    // Multi-tap state
    private var lastSentIndex = NOT_A_KEY
    private var tapCount = 0
    private var lastTapTime = -1L
    private var inMultiTap = false
    private val previewLabel = StringBuilder(1)
    
    private var previousKey = NOT_A_KEY
    private var listener: OnKeyboardActionListener? = null
    
    private val keyboardSwitcher = KeyboardSwitcher.getInstance()
    private val hasDistinctMultitouch = proxy.hasDistinctMultitouch()
    private val delayBeforeKeyRepeatStart = resources.getInteger(R.integer.config_delay_before_key_repeat_start)
    private val multiTapKeyTimeout = resources.getInteger(R.integer.config_multi_tap_key_timeout)
    
    init {
        Companion.slideKeyHack = slideKeyHack
        resetMultiTap()
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════
    
    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener) {
        this.listener = listener
    }
    
    fun setKeyboard(keys: Array<Key>, keyHysteresisDistance: Float) {
        require(keys.isNotEmpty() && keyHysteresisDistance >= 0)
        this.keys = keys
        this.keyHysteresisDistanceSquared = (keyHysteresisDistance * keyHysteresisDistance).toInt()
        keyboardLayoutChanged = true
    }
    
    fun getKey(keyIndex: Int): Key? = 
        if (keyIndex in keys.indices) keys[keyIndex] else null
    
    fun isModifier(): Boolean = isModifierInternal(state.keyIndex)
    
    fun isOnModifierKey(x: Int, y: Int): Boolean = 
        isModifierInternal(keyDetector.getKeyIndexAndNearbyCodes(x, y, null))
    
    fun isSpaceKey(keyIndex: Int): Boolean =
        getKey(keyIndex)?.codes?.getOrNull(0) == LatinIME.ASCII_SPACE
    
    // ═══════════════════════════════════════════════════════════════
    // TOUCH EVENTS
    // ═══════════════════════════════════════════════════════════════
    
    fun onDownEvent(x: Int, y: Int, eventTime: Long) {
        val keyIndex = keyDetector.getKeyIndexAndNearbyCodes(x, y, null)
        
        state = KeyState(
            startX = x,
            startY = y,
            downTime = eventTime,
            keyIndex = keyIndex,
            keyX = x,
            keyY = y,
            lastX = x,
            lastY = y
        )
        
        keyAlreadyProcessed = false
        isRepeatableKey = false
        isInSlidingKeyInput = false
        keyboardLayoutChanged = false
        
        checkMultiTap(eventTime, keyIndex)
        
        getKey(keyIndex)?.let { key ->
            if (key.repeatable) {
                isRepeatableKey = true
                handler.startKeyRepeatTimer(delayBeforeKeyRepeatStart.toLong(), keyIndex, this)
            }
            
            if (!key.isModifier()) {
                handler.startLongPressTimer(getLongPressTimeout(), keyIndex, this)
            }
        }
        
        showKeyPreviewAndUpdateKey(keyIndex)
        listener?.onPress(getKey(keyIndex)?.getPrimaryCode() ?: 0)
    }
    
    fun onMoveEvent(x: Int, y: Int, eventTime: Long, isOldPointer: Boolean) {
        if (keyAlreadyProcessed) return
        
        val keyIndex = keyDetector.getKeyIndexAndNearbyCodes(x, y, null)
        val oldKey = getKey(state.keyIndex)
        val newKey = getKey(keyIndex)
        
        state = state.copy(lastX = x, lastY = y)
        
        if (oldKey == newKey) {
            // Still on same key, just update preview
            showKeyPreviewAndUpdateKey(keyIndex)
            return
        }
        
        // Key changed
        if (oldKey != null) {
            // Leaving the previous key
            if (isMinorMoveBounce(x, y, keyIndex)) {
                // Small movement, ignore
                showKeyPreviewAndUpdateKey(state.keyIndex)
                return
            }
            
            isInSlidingKeyInput = true
            listener?.onRelease(oldKey.getPrimaryCode())
            resetMultiTap()
        }
        
        handler.cancelLongPressTimer()
        
        if (newKey != null) {
            state = state.copy(keyIndex = keyIndex, keyX = x, keyY = y)
            handler.startLongPressTimer(getLongPressTimeout(), keyIndex, this)
        }
        
        showKeyPreviewAndUpdateKey(keyIndex)
    }
    
    fun onUpEvent(x: Int, y: Int, eventTime: Long) {
        handler.cancelKeyTimers()
        handler.cancelPopupPreview()
        showKeyPreviewAndUpdateKey(NOT_A_KEY)
        isInSlidingKeyInput = false
        sendSlideKeys()
        
        if (keyAlreadyProcessed) return
        
        var keyIndex = keyDetector.getKeyIndexAndNearbyCodes(x, y, null)
        
        if (isMinorMoveBounce(x, y, keyIndex)) {
            keyIndex = state.keyIndex
        }
        
        if (!isRepeatableKey) {
            detectAndSendKey(keyIndex, state.keyX, state.keyY, eventTime)
        }
        
        getKey(keyIndex)?.let { proxy.invalidateKey(it) }
    }
    
    fun onCancelEvent(x: Int, y: Int, eventTime: Long) {
        handler.cancelKeyTimers()
        handler.cancelPopupPreview()
        showKeyPreviewAndUpdateKey(NOT_A_KEY)
        isInSlidingKeyInput = false
        
        getKey(state.keyIndex)?.let { proxy.invalidateKey(it) }
    }
    
    fun repeatKey(keyIndex: Int) {
        getKey(keyIndex)?.let { key ->
            detectAndSendKey(keyIndex, key.x, key.y, -1)
        }
    }
    
    fun setAlreadyProcessed() {
        keyAlreadyProcessed = true
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════
    
    private fun getLongPressTimeout(): Long {
        return if (keyboardSwitcher.isInMomentaryAutoModeSwitchState()) {
            LatinIME.sKeyboardSettings.longpressTimeout * 3L
        } else {
            LatinIME.sKeyboardSettings.longpressTimeout.toLong()
        }
    }
    
    private fun isModifierInternal(keyIndex: Int): Boolean {
        val key = getKey(keyIndex) ?: return false
        val code = key.codes.getOrNull(0) ?: return false
        
        return code in listOf(
            Keyboard.KEYCODE_SHIFT,
            Keyboard.KEYCODE_MODE_CHANGE,
            LatinKeyboardView.KEYCODE_CTRL_LEFT,
            LatinKeyboardView.KEYCODE_ALT_LEFT,
            LatinKeyboardView.KEYCODE_META_LEFT,
            LatinKeyboardView.KEYCODE_FN
        )
    }
    
    private fun isMinorMoveBounce(x: Int, y: Int, newKey: Int): Boolean {
        check(keys.isNotEmpty() && keyHysteresisDistanceSquared >= 0) { "Keyboard/hysteresis not set" }
        
        val curKey = state.keyIndex
        if (newKey == curKey) return true
        
        if (curKey in keys.indices) {
            return getSquareDistanceToKeyEdge(x, y, keys[curKey]) < keyHysteresisDistanceSquared
        }
        return false
    }
    
    private fun getSquareDistanceToKeyEdge(x: Int, y: Int, key: Key): Int {
        val edgeX = x.coerceIn(key.x, key.x + key.width)
        val edgeY = y.coerceIn(key.y, key.y + key.height)
        val dx = x - edgeX
        val dy = y - edgeY
        return dx * dx + dy * dy
    }
    
    private fun showKeyPreviewAndUpdateKey(keyIndex: Int) {
        updateKey(keyIndex)
        
        // Don't show preview for modifiers on multitouch devices
        if (hasDistinctMultitouch && isModifier()) {
            proxy.showPreview(NOT_A_KEY, this)
        } else {
            proxy.showPreview(keyIndex, this)
        }
    }
    
    private fun updateKey(keyIndex: Int) {
        if (keyAlreadyProcessed) return
        
        val oldKeyIndex = previousKey
        previousKey = keyIndex
        
        if (keyIndex != oldKeyIndex) {
            getKey(oldKeyIndex)?.let { proxy.invalidateKey(it) }
            getKey(keyIndex)?.let { proxy.invalidateKey(it) }
        }
    }
    
    private fun detectAndSendKey(index: Int, x: Int, y: Int, eventTime: Long) {
        val key = getKey(index)
        val listener = this.listener
        
        if (key == null) {
            listener?.onCancel()
            return
        }
        
        key.text?.let { text ->
            listener?.onText(text)
            listener?.onRelease(0)
            lastSentIndex = index
            lastTapTime = eventTime
            return
        }
        
        val codes = key.codes
        if (codes.isEmpty()) return
        
        var code = key.getPrimaryCode()
        val keyCodes = keyDetector.newCodeArray()
        keyDetector.getKeyIndexAndNearbyCodes(x, y, keyCodes)
        
        // Multi-tap handling
        if (inMultiTap) {
            if (tapCount != -1) {
                listener?.onKey(Keyboard.KEYCODE_DELETE, KEY_DELETE, x, y)
            } else {
                tapCount = 0
            }
            code = codes[tapCount]
        }
        
        // Swap codes if needed for key debouncing
        if (keyCodes.size >= 2 && keyCodes[0] != code && keyCodes[1] == code) {
            keyCodes[1] = keyCodes[0]
            keyCodes[0] = code
        }
        
        listener?.onKey(code, keyCodes, x, y)
        listener?.onRelease(code)
        
        lastSentIndex = index
        lastTapTime = eventTime
    }
    
    private fun checkMultiTap(eventTime: Long, keyIndex: Int) {
        val key = getKey(keyIndex) ?: return
        if (key.codes.isEmpty()) return
        
        val isMultiTap = eventTime < lastTapTime + multiTapKeyTimeout && keyIndex == lastSentIndex
        
        if (key.codes.size > 1) {
            inMultiTap = true
            tapCount = if (isMultiTap) (tapCount + 1) % key.codes.size else -1
            return
        }
        
        if (!isMultiTap) {
            resetMultiTap()
        }
    }
    
    private fun resetMultiTap() {
        lastSentIndex = NOT_A_KEY
        tapCount = 0
        lastTapTime = -1
        inMultiTap = false
    }
    
    fun getPreviewText(key: Key): CharSequence {
        return if (inMultiTap) {
            previewLabel.clear()
            previewLabel.append(key.codes[tapCount.coerceAtLeast(0)].toChar())
            previewLabel
        } else if (key.isDeadKey()) {
            DeadAccentSequence.normalize(" " + key.label)
        } else {
            key.label ?: ""
        }
    }
    
    // Getters for state
    fun getLastX(): Int = state.lastX
    fun getLastY(): Int = state.lastY
    fun getDownTime(): Long = state.downTime
    fun getStartX(): Int = state.startX
    fun getStartY(): Int = state.startY
    
    companion object {
        private const val NOT_A_KEY = LatinKeyboardBaseView.NOT_A_KEY
        private val KEY_DELETE = intArrayOf(Keyboard.KEYCODE_DELETE)
        
        var slideKeyHack = false
            private set
        private val slideKeys = mutableListOf<Key>()
        
        private fun addSlideKey(key: Key?) {
            if (key != null && slideKeyHack) {
                slideKeys.add(key)
            }
        }
        
        private fun sendSlideKeys() {
            // Implementation for slide key sending
            slideKeys.clear()
        }
    }
}
```

**LOC Reduction:** 498 → 280 (44%)

---

### Phase 3: Keyboard Core

**Duration:** 3 days  
**Risk:** High (core functionality)  
**Dependencies:** Phase 2 complete

#### File Migration Order

| Order | File | Current LOC | Target LOC |
|-------|------|-------------|------------|
| 3.1 | `Keyboard.java` | 1329 | ~500 |
| 3.2 | `LatinKeyboard.java` | 689 | ~300 |

**Key patterns for Keyboard.kt:**
- `Key` and `Row` become data classes
- XML parsing uses extension functions
- Grid calculation simplified

*(Full implementation follows same patterns as above)*

---

### Phase 4: View Layer

**Duration:** 4 days  
**Risk:** High  
**Dependencies:** Phase 3 complete

#### File Migration Order

| Order | File | Current LOC | Target LOC |
|-------|------|-------------|------------|
| 4.1 | `LatinKeyboardView.java` | 520 | ~250 |
| 4.2 | `LatinKeyboardBaseView.java` | 2094 | ~900 |

**Key changes for LatinKeyboardBaseView.kt:**

1. **Handler → Coroutines:**
```kotlin
// Before (Java)
mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SHOW_PREVIEW, keyIndex, 0), DELAY);

// After (Kotlin)
private var previewJob: Job? = null

fun showPreviewDelayed(keyIndex: Int, delay: Long) {
    previewJob?.cancel()
    previewJob = scope.launch {
        delay(delay)
        showKey(keyIndex)
    }
}
```

2. **PopupWindow management simplified:**
```kotlin
class PreviewPopupManager(private val context: Context) {
    private var popup: PopupWindow? = null
    private var previewText: TextView? = null
    
    fun show(key: Key, parent: View, x: Int, y: Int) {
        ensurePopup()
        previewText?.text = key.getCaseLabel()
        popup?.showAtLocation(parent, Gravity.NO_GRAVITY, x, y)
    }
    
    fun dismiss() {
        popup?.dismiss()
    }
    
    fun update(x: Int, y: Int, width: Int, height: Int) {
        popup?.update(x, y, width, height)
    }
    
    private fun ensurePopup() {
        if (popup == null) {
            // Initialize popup and previewText
        }
    }
}
```

---

### Phase 5: Service Layer

**Duration:** 4 days  
**Risk:** High  
**Dependencies:** Phase 4 complete

#### File Migration Order

| Order | File | Current LOC | Target LOC |
|-------|------|-------------|------------|
| 5.1 | `KeyboardSwitcher.java` | 776 | ~350 |
| 5.2 | `LatinIME.java` | 2204 | ~1000 |

**Key changes for LatinIME.kt:**

1. **State consolidation:**
```kotlin
data class IMEState(
    val modifiers: ModifierState = ModifierState(),
    val shiftState: ShiftState = ShiftState.Off,
    val composeMode: Boolean = false,
    val passwordMode: Boolean = false
)

data class ModifierState(
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val meta: Boolean = false,
    val fn: Boolean = false
) {
    fun toMetaState(): Int {
        var meta = 0
        if (ctrl) meta = meta or (KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON)
        if (alt) meta = meta or (KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON)
        if (this.meta) meta = meta or (KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON)
        return meta
    }
}

sealed class ShiftState {
    object Off : ShiftState()
    object On : ShiftState()
    object Locked : ShiftState()
    object CapsLocked : ShiftState()
}
```

2. **Giant switch → sealed class + when:**
```kotlin
sealed class KeyAction {
    data class Character(val code: Int) : KeyAction()
    data class Special(val type: SpecialKeyType) : KeyAction()
    object Delete : KeyAction()
    object Shift : KeyAction()
    object ModeChange : KeyAction()
    // etc.
}

enum class SpecialKeyType {
    ESCAPE, TAB, ENTER, 
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    HOME, END, PAGE_UP, PAGE_DOWN,
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12
}

fun handleKeyAction(action: KeyAction, x: Int, y: Int) {
    when (action) {
        is KeyAction.Character -> handleCharacter(action.code, x, y)
        is KeyAction.Delete -> handleBackspace()
        is KeyAction.Shift -> handleShift()
        is KeyAction.ModeChange -> changeKeyboardMode()
        is KeyAction.Special -> sendSpecialKey(action.type)
    }
}
```

---

### Phase 6: Settings & Activities

**Duration:** 2 days  
**Risk:** Low  
**Dependencies:** Phase 5 complete

#### File Migration Order

| Order | File |
|-------|------|
| 6.1 | `MaterialMainActivity.java` |
| 6.2 | `MaterialSettingsActivity.java` |
| 6.3 | Fragment files in `material/` |
| 6.4 | Settings items in `material/settings/` |

---

## Hilt Analysis (Optional)

### Hilt Overhead

| Item | Size/Time |
|------|-----------|
| Download | ~4 MB |
| Build time increase | +15-30 seconds |
| Boilerplate | ~100 LOC for setup |

### What Hilt Would Replace

```kotlin
// Current pattern
object KeyboardSwitcher {
    private var instance: KeyboardSwitcher? = null
    fun getInstance() = instance!!
    fun init(service: LatinIME) { instance = KeyboardSwitcher(service) }
}

// With Hilt
@Singleton
class KeyboardSwitcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: KeyboardSettings
)
```

### Recommendation: **Skip Hilt**

For this project size (~10k LOC), Hilt adds more complexity than it saves:
- ❌ Only ~3 singletons to replace
- ❌ Adds annotation processing (slower builds)
- ❌ Learning curve for contributors
- ✅ Kotlin `object` singletons work fine
- ✅ Can migrate to Hilt later if needed

---

## Testing Strategy

### Test Each Phase Before Proceeding

```bash
# After each phase:
gradle assembleDebug
gradle test
# Install and manually test on device
```

### Critical Test Cases

| Feature | Test Method |
|---------|-------------|
| Key detection | Unit tests for ProximityKeyDetector |
| Modifier keys | Manual: Ctrl+A, Alt+Tab, etc. |
| Shift states | Manual: single press, double press, caps lock |
| Popup preview | Manual: long press keys |
| Mini keyboard | Manual: long press for alternates |
| Swipe gestures | Manual: swipe up/down/left/right |
| Settings persistence | Manual: change settings, restart |

### Add These Test Dependencies

```groovy
testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
testImplementation 'app.cash.turbine:turbine:1.0.0'
testImplementation 'org.mockito.kotlin:mockito-kotlin:5.2.1'
```

---

## Rollback Plan

### If Migration Fails

```bash
# Revert to last working state
git checkout main -- app/src/main/java/

# Or reset entire branch
git checkout main
git branch -D kotlin-migration
```

### Incremental Safety

- Each phase is independent
- Java and Kotlin interop seamlessly
- Can stop at any phase with working code
- All changes in separate branch

---

## Appendix: Pattern Transformations

### Handler/Message → Coroutines

```java
// BEFORE
mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE, data), 300);

// Handler code
case MSG_UPDATE:
    doUpdate((Data) msg.obj);
    break;
```

```kotlin
// AFTER
scope.launch {
    delay(300)
    doUpdate(data)
}
```

### Anonymous Class → Lambda

```java
// BEFORE
view.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        handleClick();
    }
});
```

```kotlin
// AFTER
view.setOnClickListener { handleClick() }
```

### Static Singleton → Object

```java
// BEFORE
public class Manager {
    private static Manager sInstance;
    public static Manager getInstance() { return sInstance; }
    public static void init(Context ctx) { sInstance = new Manager(ctx); }
}
```

```kotlin
// AFTER
object Manager {
    private lateinit var context: Context
    
    fun init(ctx: Context) {
        context = ctx.applicationContext
    }
}
```

### Null Checks → Safe Calls

```java
// BEFORE
if (connection != null) {
    connection.beginBatchEdit();
    connection.commitText(text, 1);
    connection.endBatchEdit();
}
```

```kotlin
// AFTER
connection?.run {
    beginBatchEdit()
    commitText(text, 1)
    endBatchEdit()
}

// OR with extension
connection.withBatchEdit {
    commitText(text, 1)
}
```

### Switch → When

```java
// BEFORE
switch (code) {
    case KEYCODE_SHIFT:
        handleShift();
        break;
    case KEYCODE_DELETE:
        handleDelete();
        break;
    default:
        handleCharacter(code);
}
```

```kotlin
// AFTER
when (code) {
    KEYCODE_SHIFT -> handleShift()
    KEYCODE_DELETE -> handleDelete()
    else -> handleCharacter(code)
}
```

---

## Summary

| Phase | Files | Duration | Risk | LOC Before | LOC After |
|-------|-------|----------|------|------------|-----------|
| 0 | Setup | 1 day | Low | 0 | +100 (utils) |
| 1 | Data/Utilities | 2 days | Low | 2,400 | 900 |
| 2 | Detection/Tracking | 2 days | Medium | 750 | 370 |
| 3 | Keyboard Core | 3 days | High | 2,000 | 800 |
| 4 | View Layer | 4 days | High | 2,600 | 1,150 |
| 5 | Service Layer | 4 days | High | 3,000 | 1,350 |
| 6 | Settings/Activities | 2 days | Low | 1,100 | 600 |
| **Total** | **45 files** | **18 days** | | **~11,850** | **~5,270** |

**Final LOC Reduction: 56%**

---

*Last updated: 2026-01-31*  
*Migration branch: `kotlin-migration`*
