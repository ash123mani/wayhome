# How to Run WayHome

## 1. Prerequisites

- **OS:** macOS / Linux / Windows with Android SDK (`platforms: android-35`, `build-tools: 35.0.0`).
  This repo's `local.properties` points at `~/Library/Android/sdk` (macOS default); adjust the
  `sdk.dir` line if your SDK lives elsewhere.
- **JDK 17+** (project compiles with `jvmTarget 17`; JDK 21 also works to run Gradle).
- **Android Studio** (recommended) *or* command line with `./gradlew` (wrapper `8.9`, no system Gradle needed).
- **Two physical Android devices** (Android 8.0+, API 26+) with Bluetooth + WiFi hardware and
  Google Play Services — required for the *real* Nearby Connections test. Emulators cannot do
  BLE discovery, so they only exercise the UI via the fake transport path.

## 2. Build

```bash
cd studio/wayhome
./gradlew testDebugUnitTest assembleDebug
```

Outputs:

- `app/build/outputs/apk/debug/app-debug.apk` (~18 MB) — installable debug build.
- `app/build/test-results/testDebugUnitTest/` — unit test report (36 tests: haversine,
  GeoUtils, route matcher, route pipeline).

To open in Android Studio: **File → Open → `studio/wayhome`** → let Gradle sync → Run `app`
on a connected device.

## 3. Install on two devices

```bash
# device A (first device adb sees)
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
# device B: plug in second device, target it explicitly
~/Library/Android/sdk/platform-tools/adb devices          # note both serials
~/Library/Android/sdk/platform-tools/adb -s <SERIAL_B> install -r app/build/outputs/apk/debug/app-debug.apk
```

## 4. Two-device test script (the real acceptance test)

Setup on **both** devices: turn **Bluetooth ON** and **WiFi ON** manually (the Nearby API no
longer enables radios silently — late-2026 platform change). Mobile data may stay **off** to
prove offline operation; the banner will read `🔴 Internet unavailable · 🟢 Offline nearby mode active`.

| Step | Device A | Device B | Expect |
|---|---|---|---|
| 1 | Open WayHome → **Get Started** | Same | Welcome → Destination screens |
| 2 | City `Bengaluru`, Area `Whitefield` → **Find people going my way** (grant BT/Nearby perms) | City `Bengaluru`, Area `Marathahalli` → same | Banner `🔎 Searching nearby...` → `🟢 Nearby mode active · N found` |
| 3 | Wait ~5–10 s | Wait ~5–10 s | Each lists the other (`Traveller ###`, `Bengaluru · …`, `Same destination`/`Nearby destination` badge; `Going your way` on top when both are online — route check needs internet) |
| 4 | Tap **Connect** on B's card → **Chat** | Accept (auto-accept; card shows Connected) | Both cards show Connected |
| 5 | Send “Hey, going to Whitefield?” | Reply | Messages appear on both, no internet |
| 6 | Groups → **Create group** (“Whitefield Ride Group”) | Groups → open the broadcast group | Shared member list, group chat works both ways |
| 7 | Profile → **Stop sharing** | — | A disappears from B's list; **Start sharing again** re-advertises |

Pass = all 7 steps. Note the known MVP limits: foreground only (don't background the app
mid-test), ~10 m radio range, payloads are short text only.

## 5. Single-device / emulator demo

Without a second device the discovery list stays on `🔎 Searching nearby...` — expected, not a bug.
Use the emulator (or one phone) to walk the full UI flow: Welcome → Destination → Discovery →
Chat (thread opens after Connect tap) → Groups (create/open/leave) → Profile (discoverable
toggle, Stop sharing). The `FakeNearbyTransport` class exists for a scripted two-peer demo;
wiring it in is a `AppModule.provideTransport` one-line swap (see `FUTURE_SCOPE.md` §9).

## 6. Useful commands

```bash
./gradlew testDebugUnitTest          # unit tests only
./gradlew assembleDebug              # APK only
./gradlew lintDebug                  # Android lint
~/Library/Android/sdk/platform-tools/adb logcat | grep -i wayhome   # runtime logs
```

## 7. Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| `sdk.dir` / SDK not found | Edit `local.properties` → your SDK path; install platform 35 + build-tools 35 via SDK Manager |
| Devices don't see each other | BT or WiFi off on either side; too far apart (>~10 m); one app backgrounded; Play Services outdated; retry **Stop → Find people going my way** on both |
| Permission dialog loops | Grant **all** requested Nearby/BT permissions; denial blocks `startAdvertising/startDiscovery` by design |
| `🔴 Internet unavailable` banner | Informational — offline nearby mode is still active; chat/groups work |
| No `Going your way` badges (online) | Route check needs internet **and** a city with a seeded origin (currently Bengaluru only); other cities and offline mode keep basic badges — expected, not a bug |
| Point at a self-hosted OSRM | Build with `./gradlew assembleDebug -POSRM_BASE_URL=https://your-osrm-host/` (must end with `/`); see `docs/route-matching.md` |
| Gradle download slow/fails | Wrapper fetches Gradle 8.9 + Maven deps on first run; needs internet **once** for setup (the app itself needs none) |
| Kotlin/AGP version errors after upgrade | Versions are pinned in `gradle/libs.versions.toml` (Kotlin 2.0.20, AGP 8.5.2); upgrade them together, not singly |
