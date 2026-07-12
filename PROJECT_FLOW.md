# Village Clinic Ledger — Project Flow

> **Vision**: A talking ledger for a village doctor. Like Paytm soundbox — listen, understand, speak back, show card.  
> **Target User**: Dr. Manoj Kumar Jain (single doctor, village clinic, Siras)  
> **Platform**: Android APK (offline-first)  
> **Core Problem**: Replace 15-20 year diary habit with something faster, more reliable, and voice-enabled

---

## Build Phases

| # | Phase | Status | Description |
|---|-------|--------|-------------|
| 0 | **Foundation** | ✅ Done | Android project, Room DB, Material 3, Hindi+English, 4 entities (Patient, Village, Alias, Transaction) |
| 1 | **Core Ledger** | ✅ Done | Patient CRUD, transactions, running balance, search, village dropdown, audit trail (no deletes) |
| 2 | **Backup & Trust** | ✅ Done | JSON export/import, auto daily backup via WorkManager, version validation |
| 3 | **Family Accounts** | ✅ Done | `FamilyGroup` entity, `familyGroupId` on Patient, family display on detail screen |
| 4 | **Voice Search** | ✅ Done | Mic icon on search bar, `SpeechRecognizer`, RECORD_AUDIO permission, Hindi language preference |
| 5 | **Voice Query → Balance** | ✅ Done | Speaking a name shows balance dialog + "Open" button |
| 6 | **Analytics** | ✅ Done | Today/week/month summaries, top patients, village breakdown |
| 7 | **Auto Backup** | ✅ Done | Daily WorkManager backup, 30-day cleanup, status in Backup UI |
| 8 | **Voice Conversation Engine** | ✅ Done | `HindiNumberConverter`, `VoiceIntentParser`, `VoiceTtsManager`, `VoiceInputSheet` (full state machine) |
| 9 | **TTS Voice Output** | ✅ Done | Android TextToSpeech (Hindi), speaks balance/confirmations aloud via `VoiceTtsManager` |
| 10 | **Hindi Number Parser** | ✅ Done | "dhai sau" → 250, "dedh hazaar" → 1500, parsing + speech generation |
| 11 | **Smart Intent Parser** | ✅ Done | Detect search/medicine/payment/new/correction/confirm from natural speech |
| 12 | **Mic-as-King Home** | ✅ Done | Persistent bottom voice bar in MainActivity, opens `VoiceInputSheet` from any screen |
| 13 | **Fuzzy Patient Matching** | ✅ Done | Name + alias search in `findPatientByVoice`, DAO `findPatientsByNameOrAlias` |
| 14 | **Conversation State Machine** | ✅ Done | IDLE → LISTENING → PROCESSING → CONFIRMING → SAVING → DONE in `VoiceInputSheet` |
| 15 | **Disambiguation Flow** | 🔴 Not Started | "Do Ramesh hain — kaun sa?" |
| 16 | **Family-as-Primary Redesign** | 🔴 Not Started | Make Family the top-level entity in search/UI |

---

