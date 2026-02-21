package org.pocketworkstation.pckeyboard

import android.content.SharedPreferences
import android.content.res.Resources
import java.util.Locale

/**
 * Global current settings for the keyboard.
 *
 *
 * Yes, globals are evil. But the persisted shared preferences are global data
 * by definition, and trying to hide this by propagating the current manually
 * just adds a lot of complication. This is especially annoying due to Views
 * getting constructed in a way that doesn't support adding additional
 * constructor arguments, requiring post-construction method calls, which is
 * error-prone and fragile.
 *
 *
 * The comments below indicate which class is responsible for updating the
 * value, and for recreating keyboards or views as necessary. Other classes
 * MUST treat the fields as read-only values, and MUST NOT attempt to save
 * these values or results derived from them across re-initializations.
 *
 * @author klaus.weidner@gmail.com
 */
object GlobalKeyboardSettings {
    private const val TAG = "HK/Globals"

    /* Simple prefs updated by this class */
    //
    // Read by Keyboard
   
    var popupKeyboardFlags = 0x1
   
    var topRowScale = 1.0f
    //
    // Read by LatinKeyboardView
   
    var showTouchPos = false
    //
    // Read by LatinIME
   
    var keyboardModePortrait = 0
   
    var keyboardModeLandscape = 2
   
    var compactModeEnabled = true  // always on
   
    var ctrlAOverride = 0
   
    var chordingCtrlKey = 0
   
    var chordingAltKey = 0
   
    var chordingMetaKey = 0
   
    var keyClickVolume = 0.0f
   
    var keyClickMethod = 0
   
    var capsLock = true
   
    var shiftLockModifiers = false
   
    var inputLocale: Locale = Locale.getDefault()
   
    var suggestedPunctuation = "!?,."
    //
    // Read by LatinKeyboardBaseView
   
    var labelScalePref = 1.0f
    //
    // Read by PointerTracker
   
    var sendSlideKeys = 0

    /* Updated by LatinIME */
    //
    // Read by KeyboardSwitcher
   
    var keyboardMode = 0
   
    var useExtension = false
    //
    // Read by LatinKeyboardView and KeyboardSwitcher
   
    var keyboardHeightPercent = 40.0f // percent of screen height
    //
    // Read by LatinKeyboardBaseView
   
    var hintMode = 0
   
    var renderMode = 1
    //
    // Read by PointerTracker
   
    var longpressTimeout = 400
    //
    // Read by LatinIMESettings
    // These are cached values for informational display, don't use for other purposes
   
    var editorPackageName: String? = null
   
    var editorFieldName: String? = null
   
    var editorFieldId: Int = 0
   
    var editorInputType: Int = 0

    /* Updated by KeyboardSwitcher */
    //
    // Used by LatinKeyboardBaseView and LatinIME

    // Auto pref implementation follows
    private val mBoolPrefs = HashMap<String, BooleanPref>()
    private val mStringPrefs = HashMap<String, StringPref>()
    
    const val FLAG_PREF_NONE = 0
    const val FLAG_PREF_NEED_RELOAD = 0x1
    const val FLAG_PREF_RECREATE_INPUT_VIEW = 0x4
    const val FLAG_PREF_RESET_KEYBOARDS = 0x8
    const val FLAG_PREF_RESET_MODE_OVERRIDE = 0x10
    
    private var mCurrentFlags = 0

    private interface BooleanPref {
        fun set(value: Boolean)
        fun getDefault(): Boolean
        fun getFlags(): Int
    }

