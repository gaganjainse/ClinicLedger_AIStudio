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
- Daily auto-backup via WorkManager

## Tech Stack

- **Language:** Kotlin 2.0.21
- **UI:** Material 3, XML layouts, ViewBinding (no Jetpack Compose)
- **Architecture:** MVVM with LiveData/ViewModel + Repository pattern
- **Database:** Room (kapt)
- **Background:** WorkManager (daily backup scheduling)
- **Async:** Kotlin Coroutines + Flow
- **Speech:** Android SpeechRecognizer + TextToSpeech
- **Build:** Gradle (Kotlin DSL for settings, Groovy for app module)

## Project Structure

```
app/
├── data/
│   ├── local/          # Room DAOs, Database, Entities, TypeConverters
│   ├── models/         # Patient, Village, Alias, Transaction, FamilyGroup
│   ├── repository/     # PatientRepository (LiveData + coroutines)
│   └── util/           # DataSeeder
├── service/
│   ├── BackupService.kt    # WorkManager daily backup
│   ├── BackupWorker.kt
│   ├── VoiceService.kt     # Foreground service stub
│   └── ...
├── ui/
│   ├── analytics/      # AnalyticsActivity
│   ├── backup/         # BackupActivity
│   ├── patientdetail/  # PatientDetailFragment, TransactionDialogFragment
│   ├── search/         # SearchFragment
│   ├── villages/       # VillageManagementActivity
│   ├── MainActivity.kt
│   └── util/           # LayoutScaler
├── voice/
│   ├── ConversationStateMachine.kt  # Pure-Kotlin state machine
│   ├── VoiceInputSheet.kt           # BottomSheetDialogFragment + SpeechRecognizer
│   ├── VoiceIntentParser.kt         # NLU parser (Hindi/English)
│   ├── HindiNumberConverter.kt      # Parse/generate Hindi numbers
│   ├── VoiceTtsManager.kt           # TTS wrapper
│   └── ...
├── VillageClinicLedgerApp.kt        # Application class
├── VillageClinicLedgerProvider.kt   # ContentProvider for Room
└── ...
```

## Architecture Highlights

- **ConversationStateMachine** – Pure Kotlin, zero Android deps. Contains all voice-flow logic (IDLE → LISTENING → PROCESSING → CONFIRMING → SAVING → DONE/ERROR). Unit-testable without Robolectric.
- **VoiceIntentParser** – Deterministic NLU for mixed Hindi/English commands. Extracts patient name, village, medicine amount, payment amount, and intent type.
- **HindiNumberConverter** – Bidirectional: `parseHindiNumber("डेढ़ सौ") → 150.0`, `convertShort(150.0) → "डेढ़ सौ"`. Handles fractional words (सवा, डेढ़, ढाई, साढ़े, पौने, आधा, पाव).
- **TransactionDialogFragment** – Reusable dialog for medicine/payment/adjustment entries with live balance preview.
- **WorkManager backup** – Scheduled daily, retains 30 days, versioned JSON with migration checks.

## Building

```bash
./gradlew assembleDebug
```

## Testing

```bash
# Unit tests (JVM)
./gradlew testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

Test coverage currently includes:
- `VoiceIntentParserTest.kt` – 26 assertions covering amount extraction, patient/village name parsing, intent detection
- `HindiNumberConverterTest.kt` – 19 assertions covering parse/convert round-trips, fractional words, edge cases

## License

Apache 2.0 - see [LICENSE](LICENSE).