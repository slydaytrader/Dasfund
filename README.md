# DasFund Android — GitHub Actions build

This project is the Android client for DasFund (`https://dasify.co.ke/dasfund/`). It uses the existing DasFund web system and includes the offline cached-page experience.

## GitHub Actions

The workflow is at `.github/workflows/android.yml`.

Every push to `main`/`master`, pull request, or manual workflow run builds a debug APK. If all four signing secrets are configured, it also builds a signed release APK and AAB.

### Signing secrets

Add these under **Repository Settings → Secrets and variables → Actions**:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Do not commit a keystore or passwords to the repository.

## Outputs

The workflow uploads build artifacts named:

- `DasFund-debug-apk`
- `DasFund-release-apk-signed` (when signing is configured)
- `DasFund-release-aab-signed` (when signing is configured)
- `DasFund-release-aab-unsigned` (when signing is not configured)

## Offline mode

After a successful online visit, the app stores a local WebView archive. If there is no validated Internet connection, it can show the cached page and an offline/synchronization banner. Server actions such as OTP, M-PESA and contributions require connectivity.
