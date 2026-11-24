# Implementation notes

This document records how the Home Inventory design was built as a Compose Multiplatform app for Android
and iOS. It covers what is in the app, how it was tested, where it differs from the design, and what is
still open. For build and test commands, see the [README](../README.md).

## Status

Every screen in the design is built: 1a–1h, plus the sheets, dialogs and empty states. Both apps build,
and every test suite passes. The iOS app was run on the iPhone 16 Pro simulator, and its home screen
matches design 1a.

**Not yet tried on real devices.** No Android emulator or physical phone was available. The iOS
simulator has no camera. Barcode scanning, the torch, photo capture, the save and share sheets, and the
backup file picker all compile and are covered by tests that use a fake platform. None of them has been
run on hardware. Try those first on real phones.

## Test results

| Where | Tests | Result |
|---|---|---|
| JVM: logic, UI and screenshot tests | 95 | All pass |
| iOS simulator: logic and UI tests | 86 | All pass |
| Android host: logic tests only | 52 | All pass |
| Android debug APK and lint | — | Builds, 0 lint errors |
| Xcode app build (simulator) | — | Build succeeded |

The UI tests drive the real `App()` through every screen and flow. They use a fake platform for the
camera, permissions, file picker and save/share sheets. The Android host run leaves out the UI tests,
because those need a device or Robolectric.

## What's in the app

- **Theme.** The design's colors, fonts (Fraunces, Inter, JetBrains Mono), radii and shadows are mapped
  to Material 3, following the handoff card. Success, warning and gold are in a separate
  `ExtendedColors` CompositionLocal. The app has a dark theme, and evacuation mode is always dark.
- **Home.** All three dashboard layouts are built: value first, readiness first and action first. You
  switch between them in a settings sheet behind the gear icon. The same sheet has "Hide values",
  backup and restore, sample data and "Erase All Data".
- **Items.** The list has search, Room and Category dropdowns, tag filters, and an "expiring" filter
  that opens from the home card. It shows a first-run empty state and a "No matches" state.
- **Item detail.** It shows photos, status badges and a details card, and has the Mark Status sheet. A
  separate edit screen handles editing and deleting. Deleting an item also deletes its photo files.
- **Evacuation mode.** It shows the meeting point, tap-to-call contacts and the go-bag checklist.
  Long-pressing a go-bag item removes it, and "+ Add to go-bag" adds one. "I'm Evacuating" logs the
  time and can be undone. You can edit the plan and contacts.
- **Add item.** You can scan a barcode, use the torch, take a photo or enter the item manually. The
  flow includes the camera explainer and the "camera access is off" screen with Open Settings. The
  form has Save & Add Another, which shows a saved count, and Save & Close.
- **Insurance report.** It shows totals, a missing-photo warning and a preview table, with Export PDF
  and Share. The PDF is written by a small pure-Kotlin writer, so both platforms produce the same
  report, including photo thumbnails.
- **Storage.** All data is kept on the phone as one JSON file. Writes are atomic, and the data
  survives restarts.
- **Backup and restore.** Export Backup writes one JSON file that includes the photos. Settings →
  Restore from Backup works like this:
  1. It opens the system file picker.
  2. It checks the file.
  3. It shows the backup's date, item count and photo count, and asks you to confirm.
  4. It replaces everything on the phone and writes the photos back.

  Backups from the earlier format, which had no photos, are still accepted. A file that isn't a backup,
  is damaged, or came from a newer app version is rejected with a message, and nothing changes.

## Code map

| Area | Location |
|---|---|
| Theme, icons, fonts | `shared/src/commonMain/.../ui/theme`, `ui/icons`, `composeResources/font` |
| Model and business logic | `model/`, `domain/` |
| Storage, backup and restore | `data/InventoryRepository.kt`, `data/Storage.kt`, `data/Backup.kt` |
| PDF | `pdf/PdfWriter.kt`, `pdf/ReportPdf.kt` |
| Platform interface | `platform/Platform.kt` (`AppPlatform`) |
| Android platform code | `shared/src/androidMain/.../AndroidAppPlatform.kt` |
| iOS platform code | `shared/src/iosMain/.../IosAppPlatform.kt` |
| Screens and navigation | `ui/screens/`, `ui/components/`, `ui/navigation/`, `App.kt`, `AppController.kt` |
| Android app | `app/` |
| iOS app | `iosApp/` (set `TEAM_ID` in `Configuration/Config.xcconfig` to run on a device) |
| Tests | `shared/src/commonTest`, `shared/src/jvmTest` |

## Differences from the design

**Added:**
- The settings sheet, the edit/delete screen, plan and contact editing, adding and removing go-bag
  items, and "Load Sample Inventory". A fresh install starts empty.
- Backup restore.

**Changed:**
- **Camera:** the design's "Simulate Scan" button is now "Take Photo". A real scanner reads barcodes
  automatically when the camera sees one.
- **Privacy:** Android cloud backup is off. With it on, the inventory would be copied to Google Drive,
  which would contradict the app's "never uploaded" promise.
- **Android minimum version:** the minimum is API 26, because the variable fonts need it.

**Fixed during testing:**
- The report summary card had squeezed the table to zero height.
- The home title wrapped onto two lines.
- The "Add Photo" label was cut off.
- Rotating the phone reset the screen.
- The item list filters were lost when going back.
- Deleted items left their photo files on disk.

## Known limitations

- **No real-device testing.** Scanning, the torch, photo capture, the save and share sheets, and the
  file picker have not been run on hardware.
- **No instrumented Android tests.** No emulator was available to run them.
- **Hardcoded strings.** All wording is written directly in the Kotlin code instead of string
  resources, so the app can't be translated yet.
- **Photo library on the iOS simulator.** The simulator has no camera, so photo capture falls back to
  the photo library there.
- **Large backups.** Photos are stored inside the backup file, so each photo adds about 0.3–0.5 MB.

## Commit history

The work is on the `feature/compose-multiplatform-app` branch, in 10 grouped commits on top of
`init project`. Restore from backup is its own commit at the end.

| Commit | Summary |
|---|---|
| `ff4e82d` | Set up Kotlin Multiplatform build with shared module |
| `70995a6` | Add Material 3 theme, fonts and icons from the design tokens |
| `22dc7bb` | Add inventory model, domain logic, storage and PDF report |
| `bb78129` | Add platform layer for camera, files and sharing on Android and iOS |
| `9ba3550` | Add app screens, navigation and shared components |
| `d1c3a21` | Add unit, UI and screenshot tests |
| `f106c5b` | Wire Android app to the shared UI |
| `65b6f2d` | Add iOS app project |
| `8f885c5` | Add README with build, run and test instructions |
| `659c2b6` | Add restore from backup |

Commits 1–5 split the shared module, so they don't build on their own. The state after commit 9 and the
final commit both build and pass their tests.
