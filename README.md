# Home Inventory

An offline home inventory app for Android and iOS, built with Compose Multiplatform. Everything is stored on
the phone and no account is needed. It tracks what you own and what it's worth, keeps a go-bag checklist and an
evacuation plan, and exports an insurance report as a PDF.

## Screens

| Design | Screen | Where |
|---|---|---|
| 1a / 1b / 1c | Home dashboard: value first, readiness first, action first (switch in Settings ⚙) | `ui/screens/HomeScreen.kt` |
| 1d | Item list: search, Room/Category dropdowns, tag chips, first-run and no-match empty states | `ui/screens/ItemListScreen.kt` |
| 1e | Item detail: photos, status badges, fields, Edit / Add Photo / Mark Status sheet | `ui/screens/ItemDetailScreen.kt` |
| 1f | Evacuation mode (always dark): meeting point, call contacts, go-bag checklist, "I'm Evacuating" log | `ui/screens/EvacuationScreen.kt` |
| 1g | Add item: barcode scan, torch, take photo, camera-permission explainer and denied states, form | `ui/screens/AddItemScreen.kt` |
| 1h | Insurance report: totals, missing-photo warning, table preview, Export PDF, Share | `ui/screens/ReportScreen.kt` |
| — | Edit item and delete; settings (layout, hide values, sample data, erase) | `EditItemScreen.kt`, `SettingsSheet.kt` |

## Layout

```
shared/   Kotlin Multiplatform module: all UI and logic (commonMain), platform code (androidMain, iosMain)
app/      Android application (MainActivity)
iosApp/   Xcode project (SwiftUI host for the Compose UI)
```

- **Theme**: `ui/theme/` maps the design tokens to Material 3. `--le-cta` → `primary`, `--le-primary` →
  `tertiary`, borders → `outlineVariant`. Success, warning and gold live in `ExtendedColors`
  (a CompositionLocal). Fonts are Fraunces, Inter and JetBrains Mono, all variable TTFs loaded with
  `FontVariation.weight`.
- **Data**: `InventoryRepository` keeps a single JSON document in app-private storage. Writes are atomic and
  happen in the background. Cloud backup is off on Android, so the data stays on the phone.
- **Platform code** (`AppPlatform`):

  | Capability | Android | iOS |
  |---|---|---|
  | Barcode scanning | CameraX + ML Kit | AVFoundation |
  | Torch | Supported | Supported |
  | Photo capture | `TakePicture` + FileProvider | `UIImagePickerController` (photo library on the simulator) |
  | Camera permission | Supported | Supported |
  | Save | `CreateDocument` | Files export |
  | Share | Share sheet | Share sheet |
  | App Settings | Opens the app's settings page | Opens the app's settings page |
  | Phone dialer | Supported | Supported |

- **PDF**: `pdf/PdfWriter.kt` is a small pure-Kotlin PDF writer, so the report renders the same on both platforms.

## Build and run

- Android: `./gradlew :app:installDebug`, or run `app` from Android Studio. Minimum API is 26.
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run. Set `TEAM_ID` in
  `iosApp/Configuration/Config.xcconfig` to run on a device. The build phase compiles the Kotlin framework.

## Tests

```
./gradlew :shared:jvmTest                 # logic + Compose UI tests on the JVM (fast)
./gradlew :shared:iosSimulatorArm64Test   # the same tests on the iOS simulator
./gradlew :shared:testAndroidHostTest     # logic tests on the Android host
```

The tests are in `shared/src/commonTest`:

- Unit tests cover formatting, filters, expiry, the dashboard, the report, the repository and its
  persistence, the item draft and the add flow, navigation, and the PDF writer.
- UI tests (`AppUiTest`) drive the real `App()` through every screen and flow, using a fake platform for the
  camera, permissions and share/save sheets.
