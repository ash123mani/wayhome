# WayHome — Future Scope (Post-MVP Roadmap)

What the MVP deliberately leaves out, and exactly where each item plugs into the
current codebase. Nothing here changes the MVP contracts — all extensions sit
behind the existing `NearbyTransport`, `DestinationMatcher`, and repository interfaces.

Phasing: **P1** = next milestone, **P2** = medium term, **P3** = long term / research.

## 1. Route-based destination matching (P1) — ✅ SHIPPED

**Was:** `domain/matcher/DestinationMatcher.kt` (`HaversineDestinationMatcher`) — straight-line
distance thresholds (`SAME_AREA <2km`, `NEARBY <6km`) + string fallback. Two areas could be
3 km apart yet on opposite sides of an airport expressway, with no shared cab route.
**Shipped (see `docs/route-matching.md`):** OSRM road routing layered *alongside* the haversine
base rank instead of replacing it —
`domain/routing/` (`RouteMatchResult/Status/Config`, `GeoUtils`, `RouteMatcher`),
`domain/usecase/FindRouteMatchesUseCase` (same-city → ≤12 km → max 8 candidates → OSRM),
`data/routing/` (`OsrmApi`, cached `OsrmRouteRepository`, `CityRouteOriginProvider`
seeded with Bengaluru Airport), wired in `di/RoutingModule.kt` with the base URL in
`BuildConfig.OSRM_BASE_URL` (self-host override via `-POSRM_BASE_URL=...`).
Match rule: ≤2 km from route **and** ≤5 km detour, plus a configurable overshoot/direction
rule; failures report `UNAVAILABLE` and never block discovery. 32 new unit tests.
**Still open:** corridor tags per area in `assets/destinations.json` (cheap offline intermediate),
origins for Delhi/Mumbai, on-device road graph for fully-offline routing.

## 2. Online backend — `RemoteDataSource` (P1)

**Today:** `ChatRepositoryImpl.syncRemote()` is a success stub; Room is the source of truth.
**Future:**
- Implement `data/remote/` (`RemoteDataSource` interface + impl, e.g. WebSocket/MQTT or Firebase):
  upload outbox messages when internet returns, download missed group messages, resolve conflicts
  (server timestamp wins; client `id` PKs already globally unique so dedup survives the merge).
- Sync policy: offline-first preserved — remote is a *cache fill*, never a read dependency.
  `ConnectivityObserver.hasInternetNow()` already gates sync attempts.
- Push fallback for "your group agreed on a cab" notifications when the app is backgrounded
  (requires FCM + a foreground-service story — see §5).
**Files touched:** new `data/remote/`, `ChatRepositoryImpl` (outbox drain), `MessageEntity.status`
(`STORED → SENT → SYNCED`) already has the column.

## 3. True multi-hop mesh (P2)

**Today:** bitchat-style TTL flood in `ChatRepositoryImpl.forward()` (`ttl-1` + rebroadcast,
`seenIds` + Room PK dedup). Works for `A → B → C` when all are simultaneously in radio range.
**Gaps:** no routing tables, no store-and-forward for sleeping peers, no delivery confirmation,
flood duplicates scale as O(connected²).
**Future:**
- Distance-vector or epidemic routing with per-destination next-hop table in a new
  `data/mesh/RoutingTable` (Room-backed so it survives process death).
- Mailbox: hold messages for `LOST` peers and deliver on rediscovery (`PeerDao` already tracks
  `lastSeen`; add `pendingDeliveries` table).
- ACKs: `Message.DeliveryAck(id)` wire type + per-message `status` transitions already modelled.
- Adaptive TTL (hop-count estimate from routing table instead of fixed 3).
**Files touched:** `domain/model/Message.kt` (new `Ack` subtype — wire-compatible via
`classDiscriminator`), `data/Repositories.kt` (`forward()` replaced by router), `WireProtocol.kt`
unchanged (polymorphic JSON absorbs new subtypes).

## 4. Richer messaging (P1–P2)

