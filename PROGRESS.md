# Progress

## Status

All milestones are done. The app builds, passes lint and its tests, and the R8 release build succeeds. It has not been run on a device or emulator (none was reachable from this workspace), so camera scanning, real ads and consent are unverified at runtime.

## Milestones

| # | Milestone | State | Evidence |
| --- | --- | --- | --- |
| 1 | Foundation: theme from design tokens, components, navigation, brand assets | done | `assembleDebug` passes |
| 2 | Screens: all 23 designed screens and their states | done | every screen rendered and compared with `design-reference/` (table below) |
| 3 | Features: scan, batch, results, link safety, product lookup, generator, history, export, settings | done | ViewModel, repository and parser tests |
| 4 | System behavior: camera permission paths, offline, process death, rotation, 200% font | done | lint clean; large-font renders reviewed; form and history state restored from `SavedStateHandle` |
| 5 | Ads and analytics: UMP consent, preload, caps, paid-event logging | done, runtime unverified | policy tests; SDK calls checked against the SDK's public API |
| 6 | Tests | done | see below |
| 7 | Release: R8 build, bundle, docs | done | `assembleRelease` and `bundleRelease` pass |

## Verification

- `./gradlew :app:lintDebug`: 0 errors, 0 warnings.
- `./gradlew :app:testDebugUnitTest`: 148 tests, 0 failures. They include 22 screen renders and 8 large-font renders.
- `./gradlew :app:assembleRelease` and `:app:bundleRelease`: pass (R8 full mode, resource shrinking). 64-bit native libraries are 16 KB aligned.

## Open items

- Replace the stand-in price source (`FakePriceRepository`) before turning on `INFSCAN_PRICE_CHECK`.
- Supply real AdMob IDs, the privacy policy URL, the feedback address and `google-services.json` (see `RELEASE.md`).
- Run the release build on a physical device: camera, consent form, ads, sharing, gallery save.
