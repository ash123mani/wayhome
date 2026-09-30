# WayHome — Offline Wire Protocol

Transport-independent message model (`domain/model/Message.kt`), encoded as small UTF-8 JSON
blobs by `nearby/WireProtocol.kt` and carried as Nearby Connections `PAYLOAD_BYTES`.
App layers only see `Message`; only `RealNearbyTransport` knows about GMS payloads.

## 1. Envelope

Every message carries:

| Field | Type | Purpose |
|---|---|---|
| `id` | UUID string | Globally unique → duplicate suppression (Room PK + `seenIds`) |
| `senderId` | `Traveller ###` | Display + thread ownership (never a real identity) |
| `timestamp` | epoch ms | Ordering within a thread |
| `ttl` | int | Remaining flood hops; `0` = do not forward |

Polymorphic JSON uses `classDiscriminator = "type"`, `ignoreUnknownKeys = true` — so new
message types decode safely on old builds (unknown types are ignored, not crashed on).

## 2. Message types

| Type | Key fields | TTL | Forwarded? | Persisted as |
|---|---|---|---|---|
| `Advertise` | `tempName, city, area, lat?, lng?, lookingForPartners` | `0` | No (consumed by transport peer list) | Not stored (updates `peers`) |
| `Text` | `conversationId` (`dm:A\|B` sorted temp IDs), `text` | `0` (sent single-hop) | No | `TEXT` in thread `conversationId` |
| `GroupCreated` | `groupId, groupName, creatorId, members[]` | `3` | **Yes** (`ttl-1` rebroadcast) | `GROUP_CREATED` + group/member rows |
| `GroupText` | `groupId, text` | `3` | **Yes** | `GROUP_TEXT` in thread `groupId` |
| `GroupJoin` | `groupId, memberId` | `3` | **Yes** | Membership row |

Endpoint discovery itself carries no payload: the Nearby endpoint *name* is
`tempId|city|area` (≤100 chars). Full profile (`lat/lng/looking`) is exchanged as an
`Advertise` message immediately after connection acceptance.

### What is *not* on the wire

The route-matching feature (`docs/route-matching.md`) is **local-only** and never touches this
protocol. No route geometry, distance, detour, duration, or `RouteMatchStatus` is sent to any
peer, and no new `Message` subtype was added for it. Each device runs its own OSRM check
against its own already-broadcast destination, so a `Going your way` badge is a **local
opinion**, not a claim agreed with the other traveller. OSRM requests go over ordinary HTTPS
and carry only coordinates — never a `tempId`, name, or phone number.

## 3. Flood / dedup rules (bitchat-style, MVP)

Implemented in `ChatRepositoryImpl.handleIncoming` + `forward()`:

1. On receive: if `id ∈ seenIds` **or** `MessageDao.exists(id)` → drop (duplicate flood copy).
2. Persist first-seen copy to Room (insert is `IGNORE` on conflict — storage is the second net).
3. If type is `GroupCreated`/`GroupText`/`GroupJoin` **and** `ttl > 0` → rebroadcast a copy with
   `ttl - 1` to all connected endpoints.
4. `Text` (direct) and `Advertise` are never forwarded.

Result: `A → B → C` group propagation works when B is connected to both; ordering is
best-effort (timestamp-sorted per thread); delivery is at-most-once per device, at-least-once
per mesh. Limits: fixed TTL 3, no routing tables, no store-and-forward for offline peers, no
ACKs — all scheduled in `FUTURE_SCOPE.md` §3.

## 4. Size limits

`PAYLOAD_BYTES` practical ceiling ≈ 32 KB. Chat/group JSON is far below it. Anything larger
(long text, future media) needs framing/chunking first (`FUTURE_SCOPE.md` §4) — do not send
`FILE`/`STREAM` payloads until the reassembly buffer exists.
