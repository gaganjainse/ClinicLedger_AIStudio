# Village Clinic Ledger (ग्राम क्लिनिक लेजर)

An offline-first, voice-assisted clinic ledger and patient memory database designed specifically for rural clinic doctors in India. This application serves as an ironclad financial and medical audit trail, allowing doctors to manage patient histories, balance dues, and families hands-free using natural language spoken commands.

---

## 🎨 Visual Identity & Core Interface

| Home / Search with Quick Lookups | Voice Input & State Machine Sheet |
| :---: | :---: |
| Centered around a **"Mic-as-King"** persistent navigation bar. Quick cards instantly display patient aliases, localized village names, pending dues, recent activity notes, and last visit dates. | Triggered instantly from any screen. Handles speech-to-text processing, fuzzy lookups, and transactional intents completely offline. |

---

## 🚀 Key Architectural Capabilities

### 1. Clinic Memory Paradigm
The ledger eliminates the need for manual navigation to answer simple inquiries. The **Search Screen** functions as a live dashboard showing:
- **Fuzzy Search & Aliases**: Patients are instantly discoverable by their real name, regional spelling variations, or custom nicknames (stored in the `Alias` entity).
- **Embedded Clinic Context**: Running balances, custom doctor notes, parent-child family groupings, and exact "Last Visited" timestamps are surfaced directly on patient list cards.

### 2. "Mic-as-King" Voice Assistant
- **Persistent Accessibility**: The bottom mic bar is universally available across all views in the App. Tapping it opens the `VoiceInputSheet` overlay.
- **Robust Conversation State Machine**: Drives an interactive, non-blocking conversational loop:
  $$\text{IDLE} \longrightarrow \text{LISTENING} \longrightarrow \text{PROCESSING} \longrightarrow \text{CONFIRMING} \longrightarrow \text{SAVING} \longrightarrow \text{DONE}$$
- **Speech Confirmation Card**: A high-visibility confirmation panel displays the parsed parameters (Patient, Action, Amount, Remarks) with tactile **Haan (Confirm)** and **Badlo (Edit/Cancel)** controls.

### 3. Localized TTS & Hindi Number Engine
- **Voice Readbacks (TTS)**: Uses a dedicated `VoiceTtsManager` utilizing Android's TextToSpeech API configured for Hindi (`hi-IN`) and English to read aloud running balances (e.g., *"Ramesh, naya baki do sau pachas rupaee"*).
- **Smart Dialect Number Parser**: Integrates a parser (`HindiNumberConverter`) converting complex Hindi and Hinglish numeric phrases into exact integer amounts:
  - *"Dhai Sau"* (ढाई सौ) $\rightarrow$ `250`
  - *"Dedh Hazaar"* (डेढ़ हजार) $\rightarrow$ `1500`
  - *"Sawa Teen Sau"* (सवा तीन सौ) $\rightarrow$ `350`
  - Standard numeral words (*"pachas"*, *"bavan"*, *"assi"*) mapped directly to their mathematical equivalents.

### 4. Smart Intent Parsing
The app intercepts natural voice phrases, processes them locally without network dependencies, and translates them into specific actions:
- **Search Intent**: *"Ramesh ko dikhao"* $\rightarrow$ Opens Ramesh's Detail Screen.
- **Medicine Entry**: *"Ravi ko ek sau pachas ki dawa di"* $\rightarrow$ Creates a `Medicine` transaction of `150` for Ravi.
- **Payment Entry**: *"Amit ne do sau rupaee jama kiye"* $\rightarrow$ Creates a `Payment` transaction of `200` for Amit.
- **Disambiguation Flow**: If a name matches multiple patients, the system reads aloud: *"Do Ramesh mile hain — kaun sa village?"* and displays a selection list.

### 5. Type-Safe Jetpack Compose Navigation
Migrated from rigid, manual state-based views to the modern **Jetpack Navigation Compose** library:
- Centralized `NavHost` in `MainActivity.kt`.
- Clean, serializable `Screen` routes with type-safe parameters (e.g., passing patient ID and name).
- Built-in transitions supporting deep back-stack pop behaviors.

