# Remaining Java → Kotlin Migration Plan

> **For Amp agents**: This is the authoritative guide for migrating the 5 remaining Java files.
> Read this entire file before touching any code. Follow the order strictly.

---

## Files to Migrate

| Order | File | Lines | Why This Order |
|-------|------|-------|----------------|
| 1 | `LatinKeyboard.java` | 689 | Subclass of Keyboard, no View deps |
| 2 | `LatinKeyboardView.java` | 661 | Subclass of BaseView, smaller |
| 3 | `Keyboard.java` | 1,329 | Foundation — do after subclasses to avoid breaking them mid-flight |
| 4 | `LatinKeyboardBaseView.java` | 2,094 | Giant View — do after all keyboard model classes |
| 5 | `LatinIME.java` | 2,204 | IME service — depends on everything |

All files live in:
`app/src/main/java/org/pocketworkstation/pckeyboard/`

---

## Ground Rules (Read Before Every File)

1. **Verify the build passes after each file.** Run `gradle assembleDebug`. Never stack two files without a green build in between.
2. **Delete the `.java` file** after the `.kt` replacement is written and the build is green.
3. **No behaviour changes.** This is a mechanical translation, not a rewrite. Logic stays identical.
4. **No restructuring.** Don't split files, don't merge classes, don't rename public symbols.
5. **Preserve all `@JvmField` / `@JvmStatic` / `@JvmOverloads`** on anything accessed from Java files that have not yet been migrated. Remove them at the end (see Phase 6).
6. **Inner classes stay inner.** `Keyboard.Row`, `Keyboard.Key`, `LatinKeyboard.LatinKey`, `LatinKeyboardBaseView.OnKeyboardActionListener`, `LatinKeyboardBaseView.UIHandler` etc. must remain nested — other files reference them by their nested name.
7. **Match existing Kotlin style** seen in `SwipeTracker.kt`, `PointerTracker.kt`, `ModifierKeyState.kt`:
   - `companion object` for statics
   - `private companion object` for private statics
   - `const val` for primitive constants
   - `@JvmField val` / `@JvmStatic fun` where Java interop is needed
   - `when` instead of `switch`
   - `?.` / `?:` for null safety — use `!!` only when null is provably impossible
   - `kotlin.math.*` instead of `java.lang.Math.*`

---

## Phase 1 — `LatinKeyboard.java` → `LatinKeyboard.kt`

### What this class is
Subclass of `Keyboard`. Adds space bar rendering, shift lock icon management, enter key icon switching per IME action, and the `LatinKey` inner class that overrides hit-testing and drawable states.

### Conversion checklist

**Constants**
- `DEBUG_PREFERRED_LETTER`, `TAG`, `OPACITY_FULLY_OPAQUE`, `SPACE_LED_LENGTH_PERCENT`, `SPACEBAR_DRAG_THRESHOLD`, etc. → `private companion object { const val ... }`
- `sSpacebarVerticalCorrection` is a static var mutated at construction time → `companion object { @JvmField var sSpacebarVerticalCorrection: Int = 0 }`

**Fields**
- All `Drawable?` fields are nullable — use `var mShiftLockIcon: Drawable? = null`
- `mIsAlphaKeyboard`, `mIsAlphaFullKeyboard`, `mIsFnFullKeyboard` are set in constructor and never mutated → `val`
- `mSpaceKeyIndexArray: IntArray` — keep as `IntArray`

**Constructor**
```kotlin
class LatinKeyboard(context: Context, xmlLayoutResId: Int) :
    Keyboard(context, 0, xmlLayoutResId, 0, 0) { ... }

// Secondary constructor (the main one)
constructor(context: Context, xmlLayoutResId: Int, mode: Int, kbHeightPercent: Float) :
    this(context, xmlLayoutResId) { ... }
```
Wait — the Java has `this(context, 0, xmlLayoutResId, mode, kbHeightPercent)` calling the parent directly. Replicate the same delegation chain.