    private interface StringPref {
        fun set(value: String)
        fun getDefault(): String
        fun getFlags(): Int
    }

   
    fun initPrefs(prefs: SharedPreferences, resources: Resources) {
        val res = resources

        addStringPref("pref_keyboard_mode_portrait", object : StringPref {
            override fun set(value: String) {
                keyboardModePortrait = try {
                    value.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            }
            override fun getDefault(): String = res.getString(R.string.default_keyboard_mode_portrait)
            override fun getFlags(): Int = FLAG_PREF_RESET_KEYBOARDS or FLAG_PREF_RESET_MODE_OVERRIDE
        })

        addStringPref("pref_keyboard_mode_landscape", object : StringPref {
            override fun set(value: String) {
                keyboardModeLandscape = try {
                    value.toInt()
                } catch (e: NumberFormatException) {
                    2
                }
            }
            override fun getDefault(): String = res.getString(R.string.default_keyboard_mode_landscape)
            override fun getFlags(): Int = FLAG_PREF_RESET_KEYBOARDS or FLAG_PREF_RESET_MODE_OVERRIDE
        })

        addStringPref("pref_slide_keys_int", object : StringPref {
            override fun set(value: String) {
                sendSlideKeys = try {
                    value.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            }
            override fun getDefault(): String = "0"
            override fun getFlags(): Int = FLAG_PREF_NONE
        })

        addBooleanPref("pref_touch_pos", object : BooleanPref {
            override fun set(value: Boolean) {
                showTouchPos = value
            }
            override fun getDefault(): Boolean = false
            override fun getFlags(): Int = FLAG_PREF_NONE
        })

        addStringPref("pref_popup_content", object : StringPref {
            override fun set(value: String) {
                popupKeyboardFlags = try {
                    value.toInt()
                } catch (e: NumberFormatException) {
                    1
                }
            }
            override fun getDefault(): String = res.getString(R.string.default_popup_content)
            override fun getFlags(): Int = FLAG_PREF_RESET_KEYBOARDS
        })

        addStringPref("pref_label_scale_v2", object : StringPref {
            override fun set(value: String) {
                labelScalePref = try {
                    value.toFloat()
                } catch (e: NumberFormatException) {
                    1.0f
                }
            }
            override fun getDefault(): String = "1.0"
            override fun getFlags(): Int = FLAG_PREF_RECREATE_INPUT_VIEW
        })

        addStringPref("pref_top_row_scale", object : StringPref {
            override fun set(value: String) {
                topRowScale = try {
                    value.toFloat()
                } catch (e: NumberFormatException) {
                    1.0f
                }
            }
            override fun getDefault(): String = "1.0"
            override fun getFlags(): Int = FLAG_PREF_RESET_KEYBOARDS
        })

        addStringPref("pref_ctrl_a_override", object : StringPref {
            override fun set(value: String) {
                ctrlAOverride = try {
                    value.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            }
            override fun getDefault(): String = res.getString(R.string.default_ctrl_a_override)
            override fun getFlags(): Int = FLAG_PREF_RESET_KEYBOARDS
        })

        addStringPref("pref_chording_ctrl_key", object : StringPref {
            override fun set(value: String) {
                chordingCtrlKey = try {
                    value.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            }
            override fun getDefault(): String = res.getString(R.string.default_chording_ctrl_key)
            override fun getFlags(): Int = FLAG_PREF_RESET_KEYBOARDS
        })

        addStringPref("pref_chording_alt_key", object : StringPref {
            override fun set(value: String) {
                chordingAltKey = try {
                    value.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            }
            override fun getDefault(): String = res.getString(R.string.default_chording_alt_key)
            override fun getFlags(): Int = FLAG_PREF_RESET_KEYBOARDS
        })

        addStringPref("pref_chording_meta_key", object : StringPref {
            override fun set(value: String) {
                chordingMetaKey = try {
                    value.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            }
            override fun getDefault(): String = res.getString(R.string.default_chording_meta_key)
            override fun getFlags(): Int = FLAG_PREF_RESET_KEYBOARDS
        })

        addStringPref("pref_click_volume", object : StringPref {
            override fun set(value: String) {
                keyClickVolume = try {
                    value.toFloat()
                } catch (e: NumberFormatException) {
                    0.2f
                }
            }
            override fun getDefault(): String = res.getString(R.string.default_click_volume)
            override fun getFlags(): Int = FLAG_PREF_NONE
        })

        addStringPref("pref_click_method", object : StringPref {
            override fun set(value: String) {
                keyClickMethod = try {
                    value.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            }
            override fun getDefault(): String = res.getString(R.string.default_click_method)
            override fun getFlags(): Int = FLAG_PREF_NONE
        })

        addBooleanPref("pref_caps_lock", object : BooleanPref {
            override fun set(value: Boolean) {
                capsLock = value
            }
            override fun getDefault(): Boolean = res.getBoolean(R.bool.default_caps_lock)
            override fun getFlags(): Int = FLAG_PREF_NONE
        })

        addBooleanPref("pref_shift_lock_modifiers", object : BooleanPref {
            override fun set(value: Boolean) {
                shiftLockModifiers = value
            }
            override fun getDefault(): Boolean = res.getBoolean(R.bool.default_shift_lock_modifiers)
            override fun getFlags(): Int = FLAG_PREF_NONE
        })

        // Set initial values
        for (key in mBoolPrefs.keys) {
            val pref = mBoolPrefs[key]!!
            pref.set(prefs.getBoolean(key, pref.getDefault()))
        }
        for (key in mStringPrefs.keys) {
            val pref = mStringPrefs[key]!!
            pref.set(prefs.getString(key, pref.getDefault()) ?: pref.getDefault())
        }
    }

   
    fun sharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        mCurrentFlags = FLAG_PREF_NONE
        val bPref = mBoolPrefs[key]
        if (bPref != null) {
            bPref.set(prefs.getBoolean(key, bPref.getDefault()))
            mCurrentFlags = mCurrentFlags or bPref.getFlags()
        }
        val sPref = mStringPrefs[key]
        if (sPref != null) {
            sPref.set(prefs.getString(key, sPref.getDefault()) ?: sPref.getDefault())
            mCurrentFlags = mCurrentFlags or sPref.getFlags()
        }
        //if (!found) Log.i(TAG, "sharedPreferenceChanged: unhandled key=" + key)
    }

   
    fun hasFlag(flag: Int): Boolean {
        if ((mCurrentFlags and flag) != 0) {
            mCurrentFlags = mCurrentFlags and flag.inv()
            return true
        }
        return false
    }

   
    fun unhandledFlags(): Int {
        return mCurrentFlags
    }

    private fun addBooleanPref(key: String, setter: BooleanPref) {
        mBoolPrefs[key] = setter
    }

    private fun addStringPref(key: String, setter: StringPref) {
        mStringPrefs[key] = setter
    }
}
