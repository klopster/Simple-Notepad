# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug       # Build debug APK
./gradlew build               # Full build (debug + release)
./gradlew clean               # Clean build outputs
./gradlew test                # Run unit tests (JVM)
./gradlew connectedAndroidTest  # Run instrumented tests (requires device/emulator)
./gradlew lint                # Run lint checks
```

## Architecture

Single-activity app using Jetpack Compose with no XML layouts.

**Data flow:** All UI state is held in `remember { mutableStateOf(...) }` locals within composables — there is no ViewModel layer. State is persisted via `FileRepo` (a singleton object) which reads/writes a single `notes_db.json` file in the app's internal storage using `kotlinx-serialization`.

**Data model** (defined in `FileRepo.kt`):
```
Database → topics: List<Topic> → notes: List<Note>
```
Each `Topic` has an `id`, `name`, and list of `Note`s. Each `Note` has `id`, `text`, `date` (dd/MM/yyyy), and optional `desc`.

**Navigation** uses `NavHost` in `MainActivity.kt` with three routes:
- `home` — topic list with search
- `notes/{topicId}` — notes within a topic
- `desc/{topicId}/{noteId}` — full-screen description editor

**All composables** live in `MainActivity.kt`. There is no separate screen/composable file — the entire UI is in one file (~691 lines).

**PDF export** uses `com.itextpdf:itextg` (iText 5). Exported files are shared via `FileProvider` (paths configured in `res/xml/file_paths.xml`).

## Key Files

- `app/src/main/java/com/example/simplenotepad/MainActivity.kt` — all UI composables and app logic
- `app/src/main/java/com/example/simplenotepad/data/FileRepo.kt` — persistence, sorting, pagination, export helpers
- `app/src/main/java/com/example/simplenotepad/ui/theme/Theme.kt` — Material 3 theme (green primary `#4CAF50`)
- `gradle/libs.versions.toml` — centralized dependency version catalog
- `app/build.gradle.kts` — minSdk 26, targetSdk 36, JVM 11

## Tech Stack

- Kotlin 2.0.21, Android Gradle Plugin 8.13.0
- Jetpack Compose (BOM 2024.09.00), Material 3
- Navigation Compose 2.8.2
- kotlinx-serialization-json 1.7.3
- iTextG 5.5.10 (PDF generation)
