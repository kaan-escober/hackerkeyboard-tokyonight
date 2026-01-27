# Changelog

All notable changes to this project will be documented in this file.

## [Edge-to-Edge Navigation Integration] - 2026-01-14

### Added
- **Seamless Navigation Bar Integration**: The keyboard now extends behind the system navigation bar, matching the selected theme's background color (Storm, Night, Moon, Day).
- **Edge-to-Edge Display**: Enabled `SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION` to allow the keyboard window to draw full-screen.
- **Smart Bottom Padding**: Added dynamic bottom padding (2.7x system navigation bar height) to push keys up, ensuring a comfortable "chin" area that prevents accidental system gestures while typing.
- **Theme-Aware Status**: The navigation bar now correctly respects the light/dark mode of the active theme for its icons (e.g., dark icons for Day theme).

## [Material 3 Welcome Page] - 2026-01-12

### Added
- **Material 3 Welcome Activity**: Implemented a modern, theme-aware welcome/setup page as an alternative to the legacy Main activity.
    - **Setup Card**: Step-by-step setup with numbered indicators for enabling keyboard and setting input method.
    - **Customize Card**: Quick access to the Material 3 Settings activity.
    - **Test Keyboard Card**: Built-in text field for testing keyboard functionality.
    - **About Card**: Project information with description, GitHub link, and license info.
