# CLAUDE.md
!!! CRITICAL: always use system gradle. never use ./gradlew
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

This is an Android project managed by Gradle. Use the system `gradle` command for all commands.

### Build Commands
- `Build APK (Debug)`: `gradle assembleDebug`
- `Build APK (Release)`: `gradle assembleRelease`
- `Clean Build`: `gradle clean` !don't use it

### Test Commands
- **Run all unit tests**: `gradle test`
- **Run all unit tests (verbose)**: `gradle test --info`
- **Run specific test class**: `gradle test --tests ModifierKeyStateTest`
- **Run specific test method**: `gradle test --tests ModifierKeyStateTest.testOnPress_transitionsToPressing`
- **Run test suite**: `gradle test --tests AllTestsSuite`
- **Run with coverage**: `gradle testDebugUnitTest --info`

### Test Structure
- **Unit Tests (JUnit + Robolectric)**: `app/src/test/java/`
  - `state/` - State management tests (ModifierKeyState, TextEntryState)
  - `composition/` - Word composition tests (WordComposer)
  - `model/` - Data model tests (Keyboard, Key)
  - `input/` - Input detection tests (KeyDetector, PointerTracker)
- **Instrumentation Tests**: `app/src/androidTest/java/`

### Test Coverage
**Status**: ✅ **118 tests passing** (Pure JVM - no Robolectric)

Current test coverage focuses on Kotlin migration safety:
- ✅ ModifierKeyState (25 tests - state machine, chording logic)
- ✅ TextEntryState (45 tests - state transitions, session management)
- ✅ WordComposer (48 tests - composition, capitalization, backspace)
- 🔲 Keyboard (XML parsing, geometry - TODO)
- 🔲 KeyDetector implementations (touch mapping - TODO)
- 🔲 IME integration smoke tests (TODO)

See [TEST_RESULTS.md](TEST_RESULTS.md) for detailed test report.

## Code Architecture

Hacker's Keyboard is a specialized Android Input Method Editor (IME) focusing on power-user features and modern Tokyo Night aesthetics.

### Core Components
- **Input Method Service**: `org.pocketworkstation.pckeyboard.LatinIME` is the main entry point for the keyboard service. It manages the lifecycle, settings changes, and handles input events.
- **Keyboard View Logic**:
    - `LatinKeyboardView` & `LatinKeyboardBaseView`: Handle the rendering and touch interaction for the keyboard.
    - `KeyboardSwitcher`: Manages switching between different keyboard layouts (e.g., QWERTY, symbols, different languages).
- **Rendering Engine**:
    - `org.pocketworkstation.pckeyboard.graphics.SeamlessPopupDrawable`: A custom engine that renders keys and popups as a single continuous shape for a modern UX.
- **Theme System**:
    - `org.pocketworkstation.pckeyboard.TokyoNightPalette`: Defines the color schemes for the Tokyo Night variants (Storm, Night, Moon, Day).
- **UI & Settings**:
    - `org.pocketworkstation.pckeyboard.MaterialMainActivity`: The main app entry point (Welcome/About page).
    - `org.pocketworkstation.pckeyboard.MaterialSettingsActivity`: Handles keyboard configuration using modern Material 3 components.

### Package Structure
- `org.pocketworkstation.pckeyboard`: Core IME logic and activities.
- `org.pocketworkstation.pckeyboard.graphics`: Custom drawing and animation logic.
- `org.pocketworkstation.pckeyboard.material`: Material Design specific UI components.
