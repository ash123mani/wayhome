# Route-based matching

WayHome can verify that a nearby passenger is actually **going the same way
home**, using road routing instead of straight-line distance.

## Pipeline

```text
Nearby Discovery
       ↓
Candidate Users (Peer + basic MatchLevel)
       ↓
FindRouteMatchesUseCase
  · same city?
  · cheap haversine filter (≤ 12 km, nearest 8 first)
  · network available + known city origin?
       ↓
RouteMatcher (OsrmRouteMatcher)
  · Airport → A (direct, cached)
  · B within 2 km of A's route?  (else stop: 1 call)
  · B beyond A past tolerance?    (else stop: configurable)
  · Airport → B → A (via)
  · detour = via − direct ≤ 5 km?
       ↓
Match Score → "Going your way" → Chat → Temporary Ride Group
```

## Rules

A partner matches only when **both** hold:

- `distanceFromRouteMeters <= 2_000`
- `detourMeters <= 5_000`

plus the direction rule: B's overshoot past A's destination along the route
must stay within `maxOvershootBeyondDestinationMeters` (default 1 km) unless
`allowBeyondDestination` is enabled. The result also exposes
`partnerPositionOnRouteMeters` (along-route distance from the origin to B's
nearest route point) so the rule can be refined later.

All thresholds live in `RouteMatchConfig` (`domain/routing/RouteModels.kt`).

## What the user sees

A route verdict only ever changes the **badge and the ranking**, never the discovery
list itself. Peers keep their basic geographic badge (`Same destination` /
`Nearby destination` / `Same city` / `Nearby, different way`) and additionally show:

| Verdict | Badge |
|---|---|
| `MATCH`, detour < 1.5 km | green `Going your way` |
| `MATCH`, detour ≥ 1.5 km | green `Going your way · ~N km detour` (N = rounded km) |
| `NOT_A_MATCH` / `UNAVAILABLE` | no route badge; basic geographic copy only |

Route-verified peers are sorted to the top of Home (then by smallest detour), so the
resulting list is `Going your way · N` on the Home summary chip.

The check is debounced ~800 ms, cancelled when the peer set changes, and re-runs
automatically when connectivity returns — there is no retry button. Raw route metrics
(distance-from-route, overshoot, OSRM duration) are intentionally **not** surfaced; they
are debug/telemetry-only.

A badge is a **local opinion**, not a mutual handshake: each device evaluates the peer
against its own destination, and no route data is ever exchanged over Nearby.

## Failure model

Routing failures are **not** mismatches:

| Status | Meaning | UI |
|---|---|---|
| `MATCH` | going your way | `Going your way` (or `· ~N km detour`) |
| `NOT_A_MATCH` | checked, not suitable | falls back to basic geographic copy |
| `UNAVAILABLE` | OSRM unreachable, no origin, bad coords | falls back silently; never blocks discovery |

Set `allowBasicFallback = true` only if product explicitly wants failures
reported as `NOT_A_MATCH`.

## Backend

- OpenStreetMap data via the OSRM HTTP Route API (`overview=full`,
  `geometries=geojson`); coordinates always sent as **longitude,latitude**.
- Base URL is `BuildConfig.OSRM_BASE_URL` (default
  `https://router.project-osrm.org/`, development only). Self-host with:
  `./gradlew assembleDebug -POSRM_BASE_URL=https://your-osrm-host/`
- `origin → destination` routes are cached in memory (64 entries);
  lateral misses skip the second OSRM call entirely.

## Origins

`CityRouteOriginProvider` seeds journey origins per city (currently Bengaluru
Airport). Cities without an origin return null → route matching stays
`UNAVAILABLE` there and basic matching applies. Extend the map (or load
`assets/destinations.json`) to roll out further cities.

## Privacy

Only approximate destination areas already exchanged over Nearby are sent to
OSRM. No names, phone numbers, or live locations. Offline discovery works
with zero network; route badges simply don't appear. The public demo server sees
request IPs — self-host before any pilot.

## Verification status

- 32 route unit tests (`GeoUtilsTest` 12, `RouteMatcherTest` 14,
  `FindRouteMatchesUseCaseTest` 6) plus 4 pre-existing matcher tests = **36 total,
  0 failures** via `./gradlew testDebugUnitTest assembleDebug`.
- Live contract check against the default public server: `code: Ok`, Bengaluru Airport
  → Whitefield = 19,263.5 m, `LineString` with 486 points, first coordinate
  `[77.6163, 12.9941]` (confirming `longitude,latitude` ordering).
- **Not yet verified end-to-end:** a real two-device session where a peer earns
  `Going your way`, since the emulator cannot produce nearby peers. Until that runs,
  treat the UI integration as untested in the field — the domain logic is unit-covered.

