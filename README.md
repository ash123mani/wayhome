# WayHome — “Meet the people around you who are going your way.”

Native Android MVP that helps airport / flight passengers going home in a similar
direction discover each other, chat 1:1, form a temporary ride group, and arrange a
shared cab themselves — **offline-first** via Google Nearby Connections
(Bluetooth + BLE + WiFi; discovery, chat, and groups need no internet).
When internet *is* available, an optional OSRM check verifies a peer is truly
on your road route and shows a `Going your way` badge (see
[`docs/route-matching.md`](docs/route-matching.md)).

- No login, no signup, no email/phone. Temporary session identity (`Traveller ###`).
- Only `City · Area` is ever shared. No exact address, live location, or contact details.
- WayHome does **not** book cabs, handle payments, or run a backend in the MVP.

## Quickstart (5 minutes)

Prerequisites: Android Studio (or JDK 17+ + Android SDK with platforms 35, build-tools 35),
two physical Android devices for the real Nearby test (emulator covers UI only).

```bash
cd studio/wayhome
./gradlew testDebugUnitTest assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

Install on **two** Bluetooth+WiFi-capable devices and run the 2-device test:

1. Both: open WayHome → Get Started → pick City/Area → **Find people going my way**
   (grant Nearby/Bluetooth permissions when asked; turn BT + WiFi **on** manually).
2. Each device should list the other as `Traveller ### · City · Area` → **Connect** → **Chat**.
   (With internet on, route-verified peers rank first with a `Going your way` badge;
   offline you get the basic `Same destination` / `Nearby destination` badges.)
3. Send messages both ways (works with mobile data / WiFi **off**).
4. One device: Groups → Create group → the other joins via the broadcast → group chat.

Full walkthrough, emulator demo, and troubleshooting: [`docs/HOW_TO_RUN.md`](docs/HOW_TO_RUN.md).

## Docs

| Doc | Contents |
|---|---|
| `ARCHITECTURE.md` | Module layout, contracts, offline data flow, platform limitations, verify steps |
| `FUTURE_SCOPE.md` | Phased post-MVP roadmap (backend, mesh, background — route matching §1 is now shipped) |
| `docs/route-matching.md` | OSRM route verification: pipeline, thresholds, failure model, self-hosting |
| `docs/HOW_TO_RUN.md` | Build, install, 2-device test script, emulator demo, troubleshooting |
| `docs/BUSINESS.md` | Product concept, personas, journeys, MVP scope/non-goals, metrics, risks |
| `docs/PRIVACY_SAFETY.md` | Privacy model, data handling, safety controls, permission rationale |
| `docs/WIRE_PROTOCOL.md` | Offline message envelope, types, flood/dedup rules |
| `docs/TESTING.md` | Unit + manual test plan and pass criteria |

## Tech snapshot

Kotlin 2.0.20 · AGP 8.5.2 · Compose BOM 2024.09.00 · Hilt · Room 2.6.1 ·
`play-services-nearby:18.7.0` · Retrofit 2.11.0 + OkHttp 4.12.0 (OSRM only) ·
`minSdk 26`, `compile/targetSdk 35` · package `com.wayhome` ·
strategy `P2P_CLUSTER` · serviceId `com.wayhome.NEARBY`.
