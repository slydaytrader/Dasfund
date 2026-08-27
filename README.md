# DasFund Android — GitHub Actions + Offline Cache

This project wraps the existing DasFund web application and adds an offline-first cached view.

## Offline behaviour

After a member successfully opens DasFund while online, the app saves the current WebView page as a local web archive. If there is no validated Internet connection later, the app opens the most recently cached page and displays:

> Mobile data/Internet is off — showing cached information. Data not synchronized.

When a validated connection returns, the app returns to the live DasFund site. Android's `NET_CAPABILITY_VALIDATED` is used to distinguish an actually validated Internet connection from a network that is merely connected. See Android's network-state documentation.

**Important:** offline mode is read-only from the user's perspective. Contributions, M-PESA requests, OTP requests, profile changes and other server operations require a live connection. The cached screen can be stale.

## Signing

The GitHub workflow is set up so a release can be signed using GitHub Actions Secrets. Do not commit a keystore or passwords to the repository.

For Google Play distribution, the recommended path is to upload an AAB to Play Console and use Google Play App Signing. Google then signs the APKs delivered to users. A locally signed APK by itself does not guarantee that Play Protect will never show a warning.

For direct distribution outside Google Play, Android's developer-verification rollout is also relevant. Registration/verification is separate from APK signing.

## GitHub Actions secrets for release signing

Create these repository secrets:

- `ANDROID_KEYSTORE_BASE64` — base64 of your release/upload `.jks` file
- `ANDROID_KEYSTORE_PASSWORD` — keystore password
- `ANDROID_KEY_ALIAS` — key alias
- `ANDROID_KEY_PASSWORD` — key password

The workflow will create `keystore.jks` only inside the runner and delete it after the build.