## Current Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     ANDROID APP (Kotlin)                     │
├─────────────────────────────────────────────────────────────┤
│  UI Layer                                                   │
│  ├── SearchFragment        (voice search, recent, FAB)      │
│  ├── PatientDetailFragment (profile, transactions, family)  │
│  ├── MainActivity          (add patient dialog, menu)       │
│  ├── AnalyticsActivity     (today/week/month stats)         │
│  ├── BackupActivity        (export/import/auto status)      │
│  ├── VillageManagementActivity (add/edit/delete villages)   │
│  └── Settings (via menu)                                    │
├─────────────────────────────────────────────────────────────┤
│  ViewModel Layer                                            │
│  ├── SearchViewModel       (switchMap search, isLoading)    │
│  └── PatientDetailViewModel (patient, aliases, family)      │
├─────────────────────────────────────────────────────────────┤
│  Repository Layer                                            │
│  └── PatientRepository     (all DAO access, recalculate)    │
├─────────────────────────────────────────────────────────────┤
│  Data Layer                                                 │
│  ├── PatientDao / VillageDao / AliasDao / TransactionDao    │
│  ├── FamilyGroupDao        (family grouping)                │
│  ├── VillageClinicLedgerDatabase (Room, FK, migrations)     │
│  └── Entities: Patient, Village, Alias, Transaction,        │
│               FamilyGroup                                   │
├─────────────────────────────────────────────────────────────┤
│  Services                                                   │
│  ├── BackupWorker          (WorkManager daily backup)       │
│  └── BackupService         (scheduler)                      │
└─────────────────────────────────────────────────────────────┘
```

---

## Screens (7 total — aligned with Antigravity spec)

| Screen | Status | Antigravity Ref | Notes |
|--------|--------|-----------------|-------|
| 1. **Home / Search** | ✅ Done | "Mic is King" | Recent patients, search bar, balance in results, FAB for quick entry |
| 2. **Listening / Voice Input** | ✅ Done | Flows 1-8 | `VoiceInputSheet` with full state machine, persistent bottom voice bar, all 6 flows + confirmation |
| 3. **Voice Confirmation Card** | ✅ Done | "Sahi Hai? / Badlo?" | `showConfirmationCard` + TTS "Sahi hai?" + Haan/Nahi buttons + voice loop |
| 4. **Patient Detail** | ✅ Done | Profile + history | Shows name, village, phone, balance, aliases, transactions, family |
| 5. **Manual Entry** | ✅ Done | Fallback forms | Medicine/Payment/Adjustment dialogs with amount, notes, reason |
| 6. **Analytics** | ✅ Done | Stats dashboard | Today/week/month collections, top patients, village breakdown |
| 7. **Settings / Backup** | ✅ Done | Villages + backup | Village CRUD, export/import, auto backup status |

---

## Data Model (current state)

### Patient
| Field | Status | Notes |
|-------|--------|-------|
| `id` | ✅ | PK auto |
| `name` | ✅ | |
| `villageId` | ✅ | FK → Village |
| `phone` | ✅ | Optional |
| `familyGroupId` | ✅ | FK → FamilyGroup (nullable) |
| `currentBalance` | ✅ | Denormalized, recalculated from transactions |
| `createdAt` / `updatedAt` | ✅ | |

### FamilyGroup
| Field | Status |
|-------|--------|
| `id` | ✅ |
| `name` | ✅ |
| `headPatientId` | ✅ |
| `villageId` | ✅ |
| `createdAt` | ✅ |

### Transaction
| Field | Status | Notes |
|-------|--------|-------|
| `id` | ✅ | |
| `patientId` | ✅ | FK CASCADE |
| `type` | ✅ | medicine, payment, adjustment |
| `amount` | ✅ | Positive only |
| `notes` | ✅ | |
| `createdAt` | ✅ | Never deleted |

### Alias
| Field | Status |
|-------|--------|
| `id` | ✅ |
| `patientId` | ✅ FK CASCADE |
| `alias` | ✅ |

### Village (8 pre-seeded + custom)
| English | Hindi | Status |
|---------|-------|--------|
| Siras | सिरस | ✅ In DB |
| Mehtabpura | मेहताबपुरा | ✅ |
| Jhilai | झिलाई | ✅ |
| Bassi | बस्सी | ✅ |
| Shyosinghpura | श्योसिंघपुरा | ✅ |
| Mandaliya | मंडालिया | ✅ |
| Nala | नला | ✅ |
| Piplya | पीपल्या | ✅ |

> **Note**: Village model has `name` only — no `nameHindi` field yet. Hindi names stored in strings.xml for UI.

---

## Voice Engine — Current vs Target

### What's Built (Android SpeechRecognizer)
| Capability | Status |
|-----------|--------|
| Mic button on search bar | ✅ |
| Speech → text (English + Hindi) | ✅ |
| Fill search text with recognized name | ✅ |
| Lookup patient balance by spoken name | ✅ |
| Show balance dialog + "Open" button | ✅ |
| Hindi language preference (`hi-IN`) | ✅ |
| SpeechRecognizer instance reuse | ✅ |

### What's Missing (Antigravity Spec)
| Capability | Priority | Description |
|-----------|----------|-------------|
| **Text-to-Speech (Hindi)** | P0 | App speaks: "Ravi par ₹1,450 baki hain" |
| **Number → Hindi converter** | P0 | 1450 → "ek hazaar chaar sau pachaas" |
| **Smart Intent Parser** | P0 | Detect "dawa di" (medicine) vs "diye" (payment) vs "kitna baki" (search) |
| **Medicine+Payment combo** | P1 | "dawa di 300, 200 diye, 100 udhaar" |
| **New patient from voice** | P1 | "Naya patient Chotu, Jhilai se, dedh sau dawa" |
| **Confirmation loop** | P1 | Shows card + TTS "Sahi hai?" → mic waits for "Haan" / "Nahi" |
| **Disambiguation** | P2 | "Do Ramesh hain — kaun sa?" |
| **Correction by voice** | P2 | "Galat ho gaya, hata do" |
| **Conversation state machine** | P2 | listen → parse → speak → confirm → save → loop |
| **Fuzzy phonetic matching** | P2 | Handle speech-to-text errors for village names |

---

## Complete Voice Flows (Target)

### Flow 1: Check Balance
```
Father: "Ravi kitna baki hai?"
  → App: "Ravi par ₹1,450 baki hain. Pichli baar 12 June ko aaye the."
  → Card: Ravi | Outstanding: ₹1,450 | Last Visit: 12 Jun | [Full Details]
