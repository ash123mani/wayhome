# WayHome — Architecture (Offline-first MVP)

Native Android app (Kotlin + Jetpack Compose + MVVM + Hilt + Room + Google Nearby Connections).
No login/signup, no WayHome backend, no cab booking. Temporary session identity only. The one
outbound call in the whole app is the optional OSRM route check.

## Project layout

```
wayhome/
  settings.gradle.kts, build.gradle.kts, gradle.properties, local.properties (sdk.dir)
  gradle/libs.versions.toml, gradlew
  app/
    src/main/AndroidManifest.xml      # per-API Nearby permission set, no contacts/camera/mic
    src/main/assets/destinations.json # multi-city seed (Bengaluru/Delhi/Mumbai + lat/lng)
    src/main/java/com/wayhome/
      WayHomeApp.kt                   # @HiltAndroidApp
      core/                           # IdentityManager (DataStore temp ID), PermissionHelper, ConnectivityObserver
      domain/model/                   # UserProfile, Peer, Destination, Location, MatchResult, RideGroup, Message
      domain/matcher/                 # DestinationMatcher + HaversineDestinationMatcher (offline base rank)
      domain/routing/                 # RouteMatchResult/Status/Config, GeoUtils, RouteMatcher, RouteOriginProvider
      domain/usecase/                 # FindRouteMatchesUseCase (candidate pipeline + ranking)
      domain/repository/              # ProfileRepository, DiscoveryRepository, ChatRepository, RouteRepository
      data/local/                     # Room: WayHomeDb, Entities, MessageDao, PeerDao, GroupDao
      data/                           # Repositories.kt (impls), DestinationCatalog
      data/routing/                   # OsrmApi, DTOs, OsrmRouteRepository (cached), CityRouteOriginProvider
      nearby/                         # NearbyTransport, RealNearbyTransport, FakeNearbyTransport, WireProtocol
      di/                             # AppModule, RoutingModule (OSRM graph), qualifiers
      presentation/                   # MainActivity, Nav, theme + welcome/destination/discovery/chat/group/profile
    src/test/.../HaversineDestinationMatcherTest.kt (4 tests)
    src/test/.../domain/routing/GeoUtilsTest.kt (12 tests)
    src/test/.../domain/routing/RouteMatcherTest.kt (14 tests)
    src/test/.../domain/usecase/FindRouteMatchesUseCaseTest.kt (6 tests)
```

## Key contracts (transport-independent)

```kotlin
interface NearbyTransport {           // app never touches GMS directly
    fun startDiscovery(); fun stopDiscovery()
    fun startAdvertising(tempId, destination, looking); fun stopAdvertising()
    fun connect(peerEndpointId); fun disconnect(peerEndpointId)
    fun send(peerEndpointId, message); fun broadcast(message)
    fun observePeers(): Flow<List<Peer>>
    fun observeIncoming(): Flow<IncomingEnvelope>
}
interface DestinationMatcher {
    fun calculateMatch(source: Location?, a: Destination, b: Destination): MatchResult
}
sealed class Message { Advertise, Text, GroupCreated, GroupText, GroupJoin }
// every message: id + senderId + timestamp + ttl → dedup + flood control
```

`ChatRepository` composes `LocalDataSource (Room)` + `NearbyDataSource (Transport)`; `RemoteDataSource`
is a stub (`syncRemote()` returns success) so an online backend can be added later without touching UI.

## Offline data flow

```
Destination screen → save City·Area → transport.startAdvertising(tempId|city|area) + startDiscovery()
  → Discovery: endpoint found → Peer(tempId, City·Area) → matcher sorts same-way first
  → Connect → requestConnection → auto-accept → exchange Message.Advertise (full lat/lng/looking)
  → 1:1 Chat: Message.Text via sendPayload(PAYLOAD_BYTES JSON) → Room persist (PK=id dedups)
  → Group: createGroup → Message.GroupCreated broadcast → members join → GroupText flood
```

## Bitchat-style mesh (MVP scope)

`P2P_CLUSTER` strategy. `ChatRepositoryImpl` keeps an in-memory `seenIds` set + Room PK check;
`GroupText/GroupCreated/GroupJoin` with `ttl>0` are rebroadcast with `ttl-1`. Direct `Text` uses
`ttl=0` (no forward). Gives `A → B → C` preparedness without a full routing table. Ordering is
best-effort; duplicates are dropped; store-and-forward is limited to currently connected peers.

