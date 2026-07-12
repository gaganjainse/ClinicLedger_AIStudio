# VillageClinicLedger

An Android app for managing patient records and clinic operations in village healthcare settings. Built with Kotlin and Material 3 (XML layouts + ViewBinding).

## Features

- Patient registration and record management
- Village-wise patient grouping
- Family accounts and aliases
- Voice input with Hindi/English speech recognition
- Offline-first local storage (Room database)
- Backup and restore functionality (JSON export/import)
- Analytics dashboard (today/week/month summaries)
- Hindi number parsing and TTS

## Tech Stack

- **Language:** Kotlin 2.0.21
- **UI:** Material 3, XML layouts, ViewBinding (no Compose)
- **Architecture:** MVVM with LiveData/ViewModel
- **Database:** Room (KSP/kapt)
- **Background:** WorkManager (daily backup)
- **Async:** Kotlin Coroutines + Flow
- **Build:** Gradle (Kotlin DSL for settings, Groovy for app)

## Architecture Highlights

```
app/
├── data/
│   ├── local/          # Room DAOs, Database, Entities, Converters
│   ├── models/         # Patient, Village, Alias, Transaction, FamilyGroup
│   ├── repository/     # PatientRepository (LiveData + coroutines)
│   └── util/           # DataSeeder
├── service/
│   ├── BackupService.kt    # WorkManager daily backup
│   ├── BackupWorker.kt     # JSON export/import worker
│   └── VoiceService.kt     # Foreground service (stub)
├── ui/
│   ├── MainActivity.kt         # Search + bottom nav + voice bar
│   ├── PatientDetailFragment.kt
│   ├── SearchFragment.kt
│   ├── AnalyticsActivity.kt
│   ├── BackupActivity.kt
│   └── villages/               # VillageManagementActivity
├── voice/
│   ├── ConversationStateMachine.kt   # Pure-Kotlin state machine (testable)
│   ├── VoiceInputSheet.kt            # BottomSheetDialogFragment (UI)
│   ├── VoiceIntentParser.kt          # NLP for Hindi/English commands
│   ├── HindiNumberConverter.kt       # Parse/format Hindi numbers
│   └── VoiceTtsManager.kt            # Text-to-speech wrapper
└── util/LayoutScaler.kt             # Screen-size adaptive scaling
```

## Building

```bash
./gradlew assembleDebug
```

Requires JDK 17 and Android SDK 34.

## License

Apache 2.0 - see [LICENSE](LICENSE).