**`setImeOptions` switch → when**
```kotlin
when (options and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
    EditorInfo.IME_ACTION_GO -> { ... }
    EditorInfo.IME_ACTION_SEARCH -> { ... }
    else -> { ... }
}
```

**`setDefaultBounds` helper** — make it a private extension on `Drawable`:
```kotlin
private fun Drawable.setDefaultBounds() {
    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
}
```

**`isInside` / `inPrefList` / `distanceFrom`** — straightforward translation, keep `for (i in 0 until nearby.size)` loops as-is for clarity.

**`LatinKey` inner class**
- Must be `inner class LatinKey` (needs `outer.this` for `LatinKeyboard.this.isInside` and `LatinKeyboard.this.mVerticalGap`)
- `KEY_STATE_FUNCTIONAL_*` arrays are per-instance in Java but logically constants → move them to the `LatinKey` companion object as `@JvmField val`
- `isFunctionalKey()` → private function (no annotation needed, not called from outside)

**`getNearestKeys` override** — straightforward.

**`getTextSizeFromTheme`** — straightforward.

**`Math.abs` / `Math.min` / `Math.max`** → import from `kotlin.math`

### Build check
```
gradle assembleDebug
gradle test
```

---

## Phase 2 — `LatinKeyboardView.java` → `LatinKeyboardView.kt`

### What this class is
Subclass of `LatinKeyboardBaseView`. Adds:
- All the keyboard-specific `KEYCODE_*` constants
- Extension keyboard popup (a floating row that appears above the main keyboard)
- Multi-touch sudden-jump disambiguation
- Long-press overrides (Options key, DPad center)
- Debug auto-play instrumentation (guarded by `DEBUG_AUTO_PLAY = false`)

### Conversion checklist

**Constants**
All `static final int KEYCODE_*` → `companion object { const val KEYCODE_... = -xxx }`.

**Fields**
- `mExtensionPopup: PopupWindow?` nullable
- `mExtension: LatinKeyboardView?` nullable
- `mExtensionKeyboard: LatinKeyboard?` nullable
- `mHandler2: Handler?` nullable (only used in dead `DEBUG_AUTO_PLAY` branch)
- `mPaint: Paint?` nullable (lazy, only allocated on first draw when debug flag is on)
- `mAsciiKeys: Array<Key?>` = `arrayOfNulls(256)`
- `mLastX`, `mLastY`, `mJumpThresholdSquare`, `mLastRowY` — `var Int`
- Boolean flags — `var Boolean`

**Constructors**
```kotlin
constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
    ...
}
```

**`handleSuddenJump` switch → when**
```kotlin
when (action) {
    MotionEvent.ACTION_DOWN -> { ... }
    MotionEvent.ACTION_MOVE -> { ... }
    MotionEvent.ACTION_UP -> { ... }
}
```

**`setKeyboard` override** — `(oldKeyboard as? LatinKeyboard)?.keyReleased()` for the safe cast.

**`onLongPress`**
```kotlin
override fun onLongPress(key: Key): Boolean {
    PointerTracker.clearSlideKeys()
    return when (key.codes[0]) {
        KEYCODE_OPTIONS -> invokeOnKey(KEYCODE_OPTIONS_LONGPRESS)
        KEYCODE_DPAD_CENTER -> invokeOnKey(KEYCODE_COMPOSE)
        '0'.code -> if (keyboard == mPhoneKeyboard) invokeOnKey('+'.code) else super.onLongPress(key)
        else -> super.onLongPress(key)
    }
}
```