```

### Flow 2: Medicine Given
```
Father: "Ramesh ko dawa di, do sau rupaye"
  → App: "Ramesh ki dawa ₹200. Naya baki ₹750. Sahi hai?"
  → Card: Medicine ₹200 | Previous: ₹550 | New Due: ₹750 | [✓ Sahi Hai] [✏️ Badlo]
  → Father: "Haan" or taps ✓
  → App: "Save ho gaya."
```

### Flow 3: Medicine + Partial Payment
```
Father: "Ravi ko dawa di ₹300, ₹200 diye, ₹100 udhaar"
  → App: "Ravi ki dawa ₹300. ₹200 jama. ₹100 udhaar. Kul baki ₹1,550. Sahi hai?"
```

### Flow 4: Payment Only
```
Father: "Ram Lal ne ₹500 diye"
  → App: "Ram Lal se ₹500 jama. Baki ₹950. Sahi hai?"
```

### Flow 5: New Patient
```
Father: "Naya patient Chotu, Jhilai se, dawa di dedh sau"
  → App: "Naya patient Chotu, gaon Jhilai. Dawa ₹150, poora udhaar. Sahi hai?"
```

### Flow 6: Ambiguous Name
```
Father: "Ramesh ko dawa di"
  → App: "Do Ramesh hain. Ramesh Meena (Mehtabpura ₹500 due) ya Ramesh Bairwa (Bassi ₹200 due)? Kaun sa?"
  → Father: "Meena wala"
  → App: "Ramesh Meena. Kitne ki dawa?"
  → Father: "Do sau"
  → App: "Ramesh Meena ki dawa ₹200..."
```

### Flow 7: Correction
```
Father: "Galat ho gaya, hata do"
  → App: "Pichhli entry hata di. Baki vaapis ₹1,450. Sahi hai?"
```

### Flow 8: Confirmation Loop
```
After any card:
  Father: "Haan" / "Sahi hai" / "Theek hai"
    → App: "Save ho gaya."
  Father: "Nahi" / "Galat hai" / "Badlo"
    → App: "Theek hai, phir se bolo." → Mic reactivates
