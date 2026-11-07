# InfScan release guide

## Versioning

`versionName` is semantic (`1.0.0`). `versionCode = major * 10000 + minor * 100 + patch`, so 1.0.0 is `10000` and 1.2.3 is `10203`. Both live in `app/build.gradle.kts`.

## Build inputs

Real IDs and keys are never committed. Pass them as Gradle properties (`-P`), environment variables or in `~/.gradle/gradle.properties`:

| Property | Purpose | Default when missing |
| --- | --- | --- |
| `INFSCAN_ADMOB_APP_ID` | AdMob app ID (`ca-app-pub-…~…`) | Google's test app ID |
| `INFSCAN_AD_BANNER`, `INFSCAN_AD_NATIVE`, `INFSCAN_AD_INTERSTITIAL`, `INFSCAN_AD_APP_OPEN`, `INFSCAN_AD_REWARDED` | Ad unit IDs, named `infscan_<format>_<screen>` in AdMob | Google's test unit IDs |
| `INFSCAN_PRIVACY_URL` | Privacy policy page shown in Settings | `https://infscan.example/privacy` (placeholder) |
| `INFSCAN_FEEDBACK_EMAIL` | Address behind "Send feedback" | `feedback@infscan.example` (placeholder) |
| `INFSCAN_PRICE_CHECK` | `true` turns on the Prices tab | `false` in release |

Release builds with the default IDs show Google test ads. Check the values before uploading.

Firebase: put `google-services.json` in `app/`. It is git-ignored. Without it the app still runs and events go to the debug log only.

## Signing

1. Create an upload key once, outside the repo:
   `keytool -genkeypair -v -keystore ~/infscan-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000`
2. Create `keystore.properties` in the repo root (git-ignored):
   ```
   storeFile=/home/you/infscan-upload.jks
   storePassword=…
   keyAlias=upload
   keyPassword=…
   ```
3. Turn on Play App Signing in the Play Console and upload with the key above.

## Build the bundle

```
./gradlew :app:bundleRelease \
  -PINFSCAN_ADMOB_APP_ID=ca-app-pub-XXXX~YYYY \
  -PINFSCAN_AD_BANNER=ca-app-pub-XXXX/1111 -PINFSCAN_AD_NATIVE=ca-app-pub-XXXX/2222 \
  -PINFSCAN_AD_INTERSTITIAL=ca-app-pub-XXXX/3333 -PINFSCAN_AD_APP_OPEN=ca-app-pub-XXXX/4444 \
  -PINFSCAN_AD_REWARDED=ca-app-pub-XXXX/5555 \
  -PINFSCAN_PRIVACY_URL=https://your.site/privacy -PINFSCAN_FEEDBACK_EMAIL=you@your.site
```

The signed bundle is `app/build/outputs/bundle/release/app-release.aab`. Before uploading, run `./gradlew lint testDebugUnitTest`.

Ad revenue is logged once per impression from the SDK paid event as the Firebase `ad_impression` event. In AdMob, turn off "Share ad impression revenue with Firebase" for this app, otherwise Firebase counts each impression twice.

## Play Console checklist

**Data safety** (matches what the code does):

- Collected: device or other IDs (advertising ID, by Google Mobile Ads and Firebase), app interactions and diagnostics (Firebase Analytics, Google Mobile Ads), approximate location (inferred by Google Mobile Ads from the IP address). Purposes: advertising, analytics, app functionality. Encrypted in transit: yes.
- Not collected by InfScan: scanned and created codes, history, settings. They stay on the device and are excluded from backup.
- Shared with third parties: product barcode numbers (Open Food Facts, only when a product is scanned) and shortened link addresses (the shortener's own host, only to show where it leads). Neither is tied to an identity.
- Users can delete all history in Settings. There is no account.

**Permission declarations**

- `CAMERA`: core feature (reading codes). Asked on first scan with a way to recover from denial. No declaration form needed.
- `com.google.android.gms.permission.AD_ID` (added by the ads SDK): declare that the app uses the advertising ID for advertising and analytics.
- `WRITE_EXTERNAL_STORAGE`, capped at Android 9: only to save a created code to the gallery on Android 8 and 9. Android 10 and later use no storage permission.
- `INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE`: normal permissions, no form.
- No location, contacts, SMS, call log, notification or foreground service permissions.

**Ads declaration:** the app contains ads (yes). Ads follow the Google user messaging flow before the first request.

**Content rating:** utility app, no user-generated content shared between users, no violence or other restricted content; ads present. Expect an "Everyone" rating.

**Target audience:** 13 and older. The app is not designed for children.

**Open source and data attribution:** fonts (Bricolage Grotesque, Figtree, JetBrains Mono) are SIL Open Font License 1.1. Product details come from Open Food Facts (ODbL) and the Details tab credits it. Keep both notices in the store listing or an in-app licenses page if you add one.

## Store listing

**Title (29 of 30):** InfScan: QR & Barcode Scanner

**Short description (74 of 80):** Scan QR codes and barcodes, check prices, and create codes. Fast and free.

**Full description:**

InfScan reads QR codes and barcodes the moment they appear in your camera. Point, scan, and get a clear result: links, Wi-Fi networks, contacts, events, messages, locations, text and product codes.

What you can do

• Scan automatically with no button to press. Torch and zoom are one tap away.
• See where a link goes before you open it. Shortened and unencrypted links are flagged.
• Scan a whole stack with batch mode and export the list as a CSV file.
• Scan a code from a photo in your gallery.
• Create codes for websites, text, Wi-Fi, contacts, messages, email, events and locations. Save them as images or share them.
• Keep a searchable history with favorites. It stays on your device, and you can clear it any time.
• Look up product details from a barcode.
• Light and dark themes, large text support and right-to-left layouts.

Private by design

No account and no sign-up. Scanning works offline. InfScan asks for camera access only when you first scan, and never records or uploads what the camera sees.

Free with ads

InfScan is free and shows ads. Ads never appear on the scanning screen, and you can review your ad choices in Settings.

(Add this line only when `INFSCAN_PRICE_CHECK` is `true`: "Compare prices from a product barcode.")

**Category:** Tools. **Tags:** QR scanner, barcode scanner, QR generator, Wi-Fi QR, batch scan.

## Release steps

1. Fill the Gradle properties above, build the bundle and test it on a physical device (camera, ads, consent, sharing).
2. Upload to internal testing, then closed testing (Play requires testers for 14 days on new personal accounts).
3. Roll out to production in stages: 10%, 50%, 100%. Watch crash-free sessions and ad fill before each step.