**`ExtensionKeyboardListener`** — static in Java → just `private class` (no `inner`; it doesn't reference the outer instance):
```kotlin
private class ExtensionKeyboardListener(
    private val target: OnKeyboardActionListener
) : OnKeyboardActionListener {
    override fun onKey(...) { target.onKey(...) }
    override fun swipeDown() = true  // don't pass through
    // etc.
}
```

**`draw` override** — keep the GCUtils retry loop unchanged (it calls `super.draw(c)` and catches `OutOfMemoryError`).

**`DEBUG_AUTO_PLAY` dead code** — keep it as `const val DEBUG_AUTO_PLAY = false` and leave the guarded blocks. Do not delete them (they're part of the original codebase).

**`mHandler2` anonymous Handler** — In Kotlin, anonymous `Handler` subclasses need `object : Handler() { ... }`. Since it's inside a DEBUG block, it's fine to leave as-is style.

**`makePopupWindow`** — straightforward; use `?.` for nullable fields.

**`ColorDrawable` import** — already in scope from BaseView; if not, add `import android.graphics.drawable.ColorDrawable`.

### Build check
```
gradle assembleDebug
gradle test
```

---

## Phase 3 — `Keyboard.java` → `Keyboard.kt`

### What this class is
The foundation. Parses XML keyboard layout, builds the key grid, computes nearest-key lookup table. Contains two public nested classes: `Row` and `Key`.

### Conversion checklist

**Top-level constants → companion object**
```kotlin
companion object {
    const val DEAD_KEY_PLACEHOLDER = '\u25cc'
    val DEAD_KEY_PLACEHOLDER_STRING = DEAD_KEY_PLACEHOLDER.toString()
    const val EDGE_LEFT = 0x01
    const val EDGE_RIGHT = 0x02
    const val EDGE_TOP = 0x04
    const val EDGE_BOTTOM = 0x08
    const val KEYCODE_SHIFT = -1
    const val KEYCODE_MODE_CHANGE = -2
    // ... all KEYCODE_*, SHIFT_*, POPUP_* constants
    const val DEFAULT_LAYOUT_ROWS = 4
    const val DEFAULT_LAYOUT_COLUMNS = 10
    private const val TAG = "Keyboard"
    private const val TAG_KEYBOARD = "Keyboard"
    private const val TAG_ROW = "Row"
    private const val TAG_KEY = "Key"
    private const val SEARCH_DISTANCE = 1.8f

    @JvmStatic
    fun getDimensionOrFraction(a: TypedArray, index: Int, base: Int, defValue: Float): Float {
        val value = a.peekValue(index) ?: return defValue
        return when (value.type) {
            TypedValue.TYPE_DIMENSION -> a.getDimensionPixelOffset(index, defValue.roundToInt()).toFloat()
            TypedValue.TYPE_FRACTION -> a.getFraction(index, base, base, defValue)
            else -> defValue
        }
    }
}
```
`getDimensionOrFraction` **must be `@JvmStatic`** — called from `Row` and `Key` constructors which will be in Kotlin inner classes but also potentially from `LatinKeyboard` while it was still Java (already migrated by now, so double-check).

**`Row` nested class**
- Not `inner` in Java (static nested) → becomes just `class Row` nested inside `Keyboard`.
- But it references `parent.mDisplayWidth` etc., so keep it as a regular nested class with a `parent: Keyboard` field (same as Java).
- Constructor from XML: straightforward translation.

**`Key` nested class**
- Also static nested in Java → regular nested class with `keyboard: Keyboard` field.
- `KEY_STATE_*` arrays: these are `private static final int[]` → move to a `companion object` inside `Key`:
```kotlin
companion object {
    private val KEY_STATE_NORMAL_ON = intArrayOf(...)
    private val KEY_STATE_PRESSED_ON = intArrayOf(...)
    // etc.
}
```
- `parseCSV` — replace `StringTokenizer` with `split(",")`:
```kotlin
internal fun parseCSV(value: String): IntArray =
    if (value.isEmpty()) intArrayOf()
    else value.split(",").mapNotNull { it.trim().toIntOrNull() }.toIntArray()
```
- `isInside` — convert `if (...) return true else return false` to a single expression:
```kotlin
fun isInside(x: Int, y: Int): Boolean {
    val leftEdge = edgeFlags and EDGE_LEFT > 0
    val rightEdge = edgeFlags and EDGE_RIGHT > 0
    val topEdge = edgeFlags and EDGE_TOP > 0
    val bottomEdge = edgeFlags and EDGE_BOTTOM > 0
    return (x >= this.x || (leftEdge && x <= this.x + this.width))
        && (x < this.x + this.width || (rightEdge && x >= this.x))
        && (y >= this.y || (topEdge && y <= this.y + this.height))
        && (y < this.y + this.height || (bottomEdge && y >= this.y))
}
```
- `getCurrentDrawableState` — nested `if/else` → `when` with guard conditions:
```kotlin
fun getCurrentDrawableState(): IntArray = when {
    locked && pressed -> KEY_STATE_PRESSED_LOCK
    locked -> KEY_STATE_NORMAL_LOCK
    on && pressed -> KEY_STATE_PRESSED_ON
    on -> KEY_STATE_NORMAL_ON
    sticky && pressed -> KEY_STATE_PRESSED_OFF
    sticky -> KEY_STATE_NORMAL_OFF
    pressed -> KEY_STATE_PRESSED
    else -> KEY_STATE_NORMAL
}
```
- `getPopupKeyboardContent` — large method, translate mechanically. No logic changes.
- `toString` — use string templates.

**Keyboard fields**
- `mKeys: MutableList<Key>` → `private val mKeys: MutableList<Key> = mutableListOf()`
- `mModifierKeys: MutableList<Key>` → same
- `mShiftKey`, `mAltKey`, `mCtrlKey`, `mMetaKey` → nullable `var Key?`
- `mGridNeighbors: Array<IntArray>?` nullable
- All the `mDefault*` floats/ints → `var`

**`Math.round` → `.roundToInt()`** (import `kotlin.math.roundToInt`)

**`loadKeyboard`** — keep the imperative XML parsing loop unchanged. It's complex enough that mechanical translation is the right call.

**`fixAltChars`** — keep the loop. Replace `HashSet<Character>` with `mutableSetOf<Char>()`.

**`setEdgeFlags`** — straightforward.

**`computeNearestNeighbors`** — straightforward. Keep `System.arraycopy`.

**Private constructor for popup keyboards**
```kotlin
private constructor(
    context: Context,
    defaultHeight: Int,
    layoutTemplateResId: Int,
    characters: CharSequence,
    reversed: Boolean,
    columns: Int,
    horizontalPadding: Int
) : this(context, defaultHeight, layoutTemplateResId) { ... }
```

**Public constructors**
```kotlin
constructor(context: Context, defaultHeight: Int, xmlLayoutResId: Int) :
    this(context, defaultHeight, xmlLayoutResId, 0)

constructor(context: Context, defaultHeight: Int, xmlLayoutResId: Int, modeId: Int) :
    this(context, defaultHeight, xmlLayoutResId, modeId, 0f)

constructor(context: Context, defaultHeight: Int, xmlLayoutResId: Int, modeId: Int, kbHeightPercent: Float) {
    // primary constructor body
}
```
Note: in Kotlin you can't have a class body primary constructor and also delegate `this(...)` in the same block. Use `init {}` for the primary and delegate secondaries to it.

**`mRowCount`, `mExtensionRowCount`, `mLayoutRows`, `mLayoutColumns`** — these are `public` in Java → `var` fields (not in companion). Accessed by `LatinKeyboardView` (`newKeyboard.mRowCount`).

### Build check
```
gradle assembleDebug
gradle test
```

---

## Phase 4 — `LatinKeyboardBaseView.java` → `LatinKeyboardBaseView.kt`

### What this class is
The core `View` that renders the keyboard and dispatches touch events to `PointerTracker`. 2,094 lines. Contains: `OnKeyboardActionListener` interface, `UIHandler` inner class, `PointerQueue` inner class, drawing code, preview popup management.

### Conversion checklist

**`OnKeyboardActionListener` interface**
Keep as a regular `interface` (not `fun interface` — it has many methods). It's referenced by name throughout the codebase.

**`UIHandler` inner class**
Must be `inner class UIHandler : Handler()`. It references outer fields (`mMiniKeyboardPopup`, `mPreviewPopup`, etc.). The `MSG_*` constants go in its companion object:
```kotlin
inner class UIHandler : Handler() {
    companion object {
        private const val MSG_POPUP_PREVIEW = 1
        private const val MSG_DISMISS_PREVIEW = 2
        private const val MSG_REPEAT_KEY = 3
        private const val MSG_LONGPRESS_KEY = 4
    }
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_POPUP_PREVIEW -> showKey(msg.arg1)
            // etc.
        }
    }
    // ...
}
```

**`PointerQueue` static inner class → nested class**
No reference to outer `this`, so just `private class PointerQueue`.

**Static fields → companion object**
```kotlin
companion object {
    const val NOT_A_TOUCH_COORDINATE = -1
    const val NOT_A_KEY = -1
    private const val NUMBER_HINT_VERTICAL_ADJUSTMENT_PIXEL = -1
    private const val DEBUG = false
    private const val TAG = "HK/LatinKbdBaseView"

    // Typeface cache
    private var sCustomTypeface: Typeface? = null
    private var sCurrentFontMode = -1

    // Reflection for setLayerType (only needed pre-API 11, but keep for compatibility)
    private var sSetRenderMode: Method? = null
    private var sPrevRenderMode = -1

    private val INVERTING_MATRIX = floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )

    private fun isOneRowKeys(keys: List<Key>): Boolean { ... }
    private fun isNumberAtEdgeOfPopupChars(key: Key): Boolean { ... }
    private fun isAsciiDigit(c: Char): Boolean { ... }
}
```
`isOneRowKeys`, `isNumberAtEdgeOfPopupChars`, `isAsciiDigit` were `private static` — move to companion and keep `private`.

**Fields**
- All `Paint`, `Rect`, `Bitmap`, `Drawable` fields → nullable `var` where they're initialized lazily, non-null `val` where set in constructor.
- `mPointerTrackers: Array<PointerTracker>` — keep as array.
- `WeakHashMap` stays (`import java.util.WeakHashMap`).

**Constructors**
```kotlin
constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
    init(context, attrs, defStyle)
}
private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) { ... }
```

**Large methods** (`onDraw`, `onTouchEvent`, `showPreview`, `showMiniKeyboard`)
Translate mechanically. Don't restructure. The `onDraw` method in particular has careful bitmap caching logic — keep it line-for-line.

**`switch` → `when`** in `handleMessage`, `onTouchEvent`, etc.

**`Region.Op` usage**
`canvas.clipRect(...)` with `Region.Op` is deprecated but still works. Keep it as-is. Do not "fix" deprecation warnings.

**`View.setLayerType` reflection**
The reflection block at the bottom of the file that stores `sSetRenderMode` via `View.class.getMethod(...)` — translate mechanically:
```kotlin
init {
    try {
        sSetRenderMode = View::class.java.getMethod("setLayerType", Int::class.java, Paint::class.java)
    } catch (e: NoSuchMethodException) {
        // pre-honeycomb
    }
}
```
This goes in the companion object `init {}` block.

**`popupKeyboardIsShowing()`** — package-internal in Java → `internal fun` in Kotlin.

**`enableSlideKeyHack()`** — package-internal in Java, overridden in `LatinKeyboardView` → `open internal fun`.

**Accessing package-private members from `LatinKeyboardView`**
Both files are in the same package so `internal` visibility works.

### Build check
```
gradle assembleDebug
gradle test
```

---

## Phase 5 — `LatinIME.java` → `LatinIME.kt`

### What this class is
The `InputMethodService`. The entry point. 2,204 lines. Implements `ComposeSequencing`, `LatinKeyboardBaseView.OnKeyboardActionListener`, `SharedPreferences.OnSharedPreferenceChangeListener`.

### Conversion checklist

**Static initializer (`static { ESC_SEQUENCES = ...; CTRL_SEQUENCES = ...; }`) → companion object init**
```kotlin
companion object {
    @JvmField var ESC_SEQUENCES: Map<Int, String>
    @JvmField var CTRL_SEQUENCES: Map<Int, Int>

    // All PREF_* string constants
    const val PREF_VIBRATE_LEN = "vibrate_len"
    // ...

    // All ASCII_* / MSG_* / DELETE_ACCELERATE_AT etc.
    const val ASCII_ENTER = '\n'.code
    const val ASCII_SPACE = ' '.code
    const val ASCII_PERIOD = '.'.code

    @JvmField lateinit var sKeyboardSettings: GlobalKeyboardSettings

    init {
        ESC_SEQUENCES = mapOf(
            KeyEvent.KEYCODE_ESCAPE to "\u001b",
            // etc.
        )
        CTRL_SEQUENCES = mapOf(
            'a'.code to 1,
            // etc.
        )
    }
}
```

**`@JvmField` on `sKeyboardSettings`** — referenced everywhere as `LatinIME.sKeyboardSettings`. Keep until all callers are verified (they already are Kotlin by this point, but leave it for safety).

**`mHandler` anonymous Handler → named inner class**
The Java file has an anonymous `Handler` subclass that handles `MSG_START_TUTORIAL` and `MSG_UPDATE_SHIFT_STATE`. Convert to a named `inner class`:
```kotlin
private inner class IMEHandler : Handler() {
    companion object {
        const val MSG_START_TUTORIAL = 1
        const val MSG_UPDATE_SHIFT_STATE = 2
    }
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_START_TUTORIAL -> { ... }
            MSG_UPDATE_SHIFT_STATE -> updateShiftKeyState(currentInputEditorInfo)
        }
    }
}
private val mHandler = IMEHandler()
```

**Large `onKey` method switch → when**
```kotlin
when (primaryCode) {
    Keyboard.KEYCODE_DELETE -> handleBackspace()
    Keyboard.KEYCODE_SHIFT -> handleShift()
    Keyboard.KEYCODE_MODE_CHANGE -> changeKeyboardMode()
    ASCII_ENTER -> handleEnter()
    else -> handleCharacter(primaryCode, keyCodes)
}
```

**Modifier booleans → kept as simple `var`**
`mModCtrl`, `mModAlt`, `mModMeta`, `mModFn` — just translate as `private var mModCtrl = false` etc. No need to consolidate into a data class.

**`BroadcastReceiver` anonymous class → named inner class**
Same pattern as `IMEHandler`. Named `inner class` is cleaner.

**`AlertDialog?` field** — nullable, `var`.

**`isWordSeparator`** called from `RingCharBuffer` → keep `fun isWordSeparator(code: Int): Boolean`.

**`onSharedPreferenceChanged` big if-else → when + early returns**
```kotlin
override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
    when (key) {
        PREF_VIBRATE_ON -> mVibrateOn = prefs.getBoolean(key, false)
        PREF_SOUND_ON -> mSoundOn = prefs.getBoolean(key, false)
        // etc.
    }
    sKeyboardSettings.sharedPreferenceChanged()
}
```

**`RingCharBuffer` usage** — `LatinIMEUtil.RingCharBuffer.getInstance()` is already Kotlin. No change needed.

**Pattern / Matcher for ESC sequences** — keep `import java.util.regex.Pattern`, `import java.util.regex.Matcher`.

**`PrintWriterPrinter` / `dump`** — keep as-is (debug utility).

**`Notification` setup** — straightforward translation, use `?.` for nullable intents.

**`isFullscreenMode` override** — straightforward.

### Build check
```
gradle assembleDebug
gradle test
```

---

## Phase 6 — Post-Migration Cleanup (After All 5 Files Are Kotlin)

Once `gradle assembleDebug` and `gradle test` are green with all 5 files migrated:

### 6.1 Remove `@JvmStatic` annotations

Search for all `@JvmStatic` across the codebase. For each one, check if any Java file still calls it. Since all Java files are now gone, remove every `@JvmStatic`.

```bash
grep -rn "@JvmStatic" app/src/main/java/
```

Remove them one file at a time. Build after each file.

### 6.2 Remove `@JvmField` annotations

Same process:
```bash
grep -rn "@JvmField" app/src/main/java/
```

`@JvmField` on `lateinit var` fields accessed cross-file is fine to remove — Kotlin property access works the same way between Kotlin files.

Exception: **keep** `@JvmField` if the field is accessed from XML attributes or reflection (there are none in this codebase, but verify).

### 6.3 Remove `@JvmOverloads` if any were added

```bash
grep -rn "@JvmOverloads" app/src/main/java/
```

### 6.4 Final build and test
```
gradle assembleDebug
gradle test
```

---

## Common Kotlin Patterns — Quick Reference

### switch → when
```kotlin
// Java
switch (x) {
    case A: doA(); break;
    case B: doB(); break;
    default: doDefault();
}

// Kotlin
when (x) {
    A -> doA()
    B -> doB()
    else -> doDefault()
}
```

### null check → safe call
```kotlin
// Java
if (mShiftKey != null) { mShiftKey.on = true; }

// Kotlin
mShiftKey?.on = true
```

### cast → safe cast
```kotlin
// Java
if (oldKeyboard instanceof LatinKeyboard) { ((LatinKeyboard) oldKeyboard).keyReleased(); }

// Kotlin
(oldKeyboard as? LatinKeyboard)?.keyReleased()
```

### static fields → companion object
```kotlin
companion object {
    const val KEYCODE_SHIFT = -1          // primitive constant
    @JvmField val DEFAULT_LOCALE = Locale.getDefault()  // object constant needing Java access
    private var sInstance: Foo? = null    // mutable static
}
```

### Math imports
```kotlin
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
// Use: abs(x), max(a, b), x.roundToInt()
```

### ArrayList → mutableListOf
```kotlin
// Java: new ArrayList<Key>()
// Kotlin:
val mKeys: MutableList<Key> = mutableListOf()
```

### String templates
```kotlin
// Java: "Keyboard(" + cols + "x" + rows + ")"
// Kotlin:
"Keyboard(${cols}x${rows})"
```

### Anonymous Handler → inner class
```kotlin
private inner class MyHandler : Handler() {
    companion object {
        const val MSG_SOMETHING = 1
    }
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_SOMETHING -> doSomething()
        }
    }
}
```

### Static nested class (no outer reference)
```kotlin
// Java: private static class Foo { ... }
// Kotlin: private class Foo { ... }  (nested classes are non-inner by default)
```

### Inner class (references outer)
```kotlin
// Java: private class Foo { void bar() { OuterClass.this.field; } }
// Kotlin: private inner class Foo { fun bar() { this@OuterClass.field } }
```

---

## What NOT to Do

- ❌ Don't convert `Handler` to coroutines — that's a behavioural change
- ❌ Don't replace `SharedPreferences` with DataStore — out of scope
- ❌ Don't turn `GlobalKeyboardSettings` into a data class — it's a singleton with complex lifecycle
- ❌ Don't make `Key` or `Row` into data classes — they have mutable fields set post-construction
- ❌ Don't refactor the `loadKeyboard` XML parsing loop — it works, leave it
- ❌ Don't change any public API names — other files reference them by exact name
- ❌ Don't add coroutines anywhere — not needed for this migration
- ❌ Don't combine phases — one file at a time, build in between

---

*Covers: Keyboard.java, LatinKeyboard.java, LatinKeyboardBaseView.java, LatinKeyboardView.java, LatinIME.java*
*Created: 2026-02-20*