```

---

## Voice Engine Architecture (Target)

```
┌──────────────────────────────────────────────────┐
│                   VOICE INPUT                     │
│         Android SpeechRecognizer (hi-IN/en-IN)    │
│         Raw speech → text transcript              │
├──────────────────────────────────────────────────┤
│                  SMART PARSER                     │
│  1. Intent Detection                              │
│     search | medicine | payment | new | correction│
│  2. Entity Extraction                             │
│     Patient name (fuzzy matched)                  │
│     Village name (fuzzy matched)                  │
│     Amount (Hindi + English numbers)              │
│     Payment amount (if partial pay)               │
│  3. Build structured transaction                  │
├──────────────────────────────────────────────────┤
│                  VOICE OUTPUT                     │
│        Android TextToSpeech (hi-IN)               │
│        Structured data → Hindi sentence           │
│        Speaks confirmation aloud                  │
├──────────────────────────────────────────────────┤
│                  VISUAL CARD                      │
│        Confirmation card on screen                │
│        ✓/✏️ buttons + voice confirm              │
├──────────────────────────────────────────────────┤
│               CONFIRM / EDIT LOOP                 │
│        "Haan" → save | "Nahi" → re-record        │
└──────────────────────────────────────────────────┘
```

---

## Intent Detection Keywords

| Intent | Triggers |
|--------|----------|
| **Search** | kitna, baki, hisaab, dikhao, khata, balance, due, how much |
| **Medicine** | dawa, dawai, tablet, syrup, injection, di, diya, diye, medicine |
| **Payment** | diye, diya, jama, de gaya, paid, payment, paisa diya |
| **New Patient** | naya, nayi, pehli baar, new patient |
| **Correction** | galat, hata do, kat do, sudhar, wrong, cancel, undo |
| **Confirm Yes** | haan, ha, sahi, theek, ok, yes, correct, save karo |
| **Confirm No** | nahi, galat, nhi, badlo, no, wrong, dobara, phir se |

---

## Hindi Number Converter

| Amount | Spoken as |
|--------|-----------|
| ₹100 | "sau rupaye" |
| ₹150 | "dedh sau rupaye" |
| ₹200 | "do sau rupaye" |
| ₹250 | "dhai sau rupaye" |
| ₹500 | "paanch sau rupaye" |
| ₹750 | "saat sau pachaas rupaye" |
| ₹1,000 | "ek hazaar rupaye" |
| ₹1,450 | "ek hazaar chaar sau pachaas rupaye" |
| ₹1,500 | "pandrah sau rupaye" |

---

## Design Principles

1. **Voice-first, manual-second** — Mic is the biggest element on screen
2. **One-thumb, one-hand** — All touch targets ≥48px
3. **Running balance** — No per-visit allocation, simple bank-account style
4. **Audit trail** — Never delete transactions, corrections are adjustments
5. **Search-first** — Fastest action: Open → Search → See Due (<3 seconds)
6. **Balance in search results** — "Secret feature": show ₹ amount directly in search list
7. **Aliases solve family** — 80% of family problem solved with aliases
8. **Quick Add mode** — Rapid entry without forms (like writing in diary)
9. **Hindi speech + English storage** — Accept Hindi/Hinglish, store English
10. **Forgiving search** — Partial matches, phonetic, alias-first

---

## Completed Work Log

| Date | Change |
|------|--------|
| Session 1 | Initial project skeleton with Room, Material 3, Hindi+English support |
| Session 2 | Replace `observeForever` with `switchMap`, fix village lookup, add recent patients |
| Session 3 | Remove transaction deletion UI, add adjustment workflow, add quick entry |
| Session 4 | Remove `DoubleConverter`, enable FK pragma, allow negative adjustments |
| Session 5 | Fix NPE crash, backup FK integrity, lifecycleScope, manifest cleanup, voice search, analytics |
| Session 6 | Hindi i18n, all strings externalized, comments on all files |
| Session 7 | Fix analytics week bug, backup transaction wrap, fullBackupContent fix |
| Session 8 | Make all stubs functional (Provider, BackupService Worker, ViewModel states) |
| Session 9 | Auto backup (WorkManager), voice query→balance, Hindi voice preference |
| Session 10 | Family Grouping (entity, DAO, UI on patient detail) |
| Session 11 | Final cleanup — removed 14 dead DAO methods, unused resources, build optimizations |
| Session 12 | **Voice Engine P0**: `HindiNumberConverter`, `VoiceTtsManager` (Android TTS), `VoiceIntentParser` (8 intent types), DAO `findPatientsByNameOrAlias`, voice search strings, TTS wired into SearchFragment |
| Session 13 | **Voice Conversation P1**: `VoiceInputSheet` (BottomSheetDialogFragment) with full state machine (IDLE→LISTENING→PROCESSING→CONFIRMING→SAVING→DONE), persistent bottom voice bar in `MainActivity`, mic accessible from any screen, 6 voice flows (balance/medicine/payment/combo/new/correction) with Haan/Nahi confirmation |
| Session 14 | **Build Audit + P0 fixes**: Fixed Room kapt `NonExistentClass` (missing `import java.util.Date` in TransactionDao), Provider SQLite→Room API (`SupportSQLiteDatabase` query), removed `Transformations` (removed in lifecycle 2.7.0, replaced with `MediatorLiveData`), added missing imports. Added seed data callback (8 villages pre-populated), mic pulse animation (`anim/pulse.xml`), code audit against handoff complete. Build fully green. |
| Session 15 | **Device Testing + Bugfixes**: Installed APK on real device (Oppo, Android 14). Fixed 3 `CalledFromWrongThreadException` crashes in `VoiceInputSheet` (`setState` on IO thread — UNKNOWN handler, `onConfirmYes`, `onConfirmNo`). Fixed search `LIKE` query missing `%` wildcards in `PatientDao` & `FamilyGroupDao`. Fixed `HindiNumberConverter.parseHindiNumber` not being called in `VoiceIntentParser.extractAmounts` — Hindi spoken amounts now parse correctly. Fixed null-name display ("null नहीं मिले") in `handleMedicineEntry`, `handlePaymentEntry`, `handleMedicineAndPayment`. Verified VoiceInputSheet opens on device, mic pulse animation renders, bottom sheet UI renders correctly. Notification shade stuck on ColorOS preventing complete voice flow testing via adb. |

---

## Immediate Next Build Targets

Priority ordered (based on Antigravity spec):

| Priority | Feature | Effort | Status |
|----------|---------|--------|--------|
| **P0** | Android TextToSpeech (Hindi) — speak balance aloud | 2-3 hours | ✅ Done |
| **P0** | Hindi Number → Speech converter (₹1,450 → "ek hazaar chaar sau pachaas") | 1-2 hours | ✅ Done |
| **P0** | Smart Intent Parser — detect medicine vs payment vs search from speech | 3-4 hours | ✅ Done |
| **P1** | Confirmation loop — after parsing, show card + TTS + wait for voice confirm | 2-3 hours | ✅ Done |
| **P1** | Medicine+Payment combo from single voice command | 1-2 hours | ✅ Done |
| **P1** | New patient creation from voice | 2-3 hours | ✅ Done |
| **P0** | Device testing: test all 6 voice flows on real device (mic input needed — speak "राम का बकाया", "दवा दी 200", etc.) | 1-2 hours | 🟡 In Progress — UI verified, crashes fixed, flow needs human speech testing |
| **P1** | Fix bugs found during device testing | 2-4 hours | ⏳ |
| **P1** | Search LIKE `%` wildcard added (PatientDao, FamilyGroupDao) | 15 min | ✅ Done |
| **P1** | Hindi number parsing integrated into `VoiceIntentParser.extractAmounts` | 30 min | ✅ Done |
| **P2** | Voice disambiguation for duplicate names | 2-3 hours | 🔴 |
| **P2** | "Galat ho gaya" — voice correction of last entry | 1-2 hours | 🟡 Partial (stub only) |
| **P2** | Family-as-primary entity in search & UI | 4-6 hours | 🔴 |
| **P3** | Village-level Hindi names in DB (not just strings.xml) | 1 hour | 🔴 |
| **P3** | Defaulter list (>30, >90, >180 days) in Analytics | 1-2 hours | 🔴 |
| **P4** | Build hygiene: kapt→KSP, JVM target→17, AGP upgrade, version catalog, R8 minification | 2-3 hours | 🔴 |
