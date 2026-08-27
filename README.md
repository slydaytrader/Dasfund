# DasFund Android — GitHub Actions Edition

This project packages the existing DasFund web application at:

`https://dasify.co.ke/dasfund/`

The Android app keeps the existing PHP/MySQL DasFund backend as the source of truth.

## Build without Android Studio

This repository includes a GitHub Actions workflow at:

`.github/workflows/android.yml`

Every push to `main` or `master`, pull request, or manual workflow run builds:

- **Debug APK** — ready for testing on an Android phone.
- **Release AAB (unsigned)** — the package format used for Google Play; it still needs signing before Play Store release.

GitHub Actions installs Java, Android SDK packages, and Gradle automatically. The workflow uses Gradle's current `setup-gradle` action and Gradle 8.7. See the official Gradle documentation for GitHub Actions: https://docs.gradle.org/current/userguide/github-actions.html

## How to use

1. Create a GitHub repository, e.g. `dasfund-android`.
2. Upload all files in this project to the repository root.
3. Open **Actions** in GitHub.
4. Run **Build DasFund Android** manually, or push a change to `main`/`master`.
5. Open the completed workflow run.
6. Under **Artifacts**, download `DasFund-debug-apk` to install on a test phone.

## Google Play release

The workflow intentionally does not contain a private signing key. For Play Store publishing, create a GitHub Actions secret/variable setup for a secure Android keystore and add a signed-release job. Never commit the `.jks`/`.keystore` file or passwords to the repository.

## Current app architecture

- Android WebView shell
- HTTPS-only connection to DasFund
- Persistent web session cookies
- Android back navigation
- Pull-to-refresh
- File chooser support
- `tel:`, `sms:`, `mailto:`, `geo:` and Android intent links
- Notification permission request on Android 13+

## Next recommended phase

1. Add Firebase Cloud Messaging token registration.
2. Connect native push notifications to the existing 9 AM and 9 PM contribution-reminder system.
3. Add a native M-PESA STK Push experience.
4. Gradually replace the WebView dashboard with native Kotlin screens while keeping the existing backend.