- **GitHub Integration**: Clickable link to the project repository (https://github.com/umairimtiaz9/hackerkeyboard-tokyonight).
- **Decorative Tokyo Night Pink Lines**: Three beautiful horizontal accent lines below the About section with graduated opacity (100%, 70%, 40%).
- **Vector Icons**: Added `ic_github.xml` and `ic_open_in_new.xml` for the About section.

### Changed
- **Theme Support**: Welcome page supports all 4 Tokyo Night variants (Storm, Night, Moon, Day) with instant theme switching.
- **String Resources**: Added about_title, about_description, about_github_title, about_github_subtitle, about_license, and about_github_url.

### Removed
- **Legacy UI Purge**: Completely removed all legacy settings and welcome UI in favor of Material 3.
    - Deleted `Main.java`, `main.xml` (legacy welcome page)
    - Deleted `LatinIMESettings.java` (legacy settings activity)
    - Deleted `InputLanguageSelection.java` (legacy language picker)
    - Deleted `PrefScreenActions.java`, `PrefScreenView.java`, `PrefScreenFeedback.java` (legacy preference screens)
    - Deleted `SettingsActivity.java`, `activity_settings.xml` (legacy settings container)
- **Updated Settings Launch Points**: All keyboard settings now launch `MaterialSettingsActivity`:
    - `LatinIME.launchSettings()` - keyboard settings key
    - `NotificationReceiver` - notification settings action
    - `method.xml` - Android system IME settings entry point
- **Cleaned AndroidManifest**: Removed 5 legacy activity entries, keeping only Material 3 UI activities.

---

## [Material 3 Settings UI] - 2026-01-11

### Added
- **Complete Material 3 Settings Activity**: Implemented a modern, theme-aware settings interface replacing the legacy PreferenceActivity.
    - **Theme Selection**: Card-based theme picker with 6-color palette previews for Storm, Night, Moon, and Day variants.
    - **Language Selection**: RecyclerView-based language picker with MaterialSwitch toggles.
    - **Visual Appearance**: Sliders for keyboard height (portrait/landscape), label scale, suggestion scale, and 5th row height. MaterialButtonToggleGroup for keyboard mode (4-row, 5-row, 5-row full) and hint display mode.
    - **Input Behavior**: Comprehensive settings for auto-capitalization, suggestions, auto-complete, recorrection, caps lock, shift lock modifiers, and more.
    - **Gestures**: Dropdown menus for swipe actions (up/down/left/right) and volume key actions with 11 gesture options.
    - **Feedback**: Sliders for vibration duration and volume with real-time value display.
- **Modifier Key Code Settings**: Added Ctrl, Alt, and Meta key code dropdowns (None, Left, Right) in Input Behavior section.
- **Theme-Specific Accent Colors**: RadioButton toggles use theme-appropriate colors (Storm=Green, Night=Blue, Moon=Pink, Day=Purple).
- **Custom Dropdown Layout**: Created `item_dropdown.xml` for consistent Material 3 styled dropdown menus.
- **Vector Tab Icons**: Added Material Design icons for all settings tabs (palette, language, visibility, keyboard, gesture, vibration).
- **Color Preview Drawables**: Created rounded rectangle drawables for theme color previews.

### Changed
- **Day Theme Material 3 Colors**: Fixed color relationships - surface now lighter than background, using blue as primary color for better visibility.
- **Day Theme Keyboard Style**: Changed `kbdColorMod` from `bg_dark` to `terminal_black` for better key differentiation.
- **Dropdown Adapters**: Each dropdown now uses its own adapter instance to prevent sharing issues.
- **MaterialButtonToggleGroup**: Added `selectionRequired="true"` to prevent visual deselection when clicking already-selected buttons.
- **Info Cards**: Changed hardcoded `@color/tn_green` stroke to theme-aware `?attr/colorPrimary`.
- **Loading Spinner**: Now uses theme-aware `colorPrimary` instead of hardcoded storm green.

### Fixed
- **Tick-in-Circle Indicator**: Removed checkmark icon from theme and language cards by setting `android:checkable="false"` and `app:checkedIcon="@null"`.
- **Toggle Color Reset**: Fixed MaterialSwitch not resetting to default colors when toggled off - now properly uses ColorStateList with both checked and unchecked states.
- **Card Stroke Reset**: Fixed card borders not resetting to theme's `colorOutline` when deselected.
- **Storm Keyboard UI Colors**: Reverted accidentally changed Storm variant colors to original values.

## [Visual Modernization & Cleanup] - 2026-01-01

### Added
- **Modern Vector Icons**: Implemented a professional, "AnySoft" inspired icon set for key functional elements.
    - **Shift**: Clean outline-style arrow.
    - **Shift Locked**: Matching outline arrow with a detached "locked" bar.
    - **Backspace**: Modern "tag" shape with a centered 'X'.
    - **Enter**: Minimalist L-shaped return arrow.
    - **Settings**: Solid, modern gear icon.
    - **Search**: Refined magnifying glass vector.
    - **Mic**: Updated microphone silhouette.
    - **Tab**: Modern alignment icon.
- **Vector Feedback System**: Created/Updated `sym_keyboard_feedback_*.xml` resources for all key interactions, wrapping vector drawables in `<layer-list>` to ensure proper scaling and tinting support, replacing the legacy bitmap-based feedback system.

### Changed
- **Icon Styling**:
    - **Stroke Engine**: Switched vector paths to use white strokes (`@android:color/white`) to allow for reliable runtime tinting via `?attr/kbdColorText` and `?attr/kbdColorAccent`.
    - **Accent Integration**: Applied the theme's accent color (Purple/Magenta) to Shift, Shift Locked, and Delete keys for better visual hierarchy.
    - **Geometry Alignment**: Synchronized the dimensions and positioning of Shift and Shift Locked icons to ensure no visual jumping occurs when toggling states.
- **Resource Cleanup**:
    - **Bitmap Purge**: Deleted dozens of legacy `.png` assets for old keyboard themes and feedback icons, significantly reducing the app's resource footprint.
    - **Layout Consolidation**: Removed unused legacy layouts (`bubble_text.xml`, `voice_punctuation_hint.xml`, `voice_swipe_hint.xml`).

### Fixed
- **Build System Restoration**: Recreated `null_layout.xml` (as a `<merge />` tag) to resolve compilation errors where the build system expected this resource for specific device configurations.
- **Missing Resource Links**: Resolved multiple "cannot find symbol" errors in `LatinKeyboard.java` by ensuring all referenced feedback drawables were correctly implemented as XML vectors.

## [Tokyo Night Edition] - 2025-12-30

### Added
- **True Seamless Popups**: Implemented `SeamlessPopupDrawable`, a custom vector drawer that renders the key and popup as a single continuous shape, replacing the previous "patch" overlay solution for pixel-perfect connectivity.
- **Adaptive Connection Logic**: Implemented intelligent path generation that handles misalignment and tight spaces using adaptive Bezier curves and snap-to-grid logic (`SeamlessPopupDrawable.java`).
- **Precision Visuals**:
    - **Snap Logic**: Added sub-pixel snapping to ensure vertical strokes are perfectly straight when keys are aligned.
    - **Adaptive Fillets**: Dynamic corner radius calculation to prevent "too round" connections, ensuring a crisp "square-round" aesthetic matching the keycaps.
    - **Modifier Integration**: Updated `LatinKeyboardBaseView` to dynamically resolve `kbdColorMod` or `kbdColorAlpha` for seamless popups, ensuring proper blending for Shift/Ctrl/Alt keys.
- **CI/CD Infrastructure**:
    - **GitHub Actions Pipeline**: Implemented automated builds via `.github/workflows/android.yml` that generate unsigned APK artifacts on every push to `master`.
    - **Termux Environment Sanitization**: Added dynamic `sed` logic to the workflow to bypass Termux-specific `aapt2` and `buildToolsVersion` overrides, ensuring cloud builds are portable.
    - **Automated Release Triggers**: Configured tag-based triggers for automated GitHub Releases with generated release notes.
- **Seamless Geometry**: Updated `popup_container_tokyonight.xml` with full 8dp rounded corners and restored strokes for a polished "Pill" container that merges into the keyboard tray.

### Changed
- **Visual Refinement**: Adjusted `SeamlessPopupDrawable` stroke rendering (inset by half stroke width) to prevent clipping and ensure consistent stroke weight.
- **Curve Perfection**: Replaced approximate Bezier curves (`quadTo`) with perfect circular arcs (`arcTo`) in `SeamlessPopupDrawable` for mathematically correct rounded corners.
- **Unified Font Sizing**: Harmonized text sizes between character previews and key labels (standardized at 24sp) for a more consistent typographic weight.
- **Breathe Design Refinement**: Optimized `btn_key_tokyonight.xml` insets to guarantee a strict 4dp visual gutter across all key types.
- **Popup Animation Polish**: Fine-tuned `popup_enter_modern.xml` with better scaling and alpha transitions for a more fluid "portal" appearance.

### Fixed
- **Modifier Stroke Ghosting**: Resolved a persistent visual bug where a green border stroke appeared on sticky modifier keys (Shift, Ctrl, Alt). Standardized `LatinKey` logic to ensure all functional keys correctly inherit non-stroke styles from the Tokyo Night design system.
- **The Floating Gap**: Resolved the visual disconnect between popups and keys by increasing vertical overlap and using the patch system to hide the separator line.
- **Horizontal Drifting**: Corrected the horizontal alignment logic in `LatinKeyboardBaseView.java` to ensure mini-keyboards are perfectly centered or edge-aligned with their parent keys.
- **Sub-pixel Aliasing**: Increased the connection patch height to 6dp to safely mask the 2dp border stroke and prevent "line ghosting" artifacts.
- **Double Corner Glitch**: Fixed an issue where long popups extending to the left would render disjointed connection curves by implementing dynamic radius scaling for tight spaces.
- **Diagonal Start Cut**: Resolved a visual artifact where the path starting point created a diagonal line at the bottom-left corner of the key connection.

## [Tokyo Night Edition] - 2025-12-26

### Added
- **Modernization Phase II Implementation**:
    - **Visual Breathe Design**: Increased key gutters to `4dp` (using `2dp` insets) and corner radii to `8dp`, transforming the dense 5-row layout into a clean, premium "tile" grid.
    - **Tonal Elevation**: Deepened the contrast by darkening the tray backgrounds (`kbdColorBase`) across Storm, Night, and Moon variants.
    - **Pill Geometry**: Compressed popup dimensions to a compact `60dp` Pill shape for a professional, modern overlay aesthetic.
    - **Spring Physics**: Implemented bouncy `overshoot` interpolators for popup animations, providing tactile and responsive feedback.
    - **Portal Transition**: Added dynamic key dimming logic where the underlying key on the keyboard tray fades when its popup is active, creating a visual "portal" effect.
    - **Unified Mini-Keyboard Box**: Created a dedicated `popup_container_tokyonight.xml` for long-press variations, eliminating the "notched" border bug and creating a cohesive unified tray.
    - **Designer Typography**: Integrated **Google Sans Code** monospace font across the entire keyboard, including key labels, suggestions strip, and spacebar language markers.
    - **Dynamic Font Loading**: Implemented a centralized font management system in `LatinKeyboardBaseView` that loads custom assets with automatic fallback to system monospace.
    - **Data-Oriented Architecture**: Decoupled UI components from hardcoded color values. Introduced a semantic attribute system (`attrs.xml`) that maps roles (Base, Alpha, Mod, Highlight, Accent, Popup) to theme-specific colors.
    - **Unified Component System**: Implemented `btn_key_tokyonight.xml` (Universal Key Drawable), `preview_tokyonight_dynamic.xml` (Universal Preview), and `popup_tokyonight_dynamic.xml` (Universal Shape).
    - **Dynamic Theme Wrapping**: Modified `KeyboardSwitcher.java` to use `ContextThemeWrapper`, enabling the keyboard to swap its entire color palette at runtime without layout duplication.
    - **Surgical Vector Tinting**: Transitioned all keyboard symbols (`sym_keyboard_*.xml`) to use `?attr/kbdColorText` for perfect theme adherence.
    - **Technical Iconography**: Replaced text-based "Ctrl" and "Alt" labels with minimalist, modern vector icons.
    - **Mechanical Glow**: Implemented a dynamic **Tokyo Night Orange Glow** (`#FF9E64`) for active technical modifiers (Ctrl, Alt, Shift), simulating a backlit mechanical keycap feel.

### Fixed
- **Indicator Cleanup**: Removed the legacy orange bottom line indicator from `btn_key_tokyonight.xml` in favor of the cleaner, more professional Icon Glow system.
- **The Boxy Corner Bug**: Successfully eliminated the persistent dark rectangular artifact behind rounded popups by transitioning to a `layer-list` with explicit transparent base layers and purging theme-inherited backgrounds.
- **Optical Centering**: Fine-tuned vertical character alignment within the Pill geometry by adjusting asymmetric padding to compensate for font baseline descenders.
- **Abnormal Popup Behavior**: Corrected the `.` key popup by prioritizing character labels over hint icons in the preview window.
- **Latency Neutralization**: Set artificial popup delays to `0ms` for instantaneous user feedback.
- **Missing Dependency**: Resolved a compilation error by adding the `android.text.TextUtils` import to `LatinKeyboardBaseView.java`.

### Changed
- **Monolithic Geometry**: Refined Storm and Moon palettes to use consistent "Surface Tones," creating a unified visual slab effect.
- **Unified Selectors**: Replaced 14+ legacy `input_*.xml` layouts with 4 unified, attribute-driven layout files.
- **Typographic Scaling**: Slightly reduced global font sizes to create internal whitespace within keys, preventing a cramped appearance in high-density layouts.

## [Tokyo Night Edition] - 2025-12-25

### Added
- **Official Color Source**: Integrated `tokyonight.nvim` repository as the single source of truth for palettes.
- **Surgical Highlights**: Implemented targeted coloring for arrow keys (Purple/Magenta) without flooding the background of other modifiers.
- **Modern Fluid Popups**: Implemented a new `popup_enter_modern.xml` animation with scale, slide, and fade effects.
- **Tokyo Night Design System**: Full implementation of Storm, Night, Day, and Moon palettes.
- **Neon Aesthetic**: Added Tokyo Night green accents to Storm theme popups and previews.
- **Monospace Typography**: Forced `Typeface.MONOSPACE` and `Typeface.BOLD` for all key labels.
### Changed
- **Popup Physics**: Adjusted `key_preview_offset` to a negative value (-8dp) so popups extend upwards from the key top.
- **Vertical Tab Aesthetic**: Set popup height to 80dp for a more elegant "floating tab" appearance.
- **Pixel-Perfect Geometry**: Set all key gaps to `0dp` and used `1dp` insets to create a consistent `2dp` gutter.
- **Tonal Elevation**: Refined all palettes so Alphas, Modifiers, and Backgrounds use cohesive surface tones.
- **Refined Icons**: Updated the Shift Lock icon and other symbols to match the modern aesthetic.
- **Day Theme Fixes**: Corrected color alignment for the Day theme to ensure high contrast and proper "light mode" feel.

### Fixed
- **Purple Flood Fix**: Reverted `tn_storm_mod` to a professional dark tone, isolating the purple accent specifically to symbols.
- **Popup Geometry**: Fixed the black/boxy popup quirkiness with rounded `12dp` corners and vibrant strokes.
- **Selector Logic**: Corrected `btn_key_*.xml` selectors to point to their respective theme assets (Day, Moon, Night) instead of defaulting to Storm.
- **Variant Swap**: Fixed a bug where Storm and Moon backgrounds were swapped.

### Removed
- **Legacy Assets**: Purged all old Gingerbread, ICS, and Stone theme files and bitmap (`.9.png`) assets.

---
