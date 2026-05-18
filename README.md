# Daily Sitting

<p align="center">
  <img src="assets/daily-sitting-logo.png" alt="Daily Sitting logo" width="128" height="128">
</p>

**A minimal Android meditation timer. No accounts, no content platform, no server. Your session history lives in [Health Connect](https://health.google/health-connect-android/) — not in the app.**

[⬇ Download latest APK](https://github.com/stigmergic-org/DailySitting/releases/latest) • [View releases](https://github.com/stigmergic-org/DailySitting/releases)

---

## Why Daily Sitting?

Most meditation apps are content platforms: courses, feeds, coaching, subscriptions. Daily Sitting is just a timer. It records your sessions to Health Connect so your data is yours — readable by any other app you grant access to, and not stored on any third-party server.

- **No account required**
- **No internet required**
- **No session history stored in the app** — everything lives in Health Connect
- **Open source** (GPL-3.0)

---

## Requirements

- Android 14 or later (Health Connect is built into Android 14+)
- Health Connect enabled on your device

---

## Features

- Pick from configured meditation timers on the opening screen
- Create, edit, and delete timer presets
- Run a timer with an ending bell
- Configure optional interval bells per timer
- Record completed sessions to Health Connect
- Manually add past sessions with date, time, and duration
- View current streak, today's minutes, and weekly minutes (read from Health Connect)
- Import meditation history from Insight Timer CSV exports

---

## Installation

### Option 1: Obtainium (recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) installs and updates apps directly from GitHub releases — no Play Store needed, and you'll get notified when new versions are available.

1. Install Obtainium from its [releases page](https://github.com/ImranR98/Obtainium/releases/latest)
2. In Obtainium, tap **Add App**
3. Paste `https://github.com/stigmergic-org/DailySitting` and tap **Add**
4. Tap **Install**

Future updates can then be applied with one tap from within Obtainium.

### Option 2: Direct APK download

Download the latest APK from the [Releases page](https://github.com/stigmergic-org/DailySitting/releases/latest) and install it on your Android device.

> **Note:** You'll need to allow installation from unknown sources in your Android settings.

---

## Building from Source

**Requirements:** Android Studio, JDK, Android SDK 36 (required for the Health Connect mindfulness session API)

### Debug build

```bash
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release build

```bash
./gradlew :app:assembleRelease
```

### Running in Android Studio

1. Open this directory in Android Studio
2. Let Gradle sync finish using the included wrapper
3. Select the shared `app` run configuration
4. Connect an Android device (or GrapheneOS device) with USB debugging enabled
5. Press Run

---

## Releasing

The workflow at `.github/workflows/release-apk.yml` builds and publishes an APK on tag pushes matching `v*`.

To publish a signed release, add these repository secrets:

| Secret | Description |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded Android keystore |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Release key alias |
| `RELEASE_KEY_PASSWORD` | Release key password |

Then create a release:

```bash
git tag v0.2.3
git push origin v0.2.3
```

If signing secrets are not configured, the workflow still builds an unsigned APK.

---

## License

Project-authored code is licensed under **GPL-3.0-only**. See [`LICENSE`](LICENSE).

Third-party assets and dependencies remain under their respective licenses. See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
