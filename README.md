# Daily Sitting

<p align="center">
  <img src="assets/daily-sitting-logo.png" alt="Daily Sitting logo" width="128" height="128">
</p>

Daily Sitting is a small Android meditation timer that integrates with Health Connect. It is intentionally not a content platform: no courses, feeds, coaching, accounts, or social features.


## Product Scope

- Pick from configured meditation timers on the opening screen.
- Create, edit, and delete timer presets.
- Run a timer with an ending bell.
- Configure optional interval bells per timer.
- Record completed sessions to Health Connect.
- Manually add past sessions to Health Connect with date, time, and length.
- Show the current streak after the ending bell.
- Read mindfulness sessions from Health Connect for today, streak, weekly minutes, and the meditation log.
- Import meditation sessions from Insight Timer CSV exports.
- Keep only timer presets in app storage; session history is not persisted locally.

## Android Build

This project uses Kotlin, Jetpack Compose, Health Connect, and Gradle. The current environment used to create the project did not have Java or Gradle installed, so build verification should be run on a machine with Android tooling or in GitHub Actions.

The Health Connect mindfulness session API currently requires the Android 36 SDK platform for compilation.

Local build with Gradle installed:

```sh
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

## Local Debug Setup

1. Open this directory in Android Studio.
2. Let Gradle sync finish using the included wrapper.
3. Select the shared `app` run configuration.
4. Connect an Android or GrapheneOS device with USB debugging enabled.
5. Press Run.

The debug APK is written to:

```sh
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Release Pipeline

The workflow at `.github/workflows/release-apk.yml` builds an APK on tag pushes matching `v*` and publishes it to a GitHub Release. It can also be run manually from GitHub Actions, which uploads the APK as a workflow artifact.

To publish a signed release APK, add these repository secrets:

- `RELEASE_KEYSTORE_BASE64`: base64-encoded Android keystore file.
- `RELEASE_KEYSTORE_PASSWORD`: keystore password.
- `RELEASE_KEY_ALIAS`: release key alias.
- `RELEASE_KEY_PASSWORD`: release key password.

Create a release by pushing a tag:

```sh
git tag v0.1.0
git push origin v0.1.0
```

If signing secrets are not configured, the workflow still builds the release variant but the APK will be unsigned.

## License

Project-authored code is licensed under GPL-3.0-only. See `LICENSE`.

Third-party assets and dependencies remain under their respective licenses; see `THIRD_PARTY_NOTICES.md`.
