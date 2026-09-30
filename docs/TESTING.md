# WayHome — Testing Plan

## 1. Unit tests (run anywhere)

```bash
./gradlew testDebugUnitTest
# report: app/build/test-results/testDebugUnitTest/
```

| Test | File | Covers |
|---|---|---|
| Same-area match (<2 km → `SAME_AREA`, `goingSameWay`) | `HaversineDestinationMatcherTest` | Matcher thresholds |
| Far same-city (`SAME_CITY`, not same-way) | same | Sorting tier |
| Cross-city (`DIFFERENT`) | same | Negative case |
| Missing coords → string fallback | same | Custom city/area entries |
| Haversine / segment / projection edge cases (12) | `domain/routing/GeoUtilsTest` | Distance math, invalid-input sentinel |
| On-route / 1 km / 2 km boundary / overshoot / detour / OSRM-failure cases (14) | `domain/routing/RouteMatcherTest` | Route rule + `UNAVAILABLE` separation |
| Offline / city filter / cap / ranking / label cases (6) | `domain/usecase/FindRouteMatchesUseCaseTest` | Candidate pipeline |

Current status: **36 tests, 0 failures.** Add-before-code for: routing-table logic, wire
round-trips, flood dedup/TTL handling (drive `ChatRepositoryImpl` with `FakeNearbyTransport`).

## 2. Manual acceptance (requires 2 physical devices)

Full step-by-step script with expected results: `docs/HOW_TO_RUN.md` §4 (7 steps:
launch → destination → mutual discovery → connect/chat → group → stop-sharing).
**Pass criteria:** all 7 steps green with mobile data **off** on both devices.

Regression checklist (run before any release):

- [ ] Fresh install → temp ID minted, no login screens anywhere
- [ ] Custom city/area entry saves and advertises
- [ ] Same-area pair sorts above different-city peer; badges correct
      (`Same destination` / `Nearby destination` / `Same city` / `Nearby, different way`)
- [ ] Online (Bengaluru): route-verified peer ranks first with `Going your way`;
      offline or OSRM failure: basic badges only, no crash, discovery unaffected
- [ ] Deny permissions → clear rationale, no crash, sharing stays off
- [ ] BT/WiFi off → actionable prompt, not silent failure
- [ ] Stop sharing → disappears from peer's list within ~10 s
- [ ] Block → peer filtered; Leave group → membership removed
- [ ] Kill + restart app → profile/destination persist, chat history intact (Room)

## 3. What is NOT automated (and why)

- Real Nearby discovery/connection: needs two BLE-capable devices in radio range; no
  emulator or cloud farm reproduces it. Covered by the §2 script.
- `FakeNearbyTransport` covers UI flow on one device/emulator but asserts nothing about
  the real transport — keep it clearly labelled as demo-only.

## 4. CI suggestion

On every push: `testDebugUnitTest` + `lintDebug` + `assembleDebug`; archive the APK.
Two-device manual pass before tagging any release.
