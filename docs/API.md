# Restful-Booker-Platform API map

Target: `https://automationintesting.online` (set via `BASE_URL`, see `.env.example`).

## How this map was built

The platform is open source
([mwinteringham/restful-booker-platform](https://github.com/mwinteringham/restful-booker-platform)).
This map comes from reading the source at commit `efe2deb` (2026-10-23): the Spring Boot
controllers, services and validation annotations of the `auth`, `room`, `booking` and
`message` services, plus the Next.js `assets` app that serves the public `/api/*` paths.

The sandbox this framework was written in could not reach `automationintesting.online`
(its network policy blocks the host), so **nothing below was checked against the live
deployment**. Verify against the live site before trusting the edge cases. The
`@smoke` suite is the fastest way to do that.

## Architecture: two layers behind `/api`

```
client ──► Next.js (assets) ──┬─► app/api/** route handler ──► Spring service   (static paths)
                              └─► rewrite /api/<svc>/:path* ──► Spring service   (everything else)
```

`next.config.js` returns its rewrites as a plain array, so they are *afterFiles*
rewrites. Next.js checks static routes before them and dynamic (`[id]`) routes after
them. In practice:

| Path pattern | Served by | Consequence |
|---|---|---|
| `/api/auth/login`, `/api/auth/validate`, `/api/auth/logout` | Next.js handler | Reshaped bodies (`{token}`, `{valid:true}`), 401 for bad credentials |
| `/api/room`, `/api/booking`, `/api/booking/summary`, `/api/message`, `/api/message/count` | Next.js handler | Missing cookie gives **401** before the backend is called; error bodies are re-wrapped |
| `/api/room/{id}`, `/api/booking/{id}`, `/api/message/{id}`, `/api/message/{id}/read` | Rewritten straight to Spring | Backend status codes (**403** for bad token, 202 for delete/update) and backend bodies |

The **auth token** is sent as a cookie, `Cookie: token=<value>`, on every layer.

Each service resets its in-memory H2 database on a timer (about every 10 minutes on the
public site). The auth reset deletes **all tokens**, so a cached token can stop working
mid-run.

## Auth

| Method | Path | Body | Success | Failures |
|---|---|---|---|---|
| POST | `/api/auth/login` | `{"username","password"}` | 200 `{"token":"<16 chars>"}` | 401 `{"error":"Invalid credentials"}` |
| POST | `/api/auth/validate` | `{"token"}` | 200 `{"valid":true}` | 401 `{"error":"No token provided"}` when the token is missing; 403 `{"error":"Invalid token"}` when it is unknown |
| POST | `/api/auth/logout` | `{"token"}` | 200 `{"success":true}` (the Next.js handler does **not** call the backend, so the token stays valid) | 400 when the token is missing |

## Rooms

Room model: `roomid`, `roomName` (required, non-empty), `type` (`Single|Double|Twin|Family|Suite`),
`accessible` (bool), `image`, `description`, `features` (string[]), `roomPrice` (1–999).

| Method | Path | Auth | Success | Notes / failures |
|---|---|---|---|---|
| GET | `/api/room` | no | 200 `{"rooms":[Room…]}` | `?checkin=YYYY-MM-DD&checkout=YYYY-MM-DD` returns only rooms without a booking inside that range |
| GET | `/api/room/{id}` | no | 200 `Room` | 404 when the room is unknown |
| POST | `/api/room` | yes | 200 `{"success":true}` (no id is returned; look the room up by name) | 401 `{"errors":["Authentication required"]}` without a cookie; 400 `{"errors":[…]}` validation |
| PUT | `/api/room/{id}` | yes | 202 `Room` | 400 validation, 403 bad token, 404 |
| DELETE | `/api/room/{id}` | yes | 202 | 403 bad token, 404 |

Validation messages: `Room name must be set`,
`Type can only contain the room options Single, Double, Twin, Family or Suite`,
`must be greater than or equal to 1`, `must be less than or equal to 999`.

## Bookings

Booking model: `bookingid`, `roomid` (>= 1), `firstname` (3–18, not blank), `lastname` (3–30, not blank),
`depositpaid` (bool), `bookingdates {checkin, checkout}` (ISO dates, checkin < checkout),
optional `email` (valid address) and `phone` (11–21 chars).
When both `email` and `phone` are present, the booking service also posts a message to the
message service.

| Method | Path | Auth | Success | Notes / failures |
|---|---|---|---|---|
| POST | `/api/booking` | no | 201 `Booking` (flattened by Next.js, includes `bookingid`) | 400 `{"errors":[…]}` validation; 409 when checkout is not after checkin or the dates overlap an existing booking for the room |
| GET | `/api/booking?roomid={id}` | yes | 200 `{"bookings":[Booking…]}` | 401 without a cookie, 400 when `roomid` is missing |
| GET | `/api/booking/summary?roomid={id}` | yes (Next.js) | 200 `{"bookings":[{"bookingDates":{…}}]}` | |
| GET | `/api/booking/{id}` | yes | 200 `Booking` | 403 bad token, 404 unknown id |
| PUT | `/api/booking/{id}` | yes | 200 `{"bookingid", "booking":{…}}` | 400 validation, 403, 404, 409 |
| DELETE | `/api/booking/{id}` | yes | 202 | 403, 404 |

Validation messages: `Firstname should not be blank`, `Lastname should not be blank`,
`size must be between 3 and 18`, `size must be between 3 and 30`,
`must be greater than or equal to 1`, `must be a well-formed email address`,
`size must be between 11 and 21`.

## Messages

Message model: `messageid`, `name` (not blank), `email` (valid, not blank),
`phone` (11–21), `subject` (5–100), `description` (20–2000).

| Method | Path | Auth | Success | Notes / failures |
|---|---|---|---|---|
| POST | `/api/message` | no | 200 `{"success":true}` (no id is returned; look it up in the list) | 400 with a bare JSON array of messages |
| GET | `/api/message` | no | 200 `{"messages":[{"id","name","subject","read"}]}` | |
| GET | `/api/message/count` | no | 200 `{"count":<unread>}` | |
| GET | `/api/message/{id}` | no (backend does not check) | 200 `Message` | 404 |
| PUT | `/api/message/{id}/read` | yes | 202 | 403 bad token |
| DELETE | `/api/message/{id}` | yes | 202 | 403, 404 |

Validation messages: `Name may not be blank`, `Email may not be blank`,
`must be a well-formed email address`, `Phone must be between 11 and 21 characters.`,
`Subject must be between 5 and 100 characters.`,
`Message must be between 20 and 2000 characters.`

## Known quirks worth knowing

- Several Next.js handlers call `response.json()` on backend error responses that have
  **empty bodies** (for example a 403 or 404). That throws, and the handler then answers
  **500**. This affects only the paths in the "Next.js handler" row above. The framework's
  negative tests target paths where the source shows a deterministic status.
- `POST /api/auth/logout` does not invalidate the token (see above). The suite does not
  test logout invalidation, because that test would fail by design.
- Room and message creation do not return the new id. The framework finds the new record
  by a unique, generated name or subject.