### 6. Ironclad Financial Audit Trail
- **Strict Running Balance**: All transactions (`Medicine`, `Payment`, `Correction`, `Initial`) are logged in a single database ledger.
- **No Deletions Allowed**: Corrections are added as adjustment entries with a timestamped reason, preserving a clear, professional audit record for the clinic's accounts.

---

## 🗄️ Room Database Schema

The app runs on a highly structured local SQLite database managed via Room, consisting of five core entities to represent rural medical practices accurately:

```
  +---------------+        1        +-------------+
  |    Village    | <-------------> |   Patient   |
  +---------------+                 +-------------+
  | id (PK)       |                 | id (PK)     | <---+
  | name          |                 | name        |     |
  | createdAt     |                 | phone       |     | 1
  +---------------+                 | villageId   |     |
                                    | familyId    |     |
  +---------------+                 | notes       |     |
  |  FamilyGroup  |                 | createdAt   |     |
  +---------------+                 +-------------+     |
  | id (PK)       |                        |            |
  | name          |                        | 1          |
  | createdAt     |                        v            |
  +---------------+                 +-------------+     |
                                    | Transaction |     |
  +---------------+                 +-------------+     |
  |     Alias     | 1               | id (PK)     |     |
  +---------------+                 | patientId   | ----+
  | id (PK)       |                 | type        |
  | patientId     | --------------> | amount      |
  | name          |                 | notes       |
  +---------------+                 | date        |
                                    +-------------+
```

1. **`Patient`**: Stores name, contact number, assigned village, notes, family group, and creation timestamp.
2. **`Village`**: Holds village registries (with localized and standard names) to easily segment patients.
3. **`Alias`**: Maps colloquial nicknames or phonetic variations back to the primary patient profile.
4. **`Transaction`**: The transaction ledger. Tracks historical records (`Medicine`, `Payment`, `Correction`) with precise decimals, notes, and dates.
5. **`FamilyGroup`**: Couples family members together to support unified billing and multi-patient summaries.

---

## 📂 Project Package Structure

```
app/src/main/java/com/villageclinicledger/
│
├── data/
│   ├── local/              # Room Database, DAOs (Patient, Village, Transaction, Alias, FamilyGroup)
│   ├── models/             # Entity models (Patient, Village, Transaction, Alias, FamilyGroup)
│   └── repository/         # Combined PatientRepository for transactional operations
│
├── ui/
│   ├── search/             # SearchViewModel (handling state, query filtering, and listing)
│   ├── util/               # LocaleManager, HindiNumberConverter, VoiceIntentParser, VoiceTtsManager
│   └── compose/            # High-fidelity Jetpack Compose screens
│       ├── SearchScreen.kt         # Live Dashboard (Mic bar, patient list, memory tags)
│       ├── AddPatientScreen.kt     # Full-screen dedicated form with Village dropdown selectors
│       ├── PatientDetailScreen.kt  # Patient profiles, financial ledger, and note inputs
│       ├── VoiceInputSheet.kt      # State-machine driven speech-to-text overlay
│       ├── AnalyticsScreen.kt      # Weekly/Monthly revenue, top balances, village data
│       └── BackupScreen.kt         # WorkManager automation, backup audits, JSON utilities
│
└── MainActivity.kt         # App Entry, NavHost builder, and persistent top/bottom app bars
```

---

## ⚡ Setup, Building, and Testing

### Prerequisites
- Android SDK 34+
- Android Studio Ladybug or newer
- Kotlin 1.9.0+

### Local Build Instructions
To compile the debug APK:
```bash
gradle :app:assembleDebug
```

### Running Unit and Architecture Tests
The codebase includes local JVM unit tests and JVM-based Robolectric verification:
```bash
gradle :app:testDebugUnitTest
```

---

## 🌐 Localization Support
Fully localized string resources cleanly separate text translations:
- **English (`res/values/strings.xml`)**: Clean, clinical terminology.
- **Hindi (`res/values-hi/strings.xml`)**: Locally relevant Hindi terms.
- Settings Screen allows a seamless, persistent one-tap toggle between English and Hindi, immediately updating the localized schema references and Speech Recognizer configurations.