- **Chunking (P1):** `WireProtocol` currently assumes <32KB `PAYLOAD_BYTES`. Add frame
  header (`messageId`, `chunkIndex`, `chunkCount`) + reassembly buffer for long texts.
- **Media (P2):** Nearby `FILE` payloads for photos (e.g. "I'm at pillar 7" pickup-point photo);
  needs the `READ_MEDIA_IMAGES` permission path and thumbnail table in Room.
- **Read/delivery receipts (P2):** builds on §3 ACKs; UI already separates mine/theirs in
  `ChatScreen.kt`, so ticks slot in without redesign.
- **Message expiry (P2):** flight-scoped TTL at the *product* level (auto-delete group history
  after landing) — Room `timestamp` + WorkManager purge worker.

## 5. Background operation (P2, platform-constrained)

**Today:** foreground-only; Doze kills advertising (documented limitation).
**Future:** foreground service (`FOREGROUND_SERVICE_CONNECTED_DEVICE`) with persistent
"Sharing as Traveller 482" notification + user-visible stop action. Still requires radios on and
cannot defeat Doze indefinitely — position as "keep sharing during this trip", not always-on.
Also handle the late-2026 radio policy (no silent BT/WiFi enablement) with a pre-flight checklist
screen. **Files:** new `nearby/SharingService.kt`, `PermissionHelper` gains FGS + `POST_NOTIFICATIONS`
(API 33+) branches.

## 6. Identity & safety evolution (P2)

MVP pins temporary anonymous IDs (correct for strangers). Future, only with explicit user opt-in:
- Optional trip-scoped verification (boarding-pass scan → "verified co-passenger" badge, hashed locally).
- Report + local blocklist persistence (today `DiscoveryRepositoryImpl.blocked` is in-memory;
  persist to Room/DataStore).
- Group admin (creator can remove members) and auto-dissolve empty groups.
- Explicitly **not** planned: permanent profiles, ratings, social graph — they undermine the
  privacy model the MVP is built on.

## 7. Cab handoff (P3 — product, not transport)

WayHome never books cabs (MVP constraint, retained). Future convenience only:
- Fare-split calculator (enter total → per-head, local only).
- Deep links into the user's own ride-hailing app with prefilled destination area.
- "Share trip summary" (group name + agreed pickup point as text) to paste anywhere.

## 8. Destination data (P1)

- Grow `destinations.json` (more cities/areas/corridors) + remote config overlay once §2 exists.
- On-device geocoding for custom areas so free-text entries also get coordinates (fixes the
  matcher falling back to string equality for custom input).

## 9. Testing & quality (P1, ongoing)

- Unit: routing-table unit tests, WireProtocol round-trip + chunk reassembly tests, repository
  tests with `FakeNearbyTransport` (already injectable via `AppModule` binding swap).
  (Route-matching geometry + pipeline tests are done: `GeoUtilsTest`, `RouteMatcherTest`,
  `FindRouteMatchesUseCaseTest` — 32 tests.)
- Scenario harness: scripted `FakeNearbyTransport` timelines (peer appears → connects → drops →
  rejoins) driving ViewModel tests for the flood/dedup paths.
- Instrumented: two-device farm run (Firebase Test Lab can't do Nearby pairs — needs two local
  devices; document the `adb install` + checklist in `README`).
- CI: `testDebugUnitTest` + `lintDebug` on every push; APK artifact upload.

## 10. Platform watchlist

- `ACCESS_LOCAL_NETWORK` (`minSdk 37`) — add to `PermissionHelper` + manifest when targeting API 37+.
- Nearby Connections API behaviour changes (radio auto-enable removal already handled; watch for
  strategy/deprecation notices from Google).
- Compile SDK bumps (35 → 36 tested SDK present) and Kotlin/AGP upgrades via the version catalog.

## Non-goals (retained)

Cab booking, payments, permanent accounts, ratings, ads, cloud dependency for core flows,
exact-location sharing. Any future item that conflicts with these needs a product decision first,
not just an implementation plan.
