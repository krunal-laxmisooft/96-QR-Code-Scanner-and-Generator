# InfScan

QR code and barcode scanner and generator for Android. Kotlin, Jetpack Compose, Material 3.

- Scan QR codes and barcodes with the camera, from a photo, or in batches. Link safety hints, product lookups, history, CSV export.
- Create codes for websites, text, Wi-Fi, contacts, messages, email, events and locations.
- Google Mobile Ads (Next-Gen SDK) with the user messaging consent flow, Firebase Analytics.

Docs: [PLAN.md](PLAN.md) (what was built and why), [PROGRESS.md](PROGRESS.md) (status and verification), [RELEASE.md](RELEASE.md) (signing, ad IDs, Play Console). Design screenshots used for parity checks are in `design-reference/`.

## Build

```
./gradlew :app:assembleDebug          # debug APK with test ad IDs
./gradlew :app:testDebugUnitTest      # unit, ViewModel, repository and Compose tests
./gradlew :app:lint
./gradlew :app:assembleRelease        # R8 build
```

Render every designed screen for comparison with `design-reference/`:

```
./gradlew :app:testDebugUnitTest --tests "*DesignParityTest*" -Proborazzi.test.record=true
```

Images go to `app/build/outputs/roborazzi/`.

Fonts are licensed under the SIL Open Font License 1.1 (`licenses/OFL-1.1-fonts.txt`).
