# InfScan: build plan

Sources: [PRD](https://claude.ai/artifact/FVC3fz4iYeiFGyp2Jyejck) and [design canvas](https://claude.ai/artifact/Aa1g9XNv9XxTQAyWDnMTyy). Screenshots of every design screen are in `design-reference/`.

## Assumptions

- Application ID `com.app.vv.infscan` (the build rule for this workspace). The PRD guessed `com.infscan.app`.
- Ads run on AdMob. The PRD says "AdMob or Ad Manager"; both use the same unit naming, so only the IDs change.
- Debug builds use Google's public test ad IDs. Release IDs come from Gradle properties (`INFSCAN_*`), never from source.
- Firebase has no `google-services.json` in the repo. Analytics sends events to Firebase when the file is present and to the debug log otherwise.
- The price provider is undecided, so price check ships behind `PRICE_CHECK_ENABLED` (on in debug, off in release) with a clearly named stand-in, `FakePriceRepository`.
- minSdk 26. compileSdk and targetSdk 37, the latest stable API level (current libraries require compileSdk 37).
- Screens, copy and layout come from the design canvas. Behaviour, ad rules and limits come from the PRD.

## Requirement traceability

| PRD requirement | Screens | ViewModel | Data and platform |
| --- | --- | --- | --- |
| Auto scan (QR, EAN, UPC, Code 128, Data Matrix, more) | S05, S06, S20 | `ScanViewModel` | `CameraScanner` (CameraX + ML Kit), `CodeParser`, `HistoryRepository` |
| Torch and zoom | S05, S20 | `ScanViewModel` | `CameraScanner` |
| Scan from gallery image | S05, S06 | `ScanViewModel` | `BarcodeImageDecoder`, photo picker (no permission) |
| Batch scanning and CSV | S07 | `ScanViewModel` | `CsvExporter`, `Sharing` |
| Typed result with open, copy, share, save | S08, S09, S10, S21 | `ResultViewModel` | `CodeParser`, `HistoryRepository`, `Sharing` |
| Link safety hint (domain, HTTPS, shortened) | S08, S21 | `ResultViewModel` | `LinkSafetyChecker`, `LinkResolver` |
| Price check from a product barcode | S09, S19 | `ResultViewModel` | `ProductRepository` (Open Food Facts), `PriceRepository` (stand-in) |
| Generator: URL, text, Wi-Fi, contact, SMS, email, event, location | S11, S12, S13, S23 | `CreateHomeViewModel`, `CreateFormViewModel`, `CreatePreviewViewModel` | `PayloadBuilder`, `QrEncoder` (ZXing), `ImageSaver` |
| History, favorites, search, delete with undo | S14, S15, S22 | `HistoryViewModel` | `HistoryRepository` (Room) |
| CSV export: rewarded video or latest 50 | S17 | `HistoryViewModel` | `CsvExporter`, `AdManager` |
| Settings: theme, vibrate, open links, retention, clear history | S16, S18 | `SettingsViewModel` | `SettingsRepository` (DataStore) |
| Ad consent and privacy options | S16 | `SettingsViewModel` | `AdManager` (UMP) |
| Onboarding, no account, permission in context | S01 to S04, S06 | `OnboardingViewModel`, `ScanViewModel` | `SettingsRepository` |
| Banner on History and Create | S11, S14 | none | `BannerAdSlot` |
| Native below result and every 8 history rows | S08, S09, S10, S14 | none | `NativeAdSlot` |
| Interstitial after saving a created code | S13 | `CreatePreviewViewModel` | `AdManager`, `InterstitialPolicy` |
| App open ad on cold start only | none | none | `AdManager`, `AppOpenPolicy` |
| Rewarded ad for export | S17 | `HistoryViewModel` | `AdManager` |
| Firebase events and ad revenue | all | all | `AnalyticsTracker`, `GoogleAdManager` (paid event) |

## Screen inventory

- **S01** splash (system splash). **S02 to S04** onboarding pages.
- **S05** scan, **S06** camera access off, **S07** batch scan.
- **S08** link result, **S09** product prices, **S10** Wi-Fi result, **S19** prices offline.
- **S11** create home, **S12** Wi-Fi form, **S13** code preview.
- **S14** history, **S15** history empty, **S17** export sheet, **S18** clear history dialog.
- **S16** settings.
- **S20 to S23** dark variants of scan, link warning, history and create.
- Derived states (not in the design, built from the same components): other create forms, other result types, history search, retention dialog, date and time picker, loading and failure states for product lookups.

## Navigation

Type-safe routes (`kotlinx.serialization`): `OnboardingDest`, `ScanDest`, `CreateDest`, `HistoryDest`, `SettingsDest`, `ResultDest(id)`, `CreateFormDest(type)`, `CreatePreviewDest(id)`. The four main tabs share one back stack with state saved per tab. Result and form screens hide the bottom bar. No deep links: the PRD has none.

## Design tokens

Colors (light and dark), type scale (Bricolage Grotesque, Figtree, JetBrains Mono, bundled), 8 dp spacing, radii (12, 16, 20, 24, 28, pill), elevation levels and motion durations are extracted from the canvas into `ui/theme`. No raw colors, dp or sp values live outside the theme except in layout one-offs taken from the design.

## Architecture

Single module, one Activity. Packages: `ads`, `analytics`, `core`, `data` (`db`, `prefs`, `repo`), `di`, `domain` (`model`, `parser`, `create`), `platform`, `ui` (`theme`, `components`, `common`, `nav`, and one package per feature). Each feature is a stateful Route, a stateless Screen(uiState, onEvent), a ViewModel and a UiState. State is immutable, events go in, one-off effects come out through a channel.

## Manifest plan

- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA` (asked on first use, with a path to Settings), `VIBRATE`, `WRITE_EXTERNAL_STORAGE` capped at API 28 (saving an image on Android 8 and 9 only).
- No services, receivers or foreground services. One exported Activity. A non-exported `FileProvider` for sharing images and CSV files.
- Backup and data extraction are off. Cleartext traffic is off.
- Android 13 to 16 changes checked: no notifications, no exact alarms, edge-to-edge is on, predictive back is on, photo picker replaces storage access, camera feature is optional.

## Design versus PRD conflicts

| Topic | Design | PRD | Decision |
| --- | --- | --- | --- |
| Native ad in History | banner only | native every 8 rows | follow the PRD |
| Crashlytics | not shown | listed in analytics | left out, it needs a Firebase project and adds nothing the PRD tests |
| Splash | logo, name and tagline | not specified | the system splash cannot show text, so it shows the mark on teal |
| Wi-Fi result actions | no Copy/Share/Save row | open, copy, share, save on every result | follow the design, the Wi-Fi buttons already cover copy and share |
| Contact and event actions | not shown | add contact is "later" | built as the result's open action; Wi-Fi connect is not built |
| Created history rows | "Created today, 09:15" | not specified | shown as "Today, 09:15" under the Created tab |
| Share and Copy buttons on the preview | second button cut off | not specified | both shown (the design had a layout overflow) |
| Ad choices row | always shown | only when consent requires it | follow the PRD |
| Storage permission | not shown | "no storage permission" | none on Android 10+, capped legacy permission on 8 and 9 |

## Milestones and exit checks

1. **Foundation**: theme, components, navigation, assets. Exit: `assembleDebug` passes.
2. **Screens**: every designed screen and state. Exit: renders match `design-reference/` (see `PROGRESS.md`).
3. **Features**: scan, results, generator, history, settings. Exit: ViewModel and repository tests pass.
4. **System behavior**: permissions, offline, process death, rotation, large fonts. Exit: lint clean.
5. **Ads and analytics**: consent, preloading, caps, paid-event logging. Exit: policy tests pass.
6. **Tests**: unit, repository, Compose flows. Exit: all tests pass.
7. **Release**: R8 build, release notes. Exit: `assembleRelease` passes.
