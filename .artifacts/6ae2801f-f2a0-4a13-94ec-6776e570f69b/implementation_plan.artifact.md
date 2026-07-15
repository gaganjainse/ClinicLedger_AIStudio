# Consolidate Duplicate Functions in Village Clinic Ledger

This plan outlines the steps to identify and remove redundant logic across the project, centralizing utilities for currency, dates, and bilingual string handling.

## User Review Required

> [!IMPORTANT]
> - `LocaleManager.formatCurrency` currently rounds to `Long`. I will change it to preserve decimals if present (e.g., `₹100.50`) to maintain consistency with `BalanceCalculationTest`.
> - `Patient.formattedBalance` and `Transaction.formattedAmount` will be deprecated/removed in favor of `LocaleManager.formatCurrency`.

## Proposed Changes

### [Utils & Managers]

#### [NEW] [DateTimeUtils.kt](file:///C:/Users/gagan/Downloads/ClinicLedger-main/app/src/main/java/com/villageclinicledger/ui/util/DateTimeUtils.kt)
- Centralize "Start of Day/Week/Month" logic.
- Add helper for localized day of week names.

#### [MODIFY] [LocaleManager.kt](file:///C:/Users/gagan/Downloads/ClinicLedger-main/app/src/main/java/com/villageclinicledger/ui/util/LocaleManager.kt)
- Update `formatCurrency` to handle decimals gracefully.
- Move "Day of Week" naming here or use `DateTimeUtils`.
- Add helper for localized transaction types ("दवाई दी", "जमा किया").

### [Data Models]

#### [MODIFY] [Patient.kt](file:///C:/Users/gagan/Downloads/ClinicLedger-main/app/src/main/java/com/villageclinicledger/data/models/Patient.kt)
- Remove `formattedBalance`. Callers should use `LocaleManager.formatCurrency`.

#### [MODIFY] [Transaction.kt](file:///C:/Users/gagan/Downloads/ClinicLedger-main/app/src/main/java/com/villageclinicledger/data/models/Transaction.kt)
- Remove `formattedAmount`. Callers should use `LocaleManager.formatCurrency`.

### [UI Components]

#### [MODIFY] [MorningBriefSection.kt](file:///C:/Users/gagan/Downloads/ClinicLedger-main/app/src/main/java/com/villageclinicledger/ui/compose/components/MorningBriefSection.kt)
- Use `DateTimeUtils` for day of week.
- Use `LocaleManager.formatCurrency`.

#### [MODIFY] [CommonComponents.kt](file:///C:/Users/gagan/Downloads/ClinicLedger-main/app/src/main/java/com/villageclinicledger/ui/compose/components/CommonComponents.kt)
- Use `LocaleManager` for transaction type labels.

### [ViewModels & Repositories]

#### [MODIFY] [AnalyticsViewModel.kt](file:///C:/Users/gagan/Downloads/ClinicLedger-main/app/src/main/java/com/villageclinicledger/ui/analytics/viewmodel/AnalyticsViewModel.kt)
- Use `DateTimeUtils` for date computations.

#### [MODIFY] [PatientRepository.kt](file:///C:/Users/gagan/Downloads/ClinicLedger-main/app/src/main/java/com/villageclinicledger/data/repository/PatientRepository.kt)
- Use `DateTimeUtils` for date computations.

## Verification Plan

### Automated Tests
- Update `BalanceCalculationTest` to use `LocaleManager.formatCurrency` and verify it still passes.
- Ensure the project builds successfully with `gradle build`.

### Manual Verification
- Verify that currency displays correctly (with Rupee symbol) in all screens.
- Verify that the "Morning Brief" correctly shows today's snapshots.
- Verify that transaction type labels are still localized correctly.
