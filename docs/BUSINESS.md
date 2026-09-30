# WayHome — Business & Product Doc

## 1. Concept (one paragraph)

WayHome — “Meet the people around you who are going your way” — helps passengers at an
airport (or on the same flight, with no internet) discover nearby travellers heading home in
a similar direction, chat, form a temporary ride group, and split a cab they book themselves.
The product's job ends at **agreement**: WayHome never books, pays, tracks, or takes a cut.

## 2. Problem & insight

- Airport-to-home cab fares are high; solo night arrivals feel unsafe and wasteful.
- Strangers on the same flight often live minutes apart but have no way to find each other —
  shouting destinations in baggage claim doesn't scale, and WhatsApp groups need a coordinator.
- In-flight / arrivals-hall connectivity is unreliable, so any solution must work **offline**
  over local radio (Bluetooth/WiFi direct), not through a server.

## 3. Target users (personas)

| Persona | Situation | Core need |
|---|---|---|
| **Asha, 29** — lands 11 pm, Whitefield | Solo arrival, long expensive ride | 2–3 co-passengers, same corridor, tonight |
| **Ravi, 34** — weekly flyer, Marathahalli | Flies the same sector every week | Fast repeat flow, minimal setup each trip |
| **Meera & Kabir** — couple with luggage, Hoodi | Extra seats needed, safety in numbers | Small trusted group, clear pickup point |

Common traits: smartphone, Play-Services Android, at the airport or aircraft, low patience for
signup forms. Explicitly **not** users: drivers, fleet operators, advertisers (no such roles exist).

## 4. Core user journey (MVP)

```
Welcome → pick City · Area → [Find people going my way]
  → Nearby list (route-verified first with `Going your way`, then
  `Same destination` / `Nearby destination` / `Same city` badges)
  → Connect → 1:1 chat (“near ITPL?” / “near Hoodi?”)
  → Create group → agree pickup point + headcount
  → Stop sharing → book own cab → trip over, data stays on device
```

Success for one session = a group of 2–4 people with overlapping destinations exchanging at
least one message each and reaching a cab decision. Everything else is secondary.

## 5. MVP scope

**In:** temp identity; destination select (seeded multi-city + custom); offline discovery;
haversine match badges; OSRM route verification with `Going your way` badges (online-only,
graceful offline fallback — see `docs/route-matching.md`); 1:1 chat; group create/join/leave;
block; stop-sharing; offline banner.
**Out (explicit):** cab booking, payments/fare-splitting, accounts, ratings, ads, cloud backend,
route optimisation (matching verifies shared roads; it never plans or re-plans trips),
exact-location sharing. `FUTURE_SCOPE.md` phases these; none may silently
re-enter the MVP.

## 6. Business model boundaries

The MVP has **no revenue surface** (no ads, no commission, no data sale — there is no data to
sell; everything is on-device). Viable future options, each requiring a product decision first:
airport partnerships (listed pickup zones), optional fare-split receipt, or a pro tier for
frequent flyers. Anything involving money, identity verification, or tracking must pass the
privacy review in `PRIVACY_SAFETY.md` before being specced.

## 7. Success metrics (per-trip, local-first)

- **Discovery rate:** % of sharing sessions that find ≥1 same-city peer within 5 min.
- **Chat rate:** % of discoveries leading to ≥1 exchanged message pair.
- **Group rate:** % of chats leading to a group with ≥2 members.
- **Decision rate:** % of groups where members report an arranged cab (in-app confirm tap, P1).
- **Safety:** blocks/reports per 1,000 sessions (target: near zero; spikes trigger review).
All metrics must be computable without a server in the MVP (on-device counters); server-side
analytics only arrive with the `RemoteDataSource` phase and explicit consent.

## 8. Risks & mitigations

| Risk | Mitigation (MVP) |
|---|---|
| Stranger danger / harassment | Temp IDs, no contact/location exposure, block, stop-sharing, leave-group; see `PRIVACY_SAFETY.md` |
| Empty airport (no peers) | Honest `🔎 Searching` state; seeded demo content never fakes real people |
| Radio/permission friction | Pre-request rationale, manual radio checklist, exact per-API permission set |
| Platform dependence (Play Services, radio policy) | Isolated in `nearby/`; tracked in `FUTURE_SCOPE.md` §10 |
| Scope creep into ride-hailing | Hard boundary: WayHome ends at agreement; deep links only (P3) |

## 9. Rollout sketch

1. MVP validation: 2-device tests + closed trials with frequent flyers on one corridor
   (e.g. BLR airport → Whitefield/Marathahalli).
2. P1: route matching (✅ shipped, Bengaluru Airport origin) + delivery reliability → pilot at one airport.
3. P2+: optional backend, background sharing, safety tooling → wider rollout.