## Matching (MVP)

Haversine on seeded lat/lng: `<2km SAME_AREA`, `<6km NEARBY_AREA`, same city `SAME_CITY`, else
`DIFFERENT`. `goingSameWay = SAME_AREA || NEARBY_AREA`. Missing coordinates fall back to
normalized city/area string equality (covers custom entries).

Route matching (see `docs/route-matching.md`) layers on top without touching the above:
`FindRouteMatchesUseCase` cheap-filters candidates (same city, ≤12km, max 8) then
`OsrmRouteMatcher` checks B against the OSRM `origin → A` geometry (≤2km from route,
≤5km detour, configurable overshoot rule) via `RouteRepository` → `OsrmApi`
(base URL from `BuildConfig.OSRM_BASE_URL`). Failures report `UNAVAILABLE`, never block
discovery. Verified matches rank first with a "Going your way" badge.

## UI / design system ("Departure Board")

Purely presentational — it adds no behaviour and every screen still goes through the same
ViewModels. Everything lives in `presentation/designsystem/`.

- **Tokens:** `Color.kt` (light "departure paper" + dark "terminal night" schemes, `hairline`/
  `glow`/`onGlow` extras), `Type.kt` (serif display / sans UI / mono travel data), `Shape.kt`,
  `Theme.kt`. All fonts are **system** fonts — no font files, no runtime downloads.
- **Signature aurora:** one iris → violet → coral gradient (`c.aurora()`), reused as primary-button
  fill, card hairline, radar rings and app icon. Light/dark are separate hand-tuned palettes, not
  pure inversions, so contrast holds in both.
- **Motifs:** `JourneyRail` (dashed origin→destination), `RouteTicket` (boarding-pass summary of
  `City · Area`), `DepartureRow` (label/value status rows), `LiveDot`, `StatusPill`,
  `ScreenHeader` (eyebrow + title), `Wordmark`.
- **Chrome:** floating bottom bar (`Home / Chats / Groups / You`) with gradient-active tab and
  unread badge; screens use `TravelBackdrop`/`SkyWash` for depth instead of hard dividers.
- **Screens:** Welcome, Destination, Discovery (Home), ConnectionSheet, Chats, Groups, Profile and
  Chat are all built from these primitives.
- Accessibility: every interactive element keeps a label or `contentDescription`; the decorative
  Welcome radar is `clearAndSetSemantics` so it is not announced as a real result count.

## Permissions (per API, no extras)

- API 31+: `BLUETOOTH_ADVERTISE/CONNECT/SCAN`; API 32+: `+ NEARBY_WIFI_DEVICES`; API 31–32: `+ FINE_LOCATION`; ≤30: `FINE_LOCATION`; WiFi-state perms `maxSdk 31`. Rationale shown before request; denial blocks advertise/discover with explanation. Never requests contacts/camera/mic/SMS/phone.
- `INTERNET` (all APIs): used **only** for the optional OSRM route check. Discovery, chat, and groups work fully offline; without internet the route badges simply don't appear and matching falls back to the haversine tiers.

## Platform limitations (isolated in `nearby/`)

1. Needs Play Services + **2 physical devices**; emulator BLE discovery doesn't work (`FakeNearbyTransport` covers single-device demo).
2. BT + WiFi radios must be ON — API no longer auto-enables them (late-2026 change); app prompts.
3. Foreground-only MVP (Doze kills advertising); the Home status card shows
   `Nearby mode active` / `Offline nearby mode`.
4. `PAYLOAD_BYTES` ≤ ~32KB → JSON only, no FILE/STREAM yet.
5. Endpoint name leaks only `tempId|city|area`; full profile exchanged post-connect. Stop sharing / disconnect / leave / block always available.

## Privacy & safety

Temporary `Traveller ###` IDs, approximate `City · Area` only. No phone/email/address/live location
stored or transmitted. Blocklist + `stopAdvertising/stopDiscovery` enforced at transport + repo layers.

## Verify

```
./gradlew testDebugUnitTest assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk (~18MB, minSdk 26, target/compile 35)
# → 36 unit tests pass (4 haversine + 12 GeoUtils + 14 route matcher + 6 use-case)
# Real Nearby test: install on 2 BT+WiFi-on devices → Find my way → Say hello → Open chat → Group
```
