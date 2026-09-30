# WayHome — Privacy & Safety

Strangers meeting via an app is the core risk of this product. This doc records the
privacy model, what data exists and where, and the safety controls — for the MVP as built.

## 1. Privacy principles

1. **No identity:** no login, email, phone, or persistent account. `IdentityManager`
   mints `Traveller ###` per install/session in DataStore; reinstall = new person.
2. **Approximate destination only:** the only thing ever transmitted is `City · Area`
   (+ coarse seeded lat/lng). Never exact address, home location, or live GPS.
3. **Local-first storage:** Room database + DataStore live on the device. There is no
   WayHome server, no analytics SDK, no cloud backup (`allowBackup=false`). The single
   exception is the optional OSRM route check (online only): it sends the already-shared
   coarse origin/destination coordinates to a routing server and nothing else — no temp
   ID, no names, no live location (see §2).
4. **User-controlled visibility:** sharing is explicit (tap **Find my way**) and
   revocable at any time (**Stop sharing**).

## 2. Data inventory (MVP)

| Data | Stored where | Transmitted | Notes |
|---|---|---|---|
| Temp ID (`Traveller ###`) | DataStore | Yes — endpoint name + `Advertise.tempName` | Random 100–999, session-scoped |
| City · Area + coarse lat/lng | DataStore + Room | Yes — endpoint name (`tempId\|city\|area`, ≤100 chars) + `Advertise` post-connect | Seeded coordinates, not GPS |
| Peers seen | Room `peers` (+ `lastSeen`) | No | Cached discovery list |
| Messages | Room `messages` (PK = `id`) | Yes — to connected endpoints only | Text + group messages |
| Groups + membership | Room `groups`, `group_members` | Membership via `GroupCreated`/`GroupJoin` | Temporary, leavable |
| Blocklist | In-memory (MVP) | No | Persist in P2 — see `FUTURE_SCOPE.md` §6 |
| Coarse origin + destination coords (route check) | Nowhere (request-scoped) | Yes — OSRM server, only when online | Seeded/selected areas only; no ID attached; omitted entirely offline |
| Location/GPS, contacts, phone, email | — | — | **Never collected** |

## 3. Safety controls (shipped)

- **Stop sharing** (`ProfileScreen`): stops advertising + discovery; device disappears.
- **Disconnect:** per-peer teardown via `NearbyTransport.disconnect`.
- **Leave group** (`GroupScreen`): removes own membership; no re-add without rejoin.
- **Block user** (`DiscoveryScreen`): filters the temp ID from the peer list.
- **Permission minimalism** (`PermissionHelper` + manifest): only the per-API Nearby/BT set
  plus `INTERNET` (used solely for the OSRM route check); no contacts, camera, mic, SMS,
  or precise-location prompts beyond what the transport needs.
- **Foreground-only sharing:** nothing advertises while the app is closed (MVP has no
  background service), so "sharing" always matches a visible app state + the Home status
  card (`Nearby mode active` when online, `Offline nearby mode` when not).

## 4. Residual risks (honest)

- Temp IDs are pseudonymous, not anonymous, *within* a session: someone in your group knows
  you as `Traveller 482` for that trip. Mitigated by reinstall-fresh IDs and no linkage.
- Nearby endpoint names are visible to any WayHome scanner in radio range (~10 m), including
  non-contacts. Content is limited to temp ID + City · Area by construction.
- No message encryption in the MVP (Nearby link is encrypted at transport layer by Play
  Services, but no end-to-end layer). Sensitive personal details should never be typed into
  chat — the UI copy says so on the Destination screen.
- Harassment reporting is not yet built (P2: report + persisted blocklist).
- The default OSRM endpoint is the public demo server (`router.project-osrm.org`): fine
  for development, but its operator sees request IPs. Production must self-host (one
  Gradle flag: `-POSRM_BASE_URL=...`) before any pilot — see `docs/route-matching.md`.

## 5. Review gate for future work

Any post-MVP feature involving money, persistent identity, verification, server storage,
background sharing, or analytics must update this doc *before* implementation, per
`FUTURE_SCOPE.md` non-goals. The default answer to “can we collect X?” is no.
